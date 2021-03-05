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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.rest.resource.param.PlanSecurityParam;
import io.gravitee.rest.api.management.rest.resource.param.PlanStatusParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_GATEWAY_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_PLAN;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static java.util.Comparator.comparingInt;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API Plans"})
public class ApiPlansResource extends AbstractResource {

    @Inject
    private PlanService planService;

    @Inject
    private ApiService apiService;

    @Inject
    private GroupService groupService;

    @Context
    private ResourceContext resourceContext;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "List plans for an API",
            notes = "List all the plans accessible to the current user.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List accessible plans for current user", response = PlanEntity.class, responseContainer = "Set"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<PlanEntity> getApiPlans(
            @QueryParam("status") @DefaultValue("published") final PlanStatusParam wishedStatus,
            @QueryParam("security") final PlanSecurityParam security) {
        if (!hasPermission(RolePermission.API_PLAN, api, RolePermissionAction.READ) &&
                !hasPermission(RolePermission.API_LOG, api, RolePermissionAction.READ)) {
            throw new ForbiddenAccessException();
        }

        ApiEntity apiEntity = apiService.findById(api);

        return planService.findByApi(api).stream()
                .filter(plan -> wishedStatus.getStatuses().contains(plan.getStatus())
                        && ((isAuthenticated() && isAdmin()) || groupService.
                        isUserAuthorizedToAccessApiData(apiEntity, plan.getExcludedGroups(), getAuthenticatedUserOrNull())))
                .filter(plan -> security == null || security.getSecurities().contains(plan.getSecurity()))
                .sorted(comparingInt(PlanEntity::getOrder))
                .map(this::filterSensitiveData)
                .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a plan",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Plan successfully created", response = PlanEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = API_PLAN, acls = CREATE)
    })
    public Response createApiPlan(
            @ApiParam(name = "plan", required = true) @Valid @NotNull NewPlanEntity newPlanEntity) {
        newPlanEntity.setApi(api);
        newPlanEntity.setType(PlanType.API);

        PlanEntity planEntity = planService.create(newPlanEntity);

        return Response
                .created(this.getLocationHeader(planEntity.getId()))
                .entity(planEntity)
                .build();
    }

    @PUT
    @Path("/{plan}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a plan",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Plan successfully updated", response = PlanEntity.class),
            @ApiResponse(code = 400, message = "Bad plan format"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = API_PLAN, acls = UPDATE)
    })
    public Response updateApiPlan(
            @PathParam("plan") String plan,
            @ApiParam(name = "plan", required = true) @Valid @NotNull UpdatePlanEntity updatePlanEntity) {

        if (updatePlanEntity.getId() != null && !plan.equals(updatePlanEntity.getId())) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the plan to update")
                    .build();
        }

        // Force ID
        updatePlanEntity.setId(plan);

        PlanEntity planEntity = planService.findById(plan);
        if (! planEntity.getApi().contains(api)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the current API")
                    .build();
        }

        planEntity = planService.update(updatePlanEntity);
        return Response.ok(planEntity).build();
    }

    @GET
    @Path("/{plan}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a plan",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Plan information", response = PlanEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response getApiPlan(
            @PathParam("plan") String plan) {

        if (Visibility.PUBLIC.equals(apiService.findById(api).getVisibility())
                || hasPermission(API_PLAN, api, READ)) {
            PlanEntity planEntity = planService.findById(plan);
            if (!planEntity.getApi().equals(api)) {
                return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity("'plan' parameter does not correspond to the current API")
                        .build();
            }

            return Response.ok(planEntity).build();
        }
        throw new ForbiddenAccessException();
    }

    @DELETE
    @Path("/{plan}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a plan",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Plan successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = API_PLAN, acls = DELETE)
    })
    public Response deleteApiPlan(
            @PathParam("plan") String plan) {
        PlanEntity planEntity = planService.findById(plan);
        if (! planEntity.getApi().equals(api)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the current API")
                    .build();
        }

        planService.delete(plan);

        removePlanFromApiDefinition(plan, api);

        return Response.noContent().build();
    }

    @POST
    @Path("/{plan}/_close")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Close  a plan",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Plan successfully closed", response = PlanEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = API_PLAN, acls = UPDATE)
    })
    public Response closeApiPlan(
            @PathParam("plan") String plan) {
        PlanEntity planEntity = planService.findById(plan);
        if (! planEntity.getApi().equals(api)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the current API")
                    .build();
        }

        PlanEntity closedPlan = planService.close(plan, getAuthenticatedUser());

        removePlanFromApiDefinition(plan, api);

        return Response.ok(closedPlan).build();
    }

    @POST
    @Path("/{plan}/_publish")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Publicly publish plan",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Plan successfully published", response = PlanEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = API_PLAN, acls = UPDATE)
    })
    public Response publishApiPlan(
            @PathParam("plan") String plan) {
        PlanEntity planEntity = planService.findById(plan);
        if (! planEntity.getApi().equals(api)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the current API")
                    .build();
        }

        return Response.ok(planService.publish(plan)).build();
    }

    @POST
    @Deprecated
    @Path("/{plan}/_depreciate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deprecated, use '_deprecate' instead. Deprecate a plan",
            notes = "User must have the API_PLAN[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Plan successfully deprecated", response = PlanEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = API_PLAN, acls = UPDATE)
    })
    public Response depreciateApiPlan(
            @PathParam("plan") String plan) {

        return this.deprecateApiPlan(plan);
    }

    @POST
    @Path("/{plan}/_deprecate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deprecate a plan",
            notes = "User must have the API_PLAN[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Plan successfully deprecated", response = PlanEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = API_PLAN, acls = UPDATE)
    })
    public Response deprecateApiPlan(
            @PathParam("plan") String plan) {
        PlanEntity planEntity = planService.findById(plan);
        if (! planEntity.getApi().equals(api)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the current API")
                    .build();
        }

        return Response.ok(planService.deprecate(plan)).build();
    }

    private PlanEntity filterSensitiveData(PlanEntity entity) {

        if ( hasPermission(API_GATEWAY_DEFINITION, entity.getApi(), RolePermissionAction.READ)
                && hasPermission(API_PLAN, entity.getApi(), RolePermissionAction.READ) ) {

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

    private void removePlanFromApiDefinition(String planId, String apiId) {
        // Remove plan from api definition
        if (apiId != null) {
            ApiEntity api = apiService.findById(apiId);
            if (DefinitionVersion.V2.equals(DefinitionVersion.valueOfLabel(api.getGraviteeDefinitionVersion()))) {
                List<io.gravitee.definition.model.Plan> plans = api.getPlans().stream()
                        .filter(plan1 -> plan1.getId() != null && !plan1.getId().equals(planId))
                        .collect(Collectors.toList());

                UpdateApiEntity updateApiEntity = ApiService.convert(api);
                updateApiEntity.setPlans(plans);

                if (api.getPlans().size() != updateApiEntity.getPlans().size()) {
                    apiService.update(apiId, updateApiEntity);
                }
            }
        }
    }
}
