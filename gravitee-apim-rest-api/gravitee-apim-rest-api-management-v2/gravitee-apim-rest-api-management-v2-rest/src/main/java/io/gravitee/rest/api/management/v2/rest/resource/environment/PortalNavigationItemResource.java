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

import io.gravitee.apim.core.portal_page.exception.ItemHasChildrenException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.use_case.DeletePortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.UpdatePortalNavigationItemUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalNavigationItemsMapper;
import io.gravitee.rest.api.management.v2.rest.model.BaseUpdatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PortalNavigationItemResource extends AbstractResource {

    @Inject
    private UpdatePortalNavigationItemUseCase updatePortalNavigationItemUseCase;

    @Inject
    private DeletePortalNavigationItemUseCase deletePortalNavigationItemUseCase;

    @Inject
    private PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    private static final PortalNavigationItemsMapper mapper = PortalNavigationItemsMapper.INSTANCE;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public Response updatePortalNavigationItem(
        @PathParam("navId") String navigationItemId,
        @Valid @NotNull final BaseUpdatePortalNavigationItem updatePortalNavigationItem
    ) {
        var input = new UpdatePortalNavigationItemUseCase.Input(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            navigationItemId,
            mapper.map(updatePortalNavigationItem)
        );

        var output = updatePortalNavigationItemUseCase.execute(input);

        return Response.ok(mapper.map(output.updatedItem())).build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.DELETE) })
    public Response deletePortalNavigationItem(@PathParam("navId") String navigationItemId) {
        var navId = PortalNavigationItemId.of(navigationItemId);

        var directChildren = portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(
            GraviteeContext.getCurrentEnvironment(),
            navId
        );
        if (!directChildren.isEmpty()) {
            throw ItemHasChildrenException.forId(navigationItemId);
        }

        var input = new DeletePortalNavigationItemUseCase.Input(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            navId
        );

        deletePortalNavigationItemUseCase.execute(input);

        return Response.noContent().build();
    }
}
