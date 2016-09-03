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
import io.gravitee.management.service.exceptions.ApiNotFoundException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;

import static java.lang.String.format;

/**
 * Defines the REST resources to manage API.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiResource extends AbstractResource {

    @Context
    private UriInfo uriInfo;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiService apiService;

    @PathParam("api")
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiPermissionsRequired(ApiPermission.READ)
    public ApiEntity get() throws ApiNotFoundException {
        ApiEntity api = apiService.findById(this.api);

        final UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        final UriBuilder uriBuilder = ub.path("picture");
        if (api.getPicture() != null) {
            // force browser to get if updated
            uriBuilder.queryParam("hash", api.getPicture().hashCode());
        }
        api.setPictureUrl(uriBuilder.build().toString());
        api.setPicture(null);
        setPermission(api);

        return api;
    }

    @GET
    @Path("picture")
    @ApiPermissionsRequired(ApiPermission.READ)
    public Response picture(@Context Request request) throws ApiNotFoundException {
        apiService.findById(this.api);

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        ImageEntity image = apiService.getPicture(this.api);

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
    public Response doLifecycleAction(@QueryParam("action") LifecycleActionParam action) {
        ApiEntity api = apiService.findById(this.api);

        switch (action.getAction()) {
            case START:
                checkAPILifeCycle(api, action.getAction());
                apiService.start(api.getId(), getAuthenticatedUsername());
                break;
            case STOP:
                checkAPILifeCycle(api, action.getAction());
                apiService.stop(api.getId(), getAuthenticatedUsername());
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
    public ApiEntity update(@Valid @NotNull final UpdateApiEntity api) {
        final ApiEntity currentApi = this.get();
        // Force context-path if user is not the primary_owner or an administrator
        if (currentApi.getPermission() != MembershipType.PRIMARY_OWNER) {
            api.getProxy().setContextPath(currentApi.getProxy().getContextPath());
        }

        final ApiEntity updatedApi = apiService.update(this.api, api);
        setPermission(updatedApi);

        return updatedApi;
    }

    @DELETE
    @ApiPermissionsRequired(ApiPermission.DELETE)
    public Response delete() {
        apiService.delete(api);

        return Response.noContent().build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("deploy")
    @ApiPermissionsRequired(ApiPermission.MANAGE_LIFECYCLE)
    public Response deployAPI() {
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
    public io.gravitee.management.rest.model.ApiEntity isAPISynchronized() {
        io.gravitee.management.rest.model.ApiEntity apiEntity = new io.gravitee.management.rest.model.ApiEntity();

        apiEntity.setApiId(this.api);
        setSynchronizationState(apiEntity);

        return apiEntity;
    }

    @POST
    @Consumes
    @Produces(MediaType.APPLICATION_JSON)
    @Path("rollback")
    @ApiPermissionsRequired(ApiPermission.MANAGE_LIFECYCLE)
    public Response rollback(@Valid @NotNull final UpdateApiEntity api) {
        try {
            ApiEntity apiEntity = apiService.rollback(this.api, api);
            return Response.status(Status.OK).entity(apiEntity).build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("import")
    @ApiPermissionsRequired(ApiPermission.MANAGE_API)
    public Response importDefinition(String apiDefinition) {
        final ApiEntity apiEntity = get();
        return Response.ok(apiService.createOrUpdateWithDefinition(apiEntity, apiDefinition, getAuthenticatedUsername())).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("export")
    @ApiPermissionsRequired(ApiPermission.MANAGE_API)
    public Response exportDefinition() {
        final ApiEntity apiEntity = get();
        setPermission(apiEntity);

        return Response
                .ok(apiService.exportAsJson(api, apiEntity.getPermission()))
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

    private void setPermission(ApiEntity api) {
        if (isAdmin()) {
            api.setPermission(MembershipType.PRIMARY_OWNER);
        } else if (isAuthenticated()) {
            MemberEntity member = apiService.getMember(api.getId(), getAuthenticatedUsername());
            if (member != null) {
                api.setPermission(member.getType());
            } else {
                if (api.getVisibility() == Visibility.PUBLIC) {
                    // If API is public, all users have the user permission
                    api.setPermission(MembershipType.USER);
                }
            }
        }
    }

    private void setSynchronizationState(io.gravitee.management.rest.model.ApiEntity apiEntity) {
        if (apiService.isAPISynchronized(api)) {
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
