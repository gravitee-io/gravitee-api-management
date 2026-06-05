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

import io.gravitee.gamma.authorization.api.AuthzSchemaAdminApi;
import io.gravitee.gamma.authorization.rest.dto.AuthzSchemaRequest;
import io.gravitee.gamma.authorization.rest.dto.AuthzSchemaResponse;
import io.gravitee.gamma.authorization.rest.dto.SchemaValidationResponse;
import io.gravitee.gamma.authorization.rest.exception.AuthzCalls;
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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;

@Path("/schema")
@Produces(MediaType.APPLICATION_JSON)
public class AuthzSchemaResource {

    private final AuthzSchemaAdminApi schemaService;

    @Inject
    public AuthzSchemaResource(AuthzSchemaAdminApi schemaService) {
        this.schemaService = Objects.requireNonNull(schemaService, "schemaService must not be null");
    }

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public AuthzSchemaResponse currentSchema() {
        return AuthzCalls.execute(() ->
            new AuthzSchemaResponse(schemaService.getSchema(GraviteeContext.getCurrentEnvironment()).orElse("")));
    }

    @GET
    @Path("/parsed")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public Response parsedSchema() {
        return AuthzCalls.execute(() ->
            Response.ok(schemaService.parsedSchema(GraviteeContext.getCurrentEnvironment())).build());
    }

    @POST
    @Path("/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public SchemaValidationResponse validateSchema(@Valid AuthzSchemaRequest request) {
        return AuthzCalls.execute(() -> {
            List<String> errors = schemaService.validate(request.schema());
            return new SchemaValidationResponse(errors.isEmpty(), errors);
        });
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public AuthzSchemaResponse updateSchema(@Valid AuthzSchemaRequest request) {
        return AuthzCalls.execute(() -> {
            String env = GraviteeContext.getCurrentEnvironment();
            schemaService.saveSchema(env, request.schema());
            return new AuthzSchemaResponse(schemaService.getSchema(env).orElse(""));
        });
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.DELETE }) })
    public Response deleteSchema() {
        return AuthzCalls.execute(() -> {
            schemaService.deleteSchema(GraviteeContext.getCurrentEnvironment());
            return Response.noContent().build();
        });
    }
}
