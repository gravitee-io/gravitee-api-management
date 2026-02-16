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

import io.gravitee.apim.core.subscription.model.SubscriptionConfiguration;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.use_case.CreateSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.GetSubscriptionsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.validator.CustomApiKey;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;

/**
 * API Product Subscriptions REST resource (collection).
 * Handles collection operations: list all subscriptions and create new subscriptions.
 * Individual subscription operations are handled by ApiProductSubscriptionResource.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class ApiProductSubscriptionsResource extends AbstractResource {

    private final SubscriptionMapper subscriptionMapper = SubscriptionMapper.INSTANCE;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GetSubscriptionsUseCase getSubscriptionsUseCase;

    @Inject
    private CreateSubscriptionUseCase createSubscriptionUseCase;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private ParameterService parameterService;

    @PathParam("apiProductId")
    private String apiProductId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.READ }) })
    public Response getApiProductSubscriptions(@BeanParam @Valid PaginationParam paginationParam) {
        log.debug("Getting subscriptions for API Product {}", apiProductId);
        var output = getSubscriptionsUseCase.execute(GetSubscriptionsUseCase.Input.of(apiProductId, SubscriptionReferenceType.API_PRODUCT));
        List<io.gravitee.apim.core.subscription.model.SubscriptionEntity> subscriptions = output.subscriptions();
        if (subscriptions == null) {
            subscriptions = List.of();
        }

        List<Subscription> subscriptionList = subscriptions.stream().map(subscriptionMapper::map).map(this::filterSensitiveData).toList();

        // TODO: Pagination is currently in-memory; consider DB-level pagination in GetSubscriptionsUseCase for scalability
        int totalCount = subscriptionList.size();
        int fromIndex = Math.min((paginationParam.getPage() - 1) * paginationParam.getPerPage(), totalCount);
        int toIndex = Math.min(fromIndex + paginationParam.getPerPage(), totalCount);
        List<Subscription> pageData = fromIndex < totalCount ? subscriptionList.subList(fromIndex, toIndex) : List.of();

        log.debug("Found {} subscriptions for API Product {}", totalCount, apiProductId);
        return Response.ok(
            new SubscriptionsResponse()
                .data(new java.util.ArrayList<>(pageData))
                .pagination(PaginationInfo.computePaginationInfo((long) totalCount, pageData.size(), paginationParam))
                .links(computePaginationLinks(totalCount, paginationParam))
        ).build();
    }

    @GET
    @Path("_canCreate")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.READ }) })
    public Response canCreateApiProductSubscription(
        @QueryParam("key") @CustomApiKey @NotNull String key,
        @QueryParam("application") @NotNull String application
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        boolean canCreate = apiKeyService.canCreate(
            executionContext,
            key,
            apiProductId,
            SubscriptionReferenceType.API_PRODUCT.name(),
            application
        );
        return Response.ok(canCreate).build();
    }

    @POST
    @Path("_verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.CREATE }) })
    public Response verifyCreateApiProductSubscription(@Valid @NotNull VerifySubscription verifySubscription) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        boolean canCreate = apiKeyService.canCreate(
            executionContext,
            verifySubscription.getApiKey(),
            apiProductId,
            SubscriptionReferenceType.API_PRODUCT.name(),
            verifySubscription.getApplicationId()
        );
        return Response.ok(new VerifySubscriptionResponse().ok(canCreate)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.CREATE }) })
    public Response createApiProductSubscription(@Valid @NotNull CreateSubscription createSubscription) {
        log.debug(
            "Creating subscription for API Product {} with plan {} and application {}",
            apiProductId,
            createSubscription.getPlanId(),
            createSubscription.getApplicationId()
        );
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        if (
            StringUtils.isNotEmpty(createSubscription.getCustomApiKey()) &&
            !parameterService.findAsBoolean(executionContext, Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED, ParameterReferenceType.ENVIRONMENT)
        ) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(subscriptionInvalid("You are not allowed to provide a custom API Key"))
                .build();
        }

        SubscriptionConfiguration coreConfig = null;
        if (createSubscription.getConsumerConfiguration() != null) {
            SubscriptionConfigurationEntity configEntity = subscriptionMapper.mapConsumerConfigurationToEntity(
                createSubscription.getConsumerConfiguration()
            );
            coreConfig = SubscriptionConfiguration.builder()
                .entrypointId(configEntity.getEntrypointId())
                .channel(configEntity.getChannel())
                .entrypointConfiguration(configEntity.getEntrypointConfiguration())
                .build();
        }

        CreateSubscriptionUseCase.Input input = CreateSubscriptionUseCase.Input.builder()
            .referenceId(apiProductId)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .planId(createSubscription.getPlanId())
            .applicationId(createSubscription.getApplicationId())
            .requestMessage(null)
            .customApiKey(createSubscription.getCustomApiKey())
            .configuration(coreConfig)
            .metadata(createSubscription.getMetadata())
            .generalConditionsAccepted(null)
            .generalConditionsContentRevision(null)
            .auditInfo(getAuditInfo())
            .build();

        CreateSubscriptionUseCase.Output output = createSubscriptionUseCase.execute(input);
        Subscription subscription = subscriptionMapper.map(output.subscription());

        log.debug("Created subscription {} for API Product {}", output.subscription().getId(), apiProductId);
        return Response.created(this.getLocationHeader(output.subscription().getId())).entity(subscription).build();
    }

    @Path("/{subscriptionId}")
    public ApiProductSubscriptionResource getApiProductSubscriptionResource() {
        return resourceContext.getResource(ApiProductSubscriptionResource.class);
    }

    private Subscription filterSensitiveData(Subscription subscription) {
        // Similar to plan filtering: check if user has both API_PRODUCT_DEFINITION and API_PRODUCT_SUBSCRIPTION permissions
        if (
            hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_PRODUCT_DEFINITION,
                apiProductId,
                RolePermissionAction.READ
            ) &&
            hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_PRODUCT_SUBSCRIPTION,
                apiProductId,
                RolePermissionAction.READ
            )
        ) {
            // Return complete information if user has permission.
            return subscription;
        }

        // Return filtered subscription with limited information (similar to plan filtering)
        // Subscriptions don't have sensitive data like flows/security definitions, so we return basic fields
        Subscription filtered = new Subscription();
        filtered.setId(subscription.getId());
        filtered.setStatus(subscription.getStatus());
        if (subscription.getPlan() != null) {
            filtered.setPlan(subscription.getPlan());
        }
        if (subscription.getApplication() != null) {
            filtered.setApplication(subscription.getApplication());
        }
        filtered.setCreatedAt(subscription.getCreatedAt());
        filtered.setUpdatedAt(subscription.getUpdatedAt());
        filtered.setStartingAt(subscription.getStartingAt());
        filtered.setEndingAt(subscription.getEndingAt());
        filtered.setConsumerStatus(subscription.getConsumerStatus());
        filtered.setConsumerPausedAt(subscription.getConsumerPausedAt());

        return filtered;
    }

    private Error subscriptionInvalid(String message) {
        return new Error().httpStatus(Response.Status.BAD_REQUEST.getStatusCode()).message(message).technicalCode("subscription.invalid");
    }
}
