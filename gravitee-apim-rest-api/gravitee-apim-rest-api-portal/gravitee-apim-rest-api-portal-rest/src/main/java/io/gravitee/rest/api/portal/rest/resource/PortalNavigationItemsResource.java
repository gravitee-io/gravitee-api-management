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

import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;

import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.use_case.GetVisiblePortalNavigationApisUseCase;
import io.gravitee.apim.core.portal_page.use_case.ListPortalNavigationItemsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.mapper.PortalNavigationItemMapper;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.PortalArea;
import io.gravitee.rest.api.portal.rest.model.PortalNavigationItemsSearchResponse;
import io.gravitee.rest.api.portal.rest.model.PortalNavigationSearchInclude;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PortalNavigationItemsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ListPortalNavigationItemsUseCase listPortalNavigationItemsUseCase;

    @Inject
    private GetVisiblePortalNavigationApisUseCase getVisiblePortalNavigationApisUseCase;

    @Inject
    private ApiMapper apiMapper;

    private final PortalNavigationItemMapper portalNavigationItemMapper = PortalNavigationItemMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalNavigationItems(
        @Nonnull @QueryParam("area") PortalArea area,
        @Nullable @QueryParam("parentId") String parentId,
        @QueryParam("loadChildren") @DefaultValue("true") boolean loadChildren
    ) {
        var executionContext = getExecutionContext();
        var input = new ListPortalNavigationItemsUseCase.Input(
            executionContext.getEnvironmentId(),
            portalNavigationItemMapper.map(area),
            Optional.ofNullable(parentId).map(PortalNavigationItemId::of),
            loadChildren,
            PortalNavigationItemViewerContext.forPortal(isAuthenticated())
        );
        var output = listPortalNavigationItemsUseCase.execute(input);
        return Response.ok(portalNavigationItemMapper.map(output.items())).build();
    }

    @GET
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPortalNavigationItems(
        @QueryParam("query") String query,
        @QueryParam("type") String type,
        @QueryParam("include") Set<String> include,
        @BeanParam PaginationParam paginationParam
    ) {
        if (!"api".equalsIgnoreCase(type)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse()).build();
        }

        Set<io.gravitee.apim.core.portal_page.model.PortalNavigationSearchInclude> coreIncludes = parseIncludes(include);

        var executionContext = getExecutionContext();
        var output = getVisiblePortalNavigationApisUseCase.execute(
            new GetVisiblePortalNavigationApisUseCase.Input(
                executionContext.getEnvironmentId(),
                executionContext.getOrganizationId(),
                Optional.ofNullable(getAuthenticatedUserOrNull()),
                new PageableImpl(paginationParam.getPage(), paginationParam.getSize()),
                Optional.ofNullable(query),
                coreIncludes
            )
        );

        var page = output.apis();
        List<io.gravitee.rest.api.portal.rest.model.PortalNavigationItem> pageItems = mapPageItems(page.getContent());
        List<io.gravitee.rest.api.portal.rest.model.Api> includedApis = loadIncludedApiEntities(executionContext, output.includedApis());

        var responseBody = new PortalNavigationItemsSearchResponse()
            .data(pageItems)
            .metadata(buildSearchMetadata(paginationParam, page.getTotalElements(), pageItems.size()))
            .links(computePaginatedLinks(paginationParam.getPage(), paginationParam.getSize(), (int) page.getTotalElements()));

        if (!includedApis.isEmpty()) {
            responseBody.apis(includedApis);
        }

        return Response.ok(responseBody).build();
    }

    private Map<String, Map<String, Object>> buildSearchMetadata(PaginationParam paginationParam, long totalElements, int dataTotal) {
        int pageNumber = paginationParam.getPage();
        int pageSize = paginationParam.getSize();
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
        int startIndex = pageSize > 0 ? (pageNumber - 1) * pageSize : 0;
        int lastIndex = pageSize > 0 ? (int) Math.min((long) startIndex + pageSize, totalElements) : (int) totalElements;

        Map<String, Object> paginationMetadata = new HashMap<>();
        paginationMetadata.put(METADATA_PAGINATION_TOTAL_KEY, (int) totalElements);
        paginationMetadata.put(METADATA_PAGINATION_SIZE_KEY, pageSize);
        paginationMetadata.put(METADATA_PAGINATION_CURRENT_PAGE_KEY, pageNumber);
        paginationMetadata.put(METADATA_PAGINATION_TOTAL_PAGE_KEY, totalPages);
        paginationMetadata.put(METADATA_PAGINATION_FIRST_ITEM_INDEX_KEY, totalElements > 0 ? startIndex + 1 : 0);
        paginationMetadata.put(METADATA_PAGINATION_LAST_ITEM_INDEX_KEY, lastIndex);

        Map<String, Map<String, Object>> metadata = new HashMap<>();
        metadata.put(METADATA_PAGINATION_KEY, paginationMetadata);
        metadata.put(METADATA_DATA_KEY, Map.of(METADATA_DATA_TOTAL_KEY, dataTotal));
        return metadata;
    }

    private Set<io.gravitee.apim.core.portal_page.model.PortalNavigationSearchInclude> parseIncludes(Set<String> include) {
        Set<PortalNavigationSearchInclude> restIncludes = include == null
            ? Set.of()
            : include.stream().map(PortalNavigationSearchInclude::fromValue).collect(Collectors.toSet());
        return portalNavigationItemMapper.map(restIncludes);
    }

    private List<io.gravitee.rest.api.portal.rest.model.PortalNavigationItem> mapPageItems(
        List<io.gravitee.apim.core.portal_page.model.PortalNavigationApi> content
    ) {
        return portalNavigationItemMapper.map(
            content
                .stream()
                .map(i -> (PortalNavigationItem) i)
                .toList()
        );
    }

    private List<io.gravitee.rest.api.portal.rest.model.Api> loadIncludedApiEntities(
        io.gravitee.rest.api.service.common.ExecutionContext executionContext,
        List<io.gravitee.apim.core.api.model.Api> domainApis
    ) {
        if (domainApis.isEmpty()) {
            return List.of();
        }
        Set<String> ids = domainApis.stream().map(io.gravitee.apim.core.api.model.Api::getId).collect(Collectors.toSet());
        return apiSearchService
            .findGenericByEnvironmentAndIdIn(executionContext, ids)
            .stream()
            .map(entity -> apiMapper.convert(executionContext, entity))
            .toList();
    }

    @Path("{portalNavigationItemId}")
    public PortalNavigationItemResource getPortalNavigationItemResource() {
        return resourceContext.getResource(PortalNavigationItemResource.class);
    }
}
