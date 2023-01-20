/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.synchronizer.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.LifecycleState;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventToReactableApiAdapter {

    private static final Logger logger = LoggerFactory.getLogger(EventToReactableApiAdapter.class);

    private final ObjectMapper objectMapper;
    private final EnvironmentRepository environmentRepository;
    private final OrganizationRepository organizationRepository;

    private final Map<String, Environment> environmentMap = new ConcurrentHashMap<>();
    private final Map<String, io.gravitee.repository.management.model.Organization> organizationMap = new ConcurrentHashMap<>();

    public EventToReactableApiAdapter(
        ObjectMapper objectMapper,
        EnvironmentRepository environmentRepository,
        OrganizationRepository organizationRepository
    ) {
        this.objectMapper = objectMapper;
        this.environmentRepository = environmentRepository;
        this.organizationRepository = organizationRepository;
    }

    public Maybe<ReactableApi<?>> toReactableApi(Event apiEvent) {
        return toApiDefinition(apiEvent);
    }

    private Maybe<ReactableApi<?>> toApiDefinition(Event apiEvent) {
        try {
            // Read API definition from event
            io.gravitee.repository.management.model.Api eventPayload = objectMapper.readValue(
                apiEvent.getPayload(),
                io.gravitee.repository.management.model.Api.class
            );

            ReactableApi<?> api;

            // Check the version of the API definition to read the right model entity
            if (eventPayload.getDefinitionVersion() == null || !eventPayload.getDefinitionVersion().equals(DefinitionVersion.V4)) {
                io.gravitee.definition.model.Api eventApiDefinition = objectMapper.readValue(
                    eventPayload.getDefinition(),
                    io.gravitee.definition.model.Api.class
                );

                // Update definition with required information for deployment phase
                api = new io.gravitee.gateway.handlers.api.definition.Api(eventApiDefinition);
            } else {
                io.gravitee.definition.model.v4.Api eventApiDefinition = objectMapper.readValue(
                    eventPayload.getDefinition(),
                    io.gravitee.definition.model.v4.Api.class
                );

                // Update definition with required information for deployment phase
                api = new io.gravitee.gateway.jupiter.handlers.api.v4.Api(eventApiDefinition);
            }

            api.setEnabled(eventPayload.getLifecycleState() == LifecycleState.STARTED);
            api.setDeployedAt(apiEvent.getCreatedAt());

            enhanceWithOrgAndEnv(eventPayload.getEnvironmentId(), api);

            return Maybe.just(api);
        } catch (Exception e) {
            // Log the error and ignore this event.
            logger.error("Unable to extract api definition from event [{}].", apiEvent.getId(), e);
            return Maybe.empty();
        }
    }

    private void enhanceWithOrgAndEnv(String environmentId, ReactableApi<?> definition) {
        Environment apiEnv = null;

        if (environmentId != null) {
            apiEnv =
                environmentMap.computeIfAbsent(
                    environmentId,
                    envId -> {
                        try {
                            var environment = environmentRepository.findById(envId);

                            if (environment.isPresent()) {
                                organizationMap.computeIfAbsent(
                                    environment.get().getOrganizationId(),
                                    orgId -> {
                                        try {
                                            return organizationRepository.findById(orgId).orElse(null);
                                        } catch (Exception e) {
                                            return null;
                                        }
                                    }
                                );

                                return environment.get();
                            }

                            return null;
                        } catch (Exception e) {
                            logger.warn("An error occurred fetching the environment {} and its organization.", envId, e);
                            return null;
                        }
                    }
                );
        }

        if (apiEnv != null) {
            definition.setEnvironmentId(apiEnv.getId());
            definition.setEnvironmentHrid(apiEnv.getHrids() != null ? apiEnv.getHrids().stream().findFirst().orElse(null) : null);

            final io.gravitee.repository.management.model.Organization apiOrg = organizationMap.get(apiEnv.getOrganizationId());

            if (apiOrg != null) {
                definition.setOrganizationId(apiOrg.getId());
                definition.setOrganizationHrid(apiOrg.getHrids() != null ? apiOrg.getHrids().stream().findFirst().orElse(null) : null);
            }
        }
    }
}
