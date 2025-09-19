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
package fixtures;

import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EndpointModelFixtures {

    private EndpointModelFixtures() {}

    private static final io.gravitee.definition.model.Endpoint.EndpointBuilder BASE_MODEL_ENDPOINT_V2 =
        io.gravitee.definition.model.Endpoint.builder()
            .name("Endpoint name")
            .target("http://gravitee.io")
            .weight(1)
            .backup(false)
            .status(io.gravitee.definition.model.Endpoint.Status.UP)
            .tenants(List.of("tenant1", "tenant2"))
            .type("http")
            .inherit(true)
            .healthCheck(io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService.builder().build())
            .configuration(null);

    private static final HttpEndpoint.HttpEndpointBuilder BASE_MODEL_HTTP_ENDPOINT_V2 = HttpEndpoint.builder()
        .name("Endpoint name")
        .target("http://gravitee.io")
        .weight(1)
        .backup(false)
        .status(HttpEndpoint.Status.UP)
        .tenants(List.of("tenant1", "tenant2"))
        .type("http")
        .inherit(false)
        .healthCheck(io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService.builder().build())
        .headers(Collections.emptyList())
        .httpProxy(null)
        .httpClientOptions(null)
        .httpClientSslOptions(null);

    private static final io.gravitee.definition.model.EndpointGroup.EndpointGroupBuilder BASE_MODEL_ENDPOINTGROUP_V2 =
        io.gravitee.definition.model.EndpointGroup.builder()
            .name("Endpoint group name")
            .endpoints(Set.of(BASE_MODEL_ENDPOINT_V2.build()))
            .loadBalancer(
                io.gravitee.definition.model.LoadBalancer.builder().type(io.gravitee.definition.model.LoadBalancerType.ROUND_ROBIN).build()
            )
            .services(null)
            .httpProxy(null)
            .httpClientOptions(null)
            .httpClientSslOptions(null)
            .headers(Collections.emptyList());

    private static final Endpoint.EndpointBuilder BASE_MODEL_ENDPOINT_V4 = Endpoint.builder()
        .name("Endpoint name")
        .type("http-get")
        .weight(1)
        .inheritConfiguration(false)
        .secondary(false)
        .tenants(List.of("tenant1", "tenant2"))
        .configuration("{\n  \"nice\" : \"configuration\"\n}")
        .sharedConfigurationOverride("{\n  \"nice\" : \"configuration\"\n}")
        .services(io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices.builder().healthCheck(null).build());

    private static final EndpointGroup.EndpointGroupBuilder BASE_MODEL_ENDPOINTGROUP_V4 = EndpointGroup.builder()
        .name("Endpoint group name")
        .type("http-get")
        .loadBalancer(
            io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancer.builder()
                .type(io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType.ROUND_ROBIN)
                .build()
        )
        .sharedConfiguration("{\n  \"nice\" : \"configuration\"\n}")
        .endpoints(List.of(BASE_MODEL_ENDPOINT_V4.build()))
        .services(io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices.builder().healthCheck(null).build());

    private static final io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService.EndpointHealthCheckServiceBuilder BASE_HEALTH_CHECK_SERVICE =
        io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService.builder().enabled(true).schedule("0 */5 * * * *");

    public static io.gravitee.definition.model.Endpoint aModelEndpointV2() {
        return BASE_MODEL_ENDPOINT_V2.build();
    }

    public static HttpEndpoint aModelHttpEndpointV2() {
        return BASE_MODEL_HTTP_ENDPOINT_V2.build();
    }

    public static io.gravitee.definition.model.EndpointGroup aModelEndpointGroupV2() {
        return BASE_MODEL_ENDPOINTGROUP_V2.build();
    }

    public static Endpoint aModelEndpointV4() {
        return BASE_MODEL_ENDPOINT_V4.build();
    }

    public static EndpointGroup aModelEndpointGroupV4() {
        return BASE_MODEL_ENDPOINTGROUP_V4.build();
    }

    public static io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService aModelHealthCheckService() {
        return BASE_HEALTH_CHECK_SERVICE.build();
    }
}
