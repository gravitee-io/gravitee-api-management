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

import io.gravitee.apim.core.portal_page.use_case.CreatePortalNavigationItemUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalNavigationItemsMapper;
import io.gravitee.rest.api.management.v2.rest.model.BaseCreatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PortalNavigationItemsResource extends AbstractResource {

    @Inject
    private CreatePortalNavigationItemUseCase createPortalNavigationItemUseCase;

    private final PortalNavigationItemsMapper mapper = PortalNavigationItemsMapper.INSTANCE;

    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PAGE, acls = { RolePermissionAction.UPDATE }) })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPortalNavigationItem(@Valid @NotNull final BaseCreatePortalNavigationItem createPortalNavigationItem) {
        final var executionContext = GraviteeContext.getExecutionContext();

        final var output = createPortalNavigationItemUseCase.execute(
            new CreatePortalNavigationItemUseCase.Input(
                mapper.map(executionContext.getOrganizationId(), executionContext.getEnvironmentId(), createPortalNavigationItem)
            )
        );

        return Response.created(this.getLocationHeader(output.item().getId().toString())).entity(mapper.map(output.item())).build();
    }
}
