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

import io.gravitee.apim.core.portal_documentation.exception.PortalDocumentationNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.use_case.DeletePortalDocumentationUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetPortalDocumentationUseCase;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.PortalDocumentationMapper;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
public class PortalDocumentationResource extends AbstractResource {

    @Inject
    private GetPortalDocumentationUseCase getPortalDocumentationUseCase;

    @Inject
    private DeletePortalDocumentationUseCase deletePortalDocumentationUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PORTAL, acls = RolePermissionAction.READ) })
    public Response getPortalDocumentationByHRID(@PathParam("hrid") String portalHrid, @PathParam("docHrid") String docHrid) {
        var auditInfo = getAuditInfo();
        var documentationId = PortalPageContentId.of(
            HRIDToUUID.portalDocumentation().context(auditInfo).portal(portalHrid).hrid(docHrid).id()
        );
        try {
            var output = getPortalDocumentationUseCase.execute(new GetPortalDocumentationUseCase.Input(auditInfo, documentationId));
            return Response.ok(PortalDocumentationMapper.INSTANCE.toDocumentationState(output.pageContent(), docHrid, portalHrid)).build();
        } catch (PortalDocumentationNotFoundException e) {
            throw new HRIDNotFoundException(docHrid);
        }
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PORTAL, acls = RolePermissionAction.DELETE) })
    public Response deletePortalDocumentationByHrid(@PathParam("hrid") String portalHrid, @PathParam("docHrid") String docHrid) {
        var auditInfo = getAuditInfo();
        var documentationId = PortalPageContentId.of(
            HRIDToUUID.portalDocumentation().context(auditInfo).portal(portalHrid).hrid(docHrid).id()
        );
        try {
            deletePortalDocumentationUseCase.execute(new DeletePortalDocumentationUseCase.Input(auditInfo, documentationId));
        } catch (PortalDocumentationNotFoundException e) {
            throw new HRIDNotFoundException(docHrid);
        }
        return Response.noContent().build();
    }
}
