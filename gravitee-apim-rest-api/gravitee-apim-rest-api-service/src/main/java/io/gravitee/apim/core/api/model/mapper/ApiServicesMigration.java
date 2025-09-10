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
package io.gravitee.apim.core.api.model.mapper;

import static io.gravitee.apim.core.api.model.utils.MigrationResult.State.IMPOSSIBLE;
import static io.gravitee.apim.core.utils.CollectionUtils.size;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProvider;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import io.gravitee.definition.model.v4.service.Service;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class ApiServicesMigration {

    private final ObjectMapper jsonMapper;
    private static final String TYPE_ENDPOINT = "ENDPOINT";
    private static final List<String> ALLOWED_DISCOVERY_PLUGIN_IDS = List.of("consul-service-discovery");

    public MigrationResult<io.gravitee.definition.model.v4.service.Service> convert(
        io.gravitee.definition.model.Service v2Service,
        String type,
        String name
    ) {
        return switch (v2Service) {
            case io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService v2dynamicPropertyService -> convertDynamicPropertyService(
                v2dynamicPropertyService
            );
            case io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService vEP2HealthCheckService -> convertEPHealthCheckService(
                vEP2HealthCheckService,
                type,
                name
            );
            case io.gravitee.definition.model.services.healthcheck.HealthCheckService v2HealthCheckService -> convertHealthCheckService(
                v2HealthCheckService,
                type,
                name
            );
            case io.gravitee.definition.model.services.schedule.ScheduledService v2ScheduledService -> null;
            case io.gravitee.definition.model.services.discovery.EndpointDiscoveryService v2EndpointDiscoveryService -> mapServiceDiscovery(
                v2EndpointDiscoveryService
            );
            case null -> null;
            default -> MigrationResult.issue("Unsupported Service", IMPOSSIBLE);
        };
    }

    private MigrationResult<Service> convertHealthCheckService(HealthCheckService v2HealthCheckService, String type, String name) {
        ObjectNode config = jsonMapper.createObjectNode();
        var migrationResult = MigrationResult.value(
            Service.builder().enabled(v2HealthCheckService.isEnabled()).overrideConfiguration(false).type("http-health-check").build()
        );
        String endpointReferenceForMessage = String.format("%s : %s", type.equals(TYPE_ENDPOINT) ? "endpoint" : "endpointgroup", name);
        if (v2HealthCheckService.getSchedule() != null) {
            config.put("schedule", v2HealthCheckService.getSchedule());
        } else {
            config.putNull("schedule");
        }

        config.put("failureThreshold", 2);
        config.put("successThreshold", 2);

        // If steps exist, derive config from the first step
        var steps = v2HealthCheckService.getSteps();
        switch (size(steps)) {
            case 0 -> {
                /* nothing to do */
            }
            case 1 -> {
                HealthCheckStep step = steps.getFirst();
                if (size(step.getResponse().getAssertions()) > 1) {
                    log.error("Health check for {} cannot have more than one assertion", endpointReferenceForMessage);
                    migrationResult.addIssue(
                        "Health check for %s cannot have more than one assertion".formatted(endpointReferenceForMessage),
                        IMPOSSIBLE
                    );
                }
                try {
                    config.set(
                        "headers",
                        (
                            step.getRequest().getHeaders() == null
                                ? jsonMapper.createArrayNode()
                                : jsonMapper.valueToTree(step.getRequest().getHeaders())
                        )
                    );
                    config.set("method", jsonMapper.valueToTree(step.getRequest().getMethod()));
                    config.set("target", jsonMapper.valueToTree(step.getRequest().getPath()));
                    config.set(
                        "assertion",
                        jsonMapper.valueToTree(StringUtils.appendCurlyBraces(step.getResponse().getAssertions().getFirst()))
                    );
                    config.set("overrideEndpointPath", jsonMapper.valueToTree(step.getRequest().isFromRoot()));
                } catch (Exception e) {
                    log.error("Unable to map configuration for Health check ", e);
                    return migrationResult.addIssue("Unable to map configuration for Health check", IMPOSSIBLE);
                }
            }
            default -> {
                log.error("Health check for {} cannot have more than one step ", endpointReferenceForMessage);
                migrationResult.addIssue(
                    "Health check for %s cannot have more than one step".formatted(endpointReferenceForMessage),
                    IMPOSSIBLE
                );
            }
        }

        // Build the v4 Service
        return migrationResult.map(ser -> {
            ser.setConfiguration(config.toString());
            return ser;
        });
    }

    private MigrationResult<Service> convertEPHealthCheckService(
        EndpointHealthCheckService v2EPHealthCheckService,
        String type,
        String name
    ) {
        return convertHealthCheckService(v2EPHealthCheckService, type, name)
            .map(plainHealthCheckService -> {
                plainHealthCheckService.setOverrideConfiguration(!v2EPHealthCheckService.isInherit());
                return plainHealthCheckService;
            });
    }

    private MigrationResult<io.gravitee.definition.model.v4.service.Service> convertDynamicPropertyService(
        DynamicPropertyService v2dynamicPropertyService
    ) {
        if (v2dynamicPropertyService == null) {
            return MigrationResult.value(new Service());
        }
        ObjectNode configNode = jsonMapper.createObjectNode();
        MigrationResult<Service> migrationResult = MigrationResult.value(
            Service
                .builder()
                .overrideConfiguration(false)
                .type("http-dynamic-properties")
                .enabled(v2dynamicPropertyService.isEnabled())
                .build()
        );
        try {
            configNode.put("schedule", v2dynamicPropertyService.getSchedule());

            if (
                v2dynamicPropertyService.getProvider() != null &&
                !v2dynamicPropertyService.getProvider().equals(DynamicPropertyProvider.HTTP)
            ) {
                String errorMessage = "Unable to migrate Dynamic properties configuration as provider is different from HTTP";
                log.error(errorMessage);
                return migrationResult.addIssue(new MigrationResult.Issue(errorMessage, MigrationResult.State.IMPOSSIBLE));
            }
            if (
                v2dynamicPropertyService.getConfiguration() instanceof HttpDynamicPropertyProviderConfiguration httpDPProviderConfiguration
            ) {
                configNode.set(
                    "headers",
                    jsonMapper.valueToTree(
                        httpDPProviderConfiguration.getHeaders() != null
                            ? httpDPProviderConfiguration.getHeaders()
                            : jsonMapper.createArrayNode()
                    )
                );
                configNode.put("method", httpDPProviderConfiguration.getMethod().name());
                configNode.put("systemProxy", httpDPProviderConfiguration.isUseSystemProxy());
                configNode.put("transformation", httpDPProviderConfiguration.getSpecification());
                configNode.put("url", httpDPProviderConfiguration.getUrl());
            }
        } catch (Exception e) {
            String errorMessage = "Unable to map configuration for Dynamic Property Service";
            log.error(errorMessage, e);
            return migrationResult.addIssue(new MigrationResult.Issue(errorMessage, MigrationResult.State.IMPOSSIBLE));
        }
        return migrationResult.map(service -> {
            service.setConfiguration(configNode.toString());
            return service;
        });
    }

    private static MigrationResult<Service> mapServiceDiscovery(EndpointDiscoveryService discoveryService) {
        if (discoveryService == null || discoveryService.getProvider() == null) {
            return MigrationResult.value(new Service());
        }

        if (!ALLOWED_DISCOVERY_PLUGIN_IDS.contains(discoveryService.getProvider())) {
            return MigrationResult.issue(
                "Service discovery provider '%s' is not supported for migration. Only consul-service-discovery is supported.".formatted(
                        discoveryService.getProvider()
                    ),
                IMPOSSIBLE
            );
        }

        var service = Service
            .builder()
            .configuration(discoveryService.getConfiguration())
            .type(discoveryService.getProvider())
            .enabled(discoveryService.isEnabled())
            .build();

        return MigrationResult
            .value(service)
            .addIssue(
                "Service discovery configuration can be migrated, but the configuration page will not be available in the new version.",
                MigrationResult.State.CAN_BE_FORCED
            );
    }
}
