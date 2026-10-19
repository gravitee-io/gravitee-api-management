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

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDSpec;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDStatus;
import io.gravitee.apim.core.application.use_case.ImportApplicationCRDUseCase;
import io.gravitee.apim.core.application.use_case.ValidateApplicationCRDUseCase;
import io.gravitee.apim.rest.api.automation.helpers.CrdIdHelper;
import io.gravitee.apim.rest.api.automation.mapper.ApplicationMapper;
import io.gravitee.apim.rest.api.automation.model.ApplicationSpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
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
import java.util.Optional;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationsResource extends AbstractResource {

    @Inject
    private ImportApplicationCRDUseCase importApplicationCRDUseCase;

    @Inject
    private ValidateApplicationCRDUseCase validateApplicationCRDUseCase;

    @Inject
    private ApplicationCrudService applicationCrudService;

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
        @QueryParam("hridContainsUUID") boolean hridContainsUUID,
        @QueryParam("ignoreGroups") boolean ignoreGroups
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        var auditInfo = buildAuditInfo(executionContext, userDetails);

        ApplicationCRDSpec applicationCRDSpec = io.gravitee.rest.api.management.v2.rest.mapper.ApplicationMapper.INSTANCE.map(
            ApplicationMapper.INSTANCE.applicationSpecToApplicationCRDSpec(spec)
        );

        if (hridContainsUUID) {
            // As Automation API does not have any ID field,
            // GKO upgraded resources send the HRID as ID
            applicationCRDSpec.setId(applicationCRDSpec.getHrid());
            // HRID is removed as it does not make sense here, besides
            // it avoids confusion in the database
            applicationCRDSpec.setHrid(null);
        } else {
            CrdIdHelper.generateApplicationId(applicationCRDSpec, auditInfo);
        }

        applicationCRDSpec.setIgnoreGroups(ignoreGroups);

        authorizeImport(applicationCRDSpec.getId(), executionContext.getEnvironmentId());

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

    private void authorizeImport(String applicationId, String environmentId) {
        var existing = findApplication(applicationId, environmentId);
        boolean archived = existing.filter(application -> ApplicationStatus.ARCHIVED.name().equals(application.getStatus())).isPresent();
        // Reviving an archived application wipes its members and makes the caller its owner: the Console reserves that to admins
        if (archived && !isAdmin()) {
            throw new ForbiddenAccessException();
        }
        requireCreateOrUpdate(
            RolePermission.ENVIRONMENT_APPLICATION,
            RolePermission.APPLICATION_DEFINITION,
            existing.map(BaseApplicationEntity::getId).orElse(null)
        );
    }

    private Optional<BaseApplicationEntity> findApplication(String applicationId, String environmentId) {
        try {
            return Optional.of(applicationCrudService.findById(applicationId, environmentId));
        } catch (ApplicationNotFoundException e) {
            return Optional.empty();
        }
    }
}
