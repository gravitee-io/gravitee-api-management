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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.service.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ServiceMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TYPE_ENDPOINT = "ENDPOINT";
    private static final List<String> ALLOWED_DISCOVERY_PLUGIN_IDS = List.of("consul-service-discovery");

    private ServiceMapper() {}

    public static MigrationResult<io.gravitee.definition.model.v4.service.Service> convert(
        io.gravitee.definition.model.Service v2Service,
        String type,
        String name
    ) {
        return switch (v2Service) {
            case io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService v2dynamicPropertyService -> null;
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
            default -> MigrationResult.issue("Unsupported Service", MigrationResult.State.IMPOSSIBLE);
        };
    }

    private static MigrationResult<Service> convertHealthCheckService(HealthCheckService v2HealthCheckService, String type, String name) {
        ObjectNode config = MAPPER.createObjectNode();
        MigrationResult<Service> migrationResult = MigrationResult.value(
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
        List<HealthCheckStep> steps = v2HealthCheckService.getSteps();
        if (steps != null && !steps.isEmpty()) {
            if (steps.size() > 1) {
                log.error("Health check for {} cannot have more than one step ", endpointReferenceForMessage);
                migrationResult.addIssue(
                    new MigrationResult.Issue(
                        "Health check for " + endpointReferenceForMessage + " cannot have more than one step",
                        MigrationResult.State.IMPOSSIBLE
                    )
                );
            }
            HealthCheckStep step = steps.get(0);
            if (step.getResponse().getAssertions() != null && step.getResponse().getAssertions().size() > 1) {
                log.error("Health check for {} cannot have more than one assertion", endpointReferenceForMessage);
                migrationResult.addIssue(
                    new MigrationResult.Issue(
                        "Health check for " + endpointReferenceForMessage + " cannot have more than one assertion",
                        MigrationResult.State.IMPOSSIBLE
                    )
                );
            }
            try {
                config.set(
                    "headers",
                    (step.getRequest().getHeaders() == null ? MAPPER.createArrayNode() : MAPPER.valueToTree(step.getRequest().getHeaders()))
                );
                config.set("method", MAPPER.valueToTree(step.getRequest().getMethod()));
                config.set("target", MAPPER.valueToTree(step.getRequest().getPath()));
                config.set("assertion", MAPPER.valueToTree(StringUtils.appendCurlyBraces(step.getResponse().getAssertions().get(0))));
                config.set("overrideEndpointPath", MAPPER.valueToTree(step.getRequest().isFromRoot()));
            } catch (Exception e) {
                log.error("Unable to map configuration for Health check ", e);
                return migrationResult.addIssue(
                    new MigrationResult.Issue("Unable to map configuration for Health check", MigrationResult.State.IMPOSSIBLE)
                );
            }
        }

        // Build the v4 Service
        return migrationResult.map(ser -> {
            ser.setConfiguration(config.toString());
            return ser;
        });
    }

    private static MigrationResult<Service> convertEPHealthCheckService(
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

    private static MigrationResult<Service> mapServiceDiscovery(EndpointDiscoveryService discoveryService) {
        if (discoveryService == null) {
            return MigrationResult.value(new Service());
        }

        if (!ALLOWED_DISCOVERY_PLUGIN_IDS.contains(discoveryService.getProvider())) {
            return MigrationResult.issue(
                "Service discovery provider '" +
                discoveryService.getProvider() +
                "' is not supported for migration. Only consul-service-discovery is supported.",
                MigrationResult.State.IMPOSSIBLE
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
                new MigrationResult.Issue(
                    "Service discovery configuration can be migrated, but the configuration page will not be available in the new version.",
                    MigrationResult.State.CAN_BE_FORCED
                )
            );
    }
}
