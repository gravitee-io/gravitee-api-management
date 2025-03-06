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

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupServices;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupServicesV2;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV2;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV4;
import io.gravitee.rest.api.management.v2.rest.model.EndpointHealthCheckService;
import io.gravitee.rest.api.management.v2.rest.model.EndpointServices;
import io.gravitee.rest.api.management.v2.rest.model.EndpointStatus;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV2;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV4;
import io.gravitee.rest.api.management.v2.rest.model.HttpEndpointV2;
import io.gravitee.rest.api.management.v2.rest.model.LoadBalancer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class EndpointFixtures {

    private EndpointFixtures() {}

    private static final Supplier<HttpEndpointV2> BASE_HTTP_ENDPOINT_V2 = () ->
        new HttpEndpointV2()
            .name("Endpoint name")
            .target("http://gravitee.io")
            .weight(1)
            .backup(false)
            .status(EndpointStatus.UP)
            .tenants(List.of("tenant1", "tenant2"))
            .type("http")
            .inherit(true)
            .healthCheck(new EndpointHealthCheckService())
            .headers(Collections.emptyList())
            .httpProxy(null)
            .httpClientOptions(null)
            .httpClientSslOptions(null);
    private static final Supplier<EndpointGroupServicesV2> BASE_SERVICES_V2 = EndpointGroupServicesV2::new;

    private static final Supplier<EndpointGroupV2> BASE_ENDPOINTGROUP_V2 = () ->
        new EndpointGroupV2()
            .name("Endpoint group name")
            .endpoints(List.of(new EndpointV2(BASE_HTTP_ENDPOINT_V2.get())))
            .loadBalancer(new LoadBalancer().type(LoadBalancer.TypeEnum.ROUND_ROBIN))
            .services(BASE_SERVICES_V2.get())
            .httpProxy(null)
            .httpClientOptions(null)
            .httpClientSslOptions(null);

    private static final Supplier<EndpointGroupServices> BASE_ENDPOINTGROUP_SERVICES_V4 = EndpointGroupServices::new;

    private static final Supplier<EndpointV4> BASE_ENDPOINT_V4 = () ->
        new EndpointV4()
            .name("Endpoint name")
            .type("http-get")
            .weight(1)
            .inheritConfiguration(false)
            .secondary(false)
            .tenants(List.of("tenant1", "tenant2"))
            .configuration(new LinkedHashMap<>(Map.of("nice", "configuration")))
            .sharedConfigurationOverride(new LinkedHashMap<>(Map.of("nice", "configuration")))
            .services(new EndpointServices());

    private static final Supplier<EndpointGroupV4> BASE_ENDPOINTGROUP_V4 = () ->
        new EndpointGroupV4()
            .name("Endpoint group name")
            .type("http-get")
            .loadBalancer(new LoadBalancer().type(LoadBalancer.TypeEnum.ROUND_ROBIN))
            .sharedConfiguration(new LinkedHashMap<>(Map.of("nice", "configuration")))
            .endpoints(List.of(BASE_ENDPOINT_V4.get()))
            .services(BASE_ENDPOINTGROUP_SERVICES_V4.get());

    private static final Supplier<EndpointHealthCheckService> BASE_HEALTH_CHECK_SERVICE = () ->
        new EndpointHealthCheckService().enabled(true).schedule("0 */5 * * * *");

    public static HttpEndpointV2 anHttpEndpointV2() {
        return BASE_HTTP_ENDPOINT_V2.get();
    }

    public static EndpointV2 anEndpointV2() {
        return new EndpointV2(anHttpEndpointV2());
    }

    public static EndpointGroupV2 anEndpointGroupV2() {
        return BASE_ENDPOINTGROUP_V2.get();
    }

    public static EndpointV4 anEndpointV4() {
        return BASE_ENDPOINT_V4.get();
    }

    public static EndpointGroupV4 anEndpointGroupV4() {
        return BASE_ENDPOINTGROUP_V4.get();
    }

    public static io.gravitee.definition.model.Endpoint aModelEndpointV2() {
        return EndpointModelFixtures.aModelEndpointV2();
    }

    public static io.gravitee.definition.model.EndpointGroup aModelEndpointGroupV2() {
        return EndpointModelFixtures.aModelEndpointGroupV2();
    }

    public static Endpoint aModelEndpointHttpV4() {
        return EndpointModelFixtures.aModelEndpointHttpV4();
    }

    public static EndpointGroup aModelEndpointGroupHttpV4() {
        return EndpointModelFixtures.aModelEndpointGroupHttpV4();
    }

    public static NativeEndpoint aModelEndpointNativeV4() {
        return EndpointModelFixtures.aModelEndpointNativeV4();
    }

    public static NativeEndpointGroup aModelEndpointGroupNativeV4() {
        return EndpointModelFixtures.aModelEndpointGroupNativeV4();
    }

    public static EndpointHealthCheckService anEndpointHealthCheckService() {
        return BASE_HEALTH_CHECK_SERVICE.get();
    }
}
