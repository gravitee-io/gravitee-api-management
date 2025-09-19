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
package io.gravitee.gateway.services.healthcheck;

import static io.gravitee.node.vertx.proxy.VertxProxyOptionsUtils.buildProxyOptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.services.healthcheck.grpc.GrpcEndpointRule;
import io.gravitee.gateway.services.healthcheck.http.HttpEndpointRule;
import io.gravitee.node.api.configuration.Configuration;
import io.vertx.core.net.ProxyOptions;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointHealthcheckResolver implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointHealthcheckResolver.class);

    @Autowired
    private GatewayConfiguration gatewayConfiguration;

    @Autowired
    private Configuration configuration;

    private ProxyOptions systemProxyOptions;

    private final ObjectMapper mapper = new GraviteeMapper(false);

    @Override
    public void afterPropertiesSet() throws Exception {
        this.systemProxyOptions = null;

        // System proxy must be well configured. Check that this is the case.
        try {
            if (configuration.containsProperty("system.proxy.host")) {
                this.systemProxyOptions = buildProxyOptions(configuration);
            } else {
                LOGGER.debug("System proxy not defined");
            }
        } catch (Exception e) {
            LOGGER.warn("System proxy not properly initialized", e);
        }
    }

    /**
     * Returns a {@link Stream} of {@link Endpoint} which have to be health-checked.
     *
     * @param api
     * @return
     */
    public List<EndpointRule> resolve(Api api) {
        HealthCheckService rootHealthCheck = api.getDefinition().getServices().get(HealthCheckService.class);
        boolean hcEnabled = (rootHealthCheck != null && rootHealthCheck.isEnabled());

        // Filter to check only HTTP endpoints
        Stream<HttpEndpoint> httpEndpoints = api
            .getDefinition()
            .getProxy()
            .getGroups()
            .stream()
            .filter(group -> group.getEndpoints() != null)
            .flatMap(group ->
                group
                    .getEndpoints()
                    .stream()
                    .map(this::convertToHttpEndpoint)
                    .filter(Objects::nonNull)
                    .peek(endpoint -> applyGroupSettings(group, endpoint))
            );

        // Filtering endpoints according to tenancy configuration
        if (gatewayConfiguration.tenant().isPresent()) {
            String tenant = gatewayConfiguration.tenant().get();
            httpEndpoints = httpEndpoints.filter(
                endpoint -> endpoint.getTenants() == null || endpoint.getTenants().isEmpty() || endpoint.getTenants().contains(tenant)
            );
        }

        // Remove backup endpoints
        httpEndpoints = httpEndpoints.filter(endpoint -> !endpoint.isBackup());

        // Keep only endpoints where health-check is enabled or not settled (inherit from service)
        httpEndpoints = httpEndpoints.filter(endpoint ->
            ((endpoint.getHealthCheck() == null && hcEnabled) ||
                (endpoint.getHealthCheck() != null && endpoint.getHealthCheck().isEnabled() && !endpoint.getHealthCheck().isInherit()) ||
                (endpoint.getHealthCheck() != null &&
                    endpoint.getHealthCheck().isEnabled() &&
                    endpoint.getHealthCheck().isInherit() &&
                    hcEnabled))
        );

        return httpEndpoints
            .map(
                (Function<HttpEndpoint, EndpointRule>) endpoint -> {
                    HealthCheckService healthcheck = (endpoint.getHealthCheck() == null || endpoint.getHealthCheck().isInherit())
                        ? rootHealthCheck
                        : endpoint.getHealthCheck();
                    // The following has to be managed by the connector-api
                    if (endpoint.getType().equalsIgnoreCase("grpc")) {
                        return new GrpcEndpointRule(api, endpoint, healthcheck, systemProxyOptions);
                    } else {
                        return new HttpEndpointRule(api, endpoint, healthcheck, systemProxyOptions);
                    }
                }
            )
            .collect(Collectors.toList());
    }

    private HttpEndpoint convertToHttpEndpoint(Endpoint endpoint) {
        if (isHttpEndpoint(endpoint)) {
            try {
                // FIXME: This creates a new instance of HttpEndpoint (and so definition.model.Endpoint) instead of using
                // the one from the ManagedEndpoint. This is a temporary fix that need to be addressed, for details see
                // https://github.com/gravitee-io/issues/issues/6437
                HttpEndpoint httpEndpoint = mapper.readValue(endpoint.getConfiguration(), HttpEndpoint.class);
                endpoint.getEndpointStatusListeners().forEach(httpEndpoint::addEndpointStatusListener);
                return httpEndpoint;
            } catch (JsonProcessingException e) {
                LOGGER.warn("Cannot convert endpoint to http endpoint", e);
            }
        }
        return null;
    }

    // FIXME: https://github.com/gravitee-io/issues/issues/6437
    private boolean isHttpEndpoint(Endpoint endpoint) {
        return "http".equalsIgnoreCase(endpoint.getType()) || "grpc".equalsIgnoreCase(endpoint.getType());
    }

    private void applyGroupSettings(EndpointGroup group, HttpEndpoint endpoint) {
        if (shouldOverrideHttp(endpoint)) {
            endpoint.setHttpClientOptions(group.getHttpClientOptions());
            endpoint.setHttpClientSslOptions(group.getHttpClientSslOptions());
            endpoint.setHttpProxy(group.getHttpProxy());
            endpoint.setHeaders(group.getHeaders());
        }
    }

    private boolean shouldOverrideHttp(HttpEndpoint endpoint) {
        if (endpoint != null) {
            final boolean inherit = endpoint.getInherit() != null && endpoint.getInherit();
            // inherit or discovered endpoints
            return inherit || endpoint.getHttpClientOptions() == null;
        }
        return false;
    }

    public <T extends Endpoint> EndpointRule resolve(Api api, T endpoint) {
        HttpEndpoint httpEndpoint = convertToHttpEndpoint(endpoint);
        if (httpEndpoint != null) {
            HealthCheckService rootHealthCheck = api.getDefinition().getServices().get(HealthCheckService.class);
            boolean hcEnabled = (rootHealthCheck != null && rootHealthCheck.isEnabled());

            if (hcEnabled || httpEndpoint.getHealthCheck() != null) {
                HealthCheckService healthcheck = (httpEndpoint.getHealthCheck() == null || httpEndpoint.getHealthCheck().isInherit())
                    ? rootHealthCheck
                    : httpEndpoint.getHealthCheck();
                if (endpoint.getType().equalsIgnoreCase("http")) {
                    return new HttpEndpointRule(api, httpEndpoint, healthcheck, systemProxyOptions);
                } else if (endpoint.getType().equalsIgnoreCase("grpc")) {
                    return new GrpcEndpointRule(api, httpEndpoint, healthcheck, systemProxyOptions);
                }
            }
        }
        return null;
    }
}
