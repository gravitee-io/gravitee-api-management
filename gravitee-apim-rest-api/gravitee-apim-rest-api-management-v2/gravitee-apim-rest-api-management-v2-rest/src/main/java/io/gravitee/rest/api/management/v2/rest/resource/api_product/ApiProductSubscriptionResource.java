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

import io.gravitee.apim.core.subscription.use_case.api_product.AcceptApiProductSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.api_product.CloseApiProductSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.api_product.GetApiProductSubscriptionsUseCase;
import io.gravitee.apim.core.subscription.use_case.api_product.RejectApiProductSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.api_product.UpdateApiProductSubscriptionUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * API Product Subscription REST resource (singular).
 * Handles operations on a specific subscription.
 *
 * @author GraviteeSource Team
 */
@Slf4j
public class ApiProductSubscriptionResource extends AbstractResource {

    private final SubscriptionMapper subscriptionMapper = SubscriptionMapper.INSTANCE;

    @Inject
    private GetApiProductSubscriptionsUseCase getApiProductSubscriptionsUseCase;

    @Inject
    private UpdateApiProductSubscriptionUseCase updateApiProductSubscriptionUseCase;

    @Inject
    private AcceptApiProductSubscriptionUseCase acceptApiProductSubscriptionUseCase;

    @Inject
    private RejectApiProductSubscriptionUseCase rejectApiProductSubscriptionUseCase;

    @Inject
    private CloseApiProductSubscriptionUseCase closeApiProductSubscriptionUseCase;

    @PathParam("apiProductId")
    private String apiProductId;

    @PathParam("subscriptionId")
    private String subscriptionId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.READ }) })
    public Response getApiProductSubscription() {
        log.debug("Getting subscription {} for API Product {}", subscriptionId, apiProductId);
        var output = getApiProductSubscriptionsUseCase.execute(GetApiProductSubscriptionsUseCase.Input.of(apiProductId, subscriptionId));
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

        var input = UpdateApiProductSubscriptionUseCase.Input.builder()
            .apiProductId(apiProductId)
            .subscriptionId(subscriptionId)
            .configuration(configuration)
            .metadata(updateSubscription.getMetadata())
            .startingAt(updateSubscription.getStartingAt() != null ? updateSubscription.getStartingAt().toZonedDateTime() : null)
            .endingAt(updateSubscription.getEndingAt() != null ? updateSubscription.getEndingAt().toZonedDateTime() : null)
            .auditInfo(getAuditInfo())
            .build();

        var output = updateApiProductSubscriptionUseCase.execute(input);
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
        var input = AcceptApiProductSubscriptionUseCase.Input.builder()
            .apiProductId(apiProductId)
            .subscriptionId(subscriptionId)
            .startingAt(acceptSubscription.getStartingAt() != null ? acceptSubscription.getStartingAt().toZonedDateTime() : null)
            .endingAt(acceptSubscription.getEndingAt() != null ? acceptSubscription.getEndingAt().toZonedDateTime() : null)
            .reasonMessage(acceptSubscription.getReason())
            .customKey(acceptSubscription.getCustomApiKey())
            .auditInfo(getAuditInfo())
            .build();

        var output = acceptApiProductSubscriptionUseCase.execute(input);
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
        var input = RejectApiProductSubscriptionUseCase.Input.builder()
            .apiProductId(apiProductId)
            .subscriptionId(subscriptionId)
            .reasonMessage(rejectSubscription.getReason())
            .auditInfo(getAuditInfo())
            .build();

        var output = rejectApiProductSubscriptionUseCase.execute(input);
        log.debug("Rejected subscription {} for API Product {}", subscriptionId, apiProductId);
        return Response.ok(subscriptionMapper.map(output.subscription())).build();
    }

    @POST
    @Path("/_close")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response closeApiProductSubscription() {
        log.debug("Closing subscription {} for API Product {}", subscriptionId, apiProductId);
        var input = CloseApiProductSubscriptionUseCase.Input.builder()
            .apiProductId(apiProductId)
            .subscriptionId(subscriptionId)
            .auditInfo(getAuditInfo())
            .build();

        var output = closeApiProductSubscriptionUseCase.execute(input);
        log.debug("Closed subscription {} for API Product {}", subscriptionId, apiProductId);
        return Response.ok(subscriptionMapper.map(output.subscription())).build();
    }

    private Error subscriptionNotFoundError(String subscriptionId) {
        return new Error()
            .httpStatus(Response.Status.NOT_FOUND.getStatusCode())
            .message("Subscription [" + subscriptionId + "] cannot be found.")
            .putParametersItem("subscription", subscriptionId)
            .technicalCode("subscription.notFound");
    }
}
