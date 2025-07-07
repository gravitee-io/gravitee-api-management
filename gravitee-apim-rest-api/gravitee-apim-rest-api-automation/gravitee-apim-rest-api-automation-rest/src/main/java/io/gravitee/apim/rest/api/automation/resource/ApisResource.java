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

import io.gravitee.apim.core.api.domain_service.ValidateApiCRDDomainService;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.api.use_case.ImportApiCRDUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupPolicyPlugin;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.rest.api.automation.mapper.ApiMapper;
import io.gravitee.apim.rest.api.automation.model.ApiV4Spec;
import io.gravitee.apim.rest.api.automation.model.FlowV4;
import io.gravitee.apim.rest.api.automation.model.LegacyAPIV4Spec;
import io.gravitee.apim.rest.api.automation.model.StepV4;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Objects;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisResource extends AbstractResource {

    public static final String SHARED_POLICY_GROUP_ID_FIELD = "sharedPolicyGroupId";
    public static final String HRID_FIELD = "hrid";

    @Inject
    private ImportApiCRDUseCase importApiCRDUseCase;

    @Inject
    private ValidateApiCRDDomainService validateApiCRDDomainService;

    @Context
    private ResourceContext resourceContext;

    @Path("/{hrid}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP, acls = { RolePermissionAction.CREATE }) })
    public Response createOrUpdate(
        @Valid @NotNull LegacyAPIV4Spec spec,
        @QueryParam("dryRun") boolean dryRun,
        @QueryParam("legacy") boolean legacy
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        AuditInfo audit = AuditInfo
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

        ApiCRDSpec apiCRDSpec = io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper.INSTANCE.map(
            ApiMapper.INSTANCE.apiV4SpecToApiCRDSpec(spec)
        );

        // Just for backward compatibility with old code
        if (legacy) {
            apiCRDSpec.setId(spec.getHrid());
        }

        if (dryRun) {
            var statusBuilder = ApiCRDStatus.builder();
            validateApiCRDDomainService
                .validateAndSanitize(new ValidateApiCRDDomainService.Input(audit, apiCRDSpec))
                .peek(
                    sanitized ->
                        statusBuilder
                            .id(sanitized.spec().getId())
                            .crossId(sanitized.spec().getCrossId())
                            .organizationId(audit.organizationId())
                            .environmentId(audit.environmentId()),
                    errors -> statusBuilder.errors(ApiCRDStatus.Errors.fromErrorList(errors))
                );
            return Response.ok(ApiMapper.INSTANCE.apiV4SpecAndStatusToApiV4State(spec, statusBuilder.build())).build();
        }

        mapSharedPolicyGroupHrid(spec, audit);

        ApiCRDStatus apiCRDStatus = importApiCRDUseCase.execute(new ImportApiCRDUseCase.Input(audit, apiCRDSpec)).status();

        return Response.ok(ApiMapper.INSTANCE.apiV4SpecAndStatusToApiV4State(spec, apiCRDStatus)).build();
    }

    private void mapSharedPolicyGroupHrid(ApiV4Spec spec, AuditInfo audit) {
        CollectionUtils.stream(spec.getFlows()).forEach(f -> mapSharedPolicyGroupHrid(f, audit));
        if (spec.getPlans() != null) {
            CollectionUtils
                .stream(spec.getPlans())
                .flatMap(p -> CollectionUtils.stream(p.getFlows()))
                .forEach(f -> mapSharedPolicyGroupHrid(f, audit));
        }
    }

    private static void mapSharedPolicyGroupHrid(@Valid FlowV4 flowV4, AuditInfo audit) {
        CollectionUtils.stream(flowV4.getRequest()).forEach(s -> mapSharedPolicyGroupHrid(s, audit));
        CollectionUtils.stream(flowV4.getResponse()).forEach(s -> mapSharedPolicyGroupHrid(s, audit));
        CollectionUtils.stream(flowV4.getSubscribe()).forEach(s -> mapSharedPolicyGroupHrid(s, audit));
        CollectionUtils.stream(flowV4.getPublish()).forEach(s -> mapSharedPolicyGroupHrid(s, audit));
    }

    private static void mapSharedPolicyGroupHrid(StepV4 stepV4, AuditInfo auditInfo) {
        if (Objects.equals(stepV4.getPolicy(), SharedPolicyGroupPolicyPlugin.SHARED_POLICY_GROUP_POLICY_ID)) {
            Object configuration = stepV4.getConfiguration();
            if (configuration instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> struct = (Map<String, Object>) rawMap;
                String hrid = (String) struct.get(HRID_FIELD);
                if (hrid != null) {
                    struct.remove(HRID_FIELD);
                    struct.put(SHARED_POLICY_GROUP_ID_FIELD, IdBuilder.builder(auditInfo, hrid).buildCrossId());
                }
            }
        }
    }
}
