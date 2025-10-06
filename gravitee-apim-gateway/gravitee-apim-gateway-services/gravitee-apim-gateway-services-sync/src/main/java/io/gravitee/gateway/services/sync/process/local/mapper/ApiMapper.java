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
package io.gravitee.gateway.services.sync.process.local.mapper;

import static io.gravitee.repository.management.model.Event.EventProperties.API_ID;
import static io.gravitee.repository.management.model.Event.EventProperties.DEPLOYMENT_NUMBER;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.LifecycleState;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ApiMapper {

    private final ObjectMapper objectMapper;
    private final EnvironmentService environmentService;

    public ReactableApi<?> to(Event apiEvent) {
        try {
            // Read API definition from event
            var api = objectMapper.readValue(apiEvent.getPayload(), io.gravitee.repository.management.model.Api.class);

            ReactableApi<?> reactableApi;

            // Check the version of the API definition to read the right model entity
            if (DefinitionVersion.V4 != api.getDefinitionVersion()) {
                var eventApiDefinition = objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class);

                // Update definition with required information for deployment phase
                reactableApi = new io.gravitee.gateway.handlers.api.definition.Api(eventApiDefinition);
            } else {
                if (api.getType() == ApiType.NATIVE) {
                    var eventApiDefinition = objectMapper.readValue(api.getDefinition(), NativeApi.class);

                    // Update definition with required information for deployment phase
                    reactableApi = new io.gravitee.gateway.reactive.handlers.api.v4.NativeApi(eventApiDefinition);
                } else if (
                    api.getType() == ApiType.PROXY ||
                    api.getType() == ApiType.LLM_PROXY ||
                    api.getType() == ApiType.MESSAGE
                ) {
                    var eventApiDefinition = objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.v4.Api.class);

                    // Update definition with required information for deployment phase
                    reactableApi = new io.gravitee.gateway.reactive.handlers.api.v4.Api(eventApiDefinition);
                } else {
                    throw new IllegalArgumentException("Unsupported ApiType [" + api.getType() + "] for api: " + api.getId());
                }
            }

            reactableApi.setEnabled(api.getLifecycleState() == LifecycleState.STARTED);
            reactableApi.setDeployedAt(apiEvent.getCreatedAt());
            reactableApi.setRevision(
                Optional.ofNullable(apiEvent.getProperties())
                    .map(props -> props.get(DEPLOYMENT_NUMBER.getValue()))
                    .orElse(null)
            );

            environmentService.fill(api.getEnvironmentId(), reactableApi);

            return reactableApi;
        } catch (Exception e) {
            // Log the error and ignore this event.
            log.error("Unable to extract api definition from event [{}].", apiEvent.getId(), e);
            return null;
        }
    }
}
