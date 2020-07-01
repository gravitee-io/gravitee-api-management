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
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.SubscriptionEntity;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.model.subscription.SubscriptionQuery;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.SubscriptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Application Subscriptions"})
public class ApplicationSubscribedResource extends AbstractResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApiService apiService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List APIs subscribed by the application",
            notes = "User must have the APPLICATION_SUBSCRIPTION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Paged result of subscribed APIs", response = ApplicationEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ)
    })
    public Collection<SubscribedApi> listApiSubscribed(
            @PathParam("application") String application) {
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApplication(application);

        Collection<SubscriptionEntity> subscriptions = subscriptionService.search(subscriptionQuery);
        return subscriptions.stream()
                .map(SubscriptionEntity::getApi)
                .distinct()
                .map(api -> apiService.findById(api))
                .map(apiEntity -> new SubscribedApi(apiEntity.getId(), apiEntity.getName()))
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .collect(Collectors.toList());
    }

    private static class SubscribedApi {
        private final String id;
        private final String name;

        SubscribedApi(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
