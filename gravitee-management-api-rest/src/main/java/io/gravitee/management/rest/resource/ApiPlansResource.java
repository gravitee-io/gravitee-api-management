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
import io.gravitee.management.model.NewPlanEntity;
import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.model.PlanType;
import io.gravitee.management.model.UpdatePlanEntity;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.rest.resource.param.PlanStatusParam;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.PlanService;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API", "Plan"})
public class ApiPlansResource extends AbstractResource {

    @Inject
    private PlanService planService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiPermissionsRequired(ApiPermission.READ)
    @ApiOperation(
            value = "List plans for an API",
            notes = "List all the plans accessible to the current user.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List accessible plans for current user", response = PlanEntity.class, responseContainer = "Set"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<PlanEntity> listPlans(
            @PathParam("api") String api,
            @QueryParam("status") @DefaultValue("published") PlanStatusParam status) {

        return planService.findByApi(api).stream()
                .filter(plan -> status.getStatuses().contains(plan.getStatus()))
                .sorted((o1, o2) -> Integer.compare(o1.getOrder(), o2.getOrder()))
                .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiPermissionsRequired(ApiPermission.MANAGE_PLANS)
    @ApiOperation(value = "Create a plan",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Plan successfully created", response = PlanEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
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
    @ApiPermissionsRequired(ApiPermission.MANAGE_PLANS)
    @ApiOperation(value = "Update a plan",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Plan successfully updated", response = PlanEntity.class),
            @ApiResponse(code = 400, message = "Bad plan format"),
            @ApiResponse(code = 500, message = "Internal server error")})
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
    @ApiPermissionsRequired(ApiPermission.READ)
    @ApiOperation(value = "Get a plan",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Plan information", response = PlanEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response getPlan(
            @PathParam("api") String api,
            @PathParam("plan") String plan) {
        PlanEntity planEntity = planService.findById(plan);
        if (! planEntity.getApis().contains(api)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'plan' parameter does not correspond to the current API")
                    .build();
        }

        return Response.ok(planEntity).build();
    }

    @DELETE
    @Path("/{plan}")
    @ApiPermissionsRequired(ApiPermission.MANAGE_PLANS)
    @ApiOperation(value = "Delete a plan",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Plan successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
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
    @ApiPermissionsRequired(ApiPermission.MANAGE_PLANS)
    @ApiOperation(value = "Close  a plan",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Plan successfully closed", response = PlanEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
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

        return Response.ok(planService.close(plan)).build();
    }

    @POST
    @Path("/{plan}/_publish")
    @ApiPermissionsRequired(ApiPermission.MANAGE_PLANS)
    @ApiOperation(value = "Publicly publish plan",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Plan successfully published", response = PlanEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
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
