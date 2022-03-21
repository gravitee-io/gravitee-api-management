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
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.annotations.*;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "API Subscriptions" })
public class ApiSubscribersResource extends AbstractResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApplicationService applicationService;

    @Context
    private ResourceContext resourceContext;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List subscribers for the API", notes = "User must have the MANAGE_SUBSCRIPTIONS permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "Paged result of API subscribers",
                response = ApplicationEntity.class,
                responseContainer = "List"
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Collection<ApplicationEntity> getApiSubscribers() {
        if (
            !hasPermission(RolePermission.API_SUBSCRIPTION, api, RolePermissionAction.READ) &&
            !hasPermission(RolePermission.API_LOG, api, RolePermissionAction.READ)
        ) {
            throw new ForbiddenAccessException();
        }

        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApi(api);

        Collection<SubscriptionEntity> subscriptions = subscriptionService.search(subscriptionQuery);
        return subscriptions
            .stream()
            .map(SubscriptionEntity::getApplication)
            .distinct()
            .map(application -> applicationService.findById(GraviteeContext.getExecutionContext(), application))
            .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
            .collect(Collectors.toList());
    }
}
