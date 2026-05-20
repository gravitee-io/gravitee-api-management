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
import io.gravitee.gamma.authorization.api.AuthzPolicyAdminApi;
import io.gravitee.gamma.authorization.domain.AuthzPolicy;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicyStatus;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.rest.dto.AuthzPolicyRequest;
import io.gravitee.gamma.authorization.rest.dto.AuthzPolicyResponse;
import io.gravitee.gamma.authorization.rest.dto.PagedResponseDto;
import io.gravitee.gamma.authorization.rest.dto.UpdateAuthzPolicyRequest;
import io.gravitee.gamma.authorization.service.AuthzPolicyFilter;
import io.gravitee.gamma.authorization.service.CreateAuthzPolicyCommand;
import io.gravitee.gamma.authorization.service.UpdateAuthzPolicyCommand;
import io.gravitee.gamma.authorization.service.exception.AuthzPolicyNotFoundException;
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

@Path("/environments/{environmentId}/policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthzPoliciesResource {

    private final AuthzPolicyAdminApi service;

    @Context
    private SecurityContext securityContext;

    @Inject
    public AuthzPoliciesResource(AuthzPolicyAdminApi service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.CREATE }) })
    public Response create(@PathParam("environmentId") String environmentId, AuthzPolicyRequest request) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        AuthzPolicy created = service.create(
            caller,
            new CreateAuthzPolicyCommand(environmentId, request.name(), request.kind(), request.entityId(), request.policyText())
        );
        return Response.status(Response.Status.CREATED).entity(AuthzPolicyResponse.from(created)).build();
    }

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public PagedResponseDto<AuthzPolicyResponse> list(
        @PathParam("environmentId") String environmentId,
        @QueryParam("kind") AuthzPolicyKind kind,
        @QueryParam("entityId") String entityId,
        @QueryParam("status") AuthzPolicyStatus status,
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage
    ) {
        Pageable pageable = (page == null && perPage == null)
            ? Pageable.firstPage()
            : Pageable.of(page == null ? 1 : page, perPage == null ? Pageable.DEFAULT_PER_PAGE : perPage);
        PagedResult<AuthzPolicy> result = service.findPage(environmentId, new AuthzPolicyFilter(kind, entityId, status), pageable);
        return PagedResponseDto.from(result, AuthzPolicyResponse::from);
    }

    @GET
    @Path("/{id}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public AuthzPolicyResponse findById(@PathParam("environmentId") String environmentId, @PathParam("id") String id) {
        return service
            .findById(environmentId, id)
            .map(AuthzPolicyResponse::from)
            .orElseThrow(() -> new AuthzPolicyNotFoundException(environmentId, id));
    }

    @PUT
    @Path("/{id}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public AuthzPolicyResponse update(
        @PathParam("environmentId") String environmentId,
        @PathParam("id") String id,
        UpdateAuthzPolicyRequest request
    ) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        AuthzPolicy updated = service.update(caller, id, new UpdateAuthzPolicyCommand(request.name(), request.policyText()));
        return AuthzPolicyResponse.from(updated);
    }

    @POST
    @Path("/{id}/deploy")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public AuthzPolicyResponse deploy(@PathParam("environmentId") String environmentId, @PathParam("id") String id) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        return AuthzPolicyResponse.from(service.deploy(caller, id));
    }

    @POST
    @Path("/{id}/disable")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public AuthzPolicyResponse disable(@PathParam("environmentId") String environmentId, @PathParam("id") String id) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        return AuthzPolicyResponse.from(service.disable(caller, id));
    }

    @DELETE
    @Path("/{id}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.DELETE }) })
    public Response delete(@PathParam("environmentId") String environmentId, @PathParam("id") String id) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        service.delete(caller, id);
        return Response.noContent().build();
    }
}
