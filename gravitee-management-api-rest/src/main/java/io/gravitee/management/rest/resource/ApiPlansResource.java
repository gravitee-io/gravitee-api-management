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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.*;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.resource.param.PlanStatusParam;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.GroupService;
import io.gravitee.management.service.PlanService;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.repository.management.model.Subscription;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static io.gravitee.management.model.permissions.RolePermission.API_PLAN;
import static io.gravitee.management.model.permissions.RolePermissionAction.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API", "Plan"})
public class ApiPlansResource extends AbstractResource {

    @Inject
    private PlanService planService;

    @Inject
    private ApiService apiService;

    @Inject
    private GroupService groupService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "List plans for an API",
            notes = "List all the plans accessible to the current user.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List accessible plans for current user", response = PlanEntity.class, responseContainer = "Set"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<PlanEntity> listPlans(
            @PathParam("api") String api,
            @QueryParam("status") @DefaultValue("published") PlanStatusParam status) {

        ApiEntity apiEntity = apiService.findById(api);

        boolean lookingForUnpublishedPlan = status.getStatuses().stream().
                map(st -> !st.equals(PlanStatus.PUBLISHED)).
                reduce(Boolean::logicalOr).
                orElse(true);
        if (lookingForUnpublishedPlan && !hasPermission(API_PLAN, api, CREATE, UPDATE, DELETE)) {
            throw new ForbiddenAccessException();
        }

        if (Visibility.PUBLIC.equals(apiEntity.getVisibility())
            || hasPermission(API_PLAN, api, READ)) {

            return planService.findByApi(api).stream()
                    .filter(plan -> status.getStatuses().contains(plan.getStatus())
                            && groupService.isUserAuthorizedToAccessApiData(apiEntity, plan.getExcludedGroups(), getAuthenticatedUsernameOrNull()))
                    .sorted(Comparator.comparingInt(PlanEntity::getOrder))
                    .collect(Collectors.toList());
        }

        throw new ForbiddenAccessException();
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
    public Response createPlan(
            @PathParam("api") String api,
            @ApiParam(name = "plan", required = true) @Valid @NotNull NewPlanEntity newPlanEntity) {
        newPlanEntity.setApi(api);
        newPlanEntity.setType(PlanType.API);

        PlanEntity planEntity = planService.create(newPlanEntity);

        return Response
                .created(URI.create("/apis/" + api + "/plans/" + planEntity.getId()))
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
    public Response updatePlan(
            @PathParam("api") String api,
            @PathParam("plan") String plan,
            @ApiParam(name = "plan", required = true) @Valid @NotNull UpdatePlanEntity updatePlanEntity) {

        if (updatePlanEntity.getId() != null && ! plan.equals(updatePlanEntity.getId())) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the plan to update")
                    .build();
        }

        // Force ID
        updatePlanEntity.setId(plan);

        PlanEntity planEntity = planService.findById(plan);
        if (! planEntity.getApis().contains(api)) {
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
    public Response getPlan(
            @PathParam("api") String api,
            @PathParam("plan") String plan) {

        if (Visibility.PUBLIC.equals(apiService.findById(api).getVisibility())
                || hasPermission(API_PLAN, api, READ)) {
            PlanEntity planEntity = planService.findById(plan);
            if (!planEntity.getApis().contains(api)) {
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
    public Response deletePlan(
            @PathParam("api") String api,
            @PathParam("plan") String plan) {
        PlanEntity planEntity = planService.findById(plan);
        if (! planEntity.getApis().contains(api)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the current API")
                    .build();
        }

        planService.delete(plan);

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
    public Response closePlan(
            @PathParam("api") String api,
            @PathParam("plan") String plan) {
        PlanEntity planEntity = planService.findById(plan);
        if (! planEntity.getApis().contains(api)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the current API")
                    .build();
        }

        return Response.ok(planService.close(plan, getAuthenticatedUsername())).build();
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
    public Response publishPlan(
            @PathParam("api") String api,
            @PathParam("plan") String plan) {
        PlanEntity planEntity = planService.findById(plan);
        if (! planEntity.getApis().contains(api)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the current API")
                    .build();
        }

        return Response.ok(planService.publish(plan)).build();
    }

    @Path("/{plan}/subscriptions")
    public ApiPlanSubscriptionsResource getApiSubscriptionsResource() {
        return resourceContext.getResource(ApiPlanSubscriptionsResource.class);
    }
}
