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
package io.gravitee.rest.api.management.v2.rest.resource.logs;

import io.gravitee.apim.core.logs_engine.use_case.SearchEnvironmentLogsUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.LogsEngineMapper;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.Links;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.SearchLogsRequest;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.SearchLogsResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationLinks;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * @author GraviteeSource Team
 */
public class LogsSearchResource extends AbstractResource {

    public static final String PAGE_QUERY_PARAM_NAME = "page";
    public static final String PER_PAGE_QUERY_PARAM_NAME = "perPage";

    @Inject
    SearchEnvironmentLogsUseCase searchEnvironmentLogsUseCase;

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchLogsResponse searchLogs(
        @QueryParam(PAGE_QUERY_PARAM_NAME) @Valid @DefaultValue("1") @Min(1) int page,
        @QueryParam(PER_PAGE_QUERY_PARAM_NAME) @Valid @DefaultValue("10") @Min(1) @Max(100) int perPage,
        @Valid SearchLogsRequest request
    ) {
        var input = new SearchEnvironmentLogsUseCase.Input(
            getAuditInfo(),
            LogsEngineMapper.INSTANCE.fromRequestEntity(request, page, perPage)
        );
        var output = searchEnvironmentLogsUseCase.execute(input);
        SearchLogsResponse searchLogsResponse = LogsEngineMapper.INSTANCE.fromResponseModel(output.response());

        var links = getLinks(searchLogsResponse, input);
        searchLogsResponse.setLinks(links);

        return searchLogsResponse;
    }

    /**
     * Generates HATEOAS pagination links for the search response.
     * <p>
     * Utilizes the {@link PaginationLinks} utility to compute self, first, last,
     * next, and previous links
     * based on current pagination state and request URI. This follows REST HATEOAS
     * principles to enable
     * client-side navigation through paginated results.
     * </p>
     * <p>
     * <strong>Contract:</strong> The {@code searchLogsResponse.getPagination()} is
     * guaranteed to be non-null
     * by the {@link SearchEnvironmentLogsUseCase}, as pagination metadata is always
     * generated even for empty results.
     * </p>
     *
     * @param searchLogsResponse The search response containing pagination metadata
     *                           (non-null pagination required)
     * @param input              The original request input containing page/perPage
     *                           parameters used for link computation
     * @return Links object populated with HATEOAS links (self, first, last, next,
     *         previous)
     */
    private Links getLinks(SearchLogsResponse searchLogsResponse, SearchEnvironmentLogsUseCase.Input input) {
        Links responseLinks = new Links();

        Pagination pagination = searchLogsResponse.getPagination();

        var paginationLinks = PaginationLinks.computePaginationLinks(
            uriInfo.getRequestUri(),
            uriInfo.getQueryParameters(),
            pagination.getTotalCount(),
            new PaginationParam(input.request().page(), input.request().perPage())
        );

        if (paginationLinks != null) {
            responseLinks.setFirst(paginationLinks.getFirst());
            responseLinks.setLast(paginationLinks.getLast());
            responseLinks.setPrevious(paginationLinks.getPrevious());
            responseLinks.setNext(paginationLinks.getNext());
            responseLinks.setSelf(paginationLinks.getSelf());
        }
        return responseLinks;
    }
}
