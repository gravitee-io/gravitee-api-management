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

import io.gravitee.apim.core.theme.exception.ThemeNotFoundException;
import io.gravitee.apim.core.theme.use_case.DeletePortalThemeUseCase;
import io.gravitee.apim.core.theme.use_case.GetPortalThemeUseCase;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.PortalThemeMapper;
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

public class PortalThemeResource extends AbstractResource {

    @Inject
    private GetPortalThemeUseCase getPortalThemeUseCase;

    @Inject
    private DeletePortalThemeUseCase deletePortalThemeUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_THEME, acls = RolePermissionAction.READ) })
    public Response getThemeByHrid(@PathParam("hrid") String hrid) {
        var auditInfo = getAuditInfo();
        var themeId = HRIDToUUID.portalTheme().context(auditInfo).hrid(hrid).id();
        try {
            var output = getPortalThemeUseCase.execute(new GetPortalThemeUseCase.Input(auditInfo, themeId));
            return Response.ok(PortalThemeMapper.INSTANCE.toPortalThemeState(output.theme(), hrid, auditInfo, null)).build();
        } catch (ThemeNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_THEME, acls = RolePermissionAction.DELETE) })
    public Response deleteThemeByHrid(@PathParam("hrid") String hrid) {
        var auditInfo = getAuditInfo();
        var themeId = HRIDToUUID.portalTheme().context(auditInfo).hrid(hrid).id();
        try {
            deletePortalThemeUseCase.execute(new DeletePortalThemeUseCase.Input(auditInfo, themeId));
        } catch (ThemeNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
        return Response.noContent().build();
    }
}
