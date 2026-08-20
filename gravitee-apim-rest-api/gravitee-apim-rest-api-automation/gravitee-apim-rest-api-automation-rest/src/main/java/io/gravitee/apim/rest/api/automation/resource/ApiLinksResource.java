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

import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.use_case.CreateOrUpdateApiLinkUseCase;
import io.gravitee.apim.core.portal_page.use_case.ValidateApiLinkUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.mapper.PortalLinkMapper;
import io.gravitee.apim.rest.api.automation.model.PortalLinkSpec;
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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
public class ApiLinksResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateOrUpdateApiLinkUseCase createOrUpdateApiLinkUseCase;

    @Inject
    private ValidateApiLinkUseCase validateApiLinkUseCase;

    @Path("/{linkHrid}")
    public ApiLinkResource getApiLinkResource() {
        return resourceContext.getResource(ApiLinkResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(
        @PathParam("apiHrid") String apiHrid,
        @Valid @NotNull PortalLinkSpec spec,
        @QueryParam("dryRun") boolean dryRun
    ) {
        var auditInfo = getAuditInfo();
        var apiId = HRIDToUUID.api().context(auditInfo).hrid(apiHrid).id();
        var linkId = PortalNavigationItemId.forApiLink(auditInfo, apiId, spec.getHrid());

        var input = new CreateOrUpdateApiLinkUseCase.Input(
            auditInfo,
            apiId,
            spec.getHrid(),
            spec.getName(),
            spec.getHref(),
            spec.getLocation(),
            spec.getOrder()
        );
        var output = dryRun ? validateApiLinkUseCase.execute(input) : createOrUpdateApiLinkUseCase.execute(input);

        var state = PortalLinkMapper.INSTANCE.toApiLinkState(spec, linkId.toString(), output.errors(), auditInfo, apiHrid);

        // A dry run is a preview: severe findings are its payload, so it always answers 200.
        // A real apply that produced severe errors persisted nothing — it must not report success.
        var applyFailed = !dryRun && output.errors().stream().anyMatch(Validator.Error::isSevere);

        return Response.status(applyFailed ? Response.Status.BAD_REQUEST : Response.Status.OK).entity(state).build();
    }
}
