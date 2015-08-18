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

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.service.ApiService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Path("/apis")
public class ApisResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiService apiService;

    /**
     * List all APIs must return only visible API (ie. non-private API) for both users and teams.
     *
     * @return List all publicly visible APIs
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApiEntity> listAll() {
        Set<ApiEntity> apis = apiService.findAll();

        if (apis == null) {
            apis = new HashSet<>();
        }

        return apis;
    }

    @Path("{apiName}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }
}
