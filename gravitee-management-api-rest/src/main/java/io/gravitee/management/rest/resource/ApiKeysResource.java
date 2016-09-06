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
import io.gravitee.management.model.ApiKeyEntity;
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.KeysByApplicationEntity;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.ApiKeyService;
import io.gravitee.management.service.ApplicationService;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.validation.Valid;
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
@ApiPermissionsRequired(ApiPermission.MANAGE_API_KEYS)
@Api(tags = {"API"})
public class ApiKeysResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApiKeyService apiKeyService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, KeysByApplicationEntity> listApiKeys(@PathParam("api") String api) {
        Map<String, List<ApiKeyEntity>> keys = apiKeyService.findByApi(api);
        Map<String, KeysByApplicationEntity> keysByApplication = new HashMap<>(keys.size());

        keys.forEach((application, apiKeyEntities) -> {
            ApplicationEntity applicationEntity = applicationService.findById(application);
            KeysByApplicationEntity keysByApplicationEntity = new KeysByApplicationEntity();

            keysByApplicationEntity.setName(applicationEntity.getName());
            keysByApplicationEntity.setKeys(apiKeyEntities);

            keysByApplication.put(application, keysByApplicationEntity);
        });

        return keysByApplication;
    }

    @DELETE
    @Path("{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeApiKey(
            @PathParam("api") String api,
            @PathParam("key") String apiKey) {
        apiKeyService.revoke(apiKey);

        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    @PUT
    @Path("{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApiKeyEntity updateApiKey(
            @PathParam("api") String api,
            @PathParam("key") String apiKey,
            @Valid @NotNull ApiKeyEntity apiKeyEntity) {
        return apiKeyService.update(apiKey, apiKeyEntity);
    }

    /*
    @Path("{key}/analytics")
    public ApiKeyAnalyticsResource getApiKeyAnalyticsResource() {
        return resourceContext.getResource(ApiKeyAnalyticsResource.class);
    }
    */
}
