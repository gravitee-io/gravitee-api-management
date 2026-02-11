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

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDStatus;
import io.gravitee.apim.core.subscription.use_case.ImportSubscriptionSpecUseCase;
import io.gravitee.apim.rest.api.automation.mapper.SubscriptionMapper;
import io.gravitee.apim.rest.api.automation.model.SubscriptionSpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSubscriptionsResource extends AbstractResource {

    @Inject
    private ImportSubscriptionSpecUseCase importSubscriptionSpecUseCase;

    @Context
    private ResourceContext resourceContext;

    @Path("{hrid}")
    public ApiSubscriptionResource getSubscriptionResource() {
        return resourceContext.getResource(ApiSubscriptionResource.class);
    }

    /**
     * Create or update a subscription from a spec.
     *
     * @param legacy when true, path/spec HRIDs are used as-is as entity IDs; when false, IDs are built from context (org+env+hrid).
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(
        @Valid @NotNull SubscriptionSpec spec,
        @PathParam("apiHrid") String apiHrid,
        @QueryParam("legacy") boolean legacy
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        AuditInfo auditInfo = AuditInfo.builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor.builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();

        SubscriptionCRDSpec subscriptionCRDSpec = SubscriptionCRDSpec.builder()
            .id(legacy ? spec.getHrid() : IdBuilder.builder(auditInfo, apiHrid).withExtraId(spec.getHrid()).buildId())
            .applicationId(legacy ? spec.getApplicationHrid() : IdBuilder.builder(auditInfo, spec.getApplicationHrid()).buildId())
            .referenceId(legacy ? apiHrid : IdBuilder.builder(auditInfo, apiHrid).buildId())
            .referenceType(SubscriptionReferenceType.API)
            .planId(legacy ? spec.getPlanHrid() : IdBuilder.builder(auditInfo, apiHrid).withExtraId(spec.getPlanHrid()).buildId())
            .endingAt(spec.getEndingAt() != null ? spec.getEndingAt().toZonedDateTime() : null)
            .build();

        SubscriptionCRDStatus status = importSubscriptionSpecUseCase
            .execute(new ImportSubscriptionSpecUseCase.Input(auditInfo, subscriptionCRDSpec))
            .status();

        return Response.ok(SubscriptionMapper.INSTANCE.subscriptionSpecAndStatusToSubscriptionState(apiHrid, spec, status)).build();
    }
}
