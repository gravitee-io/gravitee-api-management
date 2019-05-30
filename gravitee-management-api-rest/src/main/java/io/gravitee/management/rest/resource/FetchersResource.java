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
import io.gravitee.management.model.FetcherEntity;
import io.gravitee.management.model.FetcherListItem;
import io.gravitee.management.rest.resource.param.FetchersParam;
import io.gravitee.management.service.FetcherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Plugin", "Fetcher"})
public class FetchersResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private FetcherService fetcherService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List fetchers")
    public Collection<FetcherListItem> list(@BeanParam FetchersParam params) {
        Stream<FetcherListItem> stream = fetcherService.findAll(params.isOnlyFilesFetchers()).stream().map(this::convert);

        if(params != null && params.getExpand() != null && !params.getExpand().isEmpty()) {
            for (String s : params.getExpand()) {
                switch (s) {
                    case "schema":
                        stream = stream.map(fetcherListItem -> {
                            fetcherListItem.setSchema(fetcherService.getSchema(fetcherListItem.getId()));
                            return fetcherListItem;
                        });
                        break;
                    default: break;
                }
            }
        }

        return stream
                .sorted(Comparator.comparing(FetcherListItem::getName))
                .collect(Collectors.toList());
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
