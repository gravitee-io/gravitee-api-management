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
import io.gravitee.apim.authorization.api.PolicyAdminApi;
import io.gravitee.apim.authorization.domain.Policy;
import io.gravitee.apim.authorization.domain.PolicyKind;
import io.gravitee.apim.authorization.service.CreatePolicyCommand;
import io.gravitee.apim.authorization.service.UpdatePolicyCommand;
import io.gravitee.apim.authorization.service.exception.PolicyNotFoundException;
import io.gravitee.gamma.authz.rest.dto.PolicyRequest;
import io.gravitee.gamma.authz.rest.dto.PolicyResponse;
import io.gravitee.gamma.authz.rest.dto.UpdatePolicyRequest;
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
public class PoliciesResource {

    private final PolicyAdminApi service;

    @Context
    private SecurityContext securityContext;

    @Inject
    public PoliciesResource(PolicyAdminApi service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.CREATE }) })
    public Response create(@PathParam("environmentId") String environmentId, PolicyRequest request) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        Policy created = service.create(
            caller,
            new CreatePolicyCommand(environmentId, request.name(), request.kind(), request.entityId(), request.policyText())
        );
        return Response.status(Response.Status.CREATED).entity(PolicyResponse.from(created)).build();
    }

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public List<PolicyResponse> list(
        @PathParam("environmentId") String environmentId,
        @QueryParam("kind") PolicyKind kind,
        @QueryParam("entityId") String entityId
    ) {
        List<Policy> policies;
        if (entityId != null) {
            policies = service.findByEntityId(environmentId, entityId);
        } else if (kind != null) {
            policies = service.findByKind(environmentId, kind);
        } else {
            policies = service.findAll(environmentId);
        }
        return policies.stream().map(PolicyResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public PolicyResponse findById(@PathParam("environmentId") String environmentId, @PathParam("id") String id) {
        return service
            .findById(environmentId, id)
            .map(PolicyResponse::from)
            .orElseThrow(() -> new PolicyNotFoundException(environmentId, id));
    }

    @PUT
    @Path("/{id}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public PolicyResponse update(
        @PathParam("environmentId") String environmentId,
        @PathParam("id") String id,
        UpdatePolicyRequest request
    ) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        Policy updated = service.update(caller, id, new UpdatePolicyCommand(request.name(), request.policyText()));
        return PolicyResponse.from(updated);
    }

    @POST
    @Path("/{id}/deploy")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public PolicyResponse deploy(@PathParam("environmentId") String environmentId, @PathParam("id") String id) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        return PolicyResponse.from(service.deploy(caller, id));
    }

    @POST
    @Path("/{id}/disable")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public PolicyResponse disable(@PathParam("environmentId") String environmentId, @PathParam("id") String id) {
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext, environmentId);
        return PolicyResponse.from(service.disable(caller, id));
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
