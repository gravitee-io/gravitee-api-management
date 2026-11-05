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
import io.gravitee.gateway.services.sync.process.repository.synchronizer.genericevent.GenericEventDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.genericevent.GenericEventPayload;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Maps generic events to and from distributed events, so secondary nodes receive the generic events the primary node deployed.
 *
 * <p>An undeploy carries a payload too: the event id is a hash, and the node needs the environment, kind and id to remove the event.
 */
@RequiredArgsConstructor
@CustomLog
public class GenericEventMapper {

    private final ObjectMapper objectMapper;

    public Maybe<GenericEventDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                var syncAction = SyncActionMapper.to(event.getSyncAction());
                var genericEvent = GenericEventPayload.read(objectMapper, event.getPayload());
                var invalid = GenericEventPayload.invalid(genericEvent, syncAction);
                if (invalid != null) {
                    log.warn("Skipping distributed generic event event [{}]: {}", event.getId(), invalid);
                    return null;
                }
                return new GenericEventDeployable(event.getId(), syncAction, genericEvent);
            } catch (Exception e) {
                log.warn("Error while reading generic event from distributed event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    public Maybe<DistributedEvent> to(final GenericEventDeployable deployable) {
        return Maybe.fromCallable(() -> {
            try {
                return DistributedEvent.builder()
                    .id(deployable.id())
                    .type(DistributedEventType.GENERIC_EVENT)
                    .syncAction(SyncActionMapper.to(deployable.syncAction()))
                    .payload(GenericEventPayload.write(objectMapper, deployable.genericEvent()))
                    .updatedAt(new Date())
                    .build();
            } catch (Exception e) {
                log.warn("Error while building distributed event from generic event [{}]", deployable.id(), e);
                return null;
            }
        });
    }
}
