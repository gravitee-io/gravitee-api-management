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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.services.healthcheck.grpc.GrpcEndpointRule;
import io.gravitee.gateway.services.healthcheck.http.HttpEndpointRule;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

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
    private Environment environment;

    private ProxyOptions systemProxyOptions;

    private final ObjectMapper mapper = new GraviteeMapper();

    @Override
    public void afterPropertiesSet() throws Exception {
        this.systemProxyOptions = null;

        // System proxy must be well configured. Check that this is the case.
        try {
            if (environment.containsProperty("system.proxy.host")) {
                ProxyOptions proxyOptions = new ProxyOptions();
                proxyOptions.setHost(environment.getProperty("system.proxy.host"));

                proxyOptions.setPort(Integer.parseInt(Objects.requireNonNull(environment.getProperty("system.proxy.port"))));
                proxyOptions.setType(ProxyType.valueOf(environment.getProperty("system.proxy.type")));

                proxyOptions.setUsername(environment.getProperty("system.proxy.username"));
                proxyOptions.setPassword(environment.getProperty("system.proxy.password"));
                this.systemProxyOptions = proxyOptions;
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
        HealthCheckService rootHealthCheck = api.getServices().get(HealthCheckService.class);
        boolean hcEnabled = (rootHealthCheck != null && rootHealthCheck.isEnabled());

        // Filter to check only HTTP endpoints
        Stream<HttpEndpoint> httpEndpoints = api
            .getProxy()
            .getGroups()
            .stream()
            .filter(group -> group.getEndpoints() != null)
            .flatMap(
                group ->
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
            httpEndpoints =
                httpEndpoints.filter(
                    endpoint -> endpoint.getTenants() == null || endpoint.getTenants().isEmpty() || endpoint.getTenants().contains(tenant)
                );
        }

        // Remove backup endpoints
        httpEndpoints = httpEndpoints.filter(endpoint -> !endpoint.isBackup());

        // Keep only endpoints where health-check is enabled or not settled (inherit from service)
        httpEndpoints =
            httpEndpoints.filter(
                endpoint ->
                    (
                        (endpoint.getHealthCheck() == null && hcEnabled) ||
                        (
                            endpoint.getHealthCheck() != null &&
                            endpoint.getHealthCheck().isEnabled() &&
                            !endpoint.getHealthCheck().isInherit()
                        ) ||
                        (
                            endpoint.getHealthCheck() != null &&
                            endpoint.getHealthCheck().isEnabled() &&
                            endpoint.getHealthCheck().isInherit() &&
                            hcEnabled
                        )
                    )
            );

        return httpEndpoints
            .map(
                (Function<HttpEndpoint, EndpointRule>) endpoint -> {
                    HealthCheckService healthcheck = (endpoint.getHealthCheck() == null || endpoint.getHealthCheck().isInherit())
                        ? rootHealthCheck
                        : endpoint.getHealthCheck();
                    // The following has to be managed by the connector-api
                    if (endpoint.getType().equalsIgnoreCase("grpc")) {
                        return new GrpcEndpointRule(api.getId(), endpoint, healthcheck, systemProxyOptions);
                    } else {
                        return new HttpEndpointRule(api.getId(), endpoint, healthcheck, systemProxyOptions);
                    }
                }
            )
            .collect(Collectors.toList());
    }

    private HttpEndpoint convertToHttpEndpoint(Endpoint endpoint) {
        if (isHttpEndpoint(endpoint)) {
            try {
                return mapper.readValue(endpoint.getConfiguration(), HttpEndpoint.class);
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
            HealthCheckService rootHealthCheck = api.getServices().get(HealthCheckService.class);
            boolean hcEnabled = (rootHealthCheck != null && rootHealthCheck.isEnabled());

            if (hcEnabled || httpEndpoint.getHealthCheck() != null) {
                HealthCheckService healthcheck = (httpEndpoint.getHealthCheck() == null || httpEndpoint.getHealthCheck().isInherit())
                    ? rootHealthCheck
                    : httpEndpoint.getHealthCheck();
                if (endpoint.getType().equalsIgnoreCase("http")) {
                    return new HttpEndpointRule(api.getId(), httpEndpoint, healthcheck, systemProxyOptions);
                } else if (endpoint.getType().equalsIgnoreCase("grpc")) {
                    return new GrpcEndpointRule(api.getId(), httpEndpoint, healthcheck, systemProxyOptions);
                }
            }
        }
        return null;
    }
}
