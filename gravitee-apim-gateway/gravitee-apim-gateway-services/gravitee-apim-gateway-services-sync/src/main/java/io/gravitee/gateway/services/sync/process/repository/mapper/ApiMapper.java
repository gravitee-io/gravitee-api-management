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

import static io.gravitee.repository.management.model.Event.EventProperties.API_ID;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ApiMapper {

    private final ObjectMapper objectMapper;
    private final EnvironmentRepository environmentRepository;
    private final OrganizationRepository organizationRepository;

    private final Map<String, Environment> environmentMap = new ConcurrentHashMap<>();
    private final Map<String, io.gravitee.repository.management.model.Organization> organizationMap = new ConcurrentHashMap<>();

    public Maybe<String> toId(Event apiEvent) {
        return Maybe.fromCallable(() -> {
            String apiId = null;
            if (apiEvent.getProperties() != null) {
                apiId = apiEvent.getProperties().get(API_ID.getValue());
            }
            if (apiId == null) {
                log.warn("Unable to extract api info from event [{}].", apiEvent.getId());
            }
            return apiId;
        });
    }

    public Maybe<ReactableApi<?>> to(Event apiEvent) {
        return Maybe.fromCallable(() -> {
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
                    var eventApiDefinition = objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.v4.Api.class);

                    // Update definition with required information for deployment phase
                    reactableApi = new io.gravitee.gateway.reactive.handlers.api.v4.Api(eventApiDefinition);
                }

                reactableApi.setEnabled(api.getLifecycleState() == LifecycleState.STARTED);
                reactableApi.setDeployedAt(apiEvent.getCreatedAt());

                enhanceWithOrgAndEnv(api.getEnvironmentId(), reactableApi);

                return reactableApi;
            } catch (Exception e) {
                // Log the error and ignore this event.
                log.error("Unable to extract api definition from event [{}].", apiEvent.getId(), e);
                return null;
            }
        });
    }

    private void enhanceWithOrgAndEnv(String environmentId, ReactableApi<?> definition) {
        if (environmentId != null) {
            Environment apiEnv = loadEnvironment(environmentId);
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

    private Environment loadEnvironment(final String environmentId) {
        return environmentMap.computeIfAbsent(
            environmentId,
            envId -> {
                try {
                    var environmentOpt = environmentRepository.findById(envId);
                    if (environmentOpt.isPresent()) {
                        Environment environment = environmentOpt.get();
                        loadOrganization(environment);
                        return environment;
                    }
                } catch (Exception e) {
                    log.warn("An error occurred fetching the environment {} and its organization.", envId, e);
                }
                return null;
            }
        );
    }

    private void loadOrganization(final Environment environment) {
        organizationMap.computeIfAbsent(
            environment.getOrganizationId(),
            orgId -> {
                try {
                    return organizationRepository.findById(orgId).orElse(null);
                } catch (Exception e) {
                    return null;
                }
            }
        );
    }
}
