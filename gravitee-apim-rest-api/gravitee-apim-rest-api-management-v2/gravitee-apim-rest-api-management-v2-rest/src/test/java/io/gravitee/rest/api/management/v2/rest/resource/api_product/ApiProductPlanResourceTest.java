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
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.PlanFixtures;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.use_case.GetPlansUseCase;
import io.gravitee.apim.core.plan.use_case.PlanOperationsUseCase;
import io.gravitee.apim.core.plan.use_case.UpdateApiProductPlanUseCase;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.management.v2.rest.model.UpdateGenericApiProductPlan;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApiProductPlanResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";
    private static final String PLAN_ID = "plan-id";

    @Inject
    private GetPlansUseCase getPlansUseCase;

    @Inject
    private UpdateApiProductPlanUseCase updateApiProductPlanUseCase;

    @Inject
    private PlanOperationsUseCase planOperationsUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/api-products/" + API_PRODUCT_ID + "/plans/" + PLAN_ID;
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
        reset(getPlansUseCase, updateApiProductPlanUseCase, planOperationsUseCase);
    }

    @Nested
    class GetApiProductPlanTest {

        @Test
        void should_get_api_product_plan_by_id() {
            Plan plan = Plan.builder()
                .id(PLAN_ID)
                .name("My plan")
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId(API_PRODUCT_ID)
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id(PLAN_ID)
                        .name("My plan")
                        .security(PlanSecurity.builder().type("API_KEY").build())
                        .mode(PlanMode.STANDARD)
                        .status(PlanStatus.PUBLISHED)
                        .build()
                )
                .build();

            when(getPlansUseCase.execute(any())).thenReturn(GetPlansUseCase.Output.single(Optional.of(plan)));

            final Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);

            var result = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.BasePlan.class);

            assertAll(() -> assertThat(result.getId()).isEqualTo(PLAN_ID), () -> assertThat(result.getName()).isEqualTo(plan.getName()));
        }

        @Test
        void should_return_404_when_plan_belongs_to_another_api_product() {
            Plan plan = Plan.builder()
                .id(PLAN_ID)
                .name("My plan")
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId("other-api-product-id")
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id(PLAN_ID)
                        .name("My plan")
                        .security(PlanSecurity.builder().type("API_KEY").build())
                        .mode(PlanMode.STANDARD)
                        .status(PlanStatus.PUBLISHED)
                        .build()
                )
                .build();

            when(getPlansUseCase.execute(any())).thenReturn(GetPlansUseCase.Output.single(Optional.of(plan)));

            final Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);

            var error = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Error.class);
            assertThat(error.getMessage()).isEqualTo("Plan [" + PLAN_ID + "] cannot be found.");
            assertThat(error.getTechnicalCode()).isEqualTo("plan.notFound");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_PLAN, API_PRODUCT_ID, RolePermissionAction.READ, () -> rootTarget().request().get());
        }
    }

    @Nested
    class UpdateApiProductPlanTest {

        @Test
        void should_update_api_product_plan() {
            Plan updatedPlan = Plan.builder()
                .id(PLAN_ID)
                .name("Updated Plan")
                .description("Updated description")
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id(PLAN_ID)
                        .name("Updated Plan")
                        .security(PlanSecurity.builder().type("API_KEY").build())
                        .mode(PlanMode.STANDARD)
                        .status(PlanStatus.PUBLISHED)
                        .build()
                )
                .build();

            when(updateApiProductPlanUseCase.execute(any())).thenReturn(new UpdateApiProductPlanUseCase.Output(updatedPlan));

            var updatePayload = new UpdateGenericApiProductPlan();
            updatePayload.setName("Updated Plan");
            updatePayload.setDescription("Updated description");

            io.gravitee.rest.api.management.v2.rest.model.BasePlan updatedResponse;
            try (Response response = rootTarget().request().put(json(updatePayload))) {
                assertThat(response.getStatus()).isEqualTo(OK_200);
                updatedResponse = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.BasePlan.class);
            }

            assertAll(
                () -> assertThat(updatedResponse.getId()).isEqualTo(PLAN_ID),
                () -> assertThat(updatedResponse.getName()).isEqualTo("Updated Plan"),
                () -> assertThat(updatedResponse.getDescription()).isEqualTo("Updated description")
            );

            var captor = ArgumentCaptor.forClass(UpdateApiProductPlanUseCase.Input.class);
            verify(updateApiProductPlanUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                var input = captor.getValue();
                soft.assertThat(input.apiProductId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(input.planToUpdate().getId()).isEqualTo(PLAN_ID);
                soft.assertThat(input.planToUpdate().getReferenceId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(input.auditInfo()).isNotNull();
            });
        }

        @Test
        void should_return_400_if_execute_fails_with_invalid_data_exception() {
            when(updateApiProductPlanUseCase.execute(any())).thenThrow(new InvalidDataException("Name is required."));

            var updatePayload = new UpdateGenericApiProductPlan();

            try (Response response = rootTarget().request().put(json(updatePayload))) {
                assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            }
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_PLAN, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().request().put(json(new UpdateGenericApiProductPlan()))
            );
        }
    }

    @Nested
    class DeleteApiProductPlanTest {

        @Test
        void should_delete_api_product_plan() {
            final Response response = rootTarget().request().delete();

            MAPIAssertions.assertThat(response).hasStatus(NO_CONTENT_204);

            var captor = ArgumentCaptor.forClass(PlanOperationsUseCase.Input.class);
            verify(planOperationsUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                var input = captor.getValue();
                soft.assertThat(input.planId()).isEqualTo(PLAN_ID);
                soft.assertThat(input.referenceId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(input.referenceType()).isEqualTo("API_PRODUCT");
            });
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_PLAN, API_PRODUCT_ID, RolePermissionAction.DELETE, () ->
                rootTarget().request().delete()
            );
        }
    }

    @Nested
    class CloseApiProductPlanTest {

        @Test
        void should_close_api_product_plan() {
            Plan plan = Plan.builder()
                .id(PLAN_ID)
                .name("My plan")
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId(API_PRODUCT_ID)
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id(PLAN_ID)
                        .name("My plan")
                        .security(PlanSecurity.builder().type("API_KEY").build())
                        .mode(PlanMode.STANDARD)
                        .status(PlanStatus.CLOSED)
                        .build()
                )
                .build();

            when(planOperationsUseCase.execute(any())).thenReturn(new PlanOperationsUseCase.Output(plan));

            final Response response = rootTarget().path("_close").request().post(null);

            assertThat(response.getStatus()).isEqualTo(OK_200);

            var result = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.BasePlan.class);
            assertThat(result.getId()).isEqualTo(PLAN_ID);

            var captor = ArgumentCaptor.forClass(PlanOperationsUseCase.Input.class);
            verify(planOperationsUseCase).execute(captor.capture());
            assertThat(captor.getValue().planId()).isEqualTo(PLAN_ID);
            assertThat(captor.getValue().referenceId()).isEqualTo(API_PRODUCT_ID);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_PLAN, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("_close").request().post(null)
            );
        }
    }

    @Nested
    class PublishApiProductPlanTest {

        @Test
        void should_publish_api_product_plan() {
            Plan plan = Plan.builder()
                .id(PLAN_ID)
                .name("My plan")
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId(API_PRODUCT_ID)
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id(PLAN_ID)
                        .name("My plan")
                        .security(PlanSecurity.builder().type("API_KEY").build())
                        .mode(PlanMode.STANDARD)
                        .status(PlanStatus.PUBLISHED)
                        .build()
                )
                .build();

            when(planOperationsUseCase.execute(any())).thenReturn(new PlanOperationsUseCase.Output(plan));

            final Response response = rootTarget().path("_publish").request().post(null);

            assertThat(response.getStatus()).isEqualTo(OK_200);

            var result = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.BasePlan.class);
            assertThat(result.getId()).isEqualTo(PLAN_ID);

            var captor = ArgumentCaptor.forClass(PlanOperationsUseCase.Input.class);
            verify(planOperationsUseCase).execute(captor.capture());
            assertThat(captor.getValue().planId()).isEqualTo(PLAN_ID);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_PLAN, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("_publish").request().post(null)
            );
        }
    }

    @Nested
    class DeprecateApiProductPlanTest {

        @Test
        void should_deprecate_api_product_plan() {
            Plan plan = Plan.builder()
                .id(PLAN_ID)
                .name("My plan")
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId(API_PRODUCT_ID)
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id(PLAN_ID)
                        .name("My plan")
                        .security(PlanSecurity.builder().type("API_KEY").build())
                        .mode(PlanMode.STANDARD)
                        .status(PlanStatus.DEPRECATED)
                        .build()
                )
                .build();

            when(planOperationsUseCase.execute(any())).thenReturn(new PlanOperationsUseCase.Output(plan));

            final Response response = rootTarget().path("_deprecate").request().post(null);

            assertThat(response.getStatus()).isEqualTo(OK_200);

            var result = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.BasePlan.class);
            assertThat(result.getId()).isEqualTo(PLAN_ID);

            var captor = ArgumentCaptor.forClass(PlanOperationsUseCase.Input.class);
            verify(planOperationsUseCase).execute(captor.capture());
            assertThat(captor.getValue().planId()).isEqualTo(PLAN_ID);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_PLAN, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("_deprecate").request().post(null)
            );
        }
    }
}
