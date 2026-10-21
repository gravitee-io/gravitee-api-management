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
package io.gravitee.apim.rest.api.automation.resource;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.model.crd.ApiKeyCRDSpec;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDStatus;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.apim.core.subscription.use_case.ImportSubscriptionCRDUseCase;
import io.gravitee.apim.rest.api.automation.helpers.ApiIdResolver;
import io.gravitee.apim.rest.api.automation.mapper.SubscriptionMapper;
import io.gravitee.apim.rest.api.automation.model.ApiKeySpec;
import io.gravitee.apim.rest.api.automation.model.SubscriptionSpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSubscriptionsResource extends AbstractResource {

    @Inject
    private ImportSubscriptionCRDUseCase importSubscriptionCRDUseCase;

    @Inject
    private SubscriptionQueryService subscriptionQueryService;

    @Context
    private ResourceContext resourceContext;

    @Path("{hrid}")
    public ApiSubscriptionResource getSubscriptionResource() {
        return resourceContext.getResource(ApiSubscriptionResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(
        @Valid @NotNull SubscriptionSpec spec,
        @PathParam("apiHrid") String apiHrid,
        @QueryParam("hridContainsUUID") boolean hridContainsUUID,
        @QueryParam("hridContainsApiUUID") boolean hridContainsApiUUID,
        @QueryParam("hridContainsAppUUID") boolean hridContainsAppUUID,
        @Context ContainerRequestContext requestContext
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        var auditInfo = buildAuditInfo(executionContext, userDetails);

        var apiId = ApiIdResolver.authorizedApiId(requestContext);

        // Legacy mode means 'Hrid' fields contains GUID from GKO Status
        // that were preexisting in the kube cluster
        SubscriptionCRDSpec subscriptionCRDSpec = SubscriptionCRDSpec.builder()
            .id(
                hridContainsUUID
                    ? spec.getHrid()
                    : HRIDToUUID.subscription().context(auditInfo).api(apiHrid).subscription(spec.getHrid()).id()
            )
            .applicationId(
                hridContainsAppUUID
                    ? spec.getApplicationHrid()
                    : HRIDToUUID.application().context(auditInfo).hrid(spec.getApplicationHrid()).id()
            )
            .referenceId(apiId)
            .referenceType(SubscriptionReferenceType.API)
            .planId(
                hridContainsApiUUID ? spec.getPlanHrid() : HRIDToUUID.plan().context(auditInfo).api(apiHrid).plan(spec.getPlanHrid()).id()
            )
            .endingAt(spec.getEndingAt() != null ? spec.getEndingAt().toZonedDateTime() : null)
            .metadata(spec.getMetadata())
            .apiKeys(toApiKeyCRDSpecs(spec.getApiKeys()))
            .consumerConfiguration(SubscriptionMapper.INSTANCE.toConsumerConfiguration(spec.getConsumerConfiguration()))
            .build();

        var existing = subscriptionQueryService.findByIdAndReferenceIdAndReferenceType(
            subscriptionCRDSpec.getId(),
            subscriptionCRDSpec.getReferenceId(),
            SubscriptionReferenceType.API
        );
        requireCreateOrUpdateOn(RolePermission.API_SUBSCRIPTION, subscriptionCRDSpec.getReferenceId(), existing.isPresent());
        // Creating or restoring a subscription gives the application access, so the caller must be allowed to subscribe it
        if (
            existing.map(subscription -> subscription.isClosed()).orElse(true) &&
            !hasPermission(executionContext, RolePermission.APPLICATION_SUBSCRIPTION, subscriptionCRDSpec.getApplicationId(), CREATE)
        ) {
            throw new ForbiddenAccessException();
        }

        SubscriptionCRDStatus status = importSubscriptionCRDUseCase
            .execute(new ImportSubscriptionCRDUseCase.Input(auditInfo, subscriptionCRDSpec.getReferenceId(), subscriptionCRDSpec))
            .status();

        return Response.ok(SubscriptionMapper.INSTANCE.subscriptionSpecAndStatusToSubscriptionState(apiHrid, spec, status)).build();
    }

    private static List<ApiKeyCRDSpec> toApiKeyCRDSpecs(List<ApiKeySpec> apiKeys) {
        if (apiKeys == null || apiKeys.isEmpty()) {
            return null;
        }
        return apiKeys
            .stream()
            .map(k ->
                ApiKeyCRDSpec.builder().key(k.getKey()).expireAt(k.getExpireAt() != null ? k.getExpireAt().toZonedDateTime() : null).build()
            )
            .toList();
    }
}
