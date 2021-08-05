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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import io.gravitee.rest.api.model.UpdateApplicationMetadataEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.ReferenceMetadataMapper;
import io.gravitee.rest.api.portal.rest.model.ReferenceMetadata;
import io.gravitee.rest.api.portal.rest.model.ReferenceMetadataInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.ApplicationMetadataService;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationMetadataResource extends AbstractResource {

    @Inject
    private ApplicationMetadataService metadataService;

    @Inject
    private ReferenceMetadataMapper referenceMetadataMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.READ) })
    public Response getMetadataByApplicationId(
        @PathParam("applicationId") String applicationId,
        @BeanParam PaginationParam paginationParam
    ) {
        List<ReferenceMetadata> applicationMetadata = metadataService
            .findAllByApplication(applicationId)
            .stream()
            .map(this.referenceMetadataMapper::convert)
            .collect(Collectors.toList());
        return Response.ok(this.createDataResponse(applicationMetadata, paginationParam, null, true)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.CREATE) })
    public Response createApplicationMetadata(
        @PathParam("applicationId") String applicationId,
        @Valid @NotNull final ReferenceMetadataInput metadata
    ) {
        // prevent creation of a metadata on an another APPLICATION
        NewApplicationMetadataEntity newApplicationMetadataEntity = this.referenceMetadataMapper.convert(metadata, applicationId);

        final ApplicationMetadataEntity applicationMetadataEntity = metadataService.create(newApplicationMetadataEntity);
        return Response
            .created(this.getLocationHeader(applicationMetadataEntity.getKey()))
            .entity(this.referenceMetadataMapper.convert(applicationMetadataEntity))
            .build();
    }

    @GET
    @Path("{metadataId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.READ) })
    public Response getApplicationMetadataByApplicationIdAndMetadataId(
        @PathParam("applicationId") String applicationId,
        @PathParam("metadataId") String metadataId
    ) {
        return Response.ok(this.referenceMetadataMapper.convert(metadataService.findByIdAndApplication(metadataId, applicationId))).build();
    }

    @PUT
    @Path("{metadataId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.UPDATE) })
    public Response updateApplicationMetadataByApplicationIdAndMetadataId(
        @PathParam("applicationId") String applicationId,
        @PathParam("metadataId") String metadataId,
        @Valid @NotNull final ReferenceMetadataInput metadata
    ) {
        // prevent creation of a metadata on an another APPLICATION
        UpdateApplicationMetadataEntity updateApplicationMetadataEntity =
            this.referenceMetadataMapper.convert(metadata, applicationId, metadataId);

        return Response.ok(this.referenceMetadataMapper.convert(metadataService.update(updateApplicationMetadataEntity))).build();
    }

    @DELETE
    @Path("{metadataId}")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.DELETE) })
    public Response deleteApplicationMetadata(
        @PathParam("applicationId") String applicationId,
        @PathParam("metadataId") String metadataId
    ) {
        metadataService.delete(metadataId, applicationId);
        return Response.noContent().build();
    }
}
