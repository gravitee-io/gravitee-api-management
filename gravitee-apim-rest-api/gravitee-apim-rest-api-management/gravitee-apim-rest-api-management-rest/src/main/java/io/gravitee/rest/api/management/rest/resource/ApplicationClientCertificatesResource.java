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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.apim.core.application_certificate.use_case.CreateClientCertificateUseCase;
import io.gravitee.apim.core.application_certificate.use_case.GetClientCertificatesUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.mapper.ClientCertificateMapper;
import io.gravitee.rest.api.management.rest.model.Pageable;
import io.gravitee.rest.api.management.rest.model.PagedResult;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * Resource for managing client certificates of an application.
 */
@Tag(name = "Application Client Certificates")
public class ApplicationClientCertificatesResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GetClientCertificatesUseCase getClientCertificatesUseCase;

    @Inject
    private CreateClientCertificateUseCase createClientCertificateUseCase;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @Parameter(name = "application", hidden = true)
    private String application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List client certificates for an application",
        description = "User must have the APPLICATION_DEFINITION[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Paginated list of client certificates",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ClientCertificatePagedResult.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public PagedResult<ClientCertificate> getApplicationClientCertificates(@Valid @BeanParam Pageable pageable) {
        Page<ClientCertificate> page = ClientCertificateMapper.INSTANCE.map(
            getClientCertificatesUseCase
                .execute(new GetClientCertificatesUseCase.Input(application, pageable.toPageable()))
                .clientCertificates()
        );
        return new PagedResult<>(page, pageable.getSize());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create a client certificate for an application",
        description = "User must have the APPLICATION_DEFINITION[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Client certificate successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CreateClientCertificate.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.CREATE) })
    public Response createApplicationClientCertificate(@Valid @NotNull final CreateClientCertificate createClientCertificate) {
        var created = createClientCertificateUseCase
            .execute(
                new CreateClientCertificateUseCase.Input(application, ClientCertificateMapper.INSTANCE.toDomain(createClientCertificate))
            )
            .clientCertificate();
        return Response.created(this.getLocationHeader(created.id())).entity(ClientCertificateMapper.INSTANCE.toDto(created)).build();
    }

    @Path("{certId}")
    public ApplicationClientCertificateResource getApplicationClientCertificateResource() {
        return resourceContext.getResource(ApplicationClientCertificateResource.class);
    }

    /**
     * Inner class for Swagger documentation of paginated result.
     */
    private static class ClientCertificatePagedResult extends PagedResult<ClientCertificate> {

        public ClientCertificatePagedResult() {
            super();
        }
    }
}
