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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.PolicyEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.PolicyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

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
@Api(tags = {"Plugins"})
public class PolicyResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private PolicyService policyService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a policy",
            notes = "User must have the MANAGEMENT_API[READ] permission to use this service")
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_API, acls = RolePermissionAction.READ)
    })
    public PolicyEntity getPolicy(
            @PathParam("policy") String policy) {
        return policyService.findById(policy);
    }

    @GET
    @Path("schema")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a policy's schema",
            notes = "User must have the MANAGEMENT_API[READ] permission to use this service")
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_API, acls = RolePermissionAction.READ)
    })
    public String getPolicySchema(
            @PathParam("policy") String policy) {
        // Check that the policy exists
        policyService.findById(policy);

        return policyService.getSchema(policy);
    }
}
