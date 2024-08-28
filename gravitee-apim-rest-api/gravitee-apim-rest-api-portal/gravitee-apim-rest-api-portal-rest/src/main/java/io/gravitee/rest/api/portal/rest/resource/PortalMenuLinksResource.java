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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.use_case.ListAllPortalMenuLinksForEnvironmentUseCase;
import io.gravitee.apim.core.portal_menu_link.use_case.ListPublicPortalMenuLinksForEnvironmentUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.PortalMenuLinkMapper;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PortalMenuLinksResource extends AbstractResource {

    @Inject
    private ListAllPortalMenuLinksForEnvironmentUseCase listAllPortalMenuLinksForEnvironmentUseCase;

    @Inject
    private ListPublicPortalMenuLinksForEnvironmentUseCase listPublicPortalMenuLinksForEnvironmentUseCase;

    private final PortalMenuLinkMapper mapper = new PortalMenuLinkMapper();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalMenuLinks() {
        var executionContext = GraviteeContext.getExecutionContext();

        List<PortalMenuLink> portalMenuLinks;

        if (isAuthenticated()) {
            portalMenuLinks =
                listAllPortalMenuLinksForEnvironmentUseCase
                    .execute(new ListAllPortalMenuLinksForEnvironmentUseCase.Input(executionContext.getEnvironmentId()))
                    .portalMenuLinkList();
        } else {
            portalMenuLinks =
                listPublicPortalMenuLinksForEnvironmentUseCase
                    .execute(new ListPublicPortalMenuLinksForEnvironmentUseCase.Input(executionContext.getEnvironmentId()))
                    .portalMenuLinkList();
        }

        return Response.ok(mapper.map(portalMenuLinks)).build();
    }
}
