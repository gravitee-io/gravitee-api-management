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
package io.gravitee.rest.api.management.v2.rest.resource.api.debug;

import io.gravitee.apim.core.debug.use_case.DebugApiUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.DebugApiMapper;
import io.gravitee.rest.api.management.v2.rest.model.DebugHttpRequest;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class ApiDebugResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @Inject
    private DebugApiUseCase debugApiUseCase;

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = { RolePermissionAction.UPDATE }) })
    public Response debugApi(DebugHttpRequest debugHttpRequest) {
        var output = debugApiUseCase.execute(
            new DebugApiUseCase.Input(apiId, DebugApiMapper.INSTANCE.map(debugHttpRequest), getAuditInfo())
        );
        return Response.accepted(DebugApiMapper.INSTANCE.map(output.debugApiEvent())).build();
    }
}
