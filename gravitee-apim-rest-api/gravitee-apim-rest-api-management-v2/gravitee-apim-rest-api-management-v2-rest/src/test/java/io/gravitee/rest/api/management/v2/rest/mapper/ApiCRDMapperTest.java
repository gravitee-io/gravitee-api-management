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
package io.gravitee.rest.api.management.v2.rest.mapper;

import fixtures.definition.FlowFixtures;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.management.v2.rest.model.ApiLifecycleState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiCRDMapperTest {

    @Test
    void should_map_to_rest_model_unpublished() {
        var restModel = ApiCRDMapper.INSTANCE.map(aCoreCRD().build());

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(restModel.getId()).isEqualTo("api-id");
            soft.assertThat(restModel.getName()).isEqualTo("My Api");
            soft.assertThat(restModel.getDescription()).isEqualTo("api-description");
            soft.assertThat(restModel.getCrossId()).isEqualTo("api-cross-id");
            soft.assertThat(restModel.getListeners()).hasSize(1);
            soft.assertThat(restModel.getEndpointGroups()).hasSize(1);
            soft.assertThat(restModel.getPlans()).hasSize(1);
            soft.assertThat(restModel.getPlans()).containsKey("keyless-key");
            soft.assertThat(restModel.getLifecycleState()).isEqualTo(ApiLifecycleState.UNPUBLISHED);
            soft.assertThat(restModel.getTags()).contains("tag");
        });
    }

    @Test
    void should_map_to_rest_model_published() {
        var restModel = ApiCRDMapper.INSTANCE.map(aCoreCRD().lifecycleState("PUBLISHED").build());

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(restModel.getId()).isEqualTo("api-id");
            soft.assertThat(restModel.getName()).isEqualTo("My Api");
            soft.assertThat(restModel.getDescription()).isEqualTo("api-description");
            soft.assertThat(restModel.getCrossId()).isEqualTo("api-cross-id");
            soft.assertThat(restModel.getListeners()).hasSize(1);
            soft.assertThat(restModel.getEndpointGroups()).hasSize(1);
            soft.assertThat(restModel.getPlans()).hasSize(1);
            soft.assertThat(restModel.getPlans()).containsKey("keyless-key");
            soft.assertThat(restModel.getLifecycleState()).isEqualTo(ApiLifecycleState.PUBLISHED);
            soft.assertThat(restModel.getTags()).contains("tag");
        });
    }

    private static ApiCRDSpec.ApiCRDSpecBuilder aCoreCRD() {
        return ApiCRDSpec
            .builder()
            .analytics(Analytics.builder().enabled(false).build())
            .crossId("api-cross-id")
            .description("api-description")
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
            )
            .flows(List.of())
            .id("api-id")
            .labels(Set.of("label-1"))
            .lifecycleState("CREATED")
            .listeners(
                List.of(
                    HttpListener
                        .builder()
                        .paths(List.of(Path.builder().path("/http_proxy").build()))
                        .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
                        .build()
                )
            )
            .name("My Api")
            .plans(
                Map.of(
                    "keyless-key",
                    PlanCRD
                        .builder()
                        .id("keyless-id")
                        .name("Keyless")
                        .security(PlanSecurity.builder().type("KEY_LESS").build())
                        .mode(PlanMode.STANDARD)
                        .validation(Plan.PlanValidationType.AUTO)
                        .status(PlanStatus.PUBLISHED)
                        .type(Plan.PlanType.API)
                        .flows(List.of(FlowFixtures.aSimpleFlowV4().toBuilder().name("plan-flow").build()))
                        .build()
                )
            )
            .properties(List.of(Property.builder().key("prop-key").value("prop-value").build()))
            .resources(List.of(Resource.builder().name("resource-name").type("resource-type").enabled(true).build()))
            .responseTemplates(Map.of("DEFAULT", Map.of("*.*", ResponseTemplate.builder().statusCode(200).build())))
            .state("STARTED")
            .tags(Set.of("tag"))
            .type("PROXY")
            .version("1.0.0")
            .visibility("PRIVATE");
    }
}
