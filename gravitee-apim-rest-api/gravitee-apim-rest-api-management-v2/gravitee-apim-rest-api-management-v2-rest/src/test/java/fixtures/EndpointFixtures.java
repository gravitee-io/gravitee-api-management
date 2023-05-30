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
package fixtures;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupServices;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV2;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV4;
import io.gravitee.rest.api.management.v2.rest.model.EndpointHealthCheckService;
import io.gravitee.rest.api.management.v2.rest.model.EndpointServices;
import io.gravitee.rest.api.management.v2.rest.model.EndpointStatus;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV2;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV4;
import io.gravitee.rest.api.management.v2.rest.model.HttpEndpointV2;
import io.gravitee.rest.api.management.v2.rest.model.LoadBalancer;
import io.gravitee.rest.api.management.v2.rest.model.ServicesV2;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("ALL")
public class EndpointFixtures {

    private static final io.gravitee.definition.model.Endpoint.EndpointBuilder BASE_MODEL_ENDPOINT_V2 =
        io.gravitee.definition.model.Endpoint
            .builder()
            .name("Endpoint name")
            .target("http://gravitee.io")
            .weight(1)
            .backup(false)
            .status(io.gravitee.definition.model.Endpoint.Status.UP)
            .tenants(List.of("tenant1", "tenant2"))
            .type("HTTP")
            .inherit(true)
            .healthCheck(io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService.builder().build())
            .configuration(null);

    private static final io.gravitee.definition.model.EndpointGroup.EndpointGroupBuilder BASE_MODEL_ENDPOINTGROUP_V2 =
        io.gravitee.definition.model.EndpointGroup
            .builder()
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

    private static final HttpEndpointV2.HttpEndpointV2Builder BASE_HTTP_ENDPOINT_V2 = HttpEndpointV2
        .builder()
        .name("Endpoint name")
        .target("http://gravitee.io")
        .weight(1)
        .backup(false)
        .status(EndpointStatus.UP)
        .tenants(List.of("tenant1", "tenant2"))
        .type("HTTP")
        .inherit(true)
        .healthCheck(EndpointHealthCheckService.builder().build())
        .headers(Collections.emptyList())
        .httpProxy(null)
        .httpClientOptions(null)
        .httpClientSslOptions(null);
    private static final ServicesV2.ServicesV2Builder BASE_SERVICES_V2 = ServicesV2
        .builder()
        .discovery(null)
        .healthCheck(null)
        .dynamicProperty(null);

    private static final EndpointGroupV2.EndpointGroupV2Builder BASE_ENDPOINTGROUP_V2 = EndpointGroupV2
        .builder()
        .name("Endpoint group name")
        .endpoints(List.of(new EndpointV2(BASE_HTTP_ENDPOINT_V2.build())))
        .loadBalancer(LoadBalancer.builder().type(LoadBalancer.TypeEnum.ROUND_ROBIN).build())
        .services(BASE_SERVICES_V2.build())
        .httpProxy(null)
        .httpClientOptions(null)
        .httpClientSslOptions(null);

    private static final Endpoint.EndpointBuilder BASE_MODEL_ENDPOINT_V4 = Endpoint
        .builder()
        .name("Endpoint name")
        .type("http-get")
        .weight(1)
        .inheritConfiguration(false)
        .secondary(false)
        .tenants(List.of("tenant1", "tenant2"))
        .configuration("{\n  \"nice\" : \"configuration\"\n}")
        .sharedConfigurationOverride("{\n  \"nice\" : \"configuration\"\n}")
        .services(io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices.builder().healthCheck(null).build());

    private static final EndpointGroup.EndpointGroupBuilder BASE_MODEL_ENDPOINTGROUP_V4 = EndpointGroup
        .builder()
        .name("Endpoint group name")
        .type("http-get")
        .loadBalancer(
            io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancer
                .builder()
                .type(io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType.ROUND_ROBIN)
                .build()
        )
        .sharedConfiguration("{\n  \"nice\" : \"configuration\"\n}")
        .endpoints(List.of(BASE_MODEL_ENDPOINT_V4.build()))
        .services(io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices.builder().healthCheck(null).build());

    private static final EndpointGroupServices.EndpointGroupServicesBuilder BASE_ENDPOINTGROUP_SERVICES_V4 = EndpointGroupServices
        .builder()
        .discovery(null)
        .healthcheck(null);

    private static final EndpointV4.EndpointV4Builder BASE_ENDPOINT_V4 = EndpointV4
        .builder()
        .name("Endpoint name")
        .type("http-get")
        .weight(1)
        .inheritConfiguration(false)
        .secondary(false)
        .tenants(List.of("tenant1", "tenant2"))
        .configuration(new LinkedHashMap<>(Map.of("nice", "configuration")))
        .sharedConfigurationOverride(new LinkedHashMap<>(Map.of("nice", "configuration")))
        .services(EndpointServices.builder().healthcheck(null).build());

    private static final EndpointGroupV4.EndpointGroupV4Builder BASE_ENDPOINTGROUP_V4 = EndpointGroupV4
        .builder()
        .name("Endpoint group name")
        .type("http-get")
        .loadBalancer(LoadBalancer.builder().type(LoadBalancer.TypeEnum.ROUND_ROBIN).build())
        .sharedConfiguration(new LinkedHashMap<>(Map.of("nice", "configuration")))
        .endpoints(List.of(BASE_ENDPOINT_V4.build()))
        .services(BASE_ENDPOINTGROUP_SERVICES_V4.build());

    public static io.gravitee.definition.model.Endpoint aModelEndpointV2() {
        return BASE_MODEL_ENDPOINT_V2.build();
    }

    public static HttpEndpointV2 anHtpEndpointV2() {
        return BASE_HTTP_ENDPOINT_V2.build();
    }

    public static EndpointV2 anEndpointV2() {
        return new EndpointV2(anHtpEndpointV2());
    }

    public static io.gravitee.definition.model.EndpointGroup aModelEndpointGroupV2() {
        return BASE_MODEL_ENDPOINTGROUP_V2.build();
    }

    public static EndpointGroupV2 anEndpointGroupV2() {
        return BASE_ENDPOINTGROUP_V2.build();
    }

    public static Endpoint aModelEndpointV4() {
        return BASE_MODEL_ENDPOINT_V4.build();
    }

    public static EndpointV4 anEndpointV4() {
        return BASE_ENDPOINT_V4.build();
    }

    public static EndpointGroup aModelEndpointGroupV4() {
        return BASE_MODEL_ENDPOINTGROUP_V4.build();
    }

    public static EndpointGroupV4 anEndpointGroupV4() {
        return BASE_ENDPOINTGROUP_V4.build();
    }
}
