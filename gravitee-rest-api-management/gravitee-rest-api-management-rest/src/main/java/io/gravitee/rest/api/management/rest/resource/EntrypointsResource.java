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
import io.gravitee.rest.api.model.EntrypointEntity;
import io.gravitee.rest.api.model.NewEntryPointEntity;
import io.gravitee.rest.api.model.UpdateEntryPointEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.service.EntrypointService;
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
    @Path("{entrypointId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.READ)
    })
    public EntrypointEntity get(final @PathParam("entrypointId") String entrypointId)  {
        return entrypointService.findById(entrypointId);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.READ)
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
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.CREATE)
    })
    public EntrypointEntity create(@Valid @NotNull final NewEntryPointEntity entrypoint) {
        return entrypointService.create(entrypoint);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.UPDATE)
    })
    public EntrypointEntity update(@Valid @NotNull final UpdateEntryPointEntity entrypoint) {
        return entrypointService.update(entrypoint);
    }

    @Path("{entrypoint}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("entrypoint") String entrypoint) {
        entrypointService.delete(entrypoint);
    }
}
