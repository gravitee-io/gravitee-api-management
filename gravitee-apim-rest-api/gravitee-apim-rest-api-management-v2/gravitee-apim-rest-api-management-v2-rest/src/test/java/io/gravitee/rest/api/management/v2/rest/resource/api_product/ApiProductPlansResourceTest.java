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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.use_case.api_product.CreateApiProductPlanUseCase;
import io.gravitee.apim.core.plan.use_case.api_product.GetApiProductPlansUseCase;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.management.v2.rest.model.ApiProductPlansResponse;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiProductPlan;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApiProductPlansResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    @Inject
    private GetApiProductPlansUseCase getApiProductPlansUseCase;

    @Inject
    private CreateApiProductPlanUseCase createApiProductPlanUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/api-products/" + API_PRODUCT_ID + "/plans";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(getApiProductPlansUseCase, createApiProductPlanUseCase);
    }

    @Nested
    class GetApiProductPlansTest {

        @Test
        void should_return_plans_list() {
            Plan plan1 = Plan.builder()
                .id("plan-1")
                .name("My plan 1")
                .order(1)
                .referenceType(io.gravitee.rest.api.model.v4.plan.GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId(API_PRODUCT_ID)
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id("plan-1")
                        .name("My plan 1")
                        .security(PlanSecurity.builder().type("API_KEY").build())
                        .mode(PlanMode.STANDARD)
                        .status(PlanStatus.PUBLISHED)
                        .build()
                )
                .build();

            Plan plan2 = Plan.builder()
                .id("plan-2")
                .name("My plan 2")
                .order(2)
                .referenceType(io.gravitee.rest.api.model.v4.plan.GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId(API_PRODUCT_ID)
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id("plan-2")
                        .name("My plan 2")
                        .security(PlanSecurity.builder().type("API_KEY").build())
                        .mode(PlanMode.STANDARD)
                        .status(PlanStatus.PUBLISHED)
                        .build()
                )
                .build();

            when(getApiProductPlansUseCase.execute(any())).thenReturn(GetApiProductPlansUseCase.Output.multiple(List.of(plan1, plan2)));

            final Response response = rootTarget().queryParam("securities", "API_KEY").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);

            ApiProductPlansResponse result = response.readEntity(ApiProductPlansResponse.class);
            assertAll(
                () -> assertThat(result.getData()).hasSize(2),
                () -> assertThat(result.getPagination()).isNotNull(),
                () -> assertThat(result.getLinks()).isNotNull()
            );
        }

        @Test
        void should_return_empty_list_when_no_plans() {
            when(getApiProductPlansUseCase.execute(any())).thenReturn(GetApiProductPlansUseCase.Output.multiple(List.of()));

            final Response response = rootTarget().queryParam("securities", "API_KEY").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            ApiProductPlansResponse result = response.readEntity(ApiProductPlansResponse.class);
            assertThat(result.getData()).isEmpty();
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_PLAN, API_PRODUCT_ID, RolePermissionAction.READ, () ->
                rootTarget().queryParam("securities", "API_KEY").request().get()
            );
        }

        @Test
        void should_pass_query_params_to_use_case() {
            when(getApiProductPlansUseCase.execute(any())).thenReturn(GetApiProductPlansUseCase.Output.multiple(List.of()));

            rootTarget()
                .queryParam("securities", "API_KEY")
                .queryParam("statuses", "PUBLISHED", "STAGING")
                .queryParam("mode", "STANDARD")
                .request()
                .get();

            ArgumentCaptor<GetApiProductPlansUseCase.Input> captor = ArgumentCaptor.forClass(GetApiProductPlansUseCase.Input.class);
            verify(getApiProductPlansUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(captor.getValue().apiProductId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(captor.getValue().query()).isNotNull();
            });
        }
    }

    @Nested
    class CreateApiProductPlanTest {

        @Test
        void should_create_api_product_plan() {
            String createdPlanId = "new-plan-id";
            Plan createdPlan = Plan.builder()
                .id(createdPlanId)
                .name("My plan")
                .referenceId(API_PRODUCT_ID)
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id(createdPlanId)
                        .name("My plan")
                        .security(PlanSecurity.builder().type("API_KEY").build())
                        .mode(PlanMode.STANDARD)
                        .status(PlanStatus.STAGING)
                        .build()
                )
                .build();
            when(createApiProductPlanUseCase.execute(any())).thenReturn(new CreateApiProductPlanUseCase.Output(createdPlanId, createdPlan));

            CreateApiProductPlan createPayload = new CreateApiProductPlan();
            createPayload.setName("My plan");
            createPayload.setDescription("Description");
            createPayload.setSecurity(new io.gravitee.rest.api.management.v2.rest.model.PlanSecurity().type(PlanSecurityType.API_KEY));
            Response response = rootTarget().request().post(json(createPayload));

            assertThat(response.getStatus()).isEqualTo(CREATED_201);
            assertThat(response.getLocation()).isNotNull();
            assertThat(response.getLocation().getPath()).contains(createdPlanId);

            ArgumentCaptor<CreateApiProductPlanUseCase.Input> captor = ArgumentCaptor.forClass(CreateApiProductPlanUseCase.Input.class);
            verify(createApiProductPlanUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(captor.getValue().apiProductId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(captor.getValue().auditInfo()).isNotNull();
            });
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_PLAN, API_PRODUCT_ID, RolePermissionAction.CREATE, () ->
                rootTarget().request().post(json(new CreateApiProductPlan()))
            );
        }

        @Test
        void should_return_400_if_execute_fails_with_invalid_data_exception() {
            when(createApiProductPlanUseCase.execute(any())).thenThrow(new InvalidDataException("Name is required."));

            CreateApiProductPlan createPayload = new CreateApiProductPlan();
            createPayload.setSecurity(new io.gravitee.rest.api.management.v2.rest.model.PlanSecurity().type(PlanSecurityType.API_KEY));
            try (Response response = rootTarget().request().post(json(createPayload))) {
                assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            }
        }

        @Test
        void should_return_400_if_missing_body() {
            try (Response response = rootTarget().request().post(json(""))) {
                assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            }
        }

        @Test
        void should_return_400_when_creating_plan_with_keyless_security() {
            CreateApiProductPlan createPayload = new CreateApiProductPlan();
            createPayload.setName("Keyless plan");
            createPayload.setDescription("Keyless plan description");
            io.gravitee.rest.api.management.v2.rest.model.PlanSecurity planSecurity =
                new io.gravitee.rest.api.management.v2.rest.model.PlanSecurity();
            planSecurity.setType(PlanSecurityType.KEY_LESS);
            createPayload.setSecurity(planSecurity);

            Response response = rootTarget().request().post(json(createPayload));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            Error error = response.readEntity(Error.class);
            assertThat(error.getMessage()).isEqualTo("Plan Security Type KeyLess is not allowed.");
            assertThat(error.getTechnicalCode()).isEqualTo("planSecurity.invalid");
            verify(createApiProductPlanUseCase, never()).execute(any());
        }
    }
}
