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
package io.gravitee.gamma.authz.rest.resource;

import io.gravitee.apim.authorization.api.AuthzCallerContext;
import io.gravitee.apim.authorization.api.EntityAdminApi;
import io.gravitee.apim.authorization.domain.Entity;
import io.gravitee.apim.authorization.domain.EntityKind;
import io.gravitee.apim.authorization.service.CreateOrReplaceEntityCommand;
import io.gravitee.apim.authorization.service.EntityFilter;
import io.gravitee.apim.authorization.service.Pageable;
import io.gravitee.apim.authorization.service.PagedResult;
import io.gravitee.apim.authorization.service.UpdateEntityCommand;
import io.gravitee.apim.authorization.service.UpsertResult;
import io.gravitee.apim.authorization.service.exception.EntityNotFoundException;
import io.gravitee.gamma.authz.rest.dto.CascadeResponse;
import io.gravitee.gamma.authz.rest.dto.EntityRequest;
import io.gravitee.gamma.authz.rest.dto.EntityResponse;
import io.gravitee.gamma.authz.rest.dto.PagedResponseDto;
import io.gravitee.gamma.authz.rest.dto.UpdateEntityRequest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
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

@Path("/environments/{environmentId}/entities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EntitiesResource {

    private final EntityAdminApi service;

    @Context
    private SecurityContext securityContext;

    @Inject
    public EntitiesResource(EntityAdminApi service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    @POST
    @Permissions(
        {
            @Permission(
                value = RolePermission.ENVIRONMENT_AUTHORIZATION,
                acls = { RolePermissionAction.CREATE, RolePermissionAction.UPDATE }
            ),
        }
    )
    public Response upsert(@PathParam("environmentId") String environmentId, EntityRequest request) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        UpsertResult result = service.upsert(
            caller,
            new CreateOrReplaceEntityCommand(
                environmentId,
                request.entityId(),
                request.kind(),
                request.attributes(),
                request.parents(),
                request.source()
            )
        );
        Response.Status status = result.created() ? Response.Status.CREATED : Response.Status.OK;
        return Response.status(status).entity(EntityResponse.from(result.entity())).build();
    }

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public PagedResponseDto<EntityResponse> list(
        @PathParam("environmentId") String environmentId,
        @QueryParam("kind") EntityKind kind,
        @QueryParam("source") String source,
        @QueryParam("entityIdPrefix") String entityIdPrefix,
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage
    ) {
        // Default to Pageable.firstPage() so callers that omit ?page/?perPage
        // still get a paged response and don't have to know about defaults.
        Pageable pageable = (page == null && perPage == null)
            ? Pageable.firstPage()
            : Pageable.of(page == null ? 1 : page, perPage == null ? Pageable.DEFAULT_PER_PAGE : perPage);
        PagedResult<Entity> result = service.findPage(environmentId, new EntityFilter(kind, source, entityIdPrefix), pageable);
        return PagedResponseDto.from(result, EntityResponse::from);
    }

    @GET
    @Path("/{entityId}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public EntityResponse findByEntityId(@PathParam("environmentId") String environmentId, @PathParam("entityId") String entityId) {
        return service
            .findByEntityId(environmentId, entityId)
            .map(EntityResponse::from)
            .orElseThrow(() -> new EntityNotFoundException(environmentId, entityId));
    }

    @PUT
    @Path("/{entityId}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public EntityResponse update(
        @PathParam("environmentId") String environmentId,
        @PathParam("entityId") String entityId,
        UpdateEntityRequest request
    ) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        Entity updated = service.update(caller, entityId, new UpdateEntityCommand(request.attributes(), request.parents()));
        return EntityResponse.from(updated);
    }

    @DELETE
    @Path("/{entityId}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.DELETE }) })
    public CascadeResponse delete(@PathParam("environmentId") String environmentId, @PathParam("entityId") String entityId) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        return CascadeResponse.from(service.delete(caller, entityId));
    }
}
