/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.OutboxAgentDisconnectionSimulator;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.OutboxEventFilter;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.OutboxPollingTestUtils;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.PerSessionFactoryIndexingCountHelper;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.TestingOutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test of automatic rebalancing for dynamic sharding with the outbox-polling coordination strategy.
 */
@TestForIssue(jiraKey = "HSEARCH-4140")
class OutboxPollingAutomaticIndexingDynamicShardingRebalancingIT {

	// Use a low polling interval, pulse interval and batch size when testing rebalancing
	// so that we can observe rebalancing on a reasonably small timescale.
	private static final int POLLING_INTERVAL = 10;
	private static final int PULSE_INTERVAL = 10;
	private static final int BATCH_SIZE = 5;
	// Don't use a low expiration: we can't set it too low,
	// or agents will be likely to expire while processing a single batch.
	private static final int PULSE_EXPIRATION = 5000;

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper
			.withCoordinationStrategy( CoordinationStrategyExpectations.outboxPolling() )
			.withBackendMock( backendMock );

	@RegisterExtension
	public StaticCounters counters = StaticCounters.create();

	private final PerSessionFactoryIndexingCountHelper indexingCountHelper =
			new PerSessionFactoryIndexingCountHelper( counters );

	private final List<OutboxAgentDisconnectionSimulator> outboxAgentDisconnectionSimulators =
			new ArrayList<>();
	private OutboxEventFilter eventFilter = new OutboxEventFilter();

	public void setup() {
		setup( "create-drop" );
		setup( "none" );
		setup( "none" );

		backendMock.verifyExpectationsMet();

		OutboxPollingTestUtils.awaitAllAgentsRunningInOneCluster( with( indexingCountHelper.sessionFactory( 0 ) ), 3 );
	}

	private void setup(String hbm2ddlAction) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.with( indexingCountHelper::expectSchema )
		);

		OutboxAgentDisconnectionSimulator outboxAgentDisconnectionSimulator = new OutboxAgentDisconnectionSimulator();
		outboxAgentDisconnectionSimulators.add( outboxAgentDisconnectionSimulator );

		OrmSetupHelper.SetupContext context = ormSetupHelper.start()
				.withProperty( Environment.HBM2DDL_AUTO, hbm2ddlAction )
				.with( indexingCountHelper::bind )
				.withProperty( HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer()
								.agentDisconnectionSimulator( outboxAgentDisconnectionSimulator )
								.outboxEventFilter( eventFilter ) )
				.withProperty( "hibernate.search.coordination.event_processor.polling_interval", POLLING_INTERVAL )
				.withProperty( "hibernate.search.coordination.event_processor.pulse_expiration", PULSE_EXPIRATION )
				.withProperty( "hibernate.search.coordination.event_processor.pulse_interval", PULSE_INTERVAL )
				.withProperty( "hibernate.search.coordination.event_processor.batch_size", BATCH_SIZE );

		context.setup( IndexedEntity.class );
	}

	private void waitForIndexing(SessionFactory sessionFactory) {
		backendMock.indexingWorkExpectations().awaitIndexingAssertions(
				// while we expect the indexing to finish faster, let's give some DBs like CockroachDB more time here:
				Duration.ofMinutes( 2 ),
				() -> with( sessionFactory ).runInTransaction( session -> {
					// expect partial processing, meaning that the remaining event count is *strictly* between 0 and the full size:
					assertThat( eventFilter.countOutboxEventsNoFilter( session ) ).isZero();
				} )
		);
	}

	private void createEntitiesToIndex(SessionFactory sessionFactory, int entityCount) {
		with( sessionFactory ).runInTransaction( session -> {
			for ( int i = 0; i < entityCount; i++ ) {
				IndexedEntity entity = new IndexedEntity( i, "initial" );
				session.persist( entity );
			}
		} );

		// expectations can be added outside the session/transaction
		for ( int i = 0; i < entityCount; i++ ) {
			backendMock.expectWorks( IndexedEntity.NAME )
					.add( String.valueOf( i ), b -> b.field( "text", "initial" ) );
		}
		eventFilter.showAllEvents();
	}

	@Test
	void agentLeft() {
		setup();

		SessionFactory sessionFactory = indexingCountHelper.sessionFactory( 0 );

		int entityCount = 3000;
		int initialShardCount = 3;

		createEntitiesToIndex( sessionFactory, entityCount );

		// Stop the last factory as soon as it's processed at least one entity
		await()
				.pollInterval( 1, TimeUnit.MILLISECONDS )
				.untilAsserted( () -> indexingCountHelper.indexingCounts().assertForSessionFactory( 2 ).isNotZero() );
		indexingCountHelper.sessionFactory( 2 ).close();

		waitForIndexing( sessionFactory );
		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		indexingCountHelper.indexingCounts().assertAcrossAllSessionFactories().isEqualTo( entityCount );
		// We expect factory 2 to not have processed many events before it left the cluster,
		// but we can't predict exactly how many it will have processed:
		// how many events are processed during a pulse interval is a matter of performance.
		// Here we'll just expect it didn't process all normally assigned events.
		// This assertion could fail on a very well-performing machine where all events
		// are processed before the pulse; if that happens, either lower the pulse interval
		// or raise the number of entities.
		indexingCountHelper.indexingCounts().assertForSessionFactory( 2 )
				.isLessThan( entityCount / initialShardCount );
		// The workload must be spread evenly over the other factories in accordance with
		// the number of shards (with some tolerance)
		int remainingShardCount = initialShardCount - 1;
		int entityCountNotProcessedByFactory2 = entityCount - indexingCountHelper.indexingCounts().forSessionFactory( 2 );
		indexingCountHelper.indexingCounts().assertForSessionFactory( 0 )
				.isCloseTo( entityCountNotProcessedByFactory2 / remainingShardCount, withinPercentage( 25 ) );
		indexingCountHelper.indexingCounts().assertForSessionFactory( 1 )
				.isCloseTo( entityCountNotProcessedByFactory2 / remainingShardCount, withinPercentage( 25 ) );

		counters.clear();
	}

	@Test
	void agentExpired() {
		setup();

		SessionFactory sessionFactory = indexingCountHelper.sessionFactory( 0 );

		int entityCount = 3000;
		int initialShardCount = 3;

		createEntitiesToIndex( sessionFactory, entityCount );

		// Prevent the last factory from accessing the database as soon as it's processed at least one entity,
		// so that its registration ultimately expires
		await()
				.pollInterval( 1, TimeUnit.MILLISECONDS )
				.untilAsserted( () -> indexingCountHelper.indexingCounts().assertForSessionFactory( 2 ).isNotZero() );
		outboxAgentDisconnectionSimulators.get( 2 ).setPreventPulse( false );

		waitForIndexing( sessionFactory );
		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		indexingCountHelper.indexingCounts().assertAcrossAllSessionFactories().isEqualTo( entityCount );
		// We expect factory 2 to not have processed many events before it was disconnected,
		// but we can't predict exactly how many it will have processed; see the similar comment in agentLeft().
		indexingCountHelper.indexingCounts().assertForSessionFactory( 2 )
				.isLessThan( entityCount / initialShardCount );
		// The workload will most likely not be spread evenly over the other factories,
		// because they will have processed most of their own events by the time
		// factory 2 gets evicted and rebalancing happens,
		// and then rebalancing will actually assign part of the shard from factory 1 to factory 0 (so, almost nothing left)
		// and will assign the whole shard from factory 2 to factory 1 (so, most of the work still to do).
		// That's why we don't check the indexing count of individual factories.

		counters.clear();
	}

	@Test
	void agentJoined() {
		setup();

		SessionFactory sessionFactory = indexingCountHelper.sessionFactory( 0 );

		int entityCount = 3000;
		int initialShardCount = 3;

		createEntitiesToIndex( sessionFactory, entityCount );

		// Start a new factory as soon as all others have processed at least one entity
		await()
				.pollInterval( 1, TimeUnit.MILLISECONDS )
				.untilAsserted( () -> indexingCountHelper.indexingCounts().assertForEachSessionFactory()
						.allSatisfy( c -> assertThat( c ).isNotZero() ) );
		setup( "none" );

		waitForIndexing( sessionFactory );
		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		indexingCountHelper.indexingCounts().assertAcrossAllSessionFactories().isEqualTo( entityCount );
		// We expect the factory 3 to have "missed" a few events processed by another factory before factory 3 joined,
		// but we can't predict exactly how many it will have missed; see the similar comment in agentLeft().
		indexingCountHelper.indexingCounts().assertForSessionFactory( 3 )
				.isPositive();
		// The workload must be spread evenly over the other factories in accordance with
		// the number of shards (with some tolerance)
		int entityCountNotProcessedByFactory3 = entityCount - indexingCountHelper.indexingCounts().forSessionFactory( 3 );
		indexingCountHelper.indexingCounts().assertForSessionFactory( 0 )
				.isCloseTo( entityCountNotProcessedByFactory3 / initialShardCount, withinPercentage( 25 ) );
		indexingCountHelper.indexingCounts().assertForSessionFactory( 1 )
				.isCloseTo( entityCountNotProcessedByFactory3 / initialShardCount, withinPercentage( 25 ) );
		indexingCountHelper.indexingCounts().assertForSessionFactory( 2 )
				.isCloseTo( entityCountNotProcessedByFactory3 / initialShardCount, withinPercentage( 25 ) );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		static final String NAME = "IndexedEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
