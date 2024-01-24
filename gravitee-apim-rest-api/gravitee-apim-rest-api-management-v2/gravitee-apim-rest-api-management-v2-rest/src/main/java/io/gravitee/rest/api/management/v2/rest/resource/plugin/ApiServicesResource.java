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
package io.gravitee.rest.api.management.v2.rest.resource.plugin;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.v4.ApiServicePluginService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * Defines the REST resources to manage ApiServices.
 *
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/plugins/api-services")
public class ApiServicesResource extends AbstractResource {

    @Inject
    private ApiServicePluginService apiServicePluginService;

    @GET
    @Path("/{apiServiceId}/schema")
    @Produces(MediaType.APPLICATION_JSON)
    public String getApiServiceSchema(@PathParam("apiServiceId") String apiServiceId) {
        // Check that the entrypoint exists
        apiServicePluginService.findById(apiServiceId);

        return apiServicePluginService.getSchema(apiServiceId);
    }
}
