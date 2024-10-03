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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import static io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY;
import static io.gravitee.repository.management.model.Plan.PlanSecurityType.OAUTH2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.NativeApi;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlanAppenderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    private PlanAppender cut;

    @BeforeEach
    public void beforeEach() {
        cut = new PlanAppender(objectMapper, planRepository, gatewayConfiguration);
    }

    @Nested
    class ApiV1Test {

        @Test
        void should_add_published_plans_for_v1_apis() throws TechnicalException {
            io.gravitee.definition.model.Api apiV1 = new io.gravitee.definition.model.Api();
            apiV1.setId("apiId");
            apiV1.setDefinitionVersion(DefinitionVersion.V1);
            io.gravitee.gateway.handlers.api.definition.Api reactableApi = new io.gravitee.gateway.handlers.api.definition.Api(apiV1);

            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(reactableApi).build();
            Plan plan = new Plan();
            plan.setId("planId");
            plan.setApi("apiId");
            plan.setStatus(Plan.Status.PUBLISHED);
            Plan plan2 = new Plan();
            plan2.setId("planId2");
            plan2.setApi("apiId");
            plan2.setStatus(Plan.Status.CLOSED);
            when(planRepository.findByApisAndEnvironments(List.of("apiId"), Set.of("env"))).thenReturn(List.of(plan, plan2));
            List<ApiReactorDeployable> appends = cut.appends(List.of(apiReactorDeployable), Set.of("env"));
            assertThat(appends).hasSize(1);
            assertThat(appends.get(0).subscribablePlans()).hasSize(1);
        }

        @Test
        void should_filter_v1_apis_without_plans() throws TechnicalException {
            io.gravitee.definition.model.Api apiV1 = new io.gravitee.definition.model.Api();
            apiV1.setId("apiId");
            apiV1.setDefinitionVersion(DefinitionVersion.V1);
            io.gravitee.gateway.handlers.api.definition.Api reactableApi = new io.gravitee.gateway.handlers.api.definition.Api(apiV1);

            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(reactableApi).build();
            when(planRepository.findByApisAndEnvironments(List.of("apiId"), Set.of("env"))).thenReturn(List.of());
            List<ApiReactorDeployable> appends = cut.appends(List.of(apiReactorDeployable), Set.of("env"));
            assertThat(appends).isEmpty();
        }
    }

    @Nested
    class ApiV2Test {

        @Test
        void should_return_v2_apis_with_plans() throws TechnicalException {
            io.gravitee.definition.model.Api apiV2 = new io.gravitee.definition.model.Api();
            apiV2.setId("apiId");
            apiV2.setDefinitionVersion(DefinitionVersion.V2);
            io.gravitee.definition.model.Plan plan = new io.gravitee.definition.model.Plan();
            plan.setId("planId");
            plan.setApi("apiId");
            plan.setSecurity(API_KEY.name());
            plan.setStatus("PUBLISHED");
            io.gravitee.definition.model.Plan plan2 = new io.gravitee.definition.model.Plan();
            plan2.setId("planId2");
            plan2.setApi("apiId");
            plan.setSecurity(OAUTH2.name());
            plan2.setStatus("CLOSED");
            apiV2.setPlans(List.of(plan, plan2));
            io.gravitee.gateway.handlers.api.definition.Api reactableApi = new io.gravitee.gateway.handlers.api.definition.Api(apiV2);

            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(reactableApi).build();
            List<ApiReactorDeployable> appends = cut.appends(List.of(apiReactorDeployable), Set.of("env"));
            assertThat(appends).hasSize(1);
            assertThat(appends.get(0).subscribablePlans()).hasSize(1);
        }

        @Test
        void should_filter_v2_apis_without_plans() throws TechnicalException {
            io.gravitee.definition.model.Api apiV2 = new io.gravitee.definition.model.Api();
            apiV2.setId("apiId");
            apiV2.setDefinitionVersion(DefinitionVersion.V2);
            io.gravitee.gateway.handlers.api.definition.Api reactableApi = new io.gravitee.gateway.handlers.api.definition.Api(apiV2);

            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(reactableApi).build();
            List<ApiReactorDeployable> appends = cut.appends(List.of(apiReactorDeployable), Set.of("env"));
            assertThat(appends).isEmpty();
        }
    }

    @Nested
    class ApiV4Test {

        @Test
        void should_return_v4_apis_with_plans() {
            io.gravitee.definition.model.v4.Api apiV4 = new io.gravitee.definition.model.v4.Api();
            apiV4.setId("apiId");
            apiV4.setDefinitionVersion(DefinitionVersion.V4);
            PlanSecurity planSecurity = new PlanSecurity();
            planSecurity.setType("api-key");
            io.gravitee.definition.model.v4.plan.Plan plan = io.gravitee.definition.model.v4.plan.Plan
                .builder()
                .id("planId")
                .security(planSecurity)
                .status(PlanStatus.PUBLISHED)
                .build();
            io.gravitee.definition.model.v4.plan.Plan plan2 = io.gravitee.definition.model.v4.plan.Plan
                .builder()
                .id("planId2")
                .security(planSecurity)
                .status(PlanStatus.CLOSED)
                .build();
            apiV4.setPlans(List.of(plan, plan2));
            Api reactableApi = new Api(apiV4);

            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(reactableApi).build();
            List<ApiReactorDeployable> appends = cut.appends(List.of(apiReactorDeployable), Set.of("env"));
            assertThat(appends).hasSize(1);
            assertThat(appends.get(0).subscribablePlans()).hasSize(1);
        }

        @Test
        void should_filter_v4_apis_without_plans() {
            io.gravitee.definition.model.v4.Api apiV4 = new io.gravitee.definition.model.v4.Api();
            apiV4.setId("apiId");
            apiV4.setDefinitionVersion(DefinitionVersion.V4);
            Api reactableApi = new Api(apiV4);

            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(reactableApi).build();
            List<ApiReactorDeployable> appends = cut.appends(List.of(apiReactorDeployable), Set.of("env"));
            assertThat(appends).isEmpty();
        }
    }

    @Nested
    class ApiV4NativeTest {

        @Test
        void should_return_v4_native_apis_with_plans() {
            when(gatewayConfiguration.hasMatchingTags(Set.of("matching"))).thenReturn(true);
            when(gatewayConfiguration.hasMatchingTags(Set.of("unmatching"))).thenReturn(false);
            final NativePlan.NativePlanBuilder<?, ?> securedPlanBuilder = NativePlan
                .builder()
                .security(PlanSecurity.builder().type("api-key").build());
            final NativePlan publishedPlanWithMatchingTag = securedPlanBuilder
                .id("plan-published-matching-tag")
                .status(PlanStatus.PUBLISHED)
                .tags(Set.of("matching"))
                .build();
            final NativePlan deprecatedPlanWithMatchingTag = securedPlanBuilder
                .id("plan-deprecated-matching-tag")
                .status(PlanStatus.DEPRECATED)
                .tags(Set.of("matching"))
                .build();
            final NativePlan publishedPlanWithNoTag = securedPlanBuilder.id("plan-published-no-tag").status(PlanStatus.PUBLISHED).build();
            final NativePlan deprecatedPlanWithNoTag = securedPlanBuilder
                .id("plan-deprecated-no-tag")
                .status(PlanStatus.DEPRECATED)
                .build();
            final NativeApi nativeApi = new NativeApi(
                io.gravitee.definition.model.v4.nativeapi.NativeApi
                    .builder()
                    .plans(
                        Map.of(
                            "plan-without-status-no-tag",
                            securedPlanBuilder.id("plan-without-status-no-tag").status(null).build(),
                            "plan-staging-no-tag",
                            securedPlanBuilder.id("plan-staging-no-tag").status(PlanStatus.STAGING).build(),
                            "plan-published-no-tag",
                            publishedPlanWithNoTag,
                            "plan-deprecated-no-tag",
                            deprecatedPlanWithNoTag,
                            "plan-closed-no-tag",
                            securedPlanBuilder.id("plan-closed-no-tag").status(PlanStatus.CLOSED).build(),
                            "plan-published-matching-tag",
                            publishedPlanWithMatchingTag,
                            "plan-deprecated-matching-tag",
                            deprecatedPlanWithMatchingTag,
                            "plan-published-invalid-matching-tag",
                            publishedPlanWithMatchingTag.toBuilder().tags(Set.of("unmatching")).build(),
                            "plan-deprecated-invalid-matching-tag",
                            deprecatedPlanWithMatchingTag.toBuilder().tags(Set.of("unmatching")).build()
                        )
                    )
                    .build()
            );

            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(nativeApi).build();
            List<ApiReactorDeployable> appends = cut.appends(List.of(apiReactorDeployable), Set.of("env"));
            assertThat(appends).hasSize(1);
            assertThat(appends.get(0).subscribablePlans())
                .hasSize(4)
                .contains(
                    publishedPlanWithMatchingTag.getId(),
                    deprecatedPlanWithMatchingTag.getId(),
                    publishedPlanWithNoTag.getId(),
                    deprecatedPlanWithNoTag.getId()
                );
        }

        @Test
        void should_filter_v4_native_apis_without_plans() {
            io.gravitee.definition.model.v4.nativeapi.NativeApi apiV4 = new io.gravitee.definition.model.v4.nativeapi.NativeApi();
            apiV4.setId("apiId");
            apiV4.setDefinitionVersion(DefinitionVersion.V4);
            NativeApi reactableApi = new NativeApi(apiV4);

            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(reactableApi).build();
            List<ApiReactorDeployable> appends = cut.appends(List.of(apiReactorDeployable), Set.of("env"));
            assertThat(appends).isEmpty();
        }
    }
}
