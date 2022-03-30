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
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.KeyMapper;
import io.gravitee.rest.api.portal.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private KeyMapper keyMapper;

    @Inject
    private SubscriptionMapper subscriptionMapper;

    private static final String INCLUDE_KEYS = "keys";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSubscriptionBySubscriptionId(
        @PathParam("subscriptionId") String subscriptionId,
        @QueryParam("include") List<String> include
    ) {
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);
        if (
            hasPermission(RolePermission.API_SUBSCRIPTION, subscriptionEntity.getApi(), RolePermissionAction.READ) ||
            hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, subscriptionEntity.getApplication(), RolePermissionAction.READ)
        ) {
            Subscription subscription = subscriptionMapper.convert(subscriptionEntity);
            if (include.contains(INCLUDE_KEYS)) {
                List<Key> keys = apiKeyService
                    .findBySubscription(GraviteeContext.getExecutionContext(), subscriptionId)
                    .stream()
                    .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                    .map(keyMapper::convert)
                    .collect(Collectors.toList());
                subscription.setKeys(keys);
            }
            return Response.ok(subscription).build();
        }
        throw new ForbiddenAccessException();
    }

    @POST
    @Path("/_close")
    @Produces(MediaType.APPLICATION_JSON)
    public Response closeSubscription(@PathParam("subscriptionId") String subscriptionId) {
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);
        if (hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, subscriptionEntity.getApplication(), RolePermissionAction.DELETE)) {
            subscriptionService.close(GraviteeContext.getExecutionContext(), subscriptionId);
            return Response.noContent().build();
        }
        throw new ForbiddenAccessException();
    }

    @Path("keys")
    public SubscriptionKeysResource getSubscriptionKeysResource() {
        return resourceContext.getResource(SubscriptionKeysResource.class);
    }
}
