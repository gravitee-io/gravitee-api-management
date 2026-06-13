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

import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.use_case.CreateOrUpdatePortalDocumentationUseCase;
import io.gravitee.apim.core.portal_page.use_case.ValidatePortalDocumentationUseCase;
import io.gravitee.apim.rest.api.automation.mapper.PortalDocumentationMapper;
import io.gravitee.apim.rest.api.automation.model.DocumentationSpec;
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
public class PortalDocumentationsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateOrUpdatePortalDocumentationUseCase createOrUpdatePortalDocumentationUseCase;

    @Inject
    private ValidatePortalDocumentationUseCase validatePortalDocumentationUseCase;

    @Path("/{docHrid}")
    public PortalDocumentationResource getPortalDocumentationResource() {
        return resourceContext.getResource(PortalDocumentationResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PORTAL, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(
        @PathParam("hrid") String portalHrid,
        @Valid @NotNull DocumentationSpec spec,
        @QueryParam("dryRun") boolean dryRun
    ) {
        var auditInfo = getAuditInfo();
        var portalId = PortalId.of(HRIDToUUID.portal().context(auditInfo).hrid(portalHrid).id());
        var documentationId = PortalPageContentId.of(
            HRIDToUUID.portalDocumentation().context(auditInfo).portal(portalHrid).hrid(spec.getHrid()).id()
        );

        var input = new CreateOrUpdatePortalDocumentationUseCase.Input(
            auditInfo,
            documentationId,
            portalId,
            spec.getName(),
            PortalDocumentationMapper.INSTANCE.toDomainType(spec.getType()),
            spec.getContent(),
            spec.getLocation(),
            spec.getOrder()
        );
        var output = dryRun ? validatePortalDocumentationUseCase.execute(input) : createOrUpdatePortalDocumentationUseCase.execute(input);

        return Response.ok(
            PortalDocumentationMapper.INSTANCE.toDocumentationState(
                spec,
                output.id() != null ? output.id().toString() : null,
                output.errors(),
                auditInfo,
                portalHrid
            )
        ).build();
    }
}
