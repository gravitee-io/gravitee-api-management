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
import io.gravitee.management.model.api.header.ApiHeaderEntity;
import io.gravitee.management.model.api.header.UpdateApiHeaderEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiHeaderService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Configuration"})
public class ApiHeaderResource extends AbstractResource {

    @Autowired
    private ApiHeaderService apiHeaderService;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_API_HEADER, acls = RolePermissionAction.UPDATE)
    })
    public ApiHeaderEntity update(@PathParam("id") String id, @Valid @NotNull final UpdateApiHeaderEntity updateApiHeaderEntity) {
        updateApiHeaderEntity.setId(id);
        return apiHeaderService.update(updateApiHeaderEntity);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_API_HEADER, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("id") String id) {
        apiHeaderService.delete(id);
    }

}
