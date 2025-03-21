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
package io.gravitee.apim.core.event.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CleanupEventsUseCase {

    private final EventCrudService eventCrudService;

    public void execute(Input input) {
        eventCrudService.cleanupEvents(input.environmentId(), input.nbEventsToKeep(), input.timeToLive());
    }

    public record Input(String environmentId, int nbEventsToKeep, Duration timeToLive) {}
}
