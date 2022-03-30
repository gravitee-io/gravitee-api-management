/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.NewTenantEntity;
import io.gravitee.rest.api.model.TenantEntity;
import io.gravitee.rest.api.model.TenantReferenceType;
import io.gravitee.rest.api.model.UpdateTenantEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.TenantService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Tenants")
public class TenantsResource extends AbstractResource {

    @Inject
    private TenantService tenantService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List tenants")
    @ApiResponse(
        responseCode = "200",
        description = "List of tenants",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = TenantEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public List<TenantEntity> getTenants() {
        return tenantService
            .findByReference(GraviteeContext.getCurrentOrganization(), TenantReferenceType.ORGANIZATION)
            .stream()
            .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
            .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a tenant", description = "User must have the MANAGEMENT_TENANT[CREATE] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of tenants",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = TenantEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_TENANT, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.ORGANIZATION_TENANT, acls = RolePermissionAction.CREATE),
        }
    )
    public List<TenantEntity> createTenants(@Valid @NotNull final List<NewTenantEntity> tenant) {
        return tenantService.create(
            GraviteeContext.getExecutionContext(),
            tenant,
            GraviteeContext.getCurrentOrganization(),
            TenantReferenceType.ORGANIZATION
        );
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a tenant", description = "User must have the MANAGEMENT_TENANT[UPDATE] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of tenants",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = TenantEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_TENANT, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.ORGANIZATION_TENANT, acls = RolePermissionAction.UPDATE),
        }
    )
    public List<TenantEntity> updateTenants(@Valid @NotNull final List<UpdateTenantEntity> tenant) {
        return tenantService.update(
            GraviteeContext.getExecutionContext(),
            tenant,
            GraviteeContext.getCurrentOrganization(),
            TenantReferenceType.ORGANIZATION
        );
    }

    @Path("{tenant}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete a tenant", description = "User must have the MANAGEMENT_TENANT[DELETE] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of tenants",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = TenantEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_TENANT, acls = RolePermissionAction.DELETE),
            @Permission(value = RolePermission.ORGANIZATION_TENANT, acls = RolePermissionAction.DELETE),
        }
    )
    public void deleteTenant(@PathParam("tenant") String tenant) {
        tenantService.delete(
            GraviteeContext.getExecutionContext(),
            tenant,
            GraviteeContext.getCurrentOrganization(),
            TenantReferenceType.ORGANIZATION
        );
    }
}
