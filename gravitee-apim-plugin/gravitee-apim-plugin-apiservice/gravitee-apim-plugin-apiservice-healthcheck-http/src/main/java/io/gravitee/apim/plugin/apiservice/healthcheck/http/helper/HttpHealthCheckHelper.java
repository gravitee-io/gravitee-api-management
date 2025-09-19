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
package io.gravitee.apim.plugin.apiservice.healthcheck.http.helper;

import static io.gravitee.apim.plugin.apiservice.healthcheck.http.HttpHealthCheckService.HTTP_HEALTH_CHECK_TYPE;
import static java.util.Objects.isNull;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpHealthCheckHelper {

    public static boolean canHandle(final Api api, final String tenant) {
        return api
            .getEndpointGroups()
            .stream()
            .anyMatch(endpointGroup -> {
                // Http health check is enabled if enabled on at least one endpoint.
                return (
                    (!isNull(endpointGroup.getServices()) && isServiceEnabled(endpointGroup.getServices().getHealthCheck())) ||
                    (!isNull(endpointGroup.getEndpoints()) &&
                        endpointGroup
                            .getEndpoints()
                            .stream()
                            .anyMatch(endpoint -> isEnabledAtEndpointLevel(endpoint, tenant)))
                );
            });
    }

    public static boolean isServiceEnabled(final ManagedEndpoint endpoint, final String tenant) {
        return (
            // Health check enabled on the endpoint.
            isEnabledAtEndpointLevel(endpoint.getDefinition(), tenant) ||
            // Or health check enabled at group level and not explicitly disable at endpoint level.
            ((isNull(endpoint.getDefinition().getServices()) ||
                    endpoint.getDefinition().getServices().getHealthCheck() == null ||
                    !endpoint.getDefinition().getServices().getHealthCheck().isEnabled()) &&
                (!isNull(endpoint.getGroup().getDefinition().getServices()) &&
                    isServiceEnabled(endpoint.getGroup().getDefinition().getServices().getHealthCheck())))
        );
    }

    private static boolean isEnabledAtEndpointLevel(final Endpoint endpoint, final String tenant) {
        return (
            !endpoint.isSecondary() &&
            !isNull(endpoint.getServices()) &&
            isServiceEnabled(endpoint.getServices().getHealthCheck()) &&
            hasTenant(endpoint, tenant)
        );
    }

    private static boolean hasTenant(final Endpoint endpoint, final String tenant) {
        return tenant == null || (endpoint.getTenants() != null && endpoint.getTenants().contains(tenant));
    }

    private static boolean isServiceEnabled(final Service healthCheckService) {
        return (
            healthCheckService != null && (healthCheckService.isEnabled() && HTTP_HEALTH_CHECK_TYPE.equals(healthCheckService.getType()))
        );
    }
}
