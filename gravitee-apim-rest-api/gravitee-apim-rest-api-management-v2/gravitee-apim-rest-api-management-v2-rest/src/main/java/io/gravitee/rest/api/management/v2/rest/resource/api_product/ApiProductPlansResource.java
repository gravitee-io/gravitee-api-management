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

import static java.util.Comparator.comparingInt;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.use_case.api_product.CreateApiProductPlanUseCase;
import io.gravitee.apim.core.plan.use_case.api_product.UpdateApiProductPlanUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiProductPlanMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiProductPlansResponse;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiProductPlan;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import io.gravitee.rest.api.management.v2.rest.model.PlanStatus;
import io.gravitee.rest.api.management.v2.rest.model.UpdateGenericApiProductPlan;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiProductPlansResource extends AbstractResource {

    private final ApiProductPlanMapper planMapper = ApiProductPlanMapper.INSTANCE;

    @Inject
    private PlanService planService;

    @Inject
    private CreateApiProductPlanUseCase createProductPlanUseCase;

    @Inject
    private UpdateApiProductPlanUseCase updateApiProductPlanUseCase;

    @Inject
    private PlanSearchService planSearchService;

    @PathParam("apiProductId")
    private String apiProductId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    //TODO need to be changed to API_PRODUCT_PLAN once implemented
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.READ }) })
    public ApiProductPlansResponse getApiProductPlans(
        @QueryParam("statuses") @DefaultValue("PUBLISHED") final Set<PlanStatus> statuses,
        @QueryParam("securities") @Nonnull Set<PlanSecurityType> securities,
        @QueryParam("mode") PlanMode planMode,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        if (planMode == null) {
            planMode = PlanMode.STANDARD;
        }
        var planQuery = PlanQuery.builder()
            .referenceId(apiProductId)
            .referenceType(Plan.ReferenceType.API_PRODUCT.name())
            .securityType(
                securities
                    .stream()
                    .map(planSecurityType -> io.gravitee.rest.api.model.v4.plan.PlanSecurityType.valueOf(planSecurityType.name()))
                    .collect(Collectors.toList())
            )
            .status(
                statuses
                    .stream()
                    .map(planStatus -> io.gravitee.definition.model.v4.plan.PlanStatus.valueOf(planStatus.name()))
                    .collect(Collectors.toList())
            )
            .mode(planMode);

        Stream<GenericPlanEntity> plansStream = planSearchService
            .searchForApiProductPlans(GraviteeContext.getExecutionContext(), planQuery.build(), getAuthenticatedUser(), isAdmin())
            .stream()
            .sorted(comparingInt(GenericPlanEntity::getOrder))
            //TODO ask if reqd as we already have permission
            .map(this::filterSensitiveData);

        //TODO filter based on subscriptions

        List<GenericPlanEntity> plans = plansStream.toList();
        List<GenericPlanEntity> paginationData = computePaginationData(plans, paginationParam);

        return new ApiProductPlansResponse()
            .data(planMapper.convert(paginationData))
            .pagination(PaginationInfo.computePaginationInfo(plans.size(), paginationData.size(), paginationParam))
            .links(computePaginationLinks(plans.size(), paginationParam));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.CREATE }) })
    public Response createApiProductPlan(@Valid @NotNull CreateApiProductPlan createPlan) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();
        var output = createProductPlanUseCase.execute(
            new CreateApiProductPlanUseCase.Input(
                apiProductId,
                api -> planMapper.map(createPlan),
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
        );

        return Response.created(this.getLocationHeader(output.id())).entity(planMapper.map(output.plan())).build();
    }

    @GET
    @Path("/{planId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.READ }) })
    public Response getApiProductPlan(@PathParam("planId") String planId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findByIdForApiProduct(executionContext, planId, apiProductId);

        if (!planEntity.getApiId().equals(apiProductId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        return Response.ok(planMapper.mapGenericPlan(planEntity)).build();
    }

    @PUT
    @Path("/{planId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response updateApiProductPlan(@PathParam("planId") String planId, @Valid @NotNull UpdateGenericApiProductPlan updatePlan) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findByIdForApiProduct(executionContext, planId, apiProductId);
        if (planEntity.getReferenceType().equals(Plan.ReferenceType.API_PRODUCT) && !planEntity.getReferenceId().equals(apiProductId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        if (planEntity instanceof BasePlanEntity) {
            var userDetails = getAuthenticatedUserDetails();
            var updatePlanEntity = planMapper.mapToPlanUpdates(updatePlan);
            updatePlanEntity.setId(planId);
            updatePlanEntity.setReferenceId(apiProductId);
            updatePlanEntity.setReferenceType(Plan.ReferenceType.API_PRODUCT.name());

            var output = updateApiProductPlanUseCase.execute(
                new UpdateApiProductPlanUseCase.Input(
                    updatePlanEntity,
                    apiProductId,
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
            );

            return Response.ok(planMapper.map(output.updated())).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(planInvalid(planId)).build();
        }
    }

    @DELETE
    @Path("/{planId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.DELETE }) })
    public Response deleteApiProductPlan(@PathParam("planId") String planId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findByIdForApiProduct(executionContext, planId, apiProductId);
        if (planEntity.getReferenceType().equals(Plan.ReferenceType.API_PRODUCT) && !planEntity.getReferenceId().equals(apiProductId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        planService.delete(executionContext, planId);

        return Response.noContent().build();
    }

    @POST
    @Path("/{planId}/_close")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response closeApiProductPlan(@PathParam("planId") String planId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findByIdForApiProduct(executionContext, planId, apiProductId);

        if (planEntity.getReferenceType().equals(Plan.ReferenceType.API_PRODUCT) && !planEntity.getReferenceId().equals(apiProductId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        return Response.ok(planMapper.mapGenericPlan(planService.closePlanForApiProduct(executionContext, planId))).build();
    }

    @POST
    @Path("/{planId}/_publish")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response publishApiProductPlan(@PathParam("planId") String planId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findByIdForApiProduct(executionContext, planId, apiProductId);

        if (planEntity.getReferenceType().equals(Plan.ReferenceType.API_PRODUCT) && !planEntity.getReferenceId().equals(apiProductId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        return Response.ok(planMapper.map(planService.publish(executionContext, planId))).build();
    }

    @POST
    @Path("/{planId}/_deprecate")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response deprecateApiProductPlan(@PathParam("planId") String planId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findByIdForApiProduct(executionContext, planId, apiProductId);

        if (planEntity.getReferenceType().equals(Plan.ReferenceType.API_PRODUCT) && !planEntity.getReferenceId().equals(apiProductId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        return Response.ok(planMapper.map(planService.deprecate(executionContext, planId))).build();
    }

    private GenericPlanEntity filterSensitiveData(GenericPlanEntity entity) {
        if (
            hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_GATEWAY_DEFINITION,
                entity.getApiId(),
                RolePermissionAction.READ
            ) &&
            hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_PLAN, entity.getApiId(), RolePermissionAction.READ)
        ) {
            // Return complete information if user has permission.
            return entity;
        }

        PlanEntity filtered = new PlanEntity();

        filtered.setId(entity.getId());
        filtered.setCharacteristics(entity.getCharacteristics());
        filtered.setName(entity.getName());
        filtered.setDescription(entity.getDescription());
        filtered.setOrder(entity.getOrder());
        filtered.setMode(entity.getPlanMode());
        filtered.setSecurity(entity.getPlanSecurity());
        filtered.setType(filtered.getType());
        filtered.setValidation(filtered.getValidation());
        filtered.setCommentRequired(entity.isCommentRequired());
        filtered.setCommentMessage(entity.getCommentMessage());
        filtered.setGeneralConditions(entity.getGeneralConditions());
        filtered.setStatus(entity.getPlanStatus());

        return filtered;
    }

    private Error planNotFoundError(String plan) {
        return new Error()
            .httpStatus(Response.Status.NOT_FOUND.getStatusCode())
            .message("Plan [" + plan + "] cannot be found.")
            .putParametersItem("plan", plan)
            .technicalCode("plan.notFound");
    }

    private Error planInvalid(String plan) {
        return new Error()
            .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
            .message("Plan [" + plan + "] is not valid.")
            .putParametersItem("plan", plan)
            .technicalCode("plan.invalid");
    }
}
