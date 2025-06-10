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
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.api.use_case.ImportApiCRDUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.automation.mapper.ApiMapper;
import io.gravitee.apim.rest.api.automation.model.ApiV4Spec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
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
public class ApisResource extends AbstractResource {

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
    public Response createOrUpdate(@Valid @NotNull ApiV4Spec spec, @QueryParam("dryRun") boolean dryRun) {
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

        if (dryRun) {
            var statusBuilder = ApiCRDStatus.builder();
            validateApiCRDDomainService
                .validateAndSanitize(
                    new ValidateApiCRDDomainService.Input(
                        audit,
                        io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper.INSTANCE.map(
                            ApiMapper.INSTANCE.apiV4SpecToApiCRDSpec(spec)
                        )
                    )
                )
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

        ApiCRDStatus apiCRDStatus = importApiCRDUseCase
            .execute(
                new ImportApiCRDUseCase.Input(
                    audit,
                    io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper.INSTANCE.map(ApiMapper.INSTANCE.apiV4SpecToApiCRDSpec(spec))
                )
            )
            .status();

        return Response.ok(ApiMapper.INSTANCE.apiV4SpecAndStatusToApiV4State(spec, apiCRDStatus)).build();
    }
}
