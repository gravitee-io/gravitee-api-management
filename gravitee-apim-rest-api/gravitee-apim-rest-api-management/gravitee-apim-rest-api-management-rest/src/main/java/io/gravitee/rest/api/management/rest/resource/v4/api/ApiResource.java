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
package io.gravitee.rest.api.management.rest.resource.v4.api;

import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.ListenerHttp;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.exception.InvalidImageException;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.JsonPatchService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
@Hidden
@Tag(name = "APIs")
public class ApiResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @PathParam("api")
    @Parameter(name = "api", required = true, description = "The ID of the API")
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get the API",
        description = "User must have the READ permission on the API_DEFINITION to use this service on a private API."
    )
    @ApiResponse(
        responseCode = "200",
        description = "API definition",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getApi() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApiEntity apiEntity = apiServiceV4.findById(executionContext, api);

        if (!canManageV4Api(apiEntity)) {
            throw new ForbiddenAccessException();
        }

        if (hasPermission(executionContext, RolePermission.API_DEFINITION, api, RolePermissionAction.READ)) {
            setPictures(apiEntity);
        } else {
            filterSensitiveData(apiEntity);
        }
        return Response.ok(apiEntity).tag(Long.toString(apiEntity.getUpdatedAt().getTime())).lastModified(apiEntity.getUpdatedAt()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update the API", description = "User must have the MANAGE_API permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "API successfully updated",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = io.gravitee.rest.api.model.api.ApiEntity.class)
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.API_GATEWAY_DEFINITION, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response updateApi(@Context HttpHeaders headers, @Valid @NotNull final UpdateApiEntity apiToUpdate) {
        if (!api.equals(apiToUpdate.getId())) {
            throw new BadRequestException("'apiId' is not the same that the API in payload");
        }

        final Response responseApi = getApi();
        Response.ResponseBuilder builder = evaluateIfMatch(headers, responseApi.getEntityTag().getValue());

        if (builder != null) {
            return builder.build();
        }

        try {
            ImageUtils.verify(apiToUpdate.getPicture());
            ImageUtils.verify(apiToUpdate.getBackground());
        } catch (InvalidImageException e) {
            throw new BadRequestException("Invalid image format");
        }

        final ApiEntity currentApi = (ApiEntity) responseApi.getEntity();

        // Force listeners if user is not the primary_owner or an administrator
        if (
            !hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_GATEWAY_DEFINITION,
                api,
                RolePermissionAction.UPDATE
            ) &&
            !Objects.equals(currentApi.getPrimaryOwner().getId(), getAuthenticatedUser()) &&
            !isAdmin()
        ) {
            apiToUpdate.setListeners(currentApi.getListeners());
        }

        final ApiEntity updatedApi = apiServiceV4.update(GraviteeContext.getExecutionContext(), api, apiToUpdate, getAuthenticatedUser());
        setPictures(updatedApi);

        return Response
            .ok(updatedApi)
            .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
            .lastModified(updatedApi.getUpdatedAt())
            .build();
    }

    @DELETE
    @Operation(summary = "Delete the API", description = "User must have the DELETE permission to use this service")
    @ApiResponse(responseCode = "204", description = "API successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.DELETE) })
    public Response deleteApi() {
        apiServiceV4.delete(GraviteeContext.getExecutionContext(), api);

        return Response.noContent().build();
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
                ListenerHttp httpListener = (ListenerHttp) first.get();
                if (httpListener.getPaths() != null && !httpListener.getPaths().isEmpty()) {
                    Path path = httpListener.getPaths().get(0);
                    Path filteredPath = new Path(path.getPath());
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
}
