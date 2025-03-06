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

import io.gravitee.rest.api.management.v2.rest.model.ApiServicesV2;
import io.gravitee.rest.api.management.v2.rest.model.DynamicPropertyProvider;
import io.gravitee.rest.api.management.v2.rest.model.DynamicPropertyService;
import io.gravitee.rest.api.management.v2.rest.model.DynamicPropertyServiceConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.EndpointDiscoveryService;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupServicesV2;
import io.gravitee.rest.api.management.v2.rest.model.HealthCheckService;
import io.gravitee.rest.api.management.v2.rest.model.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.HttpMethod;
import io.gravitee.rest.api.management.v2.rest.model.ServiceV4;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ServiceFixture {

    private ServiceFixture() {}

    private static final Supplier<EndpointDiscoveryService> BASE_ENDPOINT_DISCOVERY_SERVICE = () ->
        new EndpointDiscoveryService().enabled(true).provider("consul").configuration(new LinkedHashMap<>(Map.of("nice", "configuration")));
    private static final Supplier<DynamicPropertyService> BASE_DYNAMIC_PROPERTY_SERVICE = () ->
        new DynamicPropertyService()
            .enabled(true)
            .schedule("0 */5 * * * *")
            .provider(DynamicPropertyProvider.HTTP)
            .configuration(
                new DynamicPropertyServiceConfiguration(
                    new HttpDynamicPropertyProviderConfiguration()
                        .url("http://localhost")
                        .method(HttpMethod.GET)
                        .specification(String.valueOf(new LinkedHashMap<>(Map.of("nice", "configuration"))))
                        .body("body")
                )
            );
    private static final Supplier<HealthCheckService> BASE_HEALTH_CHECK_SERVICE = HealthCheckService::new;

    private static final Supplier<ApiServicesV2> BASE_API_SERVICES_V2 = () ->
        new ApiServicesV2().dynamicProperty(BASE_DYNAMIC_PROPERTY_SERVICE.get()).healthCheck(BASE_HEALTH_CHECK_SERVICE.get());

    private static final Supplier<EndpointGroupServicesV2> BASE_ENDPOINT_GROUP_SERVICES_V2 = () ->
        new EndpointGroupServicesV2().discovery(BASE_ENDPOINT_DISCOVERY_SERVICE.get());

    private static final Supplier<ServiceV4> BASE_API_V4_SERVICE = () ->
        new ServiceV4().type("dynamicProperty").configuration(new LinkedHashMap<>(Map.of("url", "http://localhost"))).enabled(true);

    public static ApiServicesV2 anApiServicesV2() {
        return BASE_API_SERVICES_V2.get();
    }

    public static EndpointGroupServicesV2 anEndpointGroupServicesV2() {
        return BASE_ENDPOINT_GROUP_SERVICES_V2.get();
    }

    public static ServiceV4 aServiceV4() {
        return BASE_API_V4_SERVICE.get();
    }

    public static HealthCheckService aHealthCheckService() {
        return BASE_HEALTH_CHECK_SERVICE.get();
    }
}
