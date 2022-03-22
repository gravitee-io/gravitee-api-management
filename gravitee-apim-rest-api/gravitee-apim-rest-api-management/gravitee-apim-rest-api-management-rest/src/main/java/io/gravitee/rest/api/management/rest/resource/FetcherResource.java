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
import io.gravitee.rest.api.model.FetcherEntity;
import io.gravitee.rest.api.service.FetcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Plugins")
public class FetcherResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private FetcherService fetcherService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a fetcher plugin")
    @ApiResponse(
        responseCode = "200",
        description = "Fetcher",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FetcherEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Fetcher not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public FetcherEntity getFetcher(@PathParam("fetcher") String fetcher) {
        return fetcherService.findById(fetcher);
    }

    @GET
    @Path("schema")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a fetcher plugin's schema")
    @ApiResponse(
        responseCode = "200",
        description = "Fetcher's schema",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "string"))
    )
    @ApiResponse(responseCode = "404", description = "Fetcher not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public String getFetcherSchema(@PathParam("fetcher") String fetcher) {
        // Check that the fetcher exists
        fetcherService.findById(fetcher);

        return fetcherService.getSchema(fetcher);
    }
}
