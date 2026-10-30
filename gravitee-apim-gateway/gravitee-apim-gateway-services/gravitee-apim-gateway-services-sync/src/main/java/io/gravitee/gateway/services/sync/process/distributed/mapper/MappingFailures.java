/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.process.distributed.mapper;

import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncMetrics;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import lombok.CustomLog;

/**
 * Reports a mapping failure without breaking the stream of events it belongs to: the failing item is skipped (the
 * caller returns {@code null} from a {@code Maybe.fromCallable}), counted, and logged at error level the first
 * time only, since the overlapping sync windows would otherwise report the same item on every cycle.
 */
@CustomLog
final class MappingFailures {

    private MappingFailures() {}

    /**
     * The primary could not build the distributed event of an entity it deployed: the other gateways will not
     * receive it. Deterministic, so the window is not replayed for it.
     */
    static <T> T deadLetter(final DistributedSyncMetrics metrics, final DistributedEventType type, final String id, final Throwable cause) {
        if (metrics.recordDeadLetter(type, id)) {
            log.error("Unable to build the distributed event of {} [{}], other gateways will not receive it", type, id, cause);
        } else {
            log.debug("Unable to build the distributed event of {} [{}]", type, id, cause);
        }
        return null;
    }

    /**
     * A secondary could not read a distributed event back into an entity.
     */
    static <T> T decodeFailure(final DistributedSyncMetrics metrics, final DistributedEvent event, final Throwable cause) {
        if (metrics.recordDecodeFailure(event.getType(), event.getId(), event.getUpdatedAt())) {
            log.error("Unable to read the distributed event of {} [{}], it is skipped", event.getType(), event.getId(), cause);
        } else {
            log.debug("Unable to read the distributed event of {} [{}]", event.getType(), event.getId(), cause);
        }
        return null;
    }
}
