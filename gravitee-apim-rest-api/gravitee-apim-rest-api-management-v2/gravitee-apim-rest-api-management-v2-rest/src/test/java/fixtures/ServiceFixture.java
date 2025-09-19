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

import io.gravitee.rest.api.management.v2.rest.model.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceFixture {

    private ServiceFixture() {}

    private static final EndpointDiscoveryService.EndpointDiscoveryServiceBuilder BASE_ENDPOINT_DISCOVERY_SERVICE =
        EndpointDiscoveryService.builder()
            .enabled(true)
            .provider("consul")
            .configuration(new LinkedHashMap<>(Map.of("nice", "configuration")));
    private static final DynamicPropertyService.DynamicPropertyServiceBuilder BASE_DYNAMIC_PROPERTY_SERVICE =
        DynamicPropertyService.builder()
            .enabled(true)
            .schedule("0 */5 * * * *")
            .provider(DynamicPropertyProvider.HTTP)
            .configuration(
                new DynamicPropertyServiceConfiguration(
                    HttpDynamicPropertyProviderConfiguration.builder()
                        .url("http://localhost")
                        .method(HttpMethod.GET)
                        .specification(String.valueOf(new LinkedHashMap<>(Map.of("nice", "configuration"))))
                        .body("body")
                        .build()
                )
            );
    private static final HealthCheckService.HealthCheckServiceBuilder BASE_HEALTH_CHECK_SERVICE = HealthCheckService.builder();

    private static final ApiServicesV2.ApiServicesV2Builder BASE_API_SERVICES_V2 = ApiServicesV2.builder()
        .dynamicProperty(BASE_DYNAMIC_PROPERTY_SERVICE.build())
        .healthCheck(BASE_HEALTH_CHECK_SERVICE.build());

    private static final EndpointGroupServicesV2.EndpointGroupServicesV2Builder BASE_ENDPOINT_GROUP_SERVICES_V2 =
        EndpointGroupServicesV2.builder().discovery(BASE_ENDPOINT_DISCOVERY_SERVICE.build());

    private static final ServiceV4.ServiceV4Builder BASE_API_V4_SERVICE = ServiceV4.builder()
        .type("dynamicProperty")
        .configuration(new LinkedHashMap<>(Map.of("url", "http://localhost")))
        .enabled(true);

    public static ApiServicesV2 anApiServicesV2() {
        return BASE_API_SERVICES_V2.build();
    }

    public static EndpointGroupServicesV2 anEndpointGroupServicesV2() {
        return BASE_ENDPOINT_GROUP_SERVICES_V2.build();
    }

    public static ServiceV4 aServiceV4() {
        return BASE_API_V4_SERVICE.build();
    }

    public static HealthCheckService aHealthCheckService() {
        return BASE_HEALTH_CHECK_SERVICE.build();
    }
}
