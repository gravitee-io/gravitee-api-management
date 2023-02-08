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

import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_SUBSCRIPTION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.common.http.MediaType;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.SubscriptionConsumerStatus;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.KeyMapper;
import io.gravitee.rest.api.portal.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionConfigurationInput;
import io.gravitee.rest.api.portal.rest.model.UpdateSubscriptionInput;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

    private static final GraviteeMapper MAPPER = new GraviteeMapper();

    private static final String INCLUDE_KEYS = "keys";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSubscriptionBySubscriptionId(
        @PathParam("subscriptionId") String subscriptionId,
        @QueryParam("include") List<String> include
    ) {
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            hasPermission(executionContext, RolePermission.API_SUBSCRIPTION, subscriptionEntity.getApi(), RolePermissionAction.READ) ||
            hasPermission(executionContext, APPLICATION_SUBSCRIPTION, subscriptionEntity.getApplication(), RolePermissionAction.READ)
        ) {
            Subscription subscription = subscriptionMapper.convert(subscriptionEntity);
            if (include.contains(INCLUDE_KEYS)) {
                List<Key> keys = apiKeyService
                    .findBySubscription(executionContext, subscriptionId)
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
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (hasPermission(executionContext, APPLICATION_SUBSCRIPTION, subscriptionEntity.getApplication(), RolePermissionAction.DELETE)) {
            subscriptionService.close(executionContext, subscriptionId);
            return Response.noContent().build();
        }
        throw new ForbiddenAccessException();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSubscription(
        @PathParam("subscriptionId") String subscriptionId,
        @Valid @NotNull UpdateSubscriptionInput updateSubscriptionInput
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!hasPermission(executionContext, APPLICATION_SUBSCRIPTION, subscriptionEntity.getApplication(), UPDATE)) {
            throw new ForbiddenAccessException();
        }

        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        updateSubscriptionConfigurationEntity.setSubscriptionId(subscriptionId);
        updateSubscriptionConfigurationEntity.setMetadata(updateSubscriptionInput.getMetadata());
        SubscriptionConfigurationInput subscriptionConfigurationInput = updateSubscriptionInput.getConfiguration();
        if (subscriptionConfigurationInput != null) {
            SubscriptionConfigurationEntity subscriptionConfigurationEntity = new SubscriptionConfigurationEntity();
            subscriptionConfigurationEntity.setChannel(subscriptionConfigurationInput.getChannel());
            subscriptionConfigurationEntity.setEntrypointId(subscriptionConfigurationInput.getEntrypointId());
            subscriptionConfigurationEntity.setEntrypointConfiguration(
                MAPPER.valueToTree(subscriptionConfigurationInput.getEntrypointConfiguration())
            );
        }

        SubscriptionEntity updatedSubscription = subscriptionService.update(executionContext, updateSubscriptionConfigurationEntity);
        return Response.ok(updatedSubscription).build();
    }

    @POST
    @Path("_changeConsumerStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSubscriptionConsumerStatus(
        @PathParam("subscriptionId") String subscriptionId,
        @Parameter(required = true, schema = @Schema(allowableValues = { "STARTED", "STOPPED" })) @QueryParam(
            "status"
        ) SubscriptionConsumerStatus subscriptionConsumerStatus
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!hasPermission(executionContext, APPLICATION_SUBSCRIPTION, subscriptionEntity.getApplication(), UPDATE)) {
            throw new ForbiddenAccessException();
        }

        if (subscriptionConsumerStatus == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        switch (subscriptionConsumerStatus) {
            case STARTED:
                {
                    SubscriptionEntity updatedSubscriptionEntity = subscriptionService.resumeConsumer(executionContext, subscriptionId);
                    return Response.ok(updatedSubscriptionEntity).build();
                }
            case STOPPED:
                {
                    SubscriptionEntity updatedSubscriptionEntity = subscriptionService.pauseConsumer(executionContext, subscriptionId);
                    return Response.ok(updatedSubscriptionEntity).build();
                }
            default:
                return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @Path("keys")
    public SubscriptionKeysResource getSubscriptionKeysResource() {
        return resourceContext.getResource(SubscriptionKeysResource.class);
    }
}
