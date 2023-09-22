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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.api.DefinitionContextEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApiDefinitionContextService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
public class ApiDefinitionContextResource {

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    private String apiId;

    private final ApiDefinitionContextService definitionContextService;

    @Inject
    public ApiDefinitionContextResource(ApiDefinitionContextService definitionContextService) {
        this.definitionContextService = definitionContextService;
    }

    @PUT
    @Hidden
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response updateDefinitionContext(@Valid @NotNull DefinitionContextEntity definitionContext) {
        definitionContextService.setDefinitionContext(apiId, definitionContext);
        return Response.ok().build();
    }
}
