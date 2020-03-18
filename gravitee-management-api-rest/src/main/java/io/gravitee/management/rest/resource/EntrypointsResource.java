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
import io.gravitee.management.model.EntrypointEntity;
import io.gravitee.management.model.NewEntryPointEntity;
import io.gravitee.management.model.UpdateEntryPointEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.EntrypointService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Entrypoints"})
public class EntrypointsResource extends AbstractResource  {

    @Autowired
    private EntrypointService entrypointService;

    @GET
    @Path("{entrypoint}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_ENTRYPOINT, acls = RolePermissionAction.READ)
    })
    public EntrypointEntity get(final @PathParam("entrypoint") String entrypointId)  {
        return entrypointService.findById(entrypointId);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_ENTRYPOINT, acls = RolePermissionAction.READ)
    })
    public List<EntrypointEntity> list()  {
        return entrypointService.findAll()
                .stream()
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getValue(), o2.getValue()))
                .collect(toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_ENTRYPOINT, acls = RolePermissionAction.CREATE)
    })
    public EntrypointEntity create(@Valid @NotNull final NewEntryPointEntity entrypoint) {
        return entrypointService.create(entrypoint);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_ENTRYPOINT, acls = RolePermissionAction.UPDATE)
    })
    public EntrypointEntity update(@Valid @NotNull final UpdateEntryPointEntity entrypoint) {
        return entrypointService.update(entrypoint);
    }

    @Path("{entrypoint}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_ENTRYPOINT, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("entrypoint") String entrypoint) {
        entrypointService.delete(entrypoint);
    }
}
