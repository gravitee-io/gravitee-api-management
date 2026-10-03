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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncMetrics;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint.AccessPointDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class AccessPointMapper {

    private final ObjectMapper objectMapper;
    private final DistributedSyncMetrics metrics;

    public AccessPointMapper(final ObjectMapper objectMapper) {
        this(objectMapper, new DistributedSyncMetrics());
    }

    public Maybe<AccessPointDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                final ReactableAccessPoint accessPoint = objectMapper.readValue(event.getPayload(), ReactableAccessPoint.class);
                return AccessPointDeployable.builder()
                    .reactableAccessPoint(accessPoint)
                    .syncAction(SyncActionMapper.to(event.getSyncAction()))
                    .build();
            } catch (Exception e) {
                return MappingFailures.decodeFailure(metrics, event, e);
            }
        });
    }

    public Maybe<DistributedEvent> to(final AccessPointDeployable deployable) {
        return Maybe.fromCallable(() -> {
            try {
                return DistributedEvent.builder()
                    .payload(objectMapper.writeValueAsString(deployable.reactableAccessPoint()))
                    .id(deployable.id())
                    .type(DistributedEventType.ACCESS_POINT)
                    .syncAction(SyncActionMapper.to(deployable.syncAction()))
                    .updatedAt(new Date())
                    .build();
            } catch (Exception e) {
                return MappingFailures.deadLetter(metrics, DistributedEventType.ACCESS_POINT, deployable.id(), e);
            }
        });
    }
}
