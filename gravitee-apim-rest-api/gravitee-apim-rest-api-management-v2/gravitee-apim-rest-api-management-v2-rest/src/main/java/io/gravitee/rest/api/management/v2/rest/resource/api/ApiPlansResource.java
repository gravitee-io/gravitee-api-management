/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static java.util.Comparator.comparingInt;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PlanMapper;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import io.gravitee.rest.api.management.v2.rest.model.PlanStatus;
import io.gravitee.rest.api.management.v2.rest.model.PlansResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.management.v2.rest.security.Permission;
import io.gravitee.rest.api.management.v2.rest.security.Permissions;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.*;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/environments/{envId}/apis/{apiId}/plans")
public class ApiPlansResource extends AbstractResource {

    @Inject
    private PlanService planService;

    @Inject
    private PlanSearchService planSearchService;

    @Inject
    private GroupService groupService;

    @Context
    private ResourceContext resourceContext;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("apiId")
    private String apiId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions(
        {
            @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.READ }),
            @Permission(value = RolePermission.API_LOG, acls = { RolePermissionAction.READ }),
        }
    )
    public PlansResponse getApiPlans(
        @QueryParam("status") @DefaultValue("PUBLISHED") final Set<PlanStatus> planStatusParamList,
        @QueryParam("security") final Set<PlanSecurityType> planSecurityTypeParamList,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        var planQuery = PlanQuery
            .builder()
            .apiId(apiId)
            .securityType(
                planSecurityTypeParamList
                    .stream()
                    .map(planSecurityType -> io.gravitee.rest.api.model.v4.plan.PlanSecurityType.valueOf(planSecurityType.name()))
                    .collect(Collectors.toList())
            )
            .status(
                planStatusParamList
                    .stream()
                    .map(planStatus -> io.gravitee.definition.model.v4.plan.PlanStatus.valueOf(planStatus.name()))
                    .collect(Collectors.toList())
            );

        List<GenericPlanEntity> plans = planSearchService
            .search(GraviteeContext.getExecutionContext(), planQuery.build(), getAuthenticatedUser(), isAdmin())
            .stream()
            .sorted(comparingInt(GenericPlanEntity::getOrder))
            .map(this::filterSensitiveData)
            .collect(Collectors.toList());

        List<GenericPlanEntity> paginationData = computePaginationData(plans, paginationParam);

        return new PlansResponse()
            .data(PlanMapper.INSTANCE.convert(paginationData))
            .pagination(computePaginationInfo(plans.size(), paginationData.size(), paginationParam))
            .links(computePaginationLinks(plans.size(), paginationParam));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.CREATE }) })
    public Response createApiPlan(@Valid @NotNull NewPlanEntity newPlanEntity) {
        newPlanEntity.setApiId(apiId);
        newPlanEntity.setType(PlanType.API);

        PlanEntity planEntity = planService.create(GraviteeContext.getExecutionContext(), newPlanEntity);

        return Response.created(this.getLocationHeader(planEntity.getId())).entity(PlanMapper.INSTANCE.convert(planEntity)).build();
    }

    @GET
    @Path("/{plan}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.READ }) })
    public Response getApiPlan(@PathParam("plan") String plan) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findById(executionContext, plan);

        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(plan)).build();
        }

        return Response.ok(PlanMapper.INSTANCE.convert(planEntity)).build();
    }

    @PUT
    @Path("/{plan}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response updateApiPlan(@PathParam("plan") String plan, @Valid @NotNull UpdatePlanEntity updatePlanEntity) {
        if (updatePlanEntity.getId() != null && !plan.equals(updatePlanEntity.getId())) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("'plan' parameter does not correspond to the plan to update")
                .build();
        }

        // Force ID
        updatePlanEntity.setId(plan);

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        PlanEntity planEntity = planService.findById(executionContext, plan);
        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("'plan' parameter does not correspond to the current API").build();
        }

        PlanEntity responseEntity = planService.update(executionContext, updatePlanEntity);
        return Response.ok(PlanMapper.INSTANCE.convert(responseEntity)).build();
    }

    @DELETE
    @Path("/{plan}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.DELETE }) })
    public Response deleteApiPlan(@PathParam("plan") String plan) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        PlanEntity planEntity = planService.findById(executionContext, plan);
        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("'plan' parameter does not correspond to the current API").build();
        }

        planService.delete(executionContext, plan);

        return Response.noContent().build();
    }

    @POST
    @Path("/{plan}/_close")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response closeApiPlan(@PathParam("plan") String plan) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        PlanEntity planEntity = planService.findById(executionContext, plan);
        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("'plan' parameter does not correspond to the current API").build();
        }

        PlanEntity closedPlan = planService.close(executionContext, plan);

        return Response.ok(PlanMapper.INSTANCE.convert(closedPlan)).build();
    }

    @POST
    @Path("/{plan}/_publish")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response publishApiPlan(@PathParam("plan") String plan) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        PlanEntity planEntity = planService.findById(executionContext, plan);
        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("'plan' parameter does not correspond to the current API").build();
        }

        PlanEntity publishedPlan = planService.publish(executionContext, plan);

        return Response.ok(PlanMapper.INSTANCE.convert(publishedPlan)).build();
    }

    @POST
    @Path("/{plan}/_deprecate")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response deprecateApiPlan(@PathParam("plan") String plan) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        PlanEntity planEntity = planService.findById(executionContext, plan);
        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("'plan' parameter does not correspond to the current API").build();
        }

        PlanEntity deprecatedPlan = planService.deprecate(executionContext, plan);

        return Response.ok(deprecatedPlan).build();
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
        filtered.setSecurity(entity.getPlanSecurity());
        filtered.setType(filtered.getType());
        filtered.setValidation(filtered.getValidation());
        filtered.setCommentRequired(entity.isCommentRequired());
        filtered.setCommentMessage(entity.getCommentMessage());
        filtered.setGeneralConditions(entity.getGeneralConditions());

        return filtered;
    }

    private Error planNotFoundError(String plan) {
        return new Error()
            .httpStatus(Response.Status.NOT_FOUND.getStatusCode())
            .message("Plan [" + plan + "] can not be found.")
            .putParametersItem("plan", plan)
            .technicalCode("plan.notFound");
    }
}
