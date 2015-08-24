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

import io.gravitee.management.model.ApiKeyEntity;
import io.gravitee.management.service.ApiKeyService;
import io.gravitee.management.service.exceptions.NoValidApiKeyException;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiKeyResource {

    @PathParam("applicationName")
    private String applicationName;

    @PathParam("apiName")
    private String apiName;

    @Inject
    private ApiKeyService apiKeyService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ApiKeyEntity getCurrentApiKeyEntity() {
        Optional<ApiKeyEntity> apiKeyEntity = apiKeyService.current(applicationName, apiName);

        if (! apiKeyEntity.isPresent()) {
            throw new NoValidApiKeyException();
        }

        return apiKeyEntity.get();
    }

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApiKeyEntity> findAllApiKeys() {
        return apiKeyService.findAll(applicationName, apiName);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateApiKey() {
        ApiKeyEntity apiKeyEntity = apiKeyService.generate(applicationName, apiName);

        return Response
                .status(Response.Status.CREATED)
                .entity(apiKeyEntity)
                .build();
    }

    @DELETE
    @Path("{apiKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeApiKey(@PathParam("apiKey") String apiKey) {
        apiKeyService.revoke(apiKey);

        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }
}
