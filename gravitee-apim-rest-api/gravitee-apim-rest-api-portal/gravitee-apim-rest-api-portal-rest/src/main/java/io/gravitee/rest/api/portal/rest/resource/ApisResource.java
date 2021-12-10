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
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.filtering.FilteredEntities;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.FilterApiQuery;
import io.gravitee.rest.api.portal.rest.resource.param.ApisParam;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.filtering.FilteringService;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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
    @Path("_categories")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response listCategories(@BeanParam ApisParam apisParam) {
        Set<CategoryEntity> categories = filteringService.listCategories(
            getAuthenticatedUserOrNull(),
            convert(apisParam.getFilter()),
            convert(apisParam.getExcludedFilter())
        );
        return Response.ok(new DataResponse().data(categories)).build();
    }

    private FilteringService.FilterType convert(FilterApiQuery filter) {
        return filter != null ? FilteringService.FilterType.valueOf(filter.name()) : null;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApis(@BeanParam PaginationParam paginationParam, @BeanParam ApisParam apisParam) {
        FilteredEntities<String> filteredApis = findApisForCurrentUser(apisParam);
        Collection<String> filteredApisList = filteredApis.getFilteredItems();
        if (filteredApisList.size() > 0 && apisParam.getPromoted() != null) {
            //By default, the promoted API is the first of the list;
            String promotedApiId = filteredApisList.iterator().next();

            if (apisParam.isCategoryMode()) {
                // If apis are searched in a category, looks for the category highlighted API (HL API) and if this HL API is in the searchResult.
                // If it is, then the HL API becomes the promoted API
                String highlightedApiId = this.categoryService.findById(apisParam.getCategory()).getHighlightApi();
                if (highlightedApiId != null && filteredApisList.contains(highlightedApiId)) {
                    promotedApiId = highlightedApiId;
                }
            }
            String finalPromotedApiId = promotedApiId;
            if (apisParam.getPromoted() == Boolean.TRUE) {
                // Only the promoted API has to be returned
                if (filteredApisList.contains(finalPromotedApiId)) {
                    filteredApisList = Collections.singletonList(finalPromotedApiId);
                } else {
                    filteredApisList = Collections.emptyList();
                }
            } else if (apisParam.getPromoted() == Boolean.FALSE) {
                // All filtered API except the promoted API have to be returned
                if (apisParam.isCategoryMode() || apisParam.getCategory() != null) {
                    filteredApisList = this.findApisForCurrentUser(apisParam, createQueryFromParam(apisParam)).getFilteredItems();
                }
                filteredApisList.remove(finalPromotedApiId);
            }
        }

        return createListResponse(new ArrayList(filteredApisList), paginationParam, filteredApis.getMetadata());
    }

    private FilteredEntities<String> findApisForCurrentUser(ApisParam apisParam) {
        return findApisForCurrentUser(apisParam, null);
    }

    private FilteredEntities<String> findApisForCurrentUser(ApisParam apisParam, ApiQuery apiQuery) {
        FilteredEntities<String> filteredApis = filteringService.filterApis(
            getAuthenticatedUserOrNull(),
            convert(apisParam.getFilter()),
            convert(apisParam.getExcludedFilter()),
            apiQuery
        );
        return filteredApis;
    }

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response searchApis(
        @NotNull(message = "Input must not be null.") @QueryParam("q") String query,
        @BeanParam PaginationParam paginationParam
    ) {
        try {
            Collection<String> apisList = filteringService.searchApis(getAuthenticatedUserOrNull(), query);
            return createListResponse(new ArrayList(apisList), paginationParam);
        } catch (TechnicalException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    @Override
    protected List populatePage(List paginatedList) {
        return (List) paginatedList
            .stream()
            .map(
                o -> {
                    Api api = null;
                    if (!(o instanceof Api)) {
                        if (o instanceof String) {
                            api = apiMapper.convert(apiService.findById((String) o));
                        } else if (o instanceof ApiEntity) {
                            api = apiMapper.convert((ApiEntity) o);
                        }
                    } else {
                        api = (Api) o;
                    }
                    return addApiLinks(api);
                }
            )
            .collect(Collectors.toList());
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
        return api.links(apiMapper.computeApiLinks(PortalApiLinkHelper.apisURL(uriInfo.getBaseUriBuilder(), api.getId()), updateDate));
    }

    @Path("{apiId}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }
}
