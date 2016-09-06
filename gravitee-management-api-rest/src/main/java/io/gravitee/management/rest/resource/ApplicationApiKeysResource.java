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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.ApiKeyEntity;
import io.gravitee.management.model.KeysByApiEntity;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.model.permissions.ApplicationPermission;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.rest.security.ApplicationPermissionsRequired;
import io.gravitee.management.service.ApiKeyService;
import io.gravitee.management.service.ApiService;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Api(tags = {"Application"})
public class ApplicationApiKeysResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiService apiService;

    @Inject
    private ApiKeyService apiKeyService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.READ)
    @ApiOperation(value = "Get API keys associated to the given application",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            // TODO: it seems that Map container is not yet completely handled by Swagger
            // @ApiResponse(code = 200, message = "List of API keys by API", response = String.class, responseContainer = "Map"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Map<String, KeysByApiEntity> listApplicationApiKeys(@PathParam("application") String application) {
        Map<String, List<ApiKeyEntity>> keys = apiKeyService.findByApplication(application);
        Map<String, KeysByApiEntity> keysByApi = new HashMap<>(keys.size());

        keys.forEach((api, apiKeyEntities) -> {
            ApiEntity apiEntity = apiService.findById(api);
            KeysByApiEntity keysByApiEntity = new KeysByApiEntity();

            keysByApiEntity.setName(apiEntity.getName());
            keysByApiEntity.setVersion(apiEntity.getVersion());
            keysByApiEntity.setKeys(apiKeyEntities);

            keysByApi.put(api, keysByApiEntity);
        });

        return keysByApi;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiPermissionsRequired(ApiPermission.READ)
    @ApplicationPermissionsRequired(ApplicationPermission.MANAGE_API_KEYS)
    @ApiOperation(value = "Generate or renew the given API key",
            notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "New or updated API key", response = ApiKeyEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response generateApplicationApiKey(
            @PathParam("application") String application,
            @ApiParam(name = "api", required = true) @NotNull @QueryParam("api") String api) {
        ApiKeyEntity apiKeyEntity = apiKeyService.generateOrRenew(application, api);

        return Response
                .status(Response.Status.CREATED)
                .entity(apiKeyEntity)
                .build();
    }

    @DELETE
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.MANAGE_API_KEYS)
    @ApiOperation(value = "Revoke an existing API key",
            notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "API key successfully revoked"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response revokeApplicationApiKey(
            @PathParam("application") String application,
            @PathParam("key") String apiKey) {
        apiKeyService.revoke(apiKey);

        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    @Path("/{key}/analytics")
    public ApiKeyAnalyticsResource getApiKeyAnalyticsResource() {
        return resourceContext.getResource(ApiKeyAnalyticsResource.class);
    }
}
