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

import io.gravitee.apim.core.portal_listing.exception.PortalListingNotFoundException;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.portal_listing.use_case.DeletePortalListingUseCase;
import io.gravitee.apim.core.portal_listing.use_case.GetPortalListingUseCase;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.PortalListingMapper;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
public class PortalListingResource extends AbstractResource {

    @Inject
    private GetPortalListingUseCase getPortalListingUseCase;

    @Inject
    private DeletePortalListingUseCase deletePortalListingUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PORTAL, acls = RolePermissionAction.READ) })
    public Response getPortalListingByHRID(@PathParam("hrid") String portalHrid, @PathParam("listingHrid") String listingHrid) {
        var auditInfo = getAuditInfo();
        var listingId = PortalListingId.of(HRIDToUUID.portalListing().context(auditInfo).portal(portalHrid).hrid(listingHrid).id());
        try {
            var output = getPortalListingUseCase.execute(new GetPortalListingUseCase.Input(auditInfo, listingId));
            return Response.ok(PortalListingMapper.INSTANCE.toPortalListingState(output.portalListing(), listingHrid, portalHrid)).build();
        } catch (PortalListingNotFoundException e) {
            throw new HRIDNotFoundException(listingHrid);
        }
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PORTAL, acls = RolePermissionAction.DELETE) })
    public Response deletePortalListingByHrid(@PathParam("hrid") String portalHrid, @PathParam("listingHrid") String listingHrid) {
        var auditInfo = getAuditInfo();
        var listingId = PortalListingId.of(HRIDToUUID.portalListing().context(auditInfo).portal(portalHrid).hrid(listingHrid).id());
        try {
            deletePortalListingUseCase.execute(new DeletePortalListingUseCase.Input(auditInfo, listingId));
        } catch (PortalListingNotFoundException e) {
            throw new HRIDNotFoundException(listingHrid);
        }
        return Response.noContent().build();
    }
}
