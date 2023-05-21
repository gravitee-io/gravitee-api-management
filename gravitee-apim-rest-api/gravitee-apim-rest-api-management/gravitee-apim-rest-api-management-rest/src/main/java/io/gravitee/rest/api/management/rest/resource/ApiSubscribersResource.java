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

import static io.gravitee.rest.api.model.SubscriptionStatus.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.Pageable;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.application.ApplicationExcludeFilter;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "API Subscriptions")
public class ApiSubscribersResource extends AbstractResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApplicationService applicationService;

    @Context
    private ResourceContext resourceContext;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @Parameter(name = "api", hidden = true)
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List subscribers for the API",
        description = "User must have the MANAGE_SUBSCRIPTIONS permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Paged result of API subscribers",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ApplicationListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Collection<ApplicationListItem> getApiSubscribers(
        @QueryParam("query") final String query,
        @QueryParam("exclude") final List<ApplicationExcludeFilter> exclude,
        @Valid @BeanParam Pageable pageable
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            !hasPermission(executionContext, RolePermission.API_SUBSCRIPTION, api, RolePermissionAction.READ) &&
            !hasPermission(executionContext, RolePermission.API_LOG, api, RolePermissionAction.READ)
        ) {
            throw new ForbiddenAccessException();
        }

        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApi(api);

        Collection<SubscriptionEntity> subscriptions = subscriptionService.search(executionContext, subscriptionQuery);

        Set<String> applicationIds = subscriptions.stream().map(SubscriptionEntity::getApplication).collect(Collectors.toSet());

        if (applicationIds.isEmpty()) {
            return Collections.emptyList();
        }

        ApplicationQuery applicationQuery = new ApplicationQuery();
        if (exclude != null && !exclude.isEmpty()) {
            applicationQuery.setExcludeFilters(exclude);
        }
        applicationQuery.setIds(applicationIds);
        if (query != null && !query.isEmpty()) {
            applicationQuery.setName(query);
        }

        Sortable sortable = new SortableImpl("name", true);

        Page<ApplicationListItem> subscribersApplicationPage = applicationService.search(
            executionContext,
            applicationQuery,
            sortable,
            pageable.toPageable()
        );

        if (subscribersApplicationPage == null) {
            return Collections.emptyList();
        }

        return subscribersApplicationPage.getContent();
    }
}
