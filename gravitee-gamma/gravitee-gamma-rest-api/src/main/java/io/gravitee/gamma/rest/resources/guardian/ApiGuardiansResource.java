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
package io.gravitee.gamma.rest.resources.guardian;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.guardian.use_case.ListApiGuardiansUseCase;
import io.gravitee.gamma.rest.resources.guardian.dto.ApiGuardiansDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * The Guardians enabled on an API, under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/apis/{apiId}/guardians}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET} — the enabled Guardians, request ones before response ones, each phase in execution order; or
 *       {@code 404} when the API is unknown <em>or belongs to another environment</em>.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class ApiGuardiansResource {

    @Inject
    private ListApiGuardiansUseCase listApiGuardiansUseCase;

    @GET
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = { RolePermissionAction.READ }) })
    public ApiGuardiansDto list(@PathParam("apiId") String apiId) {
        var output = listApiGuardiansUseCase.execute(
            new ListApiGuardiansUseCase.Input(GraviteeContext.getExecutionContext().getEnvironmentId(), apiId)
        );
        return ApiGuardiansDto.from(output.guardians());
    }
}
