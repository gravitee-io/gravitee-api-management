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

import io.gravitee.apim.core.application_certificate.use_case.CreateClientCertificateUseCase;
import io.gravitee.apim.core.application_certificate.use_case.GetClientCertificatesUseCase;
import io.gravitee.apim.core.application_certificate.use_case.ValidateClientCertificateUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.clientcertificate.ValidateCertificateRequest;
import io.gravitee.rest.api.model.clientcertificate.ValidateCertificateResponse;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.PortalClientCertificateMapper;
import io.gravitee.rest.api.portal.rest.model.CreatePortalClientCertificateInput;
import io.gravitee.rest.api.portal.rest.model.PortalClientCertificate;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resource for listing and creating client certificates of an application via the portal API.
 *
 * @author GraviteeSource Team
 */
@Tag(name = "Application Client Certificates")
public class PortalApplicationClientCertificatesResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GetClientCertificatesUseCase getClientCertificatesUseCase;

    @Inject
    private CreateClientCertificateUseCase createClientCertificateUseCase;

    @Inject
    private ValidateClientCertificateUseCase validateClientCertificateUseCase;

    private final PortalClientCertificateMapper mapper = PortalClientCertificateMapper.INSTANCE;

    @PathParam("applicationId")
    @Parameter(name = "applicationId", hidden = true)
    private String applicationId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List client certificates for an application",
        description = "User must have the APPLICATION_DEFINITION[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Paginated list of client certificates",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PortalClientCertificate.class))
    )
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response listCertificates(@Valid @BeanParam PaginationParam paginationParam) {
        var output = getClientCertificatesUseCase.execute(
            new GetClientCertificatesUseCase.Input(applicationId, new PageableImpl(paginationParam.getPage(), paginationParam.getSize()))
        );

        List<PortalClientCertificate> certs = mapper.toDto(output.clientCertificates().getContent());

        Map<String, Object> paginateMetadata = new HashMap<>();
        paginateMetadata.put("totalElements", output.clientCertificates().getTotalElements());
        Map<String, Map<String, Object>> metadata = new HashMap<>();
        metadata.put("paginateMetaData", paginateMetadata);

        return createListResponse(GraviteeContext.getExecutionContext(), certs, paginationParam, metadata);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create a client certificate for an application",
        description = "User must have the APPLICATION_DEFINITION[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Client certificate successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PortalClientCertificate.class))
    )
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response createCertificate(@Valid @NotNull final CreatePortalClientCertificateInput input) {
        var created = createClientCertificateUseCase
            .execute(new CreateClientCertificateUseCase.Input(applicationId, mapper.toDomain(input)))
            .clientCertificate();
        return Response.created(this.getLocationHeader(created.id())).entity(mapper.toDto(created)).build();
    }

    @POST
    @Path("_validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Validate a PEM-encoded client certificate",
        description = "Parses the certificate and returns its metadata without persisting it. User must have the APPLICATION_DEFINITION[READ] permission to use this service."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Certificate is valid",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ValidateCertificateResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "Certificate is invalid, empty, or is a CA certificate")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response validateCertificate(@Valid @NotNull final ValidateCertificateRequest request) {
        var result = validateClientCertificateUseCase.execute(new ValidateClientCertificateUseCase.Input(request.certificate()));
        var certificateInfo = result.certificateInfo();
        return Response.ok(
            new ValidateCertificateResponse(certificateInfo.certificateExpiration(), certificateInfo.subject(), certificateInfo.issuer())
        ).build();
    }

    @Path("{certId}")
    public PortalApplicationClientCertificateResource getCertificateResource() {
        return resourceContext.getResource(PortalApplicationClientCertificateResource.class);
    }
}
