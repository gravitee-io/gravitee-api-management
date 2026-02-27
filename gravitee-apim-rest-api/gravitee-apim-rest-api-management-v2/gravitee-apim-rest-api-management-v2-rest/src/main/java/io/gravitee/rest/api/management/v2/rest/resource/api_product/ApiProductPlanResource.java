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

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.use_case.GetPlansUseCase;
import io.gravitee.apim.core.plan.use_case.PlanOperationsUseCase;
import io.gravitee.apim.core.plan.use_case.UpdateApiProductPlanUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiProductPlanMapper;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.UpdateGenericApiProductPlan;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.CustomLog;

@CustomLog
public class ApiProductPlanResource extends AbstractResource {

    private final ApiProductPlanMapper planMapper = ApiProductPlanMapper.INSTANCE;

    @Inject
    private UpdateApiProductPlanUseCase updateApiProductPlanUseCase;

    @Inject
    private GetPlansUseCase getPlansUseCase;

    @Inject
    private PlanOperationsUseCase planOperationsUseCase;

    @PathParam("apiProductId")
    private String apiProductId;

    private String DELETE = "DELETE";
    private String CLOSE = "CLOSE";
    private String PUBLISH = "PUBLISH";
    private String DEPRECATE = "DEPRECATE";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_PLAN, acls = { RolePermissionAction.READ }) })
    public Response getApiProductPlan(@PathParam("planId") String planId) {
        log.debug("Getting Plan {} for API Product {}", planId, apiProductId);
        var output = getPlansUseCase.execute(
            GetPlansUseCase.Input.forSingle(apiProductId, GenericPlanEntity.ReferenceType.API_PRODUCT.name(), planId)
        );
        var planOpt = output.plan();
        var planEntity = planOpt.orElse(null);
        if (
            planEntity != null &&
            planEntity.getReferenceType().equals(GenericPlanEntity.ReferenceType.API_PRODUCT) &&
            !planEntity.getReferenceId().equals(apiProductId)
        ) {
            log.debug("Plan {} not found for API Product {}", planId, apiProductId);
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }
        log.debug("Plan {} found for API Product {}", planId, apiProductId);
        return Response.ok(planMapper.mapGenericPlan(planEntity)).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response updateApiProductPlan(@PathParam("planId") String planId, @Valid @NotNull UpdateGenericApiProductPlan updatePlan) {
        log.debug("Updating Plan {} for API Product {}", planId, apiProductId);
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        var userDetails = getAuthenticatedUserDetails();
        var updatePlanEntity = planMapper.mapToPlanUpdates(updatePlan);
        updatePlanEntity.setId(planId);
        updatePlanEntity.setReferenceId(apiProductId);
        updatePlanEntity.setReferenceType(GenericPlanEntity.ReferenceType.API_PRODUCT);

        var output = updateApiProductPlanUseCase.execute(
            UpdateApiProductPlanUseCase.Input.builder()
                .planToUpdate(updatePlanEntity)
                .apiProductId(apiProductId)
                .auditInfo(
                    AuditInfo.builder()
                        .organizationId(executionContext.getOrganizationId())
                        .environmentId(executionContext.getEnvironmentId())
                        .actor(
                            AuditActor.builder()
                                .userId(userDetails.getUsername())
                                .userSource(userDetails.getSource())
                                .userSourceId(userDetails.getSourceId())
                                .build()
                        )
                        .build()
                )
                .build()
        );

        log.debug("Plan {} updated for API Product {}", planId, apiProductId);
        return Response.ok(planMapper.map(output.updated())).build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_PLAN, acls = { RolePermissionAction.DELETE }) })
    public Response deleteApiProductPlan(@PathParam("planId") String planId) {
        log.debug("Deleting Plan {} for API Product {}", planId, apiProductId);
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();
        planOperationsUseCase.execute(
            PlanOperationsUseCase.Input.builder()
                .planId(planId)
                .referenceId(apiProductId)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT.name())
                .operation(DELETE)
                .auditInfo(
                    AuditInfo.builder()
                        .organizationId(executionContext.getOrganizationId())
                        .environmentId(executionContext.getEnvironmentId())
                        .actor(
                            AuditActor.builder()
                                .userId(userDetails.getUsername())
                                .userSource(userDetails.getSource())
                                .userSourceId(userDetails.getSourceId())
                                .build()
                        )
                        .build()
                )
                .build()
        );
        log.debug("Plan {} deleted for API Product {}", planId, apiProductId);
        return Response.noContent().build();
    }

    @POST
    @Path("/_close")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response closeApiProductPlan(@PathParam("planId") String planId) {
        log.debug("Closing Plan {} for API Product {}", planId, apiProductId);
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();
        var output = planOperationsUseCase.execute(
            PlanOperationsUseCase.Input.builder()
                .planId(planId)
                .referenceId(apiProductId)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT.name())
                .operation(CLOSE)
                .auditInfo(
                    AuditInfo.builder()
                        .organizationId(executionContext.getOrganizationId())
                        .environmentId(executionContext.getEnvironmentId())
                        .actor(
                            AuditActor.builder()
                                .userId(userDetails.getUsername())
                                .userSource(userDetails.getSource())
                                .userSourceId(userDetails.getSourceId())
                                .build()
                        )
                        .build()
                )
                .build()
        );
        log.debug("Plan {} closed for API Product {}", planId, apiProductId);
        return Response.ok(planMapper.mapGenericPlan(output.plan())).build();
    }

    @POST
    @Path("/_publish")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response publishApiProductPlan(@PathParam("planId") String planId) {
        log.debug("Publishing Plan {} for API Product {}", planId, apiProductId);
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();
        var output = planOperationsUseCase.execute(
            PlanOperationsUseCase.Input.builder()
                .planId(planId)
                .referenceId(apiProductId)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT.name())
                .operation(PUBLISH)
                .auditInfo(
                    AuditInfo.builder()
                        .organizationId(executionContext.getOrganizationId())
                        .environmentId(executionContext.getEnvironmentId())
                        .actor(
                            AuditActor.builder()
                                .userId(userDetails.getUsername())
                                .userSource(userDetails.getSource())
                                .userSourceId(userDetails.getSourceId())
                                .build()
                        )
                        .build()
                )
                .build()
        );
        log.debug("Plan {} published for API Product {}", planId, apiProductId);
        return Response.ok(planMapper.mapGenericPlan(output.plan())).build();
    }

    @POST
    @Path("/_deprecate")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response deprecateApiProductPlan(@PathParam("planId") String planId) {
        log.debug("Deprecating Plan {} for API Product {}", planId, apiProductId);
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();
        var output = planOperationsUseCase.execute(
            PlanOperationsUseCase.Input.builder()
                .planId(planId)
                .referenceId(apiProductId)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT.name())
                .operation(DEPRECATE)
                .auditInfo(
                    AuditInfo.builder()
                        .organizationId(executionContext.getOrganizationId())
                        .environmentId(executionContext.getEnvironmentId())
                        .actor(
                            AuditActor.builder()
                                .userId(userDetails.getUsername())
                                .userSource(userDetails.getSource())
                                .userSourceId(userDetails.getSourceId())
                                .build()
                        )
                        .build()
                )
                .build()
        );
        log.debug("Plan {} deprecated for API Product {}", planId, apiProductId);
        return Response.ok(planMapper.mapGenericPlan(output.plan())).build();
    }

    private io.gravitee.rest.api.management.v2.rest.model.Error planNotFoundError(String plan) {
        log.debug("Building plan not found error for Plan {} (API Product {})", plan, apiProductId);
        return new Error()
            .httpStatus(Response.Status.NOT_FOUND.getStatusCode())
            .message("Plan [" + plan + "] cannot be found.")
            .putParametersItem("plan", plan)
            .technicalCode("plan.notFound");
    }
}
