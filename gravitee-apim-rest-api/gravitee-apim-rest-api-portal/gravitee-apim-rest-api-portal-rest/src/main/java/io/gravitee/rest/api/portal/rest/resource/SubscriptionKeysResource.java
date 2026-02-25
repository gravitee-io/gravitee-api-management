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

import io.gravitee.apim.core.api_key.use_case.RevokeSubscriptionApiKeyUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.KeyMapper;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionKeysResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private RevokeSubscriptionApiKeyUseCase revokeSubscriptionApiKeyUsecase;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private KeyMapper keyMapper;

    @POST
    @Path("/_renew")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response renewKeySubscription(@PathParam("subscriptionId") String subscriptionId) {
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            hasPermission(
                executionContext,
                RolePermission.APPLICATION_SUBSCRIPTION,
                subscriptionEntity.getApplication(),
                RolePermissionAction.UPDATE
            ) ||
            hasPermission(executionContext, RolePermission.API_SUBSCRIPTION, subscriptionEntity.getApi(), RolePermissionAction.UPDATE)
        ) {
            final Key createdKey = keyMapper.convert(apiKeyService.renew(executionContext, subscriptionEntity));
            return Response.status(Response.Status.CREATED).entity(createdKey).build();
        }
        throw new ForbiddenAccessException();
    }

    @POST
    @Path("/{apiKey}/_revoke")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeKeySubscription(@PathParam("subscriptionId") String subscriptionId, @PathParam("apiKey") String apiKey) {
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            hasPermission(
                executionContext,
                RolePermission.APPLICATION_SUBSCRIPTION,
                subscriptionEntity.getApplication(),
                RolePermissionAction.UPDATE
            ) ||
            hasPermission(executionContext, RolePermission.API_SUBSCRIPTION, subscriptionEntity.getApi(), RolePermissionAction.UPDATE)
        ) {
            final String referenceId = subscriptionEntity.getReferenceId() != null
                ? subscriptionEntity.getReferenceId()
                : subscriptionEntity.getApi();
            final String referenceType = subscriptionEntity.getReferenceType() != null ? subscriptionEntity.getReferenceType() : "API";
            revokeSubscriptionApiKeyUsecase.execute(
                new RevokeSubscriptionApiKeyUseCase.Input(subscriptionId, apiKey, referenceId, referenceType, getAuditInfo())
            );

            return Response.noContent().build();
        }
        throw new ForbiddenAccessException();
    }
}
