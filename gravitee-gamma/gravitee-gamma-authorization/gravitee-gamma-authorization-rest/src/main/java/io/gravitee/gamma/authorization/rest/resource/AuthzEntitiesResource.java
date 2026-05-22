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
package io.gravitee.gamma.authorization.rest.resource;

import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.AuthzEntityAdminApi;
import io.gravitee.gamma.authorization.domain.AuthzEntity;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.rest.dto.AuthzCascadeResponse;
import io.gravitee.gamma.authorization.rest.dto.AuthzEntityRequest;
import io.gravitee.gamma.authorization.rest.dto.AuthzEntityResponse;
import io.gravitee.gamma.authorization.rest.dto.PagedResponseDto;
import io.gravitee.gamma.authorization.rest.dto.UpdateAuthzEntityRequest;
import io.gravitee.gamma.authorization.service.AuthzEntityFilter;
import io.gravitee.gamma.authorization.service.AuthzUpsertResult;
import io.gravitee.gamma.authorization.service.CreateOrReplaceAuthzEntityCommand;
import io.gravitee.gamma.authorization.service.UpdateAuthzEntityCommand;
import io.gravitee.gamma.authorization.service.exception.AuthzEntityNotFoundException;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Objects;

@Tag(name = "Authz Entities", description = "Fine-grained authorization entities (resources, principals, groups)")
@Path("/environments/{environmentId}/entities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthzEntitiesResource {

    private final AuthzEntityAdminApi service;

    @Context
    private SecurityContext securityContext;

    @Inject
    public AuthzEntitiesResource(AuthzEntityAdminApi service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    @POST
    @Operation(summary = "Create or replace an authorization entity")
    @ApiResponse(responseCode = "201", description = "Entity created")
    @ApiResponse(responseCode = "200", description = "Existing entity replaced")
    @ApiResponse(responseCode = "400", description = "Invalid request body or entity id")
    @Permissions(
        {
            @Permission(
                value = RolePermission.ENVIRONMENT_AUTHORIZATION,
                acls = { RolePermissionAction.CREATE, RolePermissionAction.UPDATE }
            ),
        }
    )
    public Response upsert(@PathParam("environmentId") String environmentId, @Valid AuthzEntityRequest request) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        AuthzUpsertResult result = service.upsert(
            caller,
            new CreateOrReplaceAuthzEntityCommand(
                environmentId,
                request.entityId(),
                request.kind(),
                request.attributes(),
                request.parents(),
                request.source()
            )
        );
        Response.Status status = result.created() ? Response.Status.CREATED : Response.Status.OK;
        return Response.status(status).entity(AuthzEntityResponse.from(result.entity())).build();
    }

    @GET
    @Operation(summary = "List authorization entities with optional filters")
    @ApiResponse(responseCode = "200", description = "Page of entities")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public PagedResponseDto<AuthzEntityResponse> list(
        @PathParam("environmentId") String environmentId,
        @QueryParam("kind") AuthzEntityKind kind,
        @QueryParam("source") String source,
        @QueryParam("entityIdPrefix") String entityIdPrefix,
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage
    ) {
        Pageable pageable = Pageable.fromQuery(page, perPage);
        PagedResult<AuthzEntity> result = service.findPage(environmentId, new AuthzEntityFilter(kind, source, entityIdPrefix), pageable);
        return PagedResponseDto.from(result, AuthzEntityResponse::from);
    }

    @GET
    @Path("/{entityId}")
    @Operation(summary = "Fetch a single entity by entityId")
    @ApiResponse(responseCode = "200", description = "Entity found")
    @ApiResponse(responseCode = "404", description = "Entity not found")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public AuthzEntityResponse findByEntityId(@PathParam("environmentId") String environmentId, @PathParam("entityId") String entityId) {
        return service
            .findByEntityId(environmentId, entityId)
            .map(AuthzEntityResponse::from)
            .orElseThrow(() -> new AuthzEntityNotFoundException(environmentId, entityId));
    }

    @PUT
    @Path("/{entityId}")
    @Operation(summary = "Update attributes and parents of an existing entity")
    @ApiResponse(responseCode = "200", description = "Entity updated")
    @ApiResponse(responseCode = "404", description = "Entity not found")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public AuthzEntityResponse update(
        @PathParam("environmentId") String environmentId,
        @PathParam("entityId") String entityId,
        @Valid UpdateAuthzEntityRequest request
    ) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        AuthzEntity updated = service.update(caller, entityId, new UpdateAuthzEntityCommand(request.attributes(), request.parents()));
        return AuthzEntityResponse.from(updated);
    }

    @DELETE
    @Path("/{entityId}")
    @Operation(
        summary = "Delete an entity and all dependent entities/policies",
        description = "Cascade-deletes children; aborts with 413 if the cascade exceeds the configured hard limit"
    )
    @ApiResponse(responseCode = "200", description = "Entity (and cascade) deleted")
    @ApiResponse(responseCode = "413", description = "Cascade exceeds the configured hard limit")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.DELETE }) })
    public AuthzCascadeResponse delete(@PathParam("environmentId") String environmentId, @PathParam("entityId") String entityId) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        return AuthzCascadeResponse.from(service.delete(caller, entityId));
    }
}
