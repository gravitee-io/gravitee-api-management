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

import io.gravitee.apim.core.portal_page.exception.PortalLinkNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.use_case.DeletePortalLinkUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetPortalLinkUseCase;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.PortalLinkMapper;
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
import java.util.List;

/**
 * @author GraviteeSource Team
 */
public class PortalLinkResource extends AbstractResource {

    @Inject
    private GetPortalLinkUseCase getPortalLinkUseCase;

    @Inject
    private DeletePortalLinkUseCase deletePortalLinkUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PORTAL, acls = RolePermissionAction.READ) })
    public Response getPortalLinkByHRID(@PathParam("hrid") String portalHrid, @PathParam("linkHrid") String linkHrid) {
        var auditInfo = getAuditInfo();
        var portalId = HRIDToUUID.portal().context(auditInfo).hrid(portalHrid).id();
        var linkId = PortalNavigationItemId.forPortalLink(auditInfo, portalId, linkHrid);
        try {
            var output = getPortalLinkUseCase.execute(new GetPortalLinkUseCase.Input(auditInfo, linkId));
            return Response.ok(PortalLinkMapper.INSTANCE.toPortalLinkState(output.link(), linkHrid, List.of(), portalHrid)).build();
        } catch (PortalLinkNotFoundException e) {
            throw new HRIDNotFoundException(linkHrid);
        }
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PORTAL, acls = RolePermissionAction.DELETE) })
    public Response deletePortalLinkByHrid(@PathParam("hrid") String portalHrid, @PathParam("linkHrid") String linkHrid) {
        var auditInfo = getAuditInfo();
        var portalId = HRIDToUUID.portal().context(auditInfo).hrid(portalHrid).id();
        var linkId = PortalNavigationItemId.forPortalLink(auditInfo, portalId, linkHrid);
        try {
            deletePortalLinkUseCase.execute(new DeletePortalLinkUseCase.Input(auditInfo, linkId));
        } catch (PortalLinkNotFoundException e) {
            throw new HRIDNotFoundException(linkHrid);
        }
        return Response.noContent().build();
    }
}
