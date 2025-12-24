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

import io.gravitee.apim.core.application.model.crd.ApplicationCRDSpec;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDStatus;
import io.gravitee.apim.core.application.use_case.ImportApplicationCRDUseCase;
import io.gravitee.apim.core.application.use_case.ValidateApplicationCRDUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.automation.mapper.ApplicationMapper;
import io.gravitee.apim.rest.api.automation.model.ApplicationSpec;
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

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationsResource extends AbstractResource {

    @Inject
    private ImportApplicationCRDUseCase importApplicationCRDUseCase;

    @Inject
    private ValidateApplicationCRDUseCase validateApplicationCRDUseCase;

    @Context
    private ResourceContext resourceContext;

    @Path("/{hrid}")
    public ApplicationResource getApplicationResource() {
        return resourceContext.getResource(ApplicationResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_APPLICATION, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(
        @Valid @NotNull ApplicationSpec spec,
        @QueryParam("dryRun") boolean dryRun,
        @QueryParam("legacyID") boolean legacyID
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

        ApplicationCRDSpec applicationCRDSpec = io.gravitee.rest.api.management.v2.rest.mapper.ApplicationMapper.INSTANCE.map(
            ApplicationMapper.INSTANCE.applicationSpecToApplicationCRDSpec(spec)
        );

        if (legacyID) {
            // As Automation API does not have any ID field,
            // GKO upgraded resources send the HRID as ID
            applicationCRDSpec.setId(applicationCRDSpec.getHrid());
            // HRID is removed as it does not make sense here, besides
            // it avoids confusion in the database
            applicationCRDSpec.setHrid(null);
        }

        if (dryRun) {
            ApplicationCRDStatus status = validateApplicationCRDUseCase
                .execute(new ImportApplicationCRDUseCase.Input(auditInfo, applicationCRDSpec))
                .status();
            return Response.ok(ApplicationMapper.INSTANCE.applicationSpecAndStatusToApplicationState(spec, status)).build();
        }

        ApplicationCRDStatus status = importApplicationCRDUseCase
            .execute(new ImportApplicationCRDUseCase.Input(auditInfo, applicationCRDSpec))
            .status();

        return Response.ok(ApplicationMapper.INSTANCE.applicationSpecAndStatusToApplicationState(spec, status)).build();
    }
}
