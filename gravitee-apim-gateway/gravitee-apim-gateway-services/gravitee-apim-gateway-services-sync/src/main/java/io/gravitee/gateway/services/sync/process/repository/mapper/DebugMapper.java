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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.impl.ReactableEvent;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DebugMapper {

    private final EnvironmentService environmentService;

    public Maybe<Reactable> to(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                ReactableEvent<Event> reactableEvent = new ReactableEvent<>(event.getId(), event);
                reactableEvent.setDeployedAt(event.getCreatedAt());
                String environmentId = event.getEnvironments().stream().findFirst().orElse(null);
                environmentService.fill(environmentId, reactableEvent);

                return reactableEvent;
            } catch (Exception e) {
                // Log the error and ignore this event.
                log.error("Unable to extract debug api definition from event [{}].", event.getId(), e);
                return null;
            }
        });
    }
}
