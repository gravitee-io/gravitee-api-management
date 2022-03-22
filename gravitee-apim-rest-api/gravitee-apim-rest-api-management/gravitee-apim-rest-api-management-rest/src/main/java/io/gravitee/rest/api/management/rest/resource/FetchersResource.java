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
import io.gravitee.rest.api.management.rest.resource.param.FetchersParam;
import io.gravitee.rest.api.model.FetcherEntity;
import io.gravitee.rest.api.model.FetcherListItem;
import io.gravitee.rest.api.service.FetcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Plugins")
public class FetchersResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private FetcherService fetcherService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List of fetcher plugins")
    @ApiResponse(
        responseCode = "200",
        description = "List of fetchers",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = FetcherListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Collection<FetcherListItem> getFetchers(@BeanParam FetchersParam params) {
        Stream<FetcherListItem> stream = fetcherService.findAll(params.isOnlyFilesFetchers()).stream().map(this::convert);

        if (params != null && params.getExpand() != null && !params.getExpand().isEmpty()) {
            for (String s : params.getExpand()) {
                switch (s) {
                    case "schema":
                        stream =
                            stream.map(
                                fetcherListItem -> {
                                    fetcherListItem.setSchema(fetcherService.getSchema(fetcherListItem.getId()));
                                    return fetcherListItem;
                                }
                            );
                        break;
                    default:
                        break;
                }
            }
        }

        return stream.sorted(Comparator.comparing(FetcherListItem::getName)).collect(Collectors.toList());
    }

    @Path("{fetcher}")
    public FetcherResource getFetcherResource() {
        return resourceContext.getResource(FetcherResource.class);
    }

    private FetcherListItem convert(FetcherEntity fetcher) {
        FetcherListItem item = new FetcherListItem();

        item.setId(fetcher.getId());
        item.setName(fetcher.getName());
        item.setDescription(fetcher.getDescription());
        item.setVersion(fetcher.getVersion());

        return item;
    }
}
