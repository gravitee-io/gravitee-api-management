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

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.filtering.FilteredEntities;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.resource.param.ApisParam;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.filtering.FilteringService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiMapper apiMapper;

    @Inject
    private FilteringService filteringService;

    @Inject
    private CategoryService categoryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApis(@BeanParam PaginationParam paginationParam, @BeanParam ApisParam apisParam) {
        Collection<ApiEntity> apis = apiService.findPublishedByUser(getAuthenticatedUserOrNull(), createQueryFromParam(apisParam));

        FilteringService.FilterType filter = apisParam.getFilter() != null ? FilteringService.FilterType.valueOf(apisParam.getFilter().name()) : null;
        FilteringService.FilterType excludeFilter = apisParam.getExcludedFilter() != null ? FilteringService.FilterType.valueOf(apisParam.getExcludedFilter().name()) : null;

        FilteredEntities<ApiEntity> filteredApis = filteringService.filterApis(apis, filter, excludeFilter);
        List<ApiEntity> filteredApisList = filteredApis.getFilteredItems();

        Stream<ApiEntity> resultStream = filteredApisList.stream();

        if (filteredApisList.size() > 0 && apisParam.getPromoted() != null) {
            //By default, the promoted API is the first of the list;
            String promotedApiId = filteredApisList.get(0).getId();

            if (apisParam.getCategory() != null) {
                // If apis are searched in a category, looks for the category highlighted API (HL API) and if this HL API is in the searchResult.
                // If it is, then the HL API becomes the promoted API
                String highlightedApiId = this.categoryService.findById(apisParam.getCategory()).getHighlightApi();
                if (highlightedApiId != null) {
                    Optional<ApiEntity> highlightedApiInResult = filteredApisList.stream().filter(api -> api.getId().equals(highlightedApiId)).findFirst();
                    if (highlightedApiInResult.isPresent()) {
                        promotedApiId = highlightedApiInResult.get().getId();
                    }
                }
            }
            String finalPromotedApiId = promotedApiId;
            if (apisParam.getPromoted() == Boolean.TRUE) {
                // Only the promoted API has to be returned
                resultStream = resultStream.filter(api -> api.getId().equals(finalPromotedApiId));
            } else if (apisParam.getPromoted() == Boolean.FALSE) {
                // All filtered API except the promoted API have to be returned
                resultStream = resultStream.filter(api -> !api.getId().equals(finalPromotedApiId));
            }
        }

        List<Api> apisList = resultStream
                .map(apiMapper::convert)
                .map(this::addApiLinks)
                .collect(Collectors.toList());

        return createListResponse(apisList, paginationParam, filteredApis.getMetadata());
    }

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response searchApis(@NotNull(message = "Input must not be null.") @QueryParam("q") String query,
                               @BeanParam PaginationParam paginationParam) {
        Collection<ApiEntity> apis = apiService.findPublishedByUser(getAuthenticatedUserOrNull(),
                createQueryFromParam(null));

        Map<String, Object> filters = new HashMap<>();
        filters.put("api", apis.stream().map(ApiEntity::getId).collect(Collectors.toSet()));

        try {
            List<Api> apisList = apiService.search(query, filters).stream().map(apiMapper::convert)
                    .map(this::addApiLinks).collect(Collectors.toList());
            return createListResponse(apisList, paginationParam);
        } catch (TechnicalException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    private ApiQuery createQueryFromParam(ApisParam apisParam) {
        final ApiQuery apiQuery = new ApiQuery();
        if (apisParam != null) {
            apiQuery.setContextPath(apisParam.getContextPath());
            apiQuery.setLabel(apisParam.getLabel());
            apiQuery.setName(apisParam.getName());
            apiQuery.setTag(apisParam.getTag());
            apiQuery.setVersion(apisParam.getVersion());
            if (apisParam.getCategory() != null) {
                apiQuery.setCategory(apisParam.getCategory());
            }
        }
        return apiQuery;
    }


    private Api addApiLinks(Api api) {
        final OffsetDateTime updatedAt = api.getUpdatedAt();
        Date updateDate = null;
        if (updatedAt != null) {
            long epochMilli = updatedAt.toInstant().toEpochMilli();
            updateDate = new Date(epochMilli);
        }
        return api.links(apiMapper
                .computeApiLinks(PortalApiLinkHelper.apisURL(uriInfo.getBaseUriBuilder(), api.getId()), updateDate));
    }

    @Path("{apiId}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }
}
