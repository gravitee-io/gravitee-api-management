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

import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.use_case.CreateOrUpdatePortalUseCase;
import io.gravitee.apim.rest.api.automation.mapper.PortalMapper;
import io.gravitee.apim.rest.api.automation.model.PortalSpec;
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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
public class PortalsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateOrUpdatePortalUseCase createOrUpdatePortalUseCase;

    @Path("/{hrid}")
    public PortalResource getPortalResource() {
        return resourceContext.getResource(PortalResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PORTAL, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(@Valid @NotNull PortalSpec spec, @QueryParam("dryRun") boolean dryRun) {
        var auditInfo = getAuditInfo();
        var portal = Portal.of(
            PortalId.of(HRIDToUUID.portal().context(auditInfo).hrid(spec.getHrid()).id()),
            auditInfo.environmentId(),
            auditInfo.organizationId(),
            spec.getName()
        );
        var navigation = PortalMapper.INSTANCE.toCoreNavigation(spec.getNavigation());

        if (dryRun) {
            return Response.ok(PortalMapper.INSTANCE.toPortalState(portal, spec.getHrid(), navigation)).build();
        }

        var output = createOrUpdatePortalUseCase.execute(new CreateOrUpdatePortalUseCase.Input(auditInfo, portal, navigation));

        return Response.ok(PortalMapper.INSTANCE.toPortalState(output.portal(), spec.getHrid(), output.navigation())).build();
    }
}
