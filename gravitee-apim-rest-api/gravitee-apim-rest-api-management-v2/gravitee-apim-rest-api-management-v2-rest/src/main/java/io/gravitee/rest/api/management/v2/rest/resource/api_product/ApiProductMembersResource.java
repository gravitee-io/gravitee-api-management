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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.ApiProductPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import lombok.CustomLog;

@CustomLog
public class ApiProductMembersResource extends AbstractResource {

    @PathParam("apiProductId")
    private String apiProductId;

    @GET
    @Path("/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiProductMemberPermissions() {
        Map<String, char[]> permissions = new HashMap<>();
        if (isAuthenticated()) {
            final String userId = getAuthenticatedUser();
            if (isAdmin()) {
                final char[] rights = new char[] {
                    RolePermissionAction.CREATE.getId(),
                    RolePermissionAction.READ.getId(),
                    RolePermissionAction.UPDATE.getId(),
                    RolePermissionAction.DELETE.getId(),
                };
                for (ApiProductPermission perm : ApiProductPermission.values()) {
                    permissions.put(perm.getName(), rights);
                }
            } else {
                permissions = membershipService.getUserMemberPermissions(
                    GraviteeContext.getExecutionContext(),
                    MembershipReferenceType.API_PRODUCT,
                    apiProductId,
                    userId
                );
            }
        }
        return Response.ok(permissions).build();
    }
}
