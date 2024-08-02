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
package fixtures.core.model;

import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec.ApiCRDSpecBuilder;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import java.util.List;
import java.util.Map;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiCRDFixtures {

    private ApiCRDFixtures() {}

    public static final String API_ID = "api-id";
    public static final String API_CROSS_ID = "api-cross-id";
    public static final String API_NAME = "API Name";
    public static final String API_PATH = "/api-path";
    public static final String PLAN_NAME = "plan-name";
    public static final String PLAN_ID = "plan-id";

    public static ApiCRDSpecBuilder BASE_SPEC = ApiCRDSpec
        .builder()
        .id(API_ID)
        .crossId(API_CROSS_ID)
        .name(API_NAME)
        .listeners(List.of(HttpListener.builder().paths(List.of(new Path(API_PATH))).build()))
        .plans(Map.of(PLAN_NAME, PlanCRD.builder().name(PLAN_NAME).id(PLAN_ID).security(new PlanSecurity("key-less", "{}")).build()))
        .state("STARTED")
        .endpointGroups(
            List.of(
                EndpointGroup
                    .builder()
                    .name("default-group")
                    .type("http-proxy")
                    .sharedConfiguration("{}")
                    .endpoints(
                        List.of(
                            Endpoint
                                .builder()
                                .name("default-endpoint")
                                .type("http-proxy")
                                .inheritConfiguration(true)
                                .configuration("{\"target\":\"https://api.gravitee.io/echo\"}")
                                .build()
                        )
                    )
                    .build()
            )
        );

    public static ApiCRDSpec anApiCRD() {
        return BASE_SPEC.build();
    }
}
