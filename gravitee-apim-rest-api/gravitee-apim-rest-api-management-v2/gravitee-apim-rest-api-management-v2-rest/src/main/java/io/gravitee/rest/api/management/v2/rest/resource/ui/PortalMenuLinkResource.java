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

import io.gravitee.apim.core.portal_menu_link.use_case.DeletePortalMenuLinkUseCase;
import io.gravitee.apim.core.portal_menu_link.use_case.GetPortalMenuLinkUseCase;
import io.gravitee.apim.core.portal_menu_link.use_case.UpdatePortalMenuLinkUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalMenuLinkMapper;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalMenuLink;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PortalMenuLinkResource extends AbstractResource {

    @PathParam("portalMenuLinkId")
    String portalMenuLinkId;

    @Inject
    private GetPortalMenuLinkUseCase getPortalMenuLinkUseCase;

    @Inject
    private UpdatePortalMenuLinkUseCase updatePortalMenuLinkUseCase;

    @Inject
    private DeletePortalMenuLinkUseCase deletePortalMenuLinkUseCase;

    private final PortalMenuLinkMapper mapper = PortalMenuLinkMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.READ }) })
    public Response getPortalMenuLink() {
        var executionContext = GraviteeContext.getExecutionContext();

        var output = getPortalMenuLinkUseCase.execute(
            new GetPortalMenuLinkUseCase.Input(portalMenuLinkId, executionContext.getEnvironmentId())
        );

        return Response
            .ok(this.getLocationHeader(output.portalMenuLinkEntity().getId()))
            .entity(mapper.map(output.portalMenuLinkEntity()))
            .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.UPDATE }) })
    public Response updatePortalMenuLink(@Valid @NotNull final UpdatePortalMenuLink updatePortalMenuLink) {
        var executionContext = GraviteeContext.getExecutionContext();

        var output = updatePortalMenuLinkUseCase.execute(
            new UpdatePortalMenuLinkUseCase.Input(portalMenuLinkId, executionContext.getEnvironmentId(), mapper.map(updatePortalMenuLink))
        );

        return Response
            .ok(this.getLocationHeader(output.portalMenuLinkEntity().getId()))
            .entity(mapper.map(output.portalMenuLinkEntity()))
            .build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.DELETE }) })
    public Response deletePortalMenuLink() {
        var executionContext = GraviteeContext.getExecutionContext();

        deletePortalMenuLinkUseCase.execute(new DeletePortalMenuLinkUseCase.Input(portalMenuLinkId, executionContext.getEnvironmentId()));

        return Response.noContent().build();
    }
}
