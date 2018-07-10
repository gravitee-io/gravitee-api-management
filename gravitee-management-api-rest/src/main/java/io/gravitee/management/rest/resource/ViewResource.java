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
import io.gravitee.management.model.UpdateViewEntity;
import io.gravitee.management.model.ViewEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ViewService;
import io.gravitee.management.service.exceptions.UnauthorizedAccessException;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"View"})
public class ViewResource extends AbstractResource {

    @Autowired
    private ViewService viewService;

    @GET
    @Produces(APPLICATION_JSON)
    public ViewEntity get(@PathParam("id") String viewId) {
        boolean canShowView = hasPermission(RolePermission.PORTAL_VIEW, RolePermissionAction.READ);
        ViewEntity view = viewService.findById(viewId);

        if (canShowView || !view.isHidden()) {
            return view;
        }

        throw new UnauthorizedAccessException();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_VIEW, acls = RolePermissionAction.UPDATE)
    })
    public ViewEntity update(@PathParam("id") String viewId, @Valid @NotNull final UpdateViewEntity view) {
        return viewService.update(viewId, view);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_VIEW, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("id") String id) {
        viewService.delete(id);
    }

}
