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
import io.gravitee.management.model.MetadataEntity;
import io.gravitee.management.model.NewMetadataEntity;
import io.gravitee.management.model.UpdateMetadataEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.MetadataService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Metadata"})
public class MetadataResource extends AbstractResource {

    @Autowired
    private MetadataService metadataService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_METADATA, acls = RolePermissionAction.READ)
    })
    public List<MetadataEntity> list() {
        return metadataService.findAllDefault();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_METADATA, acls = RolePermissionAction.CREATE)
    })
    public MetadataEntity create(@Valid @NotNull final NewMetadataEntity metadata) {
        return metadataService.create(metadata);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_METADATA, acls = RolePermissionAction.UPDATE)
    })
    public MetadataEntity update(@Valid @NotNull final UpdateMetadataEntity metadata) {
        return metadataService.update(metadata);
    }

    @Path("{metadata}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_METADATA, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("metadata") String metadata) {
        metadataService.delete(metadata);
    }
}
