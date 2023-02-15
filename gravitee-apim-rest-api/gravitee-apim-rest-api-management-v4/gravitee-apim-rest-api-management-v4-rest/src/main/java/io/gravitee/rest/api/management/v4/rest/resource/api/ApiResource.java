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

import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.exception.InvalidImageException;
import io.gravitee.rest.api.management.v4.rest.mapper.ApiMapper;
import io.gravitee.rest.api.management.v4.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v4.rest.resource.param.LifecycleAction;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.v4.ApiStateService;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Defines the REST resources to manage API v4.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/apis/{apiId}")
public class ApiResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ParameterService parameterService;

    @Inject
    private ApiStateService apiStateService;

    @PathParam("apiId")
    private String apiId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiById() {
        ApiEntity apiEntity = getApiEntityById();

        return Response
            .ok(ApiMapper.INSTANCE.convert(apiEntity))
            .tag(Long.toString(apiEntity.getUpdatedAt().getTime()))
            .lastModified(apiEntity.getUpdatedAt())
            .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateApi(@Context HttpHeaders headers, @Valid @NotNull final UpdateApiEntity apiToUpdate) {
        if (!apiId.equals(apiToUpdate.getId())) {
            throw new BadRequestException("'apiId' is not the same as the API in payload");
        }

        final ApiEntity currentEntity = getApiEntityById();
        Response.ResponseBuilder builder = evaluateIfMatch(headers, Long.toString(currentEntity.getUpdatedAt().getTime()));

        if (builder != null) {
            return builder.build();
        }

        try {
            ImageUtils.verify(apiToUpdate.getPicture());
            ImageUtils.verify(apiToUpdate.getBackground());
        } catch (InvalidImageException e) {
            throw new BadRequestException("Invalid image format");
        }

        // Force listeners if user is not the primary_owner or an administrator
        if (
            !hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_GATEWAY_DEFINITION,
                apiId,
                RolePermissionAction.UPDATE
            ) &&
            !Objects.equals(currentEntity.getPrimaryOwner().getId(), getAuthenticatedUser()) &&
            !isAdmin()
        ) {
            apiToUpdate.setListeners(currentEntity.getListeners());
        }

        final ApiEntity updatedApi = apiServiceV4.update(
            GraviteeContext.getExecutionContext(),
            apiId,
            apiToUpdate,
            true,
            getAuthenticatedUser()
        );
        setPictures(updatedApi);

        return Response
            .ok(ApiMapper.INSTANCE.convert(updatedApi))
            .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
            .lastModified(updatedApi.getUpdatedAt())
            .build();
    }

    @DELETE
    public Response deleteApi(@QueryParam("closePlans") boolean closePlans) {
        apiServiceV4.delete(GraviteeContext.getExecutionContext(), apiId, closePlans);

        return Response.noContent().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/deployments")
    public Response deployApi(@Valid final ApiDeploymentEntity apiDeploymentEntity) {
        try {
            ApiEntity apiEntity = apiStateService.deploy(
                GraviteeContext.getExecutionContext(),
                apiId,
                getAuthenticatedUser(),
                apiDeploymentEntity
            );
            return Response
                .ok(ApiMapper.INSTANCE.convert(apiEntity))
                .tag(Long.toString(apiEntity.getUpdatedAt().getTime()))
                .lastModified(apiEntity.getUpdatedAt())
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("JsonProcessingException " + e).build();
        }
    }

    @Path("/_start")
    @POST
    public Response startAPI(@Context HttpHeaders headers) {
        final Response responseApi = getApiById();
        Response.ResponseBuilder builder = evaluateIfMatch(headers, responseApi.getEntityTag().getValue());

        if (builder != null) {
            return builder.build();
        }

        final ApiEntity apiEntity = (ApiEntity) responseApi.getEntity();
        checkApiLifeCycle(apiEntity, LifecycleAction.START);
        ApiEntity updatedApi = apiStateService.start(GraviteeContext.getExecutionContext(), apiEntity.getId(), getAuthenticatedUser());

        return Response.noContent().tag(Long.toString(updatedApi.getUpdatedAt().getTime())).lastModified(updatedApi.getUpdatedAt()).build();
    }

    @Path("/_stop")
    @POST
    public Response stopAPI(@Context HttpHeaders headers) {
        final Response responseApi = getApiById();
        Response.ResponseBuilder builder = evaluateIfMatch(headers, responseApi.getEntityTag().getValue());

        if (builder != null) {
            return builder.build();
        }

        final ApiEntity apiEntity = (ApiEntity) responseApi.getEntity();
        checkApiLifeCycle(apiEntity, LifecycleAction.STOP);
        ApiEntity updatedApi = apiStateService.stop(GraviteeContext.getExecutionContext(), apiEntity.getId(), getAuthenticatedUser());

        return Response.noContent().tag(Long.toString(updatedApi.getUpdatedAt().getTime())).lastModified(updatedApi.getUpdatedAt()).build();
    }

    @Path("/plans")
    public ApiPlansResource getApiPlansResource() {
        return resourceContext.getResource(ApiPlansResource.class);
    }

    private ApiEntity getApiEntityById() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApiEntity apiEntity = apiSearchService.findById(executionContext, apiId);

        if (!canManageApi(apiEntity)) {
            throw new ForbiddenAccessException();
        }

        if (hasPermission(executionContext, RolePermission.API_DEFINITION, apiId, RolePermissionAction.READ)) {
            setPictures(apiEntity);
        } else {
            filterSensitiveData(apiEntity);
        }
        return apiEntity;
    }

    private void setPictures(final ApiEntity apiEntity) {
        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder().path("picture");
        // force browser to get if updated
        uriBuilder.queryParam("hash", apiEntity.getUpdatedAt().getTime());
        apiEntity.setPictureUrl(uriBuilder.build().toString());
        apiEntity.setPicture(null);

        uriBuilder = uriInfo.getAbsolutePathBuilder().path("background");
        // force browser to get if updated
        uriBuilder.queryParam("hash", apiEntity.getUpdatedAt().getTime());
        apiEntity.setBackgroundUrl(uriBuilder.build().toString());
        apiEntity.setBackground(null);
    }

    private void filterSensitiveData(ApiEntity apiEntity) {
        List<Listener> listeners = apiEntity.getListeners();

        if (listeners != null) {
            Optional<Listener> first = listeners.stream().filter(listener -> ListenerType.HTTP == listener.getType()).findFirst();
            if (first.isPresent()) {
                HttpListener httpListener = (HttpListener) first.get();
                if (httpListener.getPaths() != null && !httpListener.getPaths().isEmpty()) {
                    io.gravitee.definition.model.v4.listener.http.Path path = httpListener.getPaths().get(0);
                    io.gravitee.definition.model.v4.listener.http.Path filteredPath = new io.gravitee.definition.model.v4.listener.http.Path(
                        path.getPath()
                    );
                    httpListener.setPaths(List.of(filteredPath));
                }
                httpListener.setPathMappings(null);
            }
        }
        apiEntity.setProperties(null);
        apiEntity.setServices(null);
        apiEntity.setResources(null);
        apiEntity.setResponseTemplates(null);
    }

    private void checkApiLifeCycle(ApiEntity api, LifecycleAction action) {
        if (ApiLifecycleState.ARCHIVED.equals(api.getLifecycleState())) {
            throw new BadRequestException("Deleted API can not be " + action.name().toLowerCase());
        }
        switch (api.getState()) {
            case STARTED:
                if (LifecycleAction.START.equals(action)) {
                    throw new BadRequestException("API is already started");
                }
                break;
            case STOPPED:
                if (LifecycleAction.STOP.equals(action)) {
                    throw new BadRequestException("API is already stopped");
                }

                final boolean apiReviewEnabled = parameterService.findAsBoolean(
                    GraviteeContext.getExecutionContext(),
                    Key.API_REVIEW_ENABLED,
                    ParameterReferenceType.ENVIRONMENT
                );
                if (apiReviewEnabled && api.getWorkflowState() != null && !WorkflowState.REVIEW_OK.equals(api.getWorkflowState())) {
                    throw new BadRequestException("API can not be started without being reviewed");
                }
                break;
            default:
                break;
        }
    }
}
