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
package io.gravitee.management.api.resource;

import io.gravitee.management.api.exceptions.ApiKeyNotFoundException;
import io.gravitee.management.api.model.ApiKeyEntity;
import io.gravitee.management.api.service.ApiKeyService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

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
    public ApiKeyEntity getCurrentApiKeyEntity() {
        Optional<ApiKeyEntity> apiKeyEntity = apiKeyService.getCurrentApiKey(applicationName, apiName);

        if (! apiKeyEntity.isPresent()) {
            throw new ApiKeyNotFoundException();
        }

        return apiKeyEntity.get();
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
}
