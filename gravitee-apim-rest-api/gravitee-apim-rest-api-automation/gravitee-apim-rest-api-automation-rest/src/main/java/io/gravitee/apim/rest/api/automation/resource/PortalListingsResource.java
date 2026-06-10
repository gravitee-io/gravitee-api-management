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
package io.gravitee.apim.rest.api.automation.resource;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.portal_listing.use_case.CreateOrUpdatePortalListingUseCase;
import io.gravitee.apim.core.portal_listing.use_case.ValidatePortalListingUseCase;
import io.gravitee.apim.rest.api.automation.mapper.PortalListingMapper;
import io.gravitee.apim.rest.api.automation.model.PortalListingSpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
public class PortalListingsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateOrUpdatePortalListingUseCase createOrUpdatePortalListingUseCase;

    @Inject
    private ValidatePortalListingUseCase validatePortalListingUseCase;

    @Path("/{listingHrid}")
    public PortalListingResource getPortalListingResource() {
        return resourceContext.getResource(PortalListingResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PORTAL, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(
        @PathParam("hrid") String portalHrid,
        @Valid @NotNull PortalListingSpec spec,
        @QueryParam("dryRun") boolean dryRun
    ) {
        var auditInfo = getAuditInfo();
        var apis = PortalListingMapper.INSTANCE.toDomainApis(spec.getApis());
        var portalId = PortalId.of(HRIDToUUID.portal().context(auditInfo).hrid(portalHrid).id());
        var listingId = PortalListingId.of(HRIDToUUID.portalListing().context(auditInfo).portal(portalHrid).hrid(spec.getHrid()).id());

        var input = new CreateOrUpdatePortalListingUseCase.Input(auditInfo, listingId, portalId, apis);
        var output = dryRun ? validatePortalListingUseCase.execute(input) : createOrUpdatePortalListingUseCase.execute(input);

        return Response.ok(
            PortalListingMapper.INSTANCE.toPortalListingState(spec, output.id(), output.errors(), auditInfo, portalHrid)
        ).build();
    }
}
