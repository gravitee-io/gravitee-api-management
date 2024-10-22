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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.apim.core.category.use_case.GetCategoryApisUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.mapper.CategoryMapper;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.Category;
import io.gravitee.rest.api.portal.rest.model.FilterApiQuery;
import io.gravitee.rest.api.portal.rest.resource.param.ApisParam;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.filtering.FilteringService;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisResource extends AbstractResource<Api, String> {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiMapper apiMapper;

    @Inject
    private FilteringService filteringService;

    @Inject
    private CategoryService categoryService;

    @Inject
    private ParameterService parameterService;

    @Inject
    private GetCategoryApisUseCase getCategoryApisUseCase;

    @Inject
    private ApiCategoryService apiCategoryService;

    @Inject
    private CategoryMapper categoryMapper;

    @GET
    @Path("categories")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response listCategories(@BeanParam ApisParam apisParam) {
        Set<CategoryEntity> categories = filteringService.listCategories(
            GraviteeContext.getExecutionContext(),
            getAuthenticatedUserOrNull(),
            convert(apisParam.getFilter()),
            convert(apisParam.getExcludedFilter())
        );

        var countByCategory = apiCategoryService.countApisPublishedGroupedByCategoriesForUser(getAuthenticatedUserOrNull());

        List<Category> categoryList = categories
            .stream()
            .peek(categoryEntity -> categoryEntity.setTotalApis(countByCategory.applyAsLong(categoryEntity.getId())))
            .map(categoryEntity -> categoryMapper.convert(categoryEntity, uriInfo.getBaseUriBuilder()))
            .collect(Collectors.toList());

        return Response.ok(new DataResponse().data(categoryList)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApis(@BeanParam PaginationParam paginationParam, @BeanParam ApisParam apisParam) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        Collection<String> filteredApis = new ArrayList<>(
            findApisForCurrentUser(executionContext, apisParam, createQueryFromParam(apisParam))
        );

        if (!filteredApis.isEmpty() && apisParam.getPromoted() != null) {
            //By default, the promoted API is the first of the list;
            String promotedApiId = filteredApis.iterator().next();

            if (apisParam.isCategoryMode()) {
                // If apis are searched in a category, looks for the category highlighted API (HL API) and if this HL API is in the searchResult.
                // If it is, then the HL API becomes the promoted API
                String highlightedApiId =
                    this.categoryService.findById(apisParam.getCategory(), GraviteeContext.getCurrentEnvironment()).getHighlightApi();
                if (highlightedApiId != null && filteredApis.contains(highlightedApiId)) {
                    promotedApiId = highlightedApiId;
                }
            }
            String finalPromotedApiId = promotedApiId;
            if (apisParam.getPromoted() == Boolean.TRUE) {
                // Only the promoted API has to be returned
                if (filteredApis.contains(finalPromotedApiId)) {
                    filteredApis = Collections.singletonList(finalPromotedApiId);
                } else {
                    filteredApis = Collections.emptyList();
                }
            } else if (apisParam.getPromoted() == Boolean.FALSE) {
                // All filtered API except the promoted API have to be returned
                filteredApis.remove(finalPromotedApiId);
            }
        }

        return createListResponse(executionContext, filteredApis, paginationParam, null);
    }

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response searchApis(
        @QueryParam("q") String query,
        @QueryParam("category") String category,
        @BeanParam PaginationParam paginationParam
    ) {
        try {
            final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
            Collection<String> apisList;
            if (category == null || category.isEmpty()) {
                apisList = filteringService.searchApis(executionContext, getAuthenticatedUserOrNull(), query);
            } else {
                apisList = filteringService.searchApisWithCategory(executionContext, getAuthenticatedUserOrNull(), query, category);
            }
            return createListResponse(executionContext, new ArrayList<>(apisList), paginationParam);
        } catch (TechnicalException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    @Path("{apiId}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }

    @Override
    protected List<Api> transformPageContent(ExecutionContext executionContext, List<String> pageContent) {
        if (pageContent.isEmpty()) {
            return Collections.emptyList();
        }
        final boolean apiShowTagsInApiHeaders = parameterService.findAsBoolean(
            executionContext,
            Key.PORTAL_APIS_SHOW_TAGS_IN_APIHEADER,
            ParameterReferenceType.ENVIRONMENT
        );

        ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(pageContent);
        Collection<GenericApiEntity> apiEntities = apiSearchService.search(executionContext, apiQuery);
        Comparator<String> orderingComparator = Comparator.comparingInt(pageContent::indexOf);
        return apiEntities
            .stream()
            .map(apiEntity -> {
                Api api = apiMapper.convert(executionContext, apiEntity);
                return addApiLinks(api);
            })
            .peek(api -> {
                if (!apiShowTagsInApiHeaders) {
                    api.setLabels(List.of());
                }
            })
            .sorted((o1, o2) -> orderingComparator.compare(o1.getId(), o2.getId()))
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

            boolean isCategoryMode = (apisParam.getCategory() != null && apisParam.getFilter() == null);
            if (isCategoryMode) {
                apiQuery.setCategory(apisParam.getCategory());
            } else {
                apisParam.setCategory(null);
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

    private FilteringService.FilterType convert(FilterApiQuery filter) {
        return filter != null ? FilteringService.FilterType.valueOf(filter.name()) : null;
    }

    private Collection<String> findApisForCurrentUser(final ExecutionContext executionContext, ApisParam apisParam, ApiQuery apiQuery) {
        if (!apisParam.isCategoryMode()) {
            return filteringService.filterApis(
                executionContext,
                getAuthenticatedUserOrNull(),
                convert(apisParam.getFilter()),
                convert(apisParam.getExcludedFilter()),
                apiQuery
            );
        }
        return getCategoryApisUseCase
            .execute(new GetCategoryApisUseCase.Input(executionContext, apisParam.getCategory(), getAuthenticatedUser(), false, true))
            .results()
            .stream()
            .map(result -> result.api().getId())
            .toList();
    }
}
