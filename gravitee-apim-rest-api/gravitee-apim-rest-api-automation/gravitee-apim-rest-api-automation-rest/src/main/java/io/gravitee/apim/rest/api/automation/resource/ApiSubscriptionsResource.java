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

import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.model.crd.ApiKeyCRDSpec;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDStatus;
import io.gravitee.apim.core.subscription.use_case.ImportSubscriptionCRDUseCase;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.helpers.ApiIdResolver;
import io.gravitee.apim.rest.api.automation.mapper.SubscriptionMapper;
import io.gravitee.apim.rest.api.automation.model.ApiKeySpec;
import io.gravitee.apim.rest.api.automation.model.SubscriptionSpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
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
import lombok.CustomLog;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ApiSubscriptionsResource extends AbstractResource {

    @Inject
    private ImportSubscriptionCRDUseCase importSubscriptionCRDUseCase;

    @Inject
    private ApiSearchService apiSearchService;

    @Inject
    private PlanCrudService planCrudService;

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

        // Legacy mode means 'Hrid' fields contains GUID from GKO Status
        // that were preexisting in the kube cluster
        var apiId = ApiIdResolver.authorizedApiId(requestContext);
        var planId = hridContainsApiUUID
            ? spec.getPlanHrid()
            : HRIDToUUID.plan().context(auditInfo).api(apiHrid).plan(spec.getPlanHrid()).id();
        mustBeAPlanOfTheApiInTheEnvironment(executionContext, apiHrid, apiId, spec.getPlanHrid(), planId);

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
            .planId(planId)
            .endingAt(spec.getEndingAt() != null ? spec.getEndingAt().toZonedDateTime() : null)
            .metadata(spec.getMetadata())
            .apiKeys(toApiKeyCRDSpecs(spec.getApiKeys()))
            .consumerConfiguration(SubscriptionMapper.INSTANCE.toConsumerConfiguration(spec.getConsumerConfiguration()))
            .build();

        SubscriptionCRDStatus status = importSubscriptionCRDUseCase
            .execute(new ImportSubscriptionCRDUseCase.Input(auditInfo, subscriptionCRDSpec))
            .status();

        return Response.ok(SubscriptionMapper.INSTANCE.subscriptionSpecAndStatusToSubscriptionState(apiHrid, spec, status)).build();
    }

    private void mustBeAPlanOfTheApiInTheEnvironment(
        ExecutionContext executionContext,
        String apiHrid,
        String apiId,
        String planHrid,
        String planId
    ) {
        try {
            apiSearchService.findRepositoryApiById(executionContext, apiId);
        } catch (ApiNotFoundException e) {
            log.debug("API not found for hrid: {}, operation: createOrUpdate subscription", apiHrid);
            throw new HRIDNotFoundException(apiHrid);
        }
        planCrudService
            .findByPlanIdAndReferenceIdAndReferenceType(planId, apiId, GenericPlanEntity.ReferenceType.API.name())
            .orElseThrow(() -> new HRIDNotFoundException(planHrid));
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
