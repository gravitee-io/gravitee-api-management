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
import io.gravitee.gamma.authorization.rest.exception.AuthzCalls;
import io.gravitee.gamma.authorization.service.AuthzEntityFilter;
import io.gravitee.gamma.authorization.service.AuthzUpsertResult;
import io.gravitee.gamma.authorization.service.CreateOrReplaceAuthzEntityCommand;
import io.gravitee.gamma.authorization.service.UpdateAuthzEntityCommand;
import io.gravitee.gamma.authorization.service.exception.AuthzEntityNotFoundException;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
import java.util.Objects;

@Path("/entities")
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
    @Permissions(
        {
            @Permission(
                value = RolePermission.ENVIRONMENT_AUTHORIZATION,
                acls = { RolePermissionAction.CREATE, RolePermissionAction.UPDATE }
            ),
        }
    )
    public Response upsert(@Valid AuthzEntityRequest request) {
        return AuthzCalls.execute(() -> {
            AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext);
            AuthzUpsertResult result = service.upsert(
                caller,
                new CreateOrReplaceAuthzEntityCommand(
                    caller.environmentId(),
                    request.entityId(),
                    request.kind(),
                    request.attributes(),
                    request.parents(),
                    request.source()
                )
            );
            Response.Status status = result.created() ? Response.Status.CREATED : Response.Status.OK;
            return Response.status(status).entity(AuthzEntityResponse.from(result.entity())).build();
        });
    }

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public PagedResponseDto<AuthzEntityResponse> list(
        @QueryParam("kind") AuthzEntityKind kind,
        @QueryParam("source") String source,
        @QueryParam("entityIdPrefix") String entityIdPrefix,
        @QueryParam("excludeEntityIdPrefix") String excludeEntityIdPrefix,
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage
    ) {
        return AuthzCalls.execute(() -> {
            String env = GraviteeContext.getCurrentEnvironment();
            Pageable pageable = (page == null && perPage == null)
                ? Pageable.firstPage()
                : Pageable.of(page == null ? 1 : page, perPage == null ? Pageable.DEFAULT_PER_PAGE : perPage);
            PagedResult<AuthzEntity> result = service.findPage(
                env,
                new AuthzEntityFilter(kind, source, entityIdPrefix, excludeEntityIdPrefix),
                pageable
            );
            return PagedResponseDto.from(result, AuthzEntityResponse::from);
        });
    }

    @GET
    @Path("/{entityId}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public AuthzEntityResponse findByEntityId(@PathParam("entityId") String entityId) {
        return AuthzCalls.execute(() -> {
            String env = GraviteeContext.getCurrentEnvironment();
            return service
                .findByEntityId(env, entityId)
                .map(AuthzEntityResponse::from)
                .orElseThrow(() -> new AuthzEntityNotFoundException(env, entityId));
        });
    }

    @PUT
    @Path("/{entityId}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public AuthzEntityResponse update(@PathParam("entityId") String entityId, @Valid UpdateAuthzEntityRequest request) {
        return AuthzCalls.execute(() -> {
            AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext);
            AuthzEntity updated = service.update(caller, entityId, new UpdateAuthzEntityCommand(request.attributes(), request.parents()));
            return AuthzEntityResponse.from(updated);
        });
    }

    @DELETE
    @Path("/{entityId}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.DELETE }) })
    public AuthzCascadeResponse delete(@PathParam("entityId") String entityId) {
        return AuthzCalls.execute(() -> AuthzCascadeResponse.from(service.delete(AuthzCallerResolver.resolve(securityContext), entityId)));
    }
}
