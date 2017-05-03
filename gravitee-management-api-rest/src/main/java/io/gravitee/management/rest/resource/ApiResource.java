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
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.rest.resource.LifecycleActionParam.LifecycleAction;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static java.lang.String.format;

/**
 * Defines the REST resources to manage API.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API"})
public class ApiResource extends AbstractResource {

    @Context
    private UriInfo uriInfo;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiService apiService;

    @Inject
    MembershipService membershipService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiPermissionsRequired(ApiPermission.READ)
    @ApiOperation(value = "Get the API definition",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API definition", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public ApiEntity get(@PathParam("api") String api) throws ApiNotFoundException {
        ApiEntity apiEntity = apiService.findById(api);

        final UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        final UriBuilder uriBuilder = ub.path("picture");
        if (apiEntity.getPicture() != null) {
            // force browser to get if updated
            uriBuilder.queryParam("hash", apiEntity.getPicture().hashCode());
        }
        apiEntity.setPictureUrl(uriBuilder.build().toString());
        apiEntity.setPicture(null);
        setPermission(apiEntity);

        return apiEntity;
    }

    @GET
    @Path("picture")
    @ApiPermissionsRequired(ApiPermission.READ)
    @ApiOperation(value = "Get the API's picture",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API's picture"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response picture(
            @Context Request request,
            @PathParam("api") String api) throws ApiNotFoundException {
        apiService.findById(api);

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        ImageEntity image = apiService.getPicture(api);

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder
                    .cacheControl(cc)
                    .build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response
                .ok()
                .entity(baos)
                .cacheControl(cc)
                .tag(etag)
                .type(image.getType())
                .build();
    }

    @POST
    @ApiPermissionsRequired(ApiPermission.MANAGE_LIFECYCLE)
    @ApiOperation(
            value = "Manage the API's lifecycle",
            notes = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "API's picture"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response doLifecycleAction(
            @ApiParam(required = true,  allowableValues = "START, STOP")
                @QueryParam("action") LifecycleActionParam action,
            @PathParam("api") String api) {
        ApiEntity apiEntity = apiService.findById(api);

        switch (action.getAction()) {
            case START:
                checkAPILifeCycle(apiEntity, action.getAction());
                apiService.start(apiEntity.getId(), getAuthenticatedUsername());
                break;
            case STOP:
                checkAPILifeCycle(apiEntity, action.getAction());
                apiService.stop(apiEntity.getId(), getAuthenticatedUsername());
                break;
            default:
                break;
        }

        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiPermissionsRequired(ApiPermission.MANAGE_API)
    @ApiOperation(
            value = "Update the API",
            notes = "User must have the MANAGE_APPLICATION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API successfully updated", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public ApiEntity update(
            @ApiParam(name = "api", required = true) @Valid @NotNull final UpdateApiEntity apiToUpdate,
            @PathParam("api") String api) {
        final ApiEntity currentApi = this.get(api);

        // Force context-path if user is not the primary_owner or an administrator
        if (currentApi.getPermission() != MembershipType.PRIMARY_OWNER) {
            apiToUpdate.getProxy().setContextPath(currentApi.getProxy().getContextPath());
        }

        final ApiEntity updatedApi = apiService.update(api, apiToUpdate);
        setPermission(updatedApi);

        return updatedApi;
    }

    @DELETE
    @ApiPermissionsRequired(ApiPermission.DELETE)
    @ApiOperation(
            value = "Delete the API",
            notes = "User must have the DELETE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "API successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response delete(@PathParam("api") String api) {
        apiService.delete(api);

        return Response.noContent().build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("deploy")
    @ApiPermissionsRequired(ApiPermission.MANAGE_LIFECYCLE)
    @ApiOperation(
            value = "Deploy API to gateway instances",
            notes = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API successfully deployed", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response deployAPI(@PathParam("api") String api) {
        try {
            ApiEntity apiEntity = apiService.deploy(api, getAuthenticatedUsername(), EventType.PUBLISH_API);
            setPermission(apiEntity);
            return Response.status(Status.OK).entity(apiEntity).build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("JsonProcessingException " + e).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("state")
    @ApiPermissionsRequired(ApiPermission.MANAGE_LIFECYCLE)
    @ApiOperation(
            value = "Get the state of the API",
            notes = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API's state", response = io.gravitee.management.rest.model.ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public io.gravitee.management.rest.model.ApiEntity isAPISynchronized(@PathParam("api") String api) {
        io.gravitee.management.rest.model.ApiEntity apiEntity = new io.gravitee.management.rest.model.ApiEntity();

        apiEntity.setApiId(api);
        setSynchronizationState(apiEntity);

        return apiEntity;
    }

    @POST
    @Consumes
    @Produces(MediaType.APPLICATION_JSON)
    @Path("rollback")
    @ApiPermissionsRequired(ApiPermission.MANAGE_LIFECYCLE)
    @ApiOperation(
            value = "Rollback API to a previous version",
            notes = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API successfully rollbacked", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response rollback(
            @PathParam("api") String api,
            @ApiParam(name = "api", required = true) @Valid @NotNull final UpdateApiEntity apiEntity) {
        try {
            ApiEntity rollbackedApi = apiService.rollback(api, apiEntity);
            return Response.status(Status.OK).entity(rollbackedApi).build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("import")
    @ApiPermissionsRequired(ApiPermission.MANAGE_API)
    @ApiOperation(
            value = "Update the API with an existing API definition",
            notes = "User must have the MANAGE_APPLICATION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API successfully updated from API definition", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response updateWithDefinition(
            @PathParam("api") String api,
            @ApiParam(name = "definition", required = true) String apiDefinition) {
        final ApiEntity apiEntity = get(api);
        return Response.ok(apiService.createOrUpdateWithDefinition(apiEntity, apiDefinition, getAuthenticatedUsername())).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("export")
    @ApiPermissionsRequired(ApiPermission.MANAGE_API)
    @ApiOperation(
            value = "Export the API definition in JSON format",
            notes = "User must have the MANAGE_APPLICATION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API definition", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response exportDefinition(@PathParam("api") String api,
                                     @QueryParam("exclude") @DefaultValue("") String exclude) {
        final ApiEntity apiEntity = get(api);
        return Response
                .ok(apiService.exportAsJson(api, apiEntity.getPermission(), exclude.split(",")))
                .header(HttpHeaders.CONTENT_DISPOSITION, format("attachment;filename=%s",getExportFilename(apiEntity)))
                .build();
    }

    @Path("keys")
    public ApiKeysResource getApiKeyResource() {
        return resourceContext.getResource(ApiKeysResource.class);
    }

    @Path("members")
    public ApiMembersResource getApiMembersResource() {
        return resourceContext.getResource(ApiMembersResource.class);
    }

    @Path("analytics")
    public ApiAnalyticsResource getApiAnalyticsResource() {
        return resourceContext.getResource(ApiAnalyticsResource.class);
    }

    @Path("logs")
    public ApiLogsResource getApiLogsResource() {
        return resourceContext.getResource(ApiLogsResource.class);
    }

    @Path("health")
    public ApiHealthResource getApiHealthResource() {
        return resourceContext.getResource(ApiHealthResource.class);
    }

    @Path("pages")
    public ApiPagesResource getApiPagesResource() {
        return resourceContext.getResource(ApiPagesResource.class);
    }

    @Path("events")
    public ApiEventsResource getApiEventsResource() {
        return resourceContext.getResource(ApiEventsResource.class);
    }

    @Path("plans")
    public ApiPlansResource getApiPlansResource() {
        return resourceContext.getResource(ApiPlansResource.class);
    }

    @Path("subscriptions")
    public ApiSubscriptionsResource getApiSubscriptionsResource() {
        return resourceContext.getResource(ApiSubscriptionsResource.class);
    }

    private void setPermission(ApiEntity api) {
        if (isAdmin()) {
            api.setPermission(MembershipType.PRIMARY_OWNER);
        } else if (isAuthenticated()) {
            MemberEntity member = membershipService.getMember(MembershipReferenceType.API, api.getId(), getAuthenticatedUsername());
            if (member != null) {
                api.setPermission(member.getType());
            } else  if (api.getGroup() != null) {
                member = membershipService.getMember(MembershipReferenceType.API_GROUP, api.getGroup().getId(), getAuthenticatedUsername());
                if (member != null) {
                    api.setPermission(member.getType());
                }
            }
            if (member == null && api.getVisibility() == Visibility.PUBLIC) {
                // If API is public, all users have the user permission
                api.setPermission(MembershipType.USER);
            }
        }
    }

    private void setSynchronizationState(io.gravitee.management.rest.model.ApiEntity apiEntity) {
        if (apiService.isSynchronized(apiEntity.getApiId())) {
            apiEntity.setIsSynchronized(true);
        } else {
            apiEntity.setIsSynchronized(false);
        }
    }
    
    private void checkAPILifeCycle(ApiEntity api, LifecycleAction action) {
        switch(api.getState()) {
            case STARTED:
                if (LifecycleAction.START.equals(action)) {
                    throw new BadRequestException("API is already started");
                }
                break;
            case STOPPED:
                if (LifecycleAction.STOP.equals(action)) {
                    throw new BadRequestException("API is already stopped");
                }
                break;
            default:
                break;
        }
    }

    private String getExportFilename(ApiEntity apiEntity) {
        return format("%s-%s.json", apiEntity.getName(), apiEntity.getVersion())
                .trim()
                .toLowerCase()
                .replaceAll(" +", " ")
                .replaceAll(" ", "-")
                .replaceAll("[^\\w\\s\\.]","-")
                .replaceAll("-+", "-");
    }
}
