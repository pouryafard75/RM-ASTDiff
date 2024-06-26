/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.tooling.internal.provider.runner;

import org.gradle.initialization.BuildEventConsumer;
import org.gradle.internal.progress.BuildOperationInternal;
import org.gradle.internal.progress.InternalBuildListener;
import org.gradle.tooling.internal.provider.events.*;

import java.util.Collections;

/**
 * Build listener that forwards all receiving events to the client via the provided {@code BuildEventConsumer} instance.
 *
 * @since 2.5
 */
class ClientForwardingBuildListener implements InternalBuildListener {

    private final BuildEventConsumer eventConsumer;

    ClientForwardingBuildListener(BuildEventConsumer eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    @Override
    public void started(BuildOperationInternal buildOperation) {
        eventConsumer.dispatch(new DefaultOperationStartedProgressEvent(buildOperation.getStartTime(), toBuildOperationDescriptor(buildOperation)));
    }

    @Override
    public void finished(BuildOperationInternal buildOperation) {
        eventConsumer.dispatch(new DefaultOperationFinishedProgressEvent(buildOperation.getEndTime(), toBuildOperationDescriptor(buildOperation), adaptResult(buildOperation)));
    }

    private DefaultOperationDescriptor toBuildOperationDescriptor(BuildOperationInternal buildOperation) {
        Object id = buildOperation.getId();
        String name = buildOperation.getOperationType().getName();
        String displayName = buildOperation.getOperationType().getDisplayName();
        Object parentId = buildOperation.getParentId();
        return new DefaultOperationDescriptor(id, name, displayName, parentId);
    }

    private AbstractOperationResult adaptResult(BuildOperationInternal source) {
        Throwable failure = source.getFailure();
        long startTime = source.getStartTime();
        long endTime = source.getEndTime();
        if (failure != null) {
            return new DefaultFailureResult(startTime, endTime, Collections.singletonList(DefaultFailure.fromThrowable(failure)));
        }
        return new DefaultSuccessResult(startTime, endTime);
    }
}
