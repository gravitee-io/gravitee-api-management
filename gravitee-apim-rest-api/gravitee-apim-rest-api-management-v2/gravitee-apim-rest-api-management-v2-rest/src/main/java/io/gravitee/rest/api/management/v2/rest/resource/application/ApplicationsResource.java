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
package io.gravitee.rest.api.management.v2.rest.resource.application;

import io.gravitee.apim.core.application.use_case.ImportApplicationCRDUseCase;
import io.gravitee.apim.core.application.use_case.ValidateApplicationCRDUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApplicationCRDSpec;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Path("/applications")
public class ApplicationsResource extends AbstractResource {

    @Inject
    private ImportApplicationCRDUseCase importApplicationCRDUseCase;

    @Inject
    private ValidateApplicationCRDUseCase validateCRDUseCase;

    @PUT
    @Path("/_import/crd")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE) })
    public Response createApplicationWithCRD(@Valid ApplicationCRDSpec crd, @QueryParam("dryRun") boolean dryRun) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        var input = new ImportApplicationCRDUseCase.Input(
            AuditInfo
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
                .build(),
            ApplicationMapper.INSTANCE.map(crd)
        );

        return dryRun
            ? Response.ok(validateCRDUseCase.execute(input).status()).build()
            : Response.ok(importApplicationCRDUseCase.execute(input).status()).build();
    }
}
