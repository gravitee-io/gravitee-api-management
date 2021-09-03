/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.validator.CustomApiKey;
import io.swagger.annotations.*;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
@Api(tags = { "API Keys" })
public class KeysResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiKeyService apiKeyService;

    @GET
    @Path("_canCreate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Check an API Key can be created for a given key, application, and api",
        notes = "User must have the API_SUBSCRIPTION:READ permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "API Key creation successfully checked", response = Boolean.class),
            @ApiResponse(code = 400, message = "Bad API Key parameter"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public Response verifyApiKeyCreation(
        @ApiParam(name = "key", required = true) @CustomApiKey @NotNull @QueryParam("key") String key,
        @ApiParam(name = "application", required = true) @NotNull @QueryParam("application") String application,
        @ApiParam(name = "api", required = true) @NotNull @QueryParam("api") String api
    ) {
        boolean canCreate = apiKeyService.canCreate(key, api, application);
        return Response.ok(canCreate).build();
    }

    @Path("{key}")
    public KeyResource getKeyResource() {
        return resourceContext.getResource(KeyResource.class);
    }
}
