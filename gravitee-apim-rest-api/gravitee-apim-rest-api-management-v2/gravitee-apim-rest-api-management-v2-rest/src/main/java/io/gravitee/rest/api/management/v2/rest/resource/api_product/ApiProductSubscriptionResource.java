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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import io.gravitee.apim.core.api_key.use_case.RevokeApiSubscriptionApiKeyUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.use_case.AcceptSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.CloseSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.GetSubscriptionsUseCase;
import io.gravitee.apim.core.subscription.use_case.RejectSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.UpdateSubscriptionUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.TransferSubscriptionEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;

/**
 * API Product Subscription REST resource (singular).
 * Handles operations on a specific subscription.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class ApiProductSubscriptionResource extends AbstractResource {

    private final SubscriptionMapper subscriptionMapper = SubscriptionMapper.INSTANCE;

    @Inject
    private GetSubscriptionsUseCase getSubscriptionsUseCase;

    @Inject
    private UpdateSubscriptionUseCase updateSubscriptionUseCase;

    @Inject
    private AcceptSubscriptionUseCase acceptSubscriptionUseCase;

    @Inject
    private RejectSubscriptionUseCase rejectSubscriptionUseCase;

    @Inject
    private CloseSubscriptionUseCase closeSubscriptionUseCase;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private ParameterService parameterService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private RevokeApiSubscriptionApiKeyUseCase revokeApiSubscriptionApiKeyUseCase;

    @PathParam("apiProductId")
    private String apiProductId;

    @PathParam("subscriptionId")
    private String subscriptionId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.READ }) })
    public Response getApiProductSubscription() {
        log.debug("Getting subscription {} for API Product {}", subscriptionId, apiProductId);
        var output = getSubscriptionsUseCase.execute(
            GetSubscriptionsUseCase.Input.of(apiProductId, SubscriptionReferenceType.API_PRODUCT, subscriptionId)
        );
        Optional<io.gravitee.apim.core.subscription.model.SubscriptionEntity> subscription = output.subscription();

        if (subscription.isEmpty()) {
            log.debug("Subscription {} not found for API Product {}", subscriptionId, apiProductId);
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }
        io.gravitee.apim.core.subscription.model.SubscriptionEntity coreSubscription = subscription.orElseThrow();
        log.debug("Subscription {} found for API Product {}", subscriptionId, apiProductId);
        return Response.ok(subscriptionMapper.map(coreSubscription)).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response updateApiProductSubscription(@Valid @NotNull UpdateSubscription updateSubscription) {
        log.debug("Updating subscription {} for API Product {}", subscriptionId, apiProductId);
        SubscriptionConfigurationEntity configuration = null;
        if (updateSubscription.getConsumerConfiguration() != null) {
            configuration = subscriptionMapper.mapConsumerConfigurationToEntity(updateSubscription.getConsumerConfiguration());
        }

        var input = UpdateSubscriptionUseCase.Input.builder()
            .referenceId(apiProductId)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .subscriptionId(subscriptionId)
            .configuration(configuration)
            .metadata(updateSubscription.getMetadata())
            .startingAt(updateSubscription.getStartingAt() != null ? updateSubscription.getStartingAt().toZonedDateTime() : null)
            .endingAt(updateSubscription.getEndingAt() != null ? updateSubscription.getEndingAt().toZonedDateTime() : null)
            .auditInfo(getAuditInfo())
            .build();

        var output = updateSubscriptionUseCase.execute(input);
        log.debug("Updated subscription {} for API Product {}", subscriptionId, apiProductId);
        return Response.ok(subscriptionMapper.map(output.subscription())).build();
    }

    @POST
    @Path("/_accept")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response acceptApiProductSubscription(@Valid @NotNull AcceptSubscription acceptSubscription) {
        log.debug("Accepting subscription {} for API Product {}", subscriptionId, apiProductId);
        var input = AcceptSubscriptionUseCase.Input.builder()
            .referenceId(apiProductId)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .subscriptionId(subscriptionId)
            .startingAt(acceptSubscription.getStartingAt() != null ? acceptSubscription.getStartingAt().toZonedDateTime() : null)
            .endingAt(acceptSubscription.getEndingAt() != null ? acceptSubscription.getEndingAt().toZonedDateTime() : null)
            .reasonMessage(acceptSubscription.getReason())
            .customKey(acceptSubscription.getCustomApiKey())
            .auditInfo(getAuditInfo())
            .build();

        var output = acceptSubscriptionUseCase.execute(input);
        log.debug("Accepted subscription {} for API Product {}", subscriptionId, apiProductId);
        return Response.ok(subscriptionMapper.map(output.subscription())).build();
    }

    @POST
    @Path("/_reject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response rejectApiProductSubscription(@Valid @NotNull RejectSubscription rejectSubscription) {
        log.debug("Rejecting subscription {} for API Product {}", subscriptionId, apiProductId);
        var input = RejectSubscriptionUseCase.Input.builder()
            .referenceId(apiProductId)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .subscriptionId(subscriptionId)
            .reasonMessage(rejectSubscription.getReason())
            .auditInfo(getAuditInfo())
            .build();

        var output = rejectSubscriptionUseCase.execute(input);
        log.debug("Rejected subscription {} for API Product {}", subscriptionId, apiProductId);
        return Response.ok(subscriptionMapper.map(output.subscription())).build();
    }

    @POST
    @Path("/_close")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response closeApiProductSubscription() {
        log.debug("Closing subscription {} for API Product {}", subscriptionId, apiProductId);
        var input = CloseSubscriptionUseCase.Input.builder()
            .referenceId(apiProductId)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .subscriptionId(subscriptionId)
            .auditInfo(getAuditInfo())
            .build();

        var output = closeSubscriptionUseCase.execute(input);
        log.debug("Closed subscription {} for API Product {}", subscriptionId, apiProductId);
        return Response.ok(subscriptionMapper.map(output.subscription())).build();
    }

    @POST
    @Path("/_pause")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response pauseApiProductSubscription() {
        checkSubscription();
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        return Response.ok(subscriptionMapper.map(subscriptionService.pause(executionContext, subscriptionId))).build();
    }

    @POST
    @Path("/_resume")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response resumeApiProductSubscription() {
        checkSubscription();
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        return Response.ok(subscriptionMapper.map(subscriptionService.resume(executionContext, subscriptionId))).build();
    }

    @POST
    @Path("/_resumeFailure")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response resumeFailedApiProductSubscription() {
        checkSubscription();
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        return Response.ok(subscriptionMapper.map(subscriptionService.resumeFailed(executionContext, subscriptionId))).build();
    }

    @POST
    @Path("/_transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response transferApiProductSubscription(@Valid @NotNull TransferSubscription transferSubscription) {
        checkSubscription();
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final TransferSubscriptionEntity transferSubscriptionEntity = subscriptionMapper.map(transferSubscription, subscriptionId);
        return Response.ok(
            subscriptionMapper.map(subscriptionService.transfer(executionContext, transferSubscriptionEntity, getAuthenticatedUser()))
        ).build();
    }

    @GET
    @Path("/api-keys")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.READ }) })
    public Response getApiProductSubscriptionApiKeys(@BeanParam @Valid PaginationParam paginationParam) {
        checkSubscription();
        final List<ApiKey> apiKeys = subscriptionMapper.mapToApiKeyList(
            apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), subscriptionId)
        );
        final List<ApiKey> apiKeysSubList = computePaginationData(apiKeys, paginationParam);
        return Response.ok(
            new SubscriptionApiKeysResponse()
                .data(apiKeysSubList)
                .pagination(PaginationInfo.computePaginationInfo(apiKeys.size(), apiKeysSubList.size(), paginationParam))
                .links(computePaginationLinks(apiKeys.size(), paginationParam))
        ).build();
    }

    @POST
    @Path("/api-keys/_renew")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response renewApiProductSubscriptionApiKeys(@Valid @NotNull RenewApiKey renewApiKey) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            StringUtils.isNotEmpty(renewApiKey.getCustomApiKey()) &&
            !parameterService.findAsBoolean(executionContext, Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED, ParameterReferenceType.ENVIRONMENT)
        ) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(subscriptionInvalid("You are not allowed to provide a custom API Key"))
                .build();
        }
        io.gravitee.rest.api.model.SubscriptionEntity subscriptionEntity = checkSubscription();
        checkApplicationDoesntUseSharedApiKey(executionContext, subscriptionEntity.getApplication());
        final io.gravitee.rest.api.model.ApiKeyEntity apiKeyEntity = apiKeyService.renew(
            executionContext,
            subscriptionEntity,
            renewApiKey.getCustomApiKey()
        );
        return Response.ok(subscriptionMapper.mapToApiKey(apiKeyEntity)).build();
    }

    @PUT
    @Path("/api-keys/{apiKeyId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response updateApiProductSubscriptionApiKey(@PathParam("apiKeyId") String apiKeyId, @Valid @NotNull UpdateApiKey updateApiKey) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        io.gravitee.rest.api.model.SubscriptionEntity subscriptionEntity = checkSubscription();
        checkApplicationDoesntUseSharedApiKey(executionContext, subscriptionEntity.getApplication());
        final io.gravitee.rest.api.model.ApiKeyEntity apiKeyEntity = apiKeyService.findById(executionContext, apiKeyId);
        if (!apiKeyEntity.getSubscriptionIds().contains(subscriptionId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(apiKeyNotFoundError(apiKeyId)).build();
        }
        apiKeyEntity.setExpireAt(updateApiKey.getExpireAt() != null ? java.util.Date.from(updateApiKey.getExpireAt().toInstant()) : null);
        return Response.ok(subscriptionMapper.mapToApiKey(apiKeyService.update(executionContext, apiKeyEntity))).build();
    }

    @POST
    @Path("/api-keys/{apiKeyId}/_revoke")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response revokeApiProductSubscriptionApiKey(@PathParam("apiKeyId") String apiKeyId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final var user = getAuthenticatedUserDetails();
        var result = revokeApiSubscriptionApiKeyUseCase.execute(
            new RevokeApiSubscriptionApiKeyUseCase.Input(
                apiKeyId,
                apiProductId,
                SubscriptionReferenceType.API_PRODUCT.name(),
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
        return Response.ok(subscriptionMapper.mapToApiKey(result.apiKey())).build();
    }

    @POST
    @Path("/api-keys/{apiKeyId}/_reactivate")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response reactivateApiProductSubscriptionApiKey(@PathParam("apiKeyId") String apiKeyId) {
        checkSubscription();
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        io.gravitee.rest.api.model.ApiKeyEntity apiKeyEntity = apiKeyService.findById(executionContext, apiKeyId);
        if (!apiKeyEntity.getSubscriptionIds().contains(subscriptionId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(apiKeyNotFoundError(apiKeyId)).build();
        }
        checkApplicationDoesntUseSharedApiKey(executionContext, apiKeyEntity.getApplication().getId());
        apiKeyEntity = apiKeyService.reactivate(executionContext, apiKeyEntity);
        return Response.ok(subscriptionMapper.mapToApiKey(apiKeyEntity)).build();
    }

    private io.gravitee.rest.api.model.SubscriptionEntity checkSubscription() {
        io.gravitee.rest.api.model.SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);
        if (!isApiProductSubscription(subscriptionEntity)) {
            throw new SubscriptionNotFoundException(subscriptionId);
        }
        return subscriptionEntity;
    }

    private boolean isApiProductSubscription(io.gravitee.rest.api.model.SubscriptionEntity subscriptionEntity) {
        return (
            apiProductId.equals(subscriptionEntity.getReferenceId()) &&
            SubscriptionReferenceType.API_PRODUCT.name().equals(subscriptionEntity.getReferenceType())
        );
    }

    private Error apiKeyNotFoundError(String apiKeyId) {
        return new Error()
            .httpStatus(Response.Status.NOT_FOUND.getStatusCode())
            .message("No API Key can be found.")
            .putParametersItem("apiKeyId", apiKeyId)
            .technicalCode("apiKey.notFound");
    }

    private Error subscriptionInvalid(String message) {
        return new Error().httpStatus(Response.Status.BAD_REQUEST.getStatusCode()).message(message).technicalCode("subscription.invalid");
    }

    private void checkApplicationDoesntUseSharedApiKey(ExecutionContext executionContext, String applicationId) {
        final io.gravitee.rest.api.model.ApplicationEntity applicationEntity = applicationService.findById(executionContext, applicationId);
        if (applicationEntity.hasApiKeySharedMode()) {
            throw new InvalidApplicationApiKeyModeException(
                String.format(
                    "Invalid operation for API Key mode [%s] of application [%s].",
                    applicationEntity.getApiKeyMode(),
                    applicationEntity.getId()
                )
            );
        }
    }

    private Error subscriptionNotFoundError(String subscriptionId) {
        return new Error()
            .httpStatus(Response.Status.NOT_FOUND.getStatusCode())
            .message("Subscription [" + subscriptionId + "] cannot be found.")
            .putParametersItem("subscription", subscriptionId)
            .technicalCode("subscription.notFound");
    }
}
