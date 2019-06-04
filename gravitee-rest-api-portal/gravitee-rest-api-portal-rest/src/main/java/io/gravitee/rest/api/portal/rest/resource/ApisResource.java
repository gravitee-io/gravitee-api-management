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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.rest.api.model.Visibility.PUBLIC;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED;
import static java.util.Collections.singletonList;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.ApisResponse;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Context
    private UriInfo uriInfo;
    
    @Inject
    private ApiMapper apiMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApis(@DefaultValue(PAGE_QUERY_PARAM_DEFAULT) @QueryParam("page") Integer page, @DefaultValue(SIZE_QUERY_PARAM_DEFAULT) @QueryParam("size") Integer size) {
        final ApiQuery apiQuery = new ApiQuery();
        apiQuery.setLifecycleStates(singletonList(PUBLISHED));
        
        final Collection<ApiEntity> apis;
        if (isAuthenticated()) {
            apis = apiService.findByUser(getAuthenticatedUser(), apiQuery);
        } else {
            apiQuery.setVisibility(PUBLIC);
            apis = apiService.search(apiQuery);
        }

        List<io.gravitee.rest.api.portal.rest.model.Api> apisList= apis.stream()
                .map(apiMapper::convert)
                .map(this::addApiLinks)
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .collect(Collectors.toList());
        
        int totalItems = apisList.size();
        
        apisList = this.paginateResultList(apisList, page, size);

        ApisResponse apiResponse = new ApisResponse()
                .data(apisList)
                .links(this.computePaginatedLinks(uriInfo, page, size, totalItems))
                ;
        
        return Response
                .ok(apiResponse)
                .build();
    }

    private Api addApiLinks(Api api) {
        String basePath = uriInfo.getAbsolutePathBuilder().path(api.getId()).build().toString();
        return api.links(apiMapper.computeApiLinks(basePath));
    }
    
    
    @Path("{apiId}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }
    
}
