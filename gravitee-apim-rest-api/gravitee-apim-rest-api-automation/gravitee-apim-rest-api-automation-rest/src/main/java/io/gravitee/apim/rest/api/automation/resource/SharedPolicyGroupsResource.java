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
import io.gravitee.apim.core.shared_policy_group.domain_service.ValidateSharedPolicyGroupCRDDomainService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRDStatus;
import io.gravitee.apim.core.shared_policy_group.use_case.ImportSharedPolicyGroupCRDCRDUseCase;
import io.gravitee.apim.rest.api.automation.mapper.SharedPolicyGroupMapper;
import io.gravitee.apim.rest.api.automation.model.LegacySharedPolicyGroupSpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SharedPolicyGroupsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ImportSharedPolicyGroupCRDCRDUseCase importSharedPolicyGroupCRDCRDUseCase;

    @Inject
    private ValidateSharedPolicyGroupCRDDomainService validateSharedPolicyGroupCRDDomainService;

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(
        @Valid @NotNull LegacySharedPolicyGroupSpec spec,
        @QueryParam("dryRun") boolean dryRun,
        @QueryParam("legacyID") boolean legacyID
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        var audit = AuditInfo.builder()
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

        var sharedPolicyGroupCRD = SharedPolicyGroupMapper.INSTANCE.map(spec);

        if (legacyID) {
            // As Automation API does not have any ID field,
            // GKO upgraded resources send the HRID as ID
            sharedPolicyGroupCRD.setSharedPolicyGroupId(spec.getHrid());
            // HRID is removed as it does not make sense here, besides
            // it avoids confusion in the database
            sharedPolicyGroupCRD.setHrid(null);
        }

        if (dryRun) {
            var statusBuilder = SharedPolicyGroupCRDStatus.builder();
            validateSharedPolicyGroupCRDDomainService
                .validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(audit, sharedPolicyGroupCRD))
                .peek(
                    sanitized ->
                        statusBuilder
                            .id(sanitized.crd().getSharedPolicyGroupId())
                            .crossId(sanitized.crd().getCrossId())
                            .organizationId(audit.organizationId())
                            .environmentId(audit.environmentId()),
                    errors -> statusBuilder.errors(SharedPolicyGroupCRDStatus.Errors.fromErrorList(errors))
                );
            return Response.ok(
                SharedPolicyGroupMapper.INSTANCE.withStatusInfos(SharedPolicyGroupMapper.INSTANCE.toState(spec), statusBuilder.build())
            ).build();
        }

        var output = importSharedPolicyGroupCRDCRDUseCase.execute(
            new ImportSharedPolicyGroupCRDCRDUseCase.Input(audit, sharedPolicyGroupCRD)
        );

        return Response.ok(
            SharedPolicyGroupMapper.INSTANCE.withStatusInfos(SharedPolicyGroupMapper.INSTANCE.toState(spec), output.status())
        ).build();
    }

    @Path("/{hrid}")
    public SharedPolicyGroupResource getSharedPolicyGroupResource() {
        return resourceContext.getResource(SharedPolicyGroupResource.class);
    }
}
