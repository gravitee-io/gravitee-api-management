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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.apim.core.application_certificate.use_case.DeleteClientCertificateUseCase;
import io.gravitee.apim.core.application_certificate.use_case.GetClientCertificateUseCase;
import io.gravitee.apim.core.application_certificate.use_case.UpdateClientCertificateUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.PortalClientCertificateMapper;
import io.gravitee.rest.api.portal.rest.model.PortalClientCertificate;
import io.gravitee.rest.api.portal.rest.model.UpdatePortalClientCertificateInput;
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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * Resource for managing a single client certificate of an application via the portal API.
 *
 * @author GraviteeSource Team
 */
@Tag(name = "Application Client Certificates")
public class PortalApplicationClientCertificateResource extends AbstractResource {

    @Inject
    private GetClientCertificateUseCase getClientCertificateUseCase;

    @Inject
    private UpdateClientCertificateUseCase updateClientCertificateUseCase;

    @Inject
    private DeleteClientCertificateUseCase deleteClientCertificateUseCase;

    private final PortalClientCertificateMapper mapper = PortalClientCertificateMapper.INSTANCE;

    @PathParam("certId")
    @Parameter(name = "certId", required = true, description = "Client certificate identifier")
    private String certId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get a client certificate",
        description = "User must have the APPLICATION_DEFINITION[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Client certificate",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PortalClientCertificate.class))
    )
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Client certificate not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getCertificate() {
        var cert = getClientCertificateUseCase.execute(new GetClientCertificateUseCase.Input(certId)).clientCertificate();
        return Response.ok(mapper.toDto(cert)).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update a client certificate",
        description = "User must have the APPLICATION_DEFINITION[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Updated client certificate",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PortalClientCertificate.class))
    )
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Client certificate not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response updateCertificate(@Valid @NotNull final UpdatePortalClientCertificateInput input) {
        var updated = updateClientCertificateUseCase
            .execute(new UpdateClientCertificateUseCase.Input(certId, mapper.toDomain(input)))
            .clientCertificate();
        return Response.ok(mapper.toDto(updated)).build();
    }

    @DELETE
    @Operation(
        summary = "Delete a client certificate",
        description = "User must have the APPLICATION_DEFINITION[UPDATE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Client certificate successfully deleted")
    @ApiResponse(responseCode = "400", description = "Cannot delete last certificate with active mTLS subscriptions")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Client certificate not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response deleteCertificate() {
        // ClientCertificateLastRemovalException (HTTP 400) is mapped automatically by ManagementExceptionMapper
        deleteClientCertificateUseCase.execute(new DeleteClientCertificateUseCase.Input(certId));
        return Response.noContent().build();
    }
}
