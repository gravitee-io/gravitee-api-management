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

import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.use_case.DeletePortalUseCase;
import io.gravitee.apim.core.portal.use_case.GetPortalUseCase;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.PortalMapper;
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
public class PortalResource extends AbstractResource {

    @Inject
    private GetPortalUseCase getPortalUseCase;

    @Inject
    private DeletePortalUseCase deletePortalUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PORTAL, acls = RolePermissionAction.READ) })
    public Response getPortalByHRID(@PathParam("hrid") String hrid) {
        var auditInfo = getAuditInfo();
        PortalId id = PortalId.of(HRIDToUUID.portal().context(auditInfo).hrid(hrid).id());
        try {
            var output = getPortalUseCase.execute(new GetPortalUseCase.Input(auditInfo, id));
            return Response.ok(PortalMapper.INSTANCE.toPortalState(output.portal(), hrid)).build();
        } catch (PortalNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PORTAL, acls = RolePermissionAction.DELETE) })
    public Response deletePortalByHrid(@PathParam("hrid") String hrid) {
        var auditInfo = getAuditInfo();
        PortalId id = PortalId.of(HRIDToUUID.portal().context(auditInfo).hrid(hrid).id());
        try {
            deletePortalUseCase.execute(new DeletePortalUseCase.Input(auditInfo, id));
        } catch (PortalNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
        return Response.noContent().build();
    }
}
