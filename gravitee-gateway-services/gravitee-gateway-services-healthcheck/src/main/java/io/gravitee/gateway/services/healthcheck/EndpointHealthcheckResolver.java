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
package io.gravitee.gateway.services.healthcheck;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointType;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.services.healthcheck.rule.DefaultEndpointRule;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointHealthcheckResolver {

    @Autowired
    private GatewayConfiguration gatewayConfiguration;

    /**
     * Returns a {@link Stream} of {@link Endpoint} which have to be health-checked.
     *
     * @param api
     * @return
     */
    public List<EndpointRule> resolve(Api api) {
        HealthCheckService rootHealthCheck = api.getServices().get(HealthCheckService.class);

        if (rootHealthCheck != null && rootHealthCheck.isEnabled()) {
            Stream<Endpoint> endpoints = api.getProxy().getEndpoints().stream();

            // Only HTTP endpoint
            Stream<HttpEndpoint> httpEndpoints = endpoints
                    .filter(endpoint -> endpoint.getType() == EndpointType.HTTP)
                    .map(endpoint -> (HttpEndpoint) endpoint);

            // Filtering endpoints according to tenancy configuration
            if (api.getProxy().isMultiTenant() && gatewayConfiguration.tenant().isPresent()) {
                String tenant = gatewayConfiguration.tenant().get();
                httpEndpoints = httpEndpoints
                        .filter(endpoint -> endpoint.getTenant() != null && endpoint.getTenant().equalsIgnoreCase(tenant));
            }

            // Remove backup endpoints
            httpEndpoints = httpEndpoints.filter(endpoint -> ! endpoint.isBackup());

            // Keep only endpoints where health-check is enabled or not settled (inherit from service)
            httpEndpoints = httpEndpoints.filter(endpoint ->
                    (endpoint.getHealthCheck() == null) ||
                            (endpoint.getHealthCheck() != null && endpoint.getHealthCheck().isEnabled()));

            return httpEndpoints.map((Function<HttpEndpoint, EndpointRule>) endpoint -> new DefaultEndpointRule(
                    api.getId(),
                    endpoint,
                    (endpoint.getHealthCheck() == null || endpoint.getHealthCheck().isInherit()) ?
                            rootHealthCheck : endpoint.getHealthCheck())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public void setGatewayConfiguration(GatewayConfiguration gatewayConfiguration) {
        this.gatewayConfiguration = gatewayConfiguration;
    }
}
