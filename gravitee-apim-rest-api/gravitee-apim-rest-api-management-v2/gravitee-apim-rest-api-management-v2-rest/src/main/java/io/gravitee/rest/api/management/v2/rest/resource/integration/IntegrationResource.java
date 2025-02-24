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
package io.gravitee.rest.api.management.v2.rest.resource.integration;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.integration.use_case.DeleteIngestedApisUseCase;
import io.gravitee.apim.core.integration.use_case.DeleteIntegrationUseCase;
import io.gravitee.apim.core.integration.use_case.DiscoveryUseCase;
import io.gravitee.apim.core.integration.use_case.GetIngestedApisUseCase;
import io.gravitee.apim.core.integration.use_case.GetIntegrationUseCase;
import io.gravitee.apim.core.integration.use_case.StartIngestIntegrationApisUseCase;
import io.gravitee.apim.core.integration.use_case.UpdateIntegrationUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.IntegrationMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApisIngest;
import io.gravitee.rest.api.management.v2.rest.model.AsyncJobStatus;
import io.gravitee.rest.api.management.v2.rest.model.DeletedIngestedApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.IngestedApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.IntegrationIngestionResponse;
import io.gravitee.rest.api.management.v2.rest.model.UpdateIntegration;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.IntegrationPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class IntegrationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GetIntegrationUseCase getIntegrationUsecase;

    @Inject
    private UpdateIntegrationUseCase updateIntegrationUsecase;

    @Inject
    private StartIngestIntegrationApisUseCase startIngestIntegrationApisUseCase;

    @Inject
    private DiscoveryUseCase discoveryUseCase;

    @Inject
    private DeleteIntegrationUseCase deleteIntegrationUseCase;

    @Inject
    private GetIngestedApisUseCase getIngestedApisUseCase;

    @Inject
    DeleteIngestedApisUseCase deleteIngestedApisUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.INTEGRATION_DEFINITION, acls = { RolePermissionAction.READ }) })
    public Response getIntegrationById(@PathParam("integrationId") String integrationId) {
        var integration = getIntegrationUsecase
            .execute(new GetIntegrationUseCase.Input(integrationId, GraviteeContext.getCurrentOrganization()))
            .integration();

        return Response.ok(IntegrationMapper.INSTANCE.map(integration)).build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.INTEGRATION_DEFINITION, acls = { RolePermissionAction.UPDATE }) })
    public Response updateIntegration(
        @PathParam("integrationId") String integrationId,
        @Valid @NotNull UpdateIntegration updateIntegration
    ) {
        AuditInfo auditInfo = getAuditInfo();
        var integrationToUpdate = IntegrationMapper.INSTANCE.map(updateIntegration).toBuilder().id(integrationId).build();
        var input = new UpdateIntegrationUseCase.Input(integrationToUpdate, auditInfo);
        var updatedIntegration = updateIntegrationUsecase.execute(input).integration();
        return Response.ok(IntegrationMapper.INSTANCE.map(updatedIntegration)).build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.INTEGRATION_DEFINITION, acls = { RolePermissionAction.DELETE }) })
    public void deleteIntegrationById(@PathParam("integrationId") String integrationId) {
        var input = DeleteIntegrationUseCase.Input.builder().integrationId(integrationId).build();
        deleteIntegrationUseCase.execute(input);
    }

    @POST
    @Path("/_ingest")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions(@Permission(value = RolePermission.INTEGRATION_DEFINITION, acls = { RolePermissionAction.CREATE }))
    public void ingestApis(
        @PathParam("integrationId") String integrationId,
        @Valid @NotNull ApisIngest apisIngest,
        @Suspended final AsyncResponse response
    ) {
        AuditInfo audit = getAuditInfo();

        startIngestIntegrationApisUseCase
            .execute(new StartIngestIntegrationApisUseCase.Input(integrationId, apisIngest.getApiIds(), audit))
            .subscribe(
                status -> response.resume(IntegrationIngestionResponse.builder().status(AsyncJobStatus.PENDING).build()),
                response::resume
            );
    }

    @GET
    @Path("/_preview")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions(@Permission(value = RolePermission.INTEGRATION_DEFINITION, acls = { RolePermissionAction.CREATE }))
    public void previewNewIntegrationApis(@PathParam("integrationId") String integrationId, @Suspended final AsyncResponse response) {
        AuditInfo audit = getAuditInfo();

        discoveryUseCase
            .execute(new DiscoveryUseCase.Input(integrationId, audit))
            .map(IntegrationMapper::mapper)
            .subscribe(response::resume, response::resume);
    }

    @GET
    @Path("/apis")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions(@Permission(value = RolePermission.INTEGRATION_DEFINITION, acls = { RolePermissionAction.READ }))
    public IngestedApisResponse getIngestedApis(
        @PathParam("integrationId") String integrationId,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        var pageable = new PageableImpl(paginationParam.getPage(), paginationParam.getPerPage());
        var input = new GetIngestedApisUseCase.Input(integrationId, pageable);

        var ingestedApis = getIngestedApisUseCase.execute(input).ingestedApis();

        var totalElements = ingestedApis.getTotalElements();
        return new IngestedApisResponse()
            .data(ingestedApis.getContent().stream().map(ApiMapper.INSTANCE::map).toList())
            .pagination(
                PaginationInfo.computePaginationInfo(totalElements, Math.toIntExact(ingestedApis.getPageElements()), paginationParam)
            )
            .links(computePaginationLinks(totalElements, paginationParam));
    }

    @DELETE
    @Path("/apis")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions(
        {
            @Permission(value = RolePermission.INTEGRATION_DEFINITION, acls = { RolePermissionAction.DELETE }),
            @Permission(value = RolePermission.ENVIRONMENT_INTEGRATION, acls = { RolePermissionAction.DELETE }),
        }
    )
    public DeletedIngestedApisResponse deleteIngestedApis(@PathParam("integrationId") String integrationId) {
        AuditInfo audit = getAuditInfo();

        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(integrationId).auditInfo(audit).build();
        var output = deleteIngestedApisUseCase.execute(input);

        return new DeletedIngestedApisResponse().deleted(output.deleted()).skipped(output.skipped()).errors(output.errors());
    }

    @GET
    @Path("/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(
        responseCode = "200",
        description = "Integration permissions",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = MemberEntity.class))
        )
    )
    public Map<String, char[]> getPermissions(@PathParam("integrationId") String integrationId) {
        if (isAdmin()) {
            final char[] rights = new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), RolePermissionAction.DELETE.getId() };
            return Arrays
                .stream(IntegrationPermission.values())
                .collect(Collectors.toMap(IntegrationPermission::getName, ignored -> rights));
        } else if (isAuthenticated()) {
            final String username = getAuthenticatedUser();
            final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
            return membershipService.getUserMemberPermissions(
                executionContext,
                MembershipReferenceType.INTEGRATION,
                integrationId,
                username
            );
        } else {
            return Map.of();
        }
    }

    @Path("/members")
    public IntegrationMembersResource getIntegrationMembersResource() {
        return resourceContext.getResource(IntegrationMembersResource.class);
    }
}
