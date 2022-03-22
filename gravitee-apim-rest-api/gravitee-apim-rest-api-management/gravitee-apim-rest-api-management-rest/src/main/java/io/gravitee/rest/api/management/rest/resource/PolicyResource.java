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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Plugins")
public class PolicyResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private PolicyService policyService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a policy", description = "User must have the MANAGEMENT_API[READ] permission to use this service")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public PolicyEntity getPolicy(@PathParam("policy") String policy) {
        return policyService.findById(policy);
    }

    @GET
    @Path("schema")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a policy's schema", description = "User must have the MANAGEMENT_API[READ] permission to use this service")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public String getPolicySchema(@PathParam("policy") String policy) {
        // Check that the policy exists
        policyService.findById(policy);

        return policyService.getSchema(policy);
    }

    @GET
    @Path("icon")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get a policy's icon", description = "User must have the MANAGEMENT_API[READ] permission to use this service")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public String getPolicyIcon(@PathParam("policy") String policy) {
        // Check that the policy exists
        policyService.findById(policy);

        return policyService.getIcon(policy);
    }

    @GET
    @Path("documentation")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
        summary = "Get a policy's documentation",
        description = "User must have the MANAGEMENT_API[READ] permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public String getPolicyDoc(@PathParam("policy") String policy) {
        // Check that the policy exists
        policyService.findById(policy);

        return policyService.getDocumentation(policy);
    }
}
