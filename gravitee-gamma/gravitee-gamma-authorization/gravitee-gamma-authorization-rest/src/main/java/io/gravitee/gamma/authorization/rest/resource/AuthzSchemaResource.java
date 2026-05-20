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
import io.gravitee.gamma.authorization.rest.dto.AuthzSchemaResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Objects;

// TODO(authz): when a write endpoint (POST/PUT) is added here, wire schema
//   cache invalidation across pods. Today this resource is read-only so a
//   per-pod cache is safe; a write that bypasses cross-pod invalidation will
//   leave stale schemas on every other replica.
@Path("/environments/{environmentId}/schema")
@Produces(MediaType.APPLICATION_JSON)
public class AuthzSchemaResource {

    private final AuthzSchemaAdminApi schemaService;

    @Inject
    public AuthzSchemaResource(AuthzSchemaAdminApi schemaService) {
        this.schemaService = Objects.requireNonNull(schemaService, "schemaService must not be null");
    }

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public AuthzSchemaResponse currentSchema(@PathParam("environmentId") String environmentId) {
        return new AuthzSchemaResponse(schemaService.currentGaplSchema(environmentId));
    }
}
