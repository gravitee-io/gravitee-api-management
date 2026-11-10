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

import io.gravitee.apim.core.portal.use_case.GetPortalsStatsUseCase;
import io.gravitee.apim.core.portal.use_case.ListPortalsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalMapper;
import io.gravitee.rest.api.management.v2.rest.model.PortalsResponse;
import io.gravitee.rest.api.management.v2.rest.model.PortalsStats;
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
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

public class PortalsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ListPortalsUseCase listPortalsUseCase;

    @Inject
    private GetPortalsStatsUseCase getPortalsStatsUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.READ) })
    public PortalsResponse listPortals(@QueryParam("q") String q, @BeanParam @Valid PaginationParam paginationParam) {
        var page = listPortalsUseCase
            .execute(new ListPortalsUseCase.Input(GraviteeContext.getCurrentEnvironment(), q, paginationParam.toPageable()))
            .portals();

        return new PortalsResponse()
            .data(PortalMapper.INSTANCE.map(page.getContent(), uriInfo))
            .pagination(PaginationInfo.computePaginationInfo(page.getTotalElements(), page.getContent().size(), paginationParam))
            .links(computePaginationLinks(page.getTotalElements(), paginationParam));
    }

    @GET
    @Path("_stats")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.READ) })
    public PortalsStats getPortalsStats() {
        var output = getPortalsStatsUseCase.execute(
            new GetPortalsStatsUseCase.Input(GraviteeContext.getCurrentOrganization(), GraviteeContext.getCurrentEnvironment())
        );
        return PortalMapper.INSTANCE.map(output);
    }

    @Path("{portalId}")
    public PortalResource getPortalResource() {
        return resourceContext.getResource(PortalResource.class);
    }
}
