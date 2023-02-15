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
package io.gravitee.rest.api.management.v4.rest.resource.api;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_GATEWAY_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_PLAN;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static java.util.Comparator.comparingInt;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v4.rest.mapper.PlanMapper;
import io.gravitee.rest.api.management.v4.rest.model.Plan;
import io.gravitee.rest.api.management.v4.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v4.rest.resource.param.PlanSecurityParam;
import io.gravitee.rest.api.management.v4.rest.resource.param.PlanStatusParam;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.v4.PlanService;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPlansResource extends AbstractResource {

    @Inject
    private PlanService planService;

    @Inject
    private GroupService groupService;

    @Context
    private ResourceContext resourceContext;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("apiId")
    private String apiId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Plan> getApiPlans(
        @QueryParam("status") @DefaultValue("PUBLISHED") final PlanStatusParam wishedStatus,
        @QueryParam("security") final PlanSecurityParam security
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            !hasPermission(executionContext, RolePermission.API_PLAN, apiId, RolePermissionAction.READ) &&
            !hasPermission(executionContext, RolePermission.API_LOG, apiId, RolePermissionAction.READ)
        ) {
            throw new ForbiddenAccessException();
        }

        GenericApiEntity genericApiEntity = apiSearchService.findGenericById(executionContext, apiId);

        List<PlanEntity> entities = planService
            .findByApi(executionContext, apiId)
            .stream()
            .filter(
                plan ->
                    wishedStatus.contains(plan.getStatus()) &&
                    (
                        (isAuthenticated() && isAdmin()) ||
                        groupService.isUserAuthorizedToAccessApiData(
                            genericApiEntity,
                            plan.getExcludedGroups(),
                            getAuthenticatedUserOrNull()
                        )
                    )
            )
            .filter(plan -> security == null || security.contains(PlanSecurityType.valueOfLabel(plan.getSecurity().getType())))
            .sorted(comparingInt(PlanEntity::getOrder))
            .map(this::filterSensitiveData)
            .collect(Collectors.toList());

        return PlanMapper.INSTANCE.convertList(entities);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createApiPlan(@Valid @NotNull NewPlanEntity newPlanEntity) {
        newPlanEntity.setApiId(apiId);
        newPlanEntity.setType(PlanType.API);

        PlanEntity planEntity = planService.create(GraviteeContext.getExecutionContext(), newPlanEntity);

        return Response.created(this.getLocationHeader(planEntity.getId())).entity(PlanMapper.INSTANCE.convert(planEntity)).build();
    }

    @GET
    @Path("/{plan}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiPlan(@PathParam("plan") String plan) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            Visibility.PUBLIC.equals(apiService.findById(executionContext, apiId).getVisibility()) ||
            hasPermission(GraviteeContext.getExecutionContext(), API_PLAN, apiId, READ)
        ) {
            PlanEntity planEntity = planService.findById(executionContext, plan);
            if (!planEntity.getApiId().equals(apiId)) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the current API")
                    .build();
            }

            return Response.ok(PlanMapper.INSTANCE.convert(planEntity)).build();
        }
        throw new ForbiddenAccessException();
    }

    @PUT
    @Path("/{plan}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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
    public Response deprecateApiPlan(@PathParam("plan") String plan) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        PlanEntity planEntity = planService.findById(executionContext, plan);
        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("'plan' parameter does not correspond to the current API").build();
        }

        PlanEntity deprecatedPlan = planService.deprecate(executionContext, plan);

        return Response.ok(deprecatedPlan).build();
    }

    private PlanEntity filterSensitiveData(PlanEntity entity) {
        if (
            hasPermission(GraviteeContext.getExecutionContext(), API_GATEWAY_DEFINITION, entity.getApiId(), RolePermissionAction.READ) &&
            hasPermission(GraviteeContext.getExecutionContext(), API_PLAN, entity.getApiId(), RolePermissionAction.READ)
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
        filtered.setSecurity(entity.getSecurity());
        filtered.setType(filtered.getType());
        filtered.setValidation(filtered.getValidation());
        filtered.setCommentRequired(entity.isCommentRequired());
        filtered.setCommentMessage(entity.getCommentMessage());
        filtered.setGeneralConditions(entity.getGeneralConditions());

        return filtered;
    }
}
