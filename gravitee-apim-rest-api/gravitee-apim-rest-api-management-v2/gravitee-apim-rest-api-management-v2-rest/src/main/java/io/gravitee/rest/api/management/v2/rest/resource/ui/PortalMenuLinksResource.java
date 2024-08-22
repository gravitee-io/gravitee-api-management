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
package io.gravitee.rest.api.management.v2.rest.resource.ui;

import io.gravitee.apim.core.portal_menu_link.use_case.CreatePortalMenuLinkUseCase;
import io.gravitee.apim.core.portal_menu_link.use_case.ListPortalMenuLinksForEnvironmentUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalMenuLinkMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalMenuLink;
import io.gravitee.rest.api.management.v2.rest.model.PortalMenuLinksResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PortalMenuLinksResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ListPortalMenuLinksForEnvironmentUseCase listPortalMenuLinksForEnvironmentUseCase;

    @Inject
    private CreatePortalMenuLinkUseCase createPortalMenuLinkUseCase;

    private final PortalMenuLinkMapper mapper = PortalMenuLinkMapper.INSTANCE;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.READ }) })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalMenuLinks(@BeanParam @Valid PaginationParam paginationParam) {
        var executionContext = GraviteeContext.getExecutionContext();

        var portalMenuLinks = listPortalMenuLinksForEnvironmentUseCase
            .execute(new ListPortalMenuLinksForEnvironmentUseCase.Input(executionContext.getEnvironmentId()))
            .portalMenuLinkList();

        var portalMenuLinksSubset = computePaginationData(portalMenuLinks, paginationParam);

        return Response
            .ok(
                PortalMenuLinksResponse
                    .builder()
                    .data(mapper.map(portalMenuLinksSubset))
                    .pagination(PaginationInfo.computePaginationInfo(portalMenuLinks.size(), portalMenuLinksSubset.size(), paginationParam))
                    .links(computePaginationLinks(portalMenuLinks.size(), paginationParam))
                    .build()
            )
            .build();
    }

    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.UPDATE }) })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPortalMenuLink(@Valid @NotNull final CreatePortalMenuLink createPortalMenuLink) {
        var executionContext = GraviteeContext.getExecutionContext();

        var output = createPortalMenuLinkUseCase.execute(
            new CreatePortalMenuLinkUseCase.Input(executionContext.getEnvironmentId(), mapper.map(createPortalMenuLink))
        );

        return Response
            .created(this.getLocationHeader(output.portalMenuLink().getId()))
            .entity(mapper.map(output.portalMenuLink()))
            .build();
    }

    @Path("{portalMenuLinkId}")
    public PortalMenuLinkResource getPortalMEnuLink() {
        return resourceContext.getResource(PortalMenuLinkResource.class);
    }
}
