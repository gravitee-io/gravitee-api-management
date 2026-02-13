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

import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_SUBSCRIPTION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.use_case.CloseSubscriptionUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.*;
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
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CloseSubscriptionUseCase closeSubscriptionUsecase;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private KeyMapper keyMapper;

    @Inject
    private GraviteeMapper graviteeMapper;

    private static final String INCLUDE_KEYS = "keys";
    private static final String INCLUDE_CONSUMER_CONFIGURATION = "consumerConfiguration";

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
            Subscription subscription = SubscriptionMapper.INSTANCE.map(subscriptionEntity);
            if (include.contains(INCLUDE_KEYS)) {
                List<Key> keys = apiKeyService
                    .findBySubscription(executionContext, subscriptionId)
                    .stream()
                    .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                    .map(keyMapper::convert)
                    .toList();
                subscription.setKeys(keys);
            }

            if (include.contains(INCLUDE_CONSUMER_CONFIGURATION)) {
                subscription.setConsumerConfiguration(SubscriptionMapper.INSTANCE.map(subscriptionEntity.getConfiguration()));
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
            final var user = getAuthenticatedUserDetails();

            closeSubscriptionUsecase.execute(
                new CloseSubscriptionUseCase.Input(
                    subscriptionId,
                    AuditInfo.builder()
                        .organizationId(executionContext.getOrganizationId())
                        .environmentId(executionContext.getEnvironmentId())
                        .actor(
                            AuditActor.builder()
                                .userId(user.getUsername())
                                .userSource(user.getSource())
                                .userSourceId(user.getSourceId())
                                .build()
                        )
                        .build()
                )
            );
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
        SubscriptionConfigurationInput subscriptionConfigurationInput = updateSubscriptionInput.getConfiguration();

        if (updateSubscriptionInput.getMetadata() != null) {
            updateSubscriptionConfigurationEntity.setMetadata(updateSubscriptionInput.getMetadata());
        }
        if (subscriptionConfigurationInput != null) {
            SubscriptionConfigurationEntity subscriptionConfigurationEntity = new SubscriptionConfigurationEntity();
            subscriptionConfigurationEntity.setChannel(subscriptionConfigurationInput.getChannel());
            subscriptionConfigurationEntity.setEntrypointId(subscriptionConfigurationInput.getEntrypointId());
            subscriptionConfigurationEntity.setEntrypointConfiguration(
                graviteeMapper.valueToTree(subscriptionConfigurationInput.getEntrypointConfiguration())
            );
            updateSubscriptionConfigurationEntity.setConfiguration(subscriptionConfigurationEntity);
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

        return switch (subscriptionConsumerStatus) {
            case STARTED -> {
                SubscriptionEntity updatedSubscriptionEntity = subscriptionService.resumeConsumer(executionContext, subscriptionId);
                yield Response.ok(updatedSubscriptionEntity).build();
            }
            case STOPPED -> {
                SubscriptionEntity updatedSubscriptionEntity = subscriptionService.pauseConsumer(executionContext, subscriptionId);
                yield Response.ok(updatedSubscriptionEntity).build();
            }
            default -> Response.status(Response.Status.BAD_REQUEST).build();
        };
    }

    @POST
    @Path("_resumeFailure")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resumeFailedSubscription(@PathParam("subscriptionId") String subscriptionId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!hasPermission(executionContext, APPLICATION_SUBSCRIPTION, subscriptionEntity.getApplication(), UPDATE)) {
            throw new ForbiddenAccessException();
        }

        if (subscriptionEntity.getConsumerStatus() != SubscriptionConsumerStatus.FAILURE) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        SubscriptionEntity updatedSubscriptionEntity = subscriptionService.resumeFailed(executionContext, subscriptionId);
        return Response.ok(updatedSubscriptionEntity).build();
    }

    @Path("keys")
    public SubscriptionKeysResource getSubscriptionKeysResource() {
        return resourceContext.getResource(SubscriptionKeysResource.class);
    }
}
