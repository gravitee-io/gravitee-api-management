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
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.MembershipService;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Roles"})
public class RoleUserResource extends AbstractResource  {

    @Autowired
    private MembershipService membershipService;

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_ROLE, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("scope")RoleScope scope,
                       @PathParam("role") String role,
                       @PathParam("userId") String userId) {
        if (RoleScope.MANAGEMENT.equals(scope) || RoleScope.PORTAL.equals(scope)) {
            membershipService.deleteMember(
                    RoleScope.MANAGEMENT.equals(scope) ? MembershipReferenceType.MANAGEMENT : MembershipReferenceType.PORTAL,
                    MembershipDefaultReferenceId.DEFAULT.name(),
                    userId);
        }
    }
}
