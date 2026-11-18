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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import io.gravitee.apim.core.portal.model.PortalLogo;
import io.gravitee.apim.core.portal.use_case.GetPortalLogoUseCase;
import io.gravitee.apim.core.portal.use_case.GetPortalSummaryUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalMapper;
import io.gravitee.rest.api.management.v2.rest.model.Portal;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class PortalResource extends AbstractResource {

    @Inject
    private GetPortalSummaryUseCase getPortalSummaryUseCase;

    @Inject
    private GetPortalLogoUseCase getPortalLogoUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.READ) })
    public Portal getPortal(@PathParam("portalId") String portalId) {
        var output = getPortalSummaryUseCase.execute(
            new GetPortalSummaryUseCase.Input(GraviteeContext.getCurrentEnvironment(), PortalMapper.INSTANCE.toPortalId(portalId))
        );
        return PortalMapper.INSTANCE.map(output.portal(), uriInfo);
    }

    @GET
    @Path("logo")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.READ) })
    public Response getPortalLogo(@PathParam("portalId") String portalId) {
        var logo = getPortalLogoUseCase
            .execute(new GetPortalLogoUseCase.Input(GraviteeContext.getCurrentEnvironment(), PortalMapper.INSTANCE.toPortalId(portalId)))
            .logo();
        return switch (logo) {
            case PortalLogo.Inline inline -> Response.ok(inline.image().bytes(), inline.image().mimeType()).build();
            case PortalLogo.External external -> Response.temporaryRedirect(external.url()).build();
        };
    }
}
