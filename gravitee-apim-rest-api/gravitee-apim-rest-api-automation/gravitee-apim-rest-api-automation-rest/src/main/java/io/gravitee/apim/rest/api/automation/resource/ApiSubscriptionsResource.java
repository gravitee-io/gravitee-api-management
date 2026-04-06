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
import io.gravitee.rest.api.service.common.HRIDToUUID;
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

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(
        @Valid @NotNull SubscriptionSpec spec,
        @PathParam("apiHrid") String apiHrid,
        @QueryParam("legacyID") boolean legacyID,
        @QueryParam("legacyApiID") boolean legacyApiID,
        @QueryParam("legacyAppID") boolean legacyAppID
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

        // Legacy mode means 'Hrid' fields contains GUID from GKO Status
        // that were preexisting in the kube cluster
        SubscriptionCRDSpec subscriptionCRDSpec = SubscriptionCRDSpec.builder()
            .id(legacyID ? spec.getHrid() : HRIDToUUID.subscription().context(auditInfo).api(apiHrid).subscription(spec.getHrid()).id())
            .applicationId(
                legacyAppID ? spec.getApplicationHrid() : HRIDToUUID.application().context(auditInfo).hrid(spec.getApplicationHrid()).id()
            )
            .referenceId(legacyApiID ? apiHrid : HRIDToUUID.api().context(auditInfo).hrid(apiHrid).id())
            .referenceType(SubscriptionReferenceType.API)
            .planId(legacyApiID ? spec.getPlanHrid() : HRIDToUUID.plan().context(auditInfo).api(apiHrid).plan(spec.getPlanHrid()).id())
            .endingAt(spec.getEndingAt() != null ? spec.getEndingAt().toZonedDateTime() : null)
            .metadata(spec.getMetadata())
            .build();

        SubscriptionCRDStatus status = importSubscriptionSpecUseCase
            .execute(new ImportSubscriptionSpecUseCase.Input(auditInfo, subscriptionCRDSpec))
            .status();

        return Response.ok(SubscriptionMapper.INSTANCE.subscriptionSpecAndStatusToSubscriptionState(apiHrid, spec, status)).build();
    }
}
