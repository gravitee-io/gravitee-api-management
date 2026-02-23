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
package io.gravitee.apim.core.api_product.use_case;

import static fixtures.core.model.RoleFixtures.apiProductPrimaryOwnerRoleId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.AbstractUseCaseTest;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GetApiProductsUseCaseTest extends AbstractUseCaseTest {

    private static final Instant DEPLOYED_AT = Instant.parse("2024-01-10T10:00:00Z");
    private static final Instant BEFORE_DEPLOY = Instant.parse("2024-01-09T10:00:00Z");
    private static final Instant AFTER_DEPLOY = Instant.parse("2024-01-11T10:00:00Z");

    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private final ObjectMapper objectMapper = GraviteeJacksonMapper.getInstance();

    private GetApiProductsUseCase getApiProductsUseCase;

    @BeforeEach
    void setUp() {
        userCrudService.initWith(
            List.of(
                io.gravitee.apim.core.user.model.BaseUserEntity.builder()
                    .id(USER_ID)
                    .email("user@example.com")
                    .firstname("Test")
                    .lastname("User")
                    .build()
            )
        );

        roleQueryService.resetSystemRoles(ORG_ID);

        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiProductPrimaryOwnerDomainService = new ApiProductPrimaryOwnerDomainService(
            auditService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );

        getApiProductsUseCase = new GetApiProductsUseCase(
            apiProductQueryService,
            apiProductPrimaryOwnerDomainService,
            eventLatestQueryService,
            planQueryService,
            objectMapper
        );
    }

    @AfterEach
    void tearDown() {
        apiProductQueryService.reset();
        eventLatestQueryService.reset();
        planQueryService.reset();
        membershipCrudService.reset();
        userCrudService.reset();
    }

    @Test
    void should_return_api_product_by_id() {
        ApiProduct product = ApiProduct.builder().id("api-product-id").name("Product 1").environmentId(ENV_ID).build();
        apiProductQueryService.initWith(List.of(product));

        membershipCrudService.initWith(
            List.of(
                Membership.builder()
                    .id("membership-id")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("api-product-id")
                    .roleId(apiProductPrimaryOwnerRoleId(ORG_ID))
                    .source("system")
                    .build()
            )
        );

        var input = GetApiProductsUseCase.Input.of(ENV_ID, "api-product-id", ORG_ID);
        var output = getApiProductsUseCase.execute(input);
        assertAll(
            () -> assertThat(output.apiProduct().get().getId()).isEqualTo("api-product-id"),
            () -> assertThat(output.apiProduct().get().getName()).isEqualTo("Product 1"),
            () -> assertThat(output.apiProduct().get().getPrimaryOwner()).isNotNull(),
            () -> assertThat(output.apiProduct().get().getPrimaryOwner().id()).isEqualTo(USER_ID),
            () -> assertThat(output.apiProduct().get().getPrimaryOwner().displayName()).isEqualTo("Test User")
        );
    }

    @Test
    void should_return_api_products_by_environment_id() {
        ApiProduct product1 = ApiProduct.builder().id("id1").name("P1").environmentId(ENV_ID).build();
        ApiProduct product2 = ApiProduct.builder().id("id2").name("P2").environmentId(ENV_ID).build();
        apiProductQueryService.initWith(List.of(product1, product2));

        membershipCrudService.initWith(
            List.of(
                Membership.builder()
                    .id("membership-1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("id1")
                    .roleId(apiProductPrimaryOwnerRoleId(ORG_ID))
                    .source("system")
                    .build(),
                Membership.builder()
                    .id("membership-2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("id2")
                    .roleId(apiProductPrimaryOwnerRoleId(ORG_ID))
                    .source("system")
                    .build()
            )
        );

        var input = GetApiProductsUseCase.Input.of(ENV_ID, ORG_ID);
        var output = getApiProductsUseCase.execute(input);
        Set<ApiProduct> products = output.apiProducts();
        assertThat(products).hasSize(2);
        assertThat(products).extracting(ApiProduct::getId).containsExactlyInAnyOrder("id1", "id2");
        assertThat(products).allMatch(product -> product.getPrimaryOwner() != null);
        assertThat(products).allMatch(product -> product.getPrimaryOwner().id().equals(USER_ID));
    }

    @Test
    void should_throw_exception_if_environment_id_missing() {
        var input = GetApiProductsUseCase.Input.of(null, null, ORG_ID);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> getApiProductsUseCase.execute(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("environmentId must be provided");
    }

    @Nested
    class DeploymentState {

        private static final String API_PRODUCT_ID = "api-product-id";

        @Test
        void should_be_need_redeploy_when_never_deployed() {
            apiProductQueryService.initWith(List.of(ApiProduct.builder().id(API_PRODUCT_ID).name("Product").environmentId(ENV_ID).build()));

            var output = getApiProductsUseCase.execute(GetApiProductsUseCase.Input.of(ENV_ID, API_PRODUCT_ID, ORG_ID));

            assertThat(output.apiProduct().get().getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
        }

        @Test
        void should_be_deployed_when_product_not_modified_after_last_deploy_and_no_plan_changes() throws Exception {
            ApiProduct product = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .name("Product")
                .environmentId(ENV_ID)
                .updatedAt(BEFORE_DEPLOY.atZone(ZoneId.systemDefault()))
                .build();
            apiProductQueryService.initWith(List.of(product));
            eventLatestQueryService.initWith(List.of(aDeployApiProductEvent(API_PRODUCT_ID, DEPLOYED_AT, product)));
            planQueryService.initWith(List.of(aPublishedApiProductPlan(API_PRODUCT_ID, BEFORE_DEPLOY)));

            var output = getApiProductsUseCase.execute(GetApiProductsUseCase.Input.of(ENV_ID, API_PRODUCT_ID, ORG_ID));

            assertThat(output.apiProduct().get().getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
        }

        @Test
        void should_be_need_redeploy_when_list_of_apis_in_product_changed() throws Exception {
            ApiProduct currentProduct = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .name("Product")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1", "api-2"))
                .build();
            apiProductQueryService.initWith(List.of(currentProduct));
            ApiProduct deployedProduct = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .name("Product")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1"))
                .build();
            eventLatestQueryService.initWith(List.of(aDeployApiProductEvent(API_PRODUCT_ID, DEPLOYED_AT, deployedProduct)));

            var output = getApiProductsUseCase.execute(GetApiProductsUseCase.Input.of(ENV_ID, API_PRODUCT_ID, ORG_ID));

            assertThat(output.apiProduct().get().getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
        }

        @Test
        void should_be_need_redeploy_when_plan_published_after_last_deploy() throws Exception {
            ApiProduct product = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .name("Product")
                .environmentId(ENV_ID)
                .updatedAt(BEFORE_DEPLOY.atZone(ZoneId.systemDefault()))
                .build();
            apiProductQueryService.initWith(List.of(product));
            eventLatestQueryService.initWith(List.of(aDeployApiProductEvent(API_PRODUCT_ID, DEPLOYED_AT, product)));
            planQueryService.initWith(List.of(aPublishedApiProductPlan(API_PRODUCT_ID, AFTER_DEPLOY)));

            var output = getApiProductsUseCase.execute(GetApiProductsUseCase.Input.of(ENV_ID, API_PRODUCT_ID, ORG_ID));

            assertThat(output.apiProduct().get().getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.NEED_REDEPLOY);
        }

        @Test
        void should_be_deployed_when_only_staging_plan_modified_after_last_deploy() throws Exception {
            ApiProduct product = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .name("Product")
                .environmentId(ENV_ID)
                .updatedAt(BEFORE_DEPLOY.atZone(ZoneId.systemDefault()))
                .build();
            apiProductQueryService.initWith(List.of(product));
            eventLatestQueryService.initWith(List.of(aDeployApiProductEvent(API_PRODUCT_ID, DEPLOYED_AT, product)));
            planQueryService.initWith(List.of(aStagingApiProductPlan(API_PRODUCT_ID, AFTER_DEPLOY)));

            var output = getApiProductsUseCase.execute(GetApiProductsUseCase.Input.of(ENV_ID, API_PRODUCT_ID, ORG_ID));

            assertThat(output.apiProduct().get().getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
        }

        @Test
        void should_compute_deployment_state_when_listing_products_by_environment() throws Exception {
            ApiProduct product = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .name("Product")
                .environmentId(ENV_ID)
                .updatedAt(BEFORE_DEPLOY.atZone(ZoneId.systemDefault()))
                .build();
            apiProductQueryService.initWith(List.of(product));
            eventLatestQueryService.initWith(List.of(aDeployApiProductEvent(API_PRODUCT_ID, DEPLOYED_AT, product)));
            planQueryService.initWith(List.of(aPublishedApiProductPlan(API_PRODUCT_ID, BEFORE_DEPLOY)));

            var output = getApiProductsUseCase.execute(GetApiProductsUseCase.Input.of(ENV_ID, ORG_ID));

            assertThat(output.apiProducts())
                .extracting(ApiProduct::getDeploymentState)
                .containsExactly(ApiProduct.DeploymentState.DEPLOYED);
        }

        @Test
        void should_be_need_redeploy_for_listed_product_when_plan_published_after_last_deploy() throws Exception {
            ApiProduct product = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .name("Product")
                .environmentId(ENV_ID)
                .updatedAt(BEFORE_DEPLOY.atZone(ZoneId.systemDefault()))
                .build();
            apiProductQueryService.initWith(List.of(product));
            eventLatestQueryService.initWith(List.of(aDeployApiProductEvent(API_PRODUCT_ID, DEPLOYED_AT, product)));
            planQueryService.initWith(List.of(aPublishedApiProductPlan(API_PRODUCT_ID, AFTER_DEPLOY)));

            var output = getApiProductsUseCase.execute(GetApiProductsUseCase.Input.of(ENV_ID, ORG_ID));

            assertThat(output.apiProducts())
                .extracting(ApiProduct::getDeploymentState)
                .containsExactly(ApiProduct.DeploymentState.NEED_REDEPLOY);
        }

        @Test
        void should_be_deployed_when_only_name_description_version_changed() throws Exception {
            ApiProduct currentProduct = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .name("Product v2")
                .description("New description")
                .version("2.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1"))
                .updatedAt(DEPLOYED_AT.atZone(ZoneId.systemDefault()))
                .build();
            apiProductQueryService.initWith(List.of(currentProduct));
            ApiProduct deployedProduct = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .name("Product v1")
                .description("Old description")
                .version("1.0")
                .environmentId(ENV_ID)
                .apiIds(Set.of("api-1"))
                .build();
            eventLatestQueryService.initWith(List.of(aDeployApiProductEvent(API_PRODUCT_ID, DEPLOYED_AT, deployedProduct)));
            planQueryService.initWith(List.of(aPublishedApiProductPlan(API_PRODUCT_ID, BEFORE_DEPLOY)));

            var output = getApiProductsUseCase.execute(GetApiProductsUseCase.Input.of(ENV_ID, API_PRODUCT_ID, ORG_ID));

            assertThat(output.apiProduct().get().getDeploymentState()).isEqualTo(ApiProduct.DeploymentState.DEPLOYED);
        }

        private Event aDeployApiProductEvent(String apiProductId, Instant updatedAt, ApiProduct deployedPayload) throws Exception {
            Event.EventBuilder builder = Event.builder()
                .id("event-" + apiProductId)
                .type(EventType.DEPLOY_API_PRODUCT)
                .properties(
                    new EnumMap<>(Map.of(Event.EventProperties.API_PRODUCT_ID, apiProductId, Event.EventProperties.USER, "user-id"))
                )
                .createdAt(updatedAt.atZone(ZoneId.systemDefault()))
                .updatedAt(updatedAt.atZone(ZoneId.systemDefault()));
            if (deployedPayload != null) {
                builder.payload(objectMapper.writeValueAsString(deployedPayload));
            }
            return builder.build();
        }

        private Plan aPublishedApiProductPlan(String apiProductId, Instant needRedeployAt) {
            return Plan.builder()
                .id("plan-" + apiProductId)
                .referenceId(apiProductId)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .definitionVersion(DefinitionVersion.V4)
                .apiType(ApiType.PROXY)
                .planDefinitionHttpV4(
                    fixtures.definition.PlanFixtures.HttpV4Definition.anApiKeyV4().toBuilder().status(PlanStatus.PUBLISHED).build()
                )
                .needRedeployAt(Date.from(needRedeployAt))
                .build();
        }

        private Plan aStagingApiProductPlan(String apiProductId, Instant needRedeployAt) {
            return Plan.builder()
                .id("plan-staging-" + apiProductId)
                .referenceId(apiProductId)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .definitionVersion(DefinitionVersion.V4)
                .apiType(ApiType.PROXY)
                .planDefinitionHttpV4(
                    fixtures.definition.PlanFixtures.HttpV4Definition.anApiKeyV4().toBuilder().status(PlanStatus.STAGING).build()
                )
                .needRedeployAt(Date.from(needRedeployAt))
                .build();
        }
    }
}
