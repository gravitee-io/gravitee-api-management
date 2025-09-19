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
package io.gravitee.rest.api.management.v2.rest.resource.api.event;

import static io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo.computePaginationInfo;

import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.core.event.use_case.SearchApiEventUseCase;
import io.gravitee.apim.core.event.use_case.SearchEventsUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiEventMapper;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.event.param.SearchApiEventsParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class ApiEventsResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @Inject
    private SearchEventsUseCase searchEventsUseCase;

    @Inject
    private SearchApiEventUseCase searchApiEventUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_EVENT, acls = { RolePermissionAction.READ }) })
    public EventsResponse getApiEvents(@BeanParam @Valid PaginationParam paginationParam, @BeanParam @Valid SearchApiEventsParam params) {
        var input = buildInput(paginationParam, params);

        var output = searchEventsUseCase.execute(input);

        return EventsResponse.builder()
            .data(ApiEventMapper.INSTANCE.map(output.data()))
            .pagination(computePaginationInfo(output.total(), output.data().size(), paginationParam))
            .links(computePaginationLinks(output.total(), paginationParam))
            .build();
    }

    @NotNull
    private SearchEventsUseCase.Input buildInput(PaginationParam paginationParam, SearchApiEventsParam params) {
        var executionContext = GraviteeContext.getExecutionContext();
        var query = new EventQueryService.SearchQuery(
            executionContext.getEnvironmentId(),
            Optional.empty(),
            Optional.of(apiId),
            params.getTypes() != null
                ? params
                    .getTypes()
                    .stream()
                    .map(s -> EventType.valueOf(s.toUpperCase()))
                    .toList()
                : List.of(),
            Map.of(),
            Optional.ofNullable(params.getFrom()),
            Optional.ofNullable(params.getTo())
        );
        return new SearchEventsUseCase.Input(query, new PageableImpl(paginationParam.getPage(), paginationParam.getPerPage()));
    }

    @Path("/{eventId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_EVENT, acls = { RolePermissionAction.READ }) })
    public Event getApiEvent(@PathParam("eventId") @NotBlank String eventId) {
        var input = new SearchApiEventUseCase.Input(eventId, GraviteeContext.getCurrentEnvironment(), apiId);

        return searchApiEventUseCase
            .execute(input)
            .apiEvent()
            .map(ApiEventMapper.INSTANCE::map)
            .orElseThrow(() -> new NotFoundException("Not API event with id: " + eventId + " found"));
    }
}
