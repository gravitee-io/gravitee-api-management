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

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDStatus;
import io.gravitee.apim.core.subscription.use_case.ImportSubscriptionSpecUseCase;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.apim.rest.api.automation.mapper.SubscriptionMapper;
import io.gravitee.apim.rest.api.automation.model.SubscriptionSpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Set;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsResource extends AbstractResource {

    @Inject
    private ImportSubscriptionSpecUseCase importSubscriptionSpecUseCase;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private PermissionService permissionService;

    @Inject
    private Validator validator;

    @Path("/{hrid}")
    public SubscriptionResource getSubscriptionResource() {
        return resourceContext.getResource(SubscriptionResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOrUpdate(SubscriptionSpec spec, @QueryParam("legacy") boolean legacy) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        AuditInfo auditInfo = AuditInfo
            .builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor
                    .builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();

        if (spec == null || StringUtils.isEmpty(spec.getApiHrid())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String apiId = legacy ? spec.getApiHrid() : IdBuilder.builder(auditInfo, spec.getApiHrid()).buildId();
        if (!permissionService.hasPermission(executionContext, RolePermission.API_SUBSCRIPTION, apiId)) {
            throw new ForbiddenAccessException();
        }

        Set<ConstraintViolation<SubscriptionSpec>> violations = validator.validate(spec);
        if (!violations.isEmpty()) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).toList())
                .build();
        }

        SubscriptionCRDSpec subscriptionCRDSpec = new SubscriptionCRDSpec(
            legacy ? spec.getHrid() : IdBuilder.builder(auditInfo, spec.getHrid()).buildId(),
            legacy ? spec.getApplicationHrid() : IdBuilder.builder(auditInfo, spec.getApplicationHrid()).buildId(),
            legacy ? spec.getApiHrid() : IdBuilder.builder(auditInfo, spec.getApiHrid()).buildId(),
            legacy ? spec.getPlanHrid() : IdBuilder.builder(auditInfo, spec.getApiHrid()).withExtraId(spec.getPlanHrid()).buildId(),
            spec.getEndingAt() != null ? spec.getEndingAt().toZonedDateTime() : null
        );

        SubscriptionCRDStatus status = importSubscriptionSpecUseCase
            .execute(new ImportSubscriptionSpecUseCase.Input(auditInfo, subscriptionCRDSpec))
            .status();

        return Response.ok(SubscriptionMapper.INSTANCE.subscriptionSpecAndStatusToSubscriptionState(spec, status)).build();
    }
}
