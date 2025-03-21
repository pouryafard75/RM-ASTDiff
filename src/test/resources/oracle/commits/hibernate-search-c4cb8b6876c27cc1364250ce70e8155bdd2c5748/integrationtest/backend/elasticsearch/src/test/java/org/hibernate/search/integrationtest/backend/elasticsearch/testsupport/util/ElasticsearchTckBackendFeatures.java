/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect.isActualVersion;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.types.format.impl.ElasticsearchDefaultFieldFormatProvider;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FloatFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.InstantFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.MonthDayFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;

public class ElasticsearchTckBackendFeatures extends TckBackendFeatures {

	ElasticsearchTckBackendFeatures() {
	}

	@Override
	public boolean normalizesStringMissingValues() {
		// TODO HSEARCH-3387 Elasticsearch does not apply the normalizer defined on the field
		//   to the String provided as replacement for missing values on sorting
		return false;
	}

	@Override
	public boolean normalizesStringArgumentToWildcardPredicateForAnalyzedStringField() {
		// In ES 7.7 through 7.11, wildcard predicates on analyzed fields get their pattern normalized,
		// but that was deemed a bug and fixed in 7.12.2+: https://github.com/elastic/elasticsearch/pull/53127
		// Apparently the "fix" was also introduced in OpenSearch 2.5.0
		return isActualVersion(
				esVersion -> esVersion.isBetween( "7.7", "7.12.1" ),
				osVersion -> osVersion.isLessThan( "2.5.0" )
		);
	}

	@Override
	public boolean normalizesStringArgumentToRangePredicateForAnalyzedStringField() {
		// TODO HSEARCH-3959 Elasticsearch does not normalizes arguments passed to the range predicate
		//   for text fields.
		return false;
	}

	@Override
	public boolean nonCanonicalRangeInAggregations() {
		// Elasticsearch only supports [a, b), (-Infinity, b), [a, +Infinity), but not [a, b] for example.
		return false;
	}

	@Override
	public boolean fieldsProjectableByDefault() {
		return true;
	}

	@Override
	public boolean supportsTotalHitsThresholdForScroll() {
		// If we try to customize track_total_hits for a scroll, we get an error:
		// "disabling [track_total_hits] is not allowed in a scroll context"
		return false;
	}

	@Override
	public boolean supportsTruncateAfterForScroll() {
		// See https://hibernate.atlassian.net/browse/HSEARCH-4029
		return false;
	}

	@Override
	public boolean supportsExistsForFieldWithoutDocValues(Class<?> fieldType) {
		if ( GeoPoint.class.equals( fieldType ) ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean geoDistanceSortingSupportsConfigurableMissingValues() {
		// See https://www.elastic.co/guide/en/elasticsearch/reference/7.10/sort-search-results.html
		// In particular:
		// geo distance sorting does not support configurable missing values:
		// the distance will always be considered equal to Infinity when a document does not have values for the field
		// that is used for distance computation.
		return false;
	}

	@Override
	public boolean regexpExpressionIsNormalized() {
		// Surprisingly it is!
		// See *.tck.search.predicate.RegexpPredicateSpecificsIT#normalizedField
		return true;
	}

	@Override
	public boolean termsArgumentsAreNormalized() {
		// Surprisingly it is!
		// See *.tck.search.predicate.TermsPredicateSpecificsIT#normalizedField_termIsNotNormalized
		return true;
	}

	@Override
	public boolean supportsDistanceSortWhenNestedFieldMissingInSomeTargetIndexes() {
		// Even with ignore_unmapped: true,
		// the distance sort will fail if the nested field doesn't exist in one index.
		// Elasticsearch complains it cannot find the nested field
		// ("[nested] failed to find nested object under path [nested]"),
		// but we don't have any way to tell it to ignore this.
		// See https://hibernate.atlassian.net/browse/HSEARCH-4179
		return false;
	}

	@Override
	public boolean supportsFieldSortWhenFieldMissingInSomeTargetIndexes(Class<?> fieldType) {
		// We cannot use unmapped_type for scaled floats:
		// Elasticsearch complains it needs a scaling factor, but we don't have any way to provide it.
		// See https://hibernate.atlassian.net/browse/HSEARCH-4176
		return !BigInteger.class.equals( fieldType ) && !BigDecimal.class.equals( fieldType );
	}

	@Override
	public boolean reliesOnNestedDocumentsForMultiValuedObjectProjection() {
		return false;
	}

	@Override
	public boolean supportsYearType() {
		// https://github.com/elastic/elasticsearch/issues/90187
		// Seems like this was fixed in 8.5.1 and they won't backport to 8.4:
		// https://github.com/elastic/elasticsearch/pull/90458
		return isActualVersion(
				esVersion -> !esVersion.isBetween( "8.4.2", "8.5.0" ),
				osVersion -> true
		);
	}

	@Override
	public boolean supportsExtremeScaledNumericValues() {
		// https://github.com/elastic/elasticsearch/issues/91246
		// Hopefully this will get fixed in a future version.
		return isActualVersion(
				esVersion -> !esVersion.isBetween( "7.17.7", "7.17" ) && esVersion.isLessThan( "8.5.0" ),
				// https://github.com/opensearch-project/OpenSearch/issues/12433
				osVersion -> osVersion.isLessThan( "2.12.0" )
		);
	}

	@Override
	public boolean supportsExtremeLongValues() {
		// https://github.com/elastic/elasticsearch/issues/84601
		// There doesn't seem to be any hope for this to get fixed in older versions (7.17/8.0),
		// but it's been fixed in 8.1.
		return isActualVersion(
				esVersion -> !esVersion.isBetween( "7.17.7", "7.17" ) && !esVersion.isBetween( "8.0.0", "8.0" ),
				osVersion -> true
		);
	}

	@Override
	public boolean supportsHighlighterEncoderAtFieldLevel() {
		// https://github.com/elastic/elasticsearch/issues/94028
		return false;
	}

	@Override
	public boolean supportsHighlighterFastVectorNoMatchSizeOnMultivaluedFields() {
		// https://github.com/elastic/elasticsearch/issues/94550
		return false;
	}

	@Override
	public boolean supportsHighlighterPlainOrderByScoreMultivaluedField() {
		// A plain highlighter implementation in ES had a bug
		// https://github.com/elastic/elasticsearch/issues/87210
		// that is now fixed with https://github.com/elastic/elasticsearch/pull/87414
		return isActualVersion(
				esVersion -> !esVersion.isBetween( "7.15", "8.3" ),
				osVersion -> true
		);
	}

	@Override
	public boolean supportsHighlighterUnifiedPhraseMatching() {
		return isActualVersion(
				esVersion -> !esVersion.isAtMost( "8.9" ),
				osVersion -> false
		);
	}

	public static boolean supportsIndexClosingAndOpening() {
		return isActualVersion(
				// See https://docs.aws.amazon.com/opensearch-service/latest/developerguide/supported-operations.html#version_7_10
				es -> true,
				// See https://docs.aws.amazon.com/opensearch-service/latest/developerguide/supported-operations.html#version_opensearch_1.0
				os -> true,
				aoss -> false
		);
	}

	public static boolean supportsVersionCheck() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	public static boolean supportsIndexStatusCheck() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	@Override
	public boolean supportsExplicitPurge() {
		return ElasticsearchTestDialect.get().supportsExplicitPurge();
	}

	@Override
	public boolean supportsExplicitMergeSegments() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	@Override
	public boolean supportsExplicitFlush() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	@Override
	public boolean supportsExplicitRefresh() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	@Override
	public boolean supportsVectorSearch() {
		return isActualVersion(
				es -> !es.isLessThan( "8.12.0" ),
				os -> !os.isLessThan( "2.9.0" ),
				aoss -> true
		);
	}

	@Override
	public boolean supportsVectorSearchRequiredMinimumSimilarity() {
		return isActualVersion(
				es -> true,
				os -> false,
				aoss -> false
		);
	}

	@Override
	public boolean supportsSimilarity(VectorSimilarity vectorSimilarity) {
		switch ( vectorSimilarity ) {
			case DOT_PRODUCT:
			case MAX_INNER_PRODUCT:
				return isActualVersion(
						es -> true,
						os -> false,
						aoss -> false
				);
			default:
				return true;
		}
	}

	@Override
	public boolean hasBuiltInAnalyzerDescriptorsAvailable() {
		return false;
	}

	@Override
	public boolean canPerformTermsQuery(FieldTypeDescriptor<?, ?> fieldType) {
		return isActualVersion(
				es -> true,
				// https://github.com/opensearch-project/OpenSearch/issues/12432
				os -> !os.isMatching( "2.12.0" ) || !FloatFieldTypeDescriptor.INSTANCE.equals( fieldType ),
				aoss -> true
		);
	}

	@Override
	public boolean knnWorksInsideNestedPredicateWithImplicitFilters() {
		return false;
	}

	@Override
	public <F> String formatForQueryStringPredicate(FieldTypeDescriptor<F, ?> descriptor, F value) {
		ElasticsearchDefaultFieldFormatProvider formatProvider = ElasticsearchTestDialect.get().getDefaultFieldFormatProvider();
		if ( TemporalAccessor.class.isAssignableFrom( descriptor.getJavaType() ) ) {
			@SuppressWarnings("unchecked")
			var formatter = formatProvider
					.getDefaultDateTimeFormatter( ( (Class<? extends TemporalAccessor>) descriptor.getJavaType() ) )
					.withLocale( Locale.ROOT );
			if ( InstantFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
				return formatter
						.withZone( ZoneOffset.UTC )
						.format( (Instant) value );
			}
			if ( MonthDayFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
				return formatter
						.format( LocalDate.of( 0, ( (MonthDay) value ).getMonth(), ( (MonthDay) value ).getDayOfMonth() ) );
			}
			return formatter.format( (TemporalAccessor) value );
		}

		return super.formatForQueryStringPredicate( descriptor, value );
	}

	@Override
	public boolean queryStringFailOnPatternQueries() {
		return isActualVersion(
				es -> true,
				os -> false,
				aoss -> false
		);
	}
}
