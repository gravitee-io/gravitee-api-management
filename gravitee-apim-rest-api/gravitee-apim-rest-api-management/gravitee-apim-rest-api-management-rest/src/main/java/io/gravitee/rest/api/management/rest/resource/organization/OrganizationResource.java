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
package io.gravitee.rest.api.management.rest.resource.organization;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.*;
import io.gravitee.rest.api.management.rest.resource.auth.OAuth2AuthenticationResource;
import io.gravitee.rest.api.management.rest.resource.installation.InstallationResource;
import io.gravitee.rest.api.management.rest.resource.portal.SocialIdentityProvidersResource;
import io.gravitee.rest.api.management.rest.resource.search.SearchResource;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OrganizationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private IdentityProviderService identityProviderService;

    @Inject
    private IdentityProviderActivationService identityProviderActivationService;

    @Inject
    private OrganizationService organizationService;

    @GET
    @Path("/identities")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_IDENTITY_PROVIDER_ACTIVATION, acls = RolePermissionAction.READ))
    @Operation(
        summary = "Get the list of identity provider activations for current organization",
        description = "User must have the ORGANIZATION_IDENTITY_PROVIDER_ACTIVATION[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List identity provider activations for current organization",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = IdentityProviderActivationEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Set<IdentityProviderActivationEntity> listIdentityProviderActivations() {
        return identityProviderActivationService.findAllByTarget(
            new ActivationTarget(GraviteeContext.getCurrentOrganization(), IdentityProviderActivationReferenceType.ORGANIZATION)
        );
    }

    @PUT
    @Path("/identities")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions(
        @Permission(
            value = RolePermission.ORGANIZATION_IDENTITY_PROVIDER_ACTIVATION,
            acls = { RolePermissionAction.CREATE, RolePermissionAction.DELETE, RolePermissionAction.UPDATE }
        )
    )
    @Operation(summary = "Update available organization identities", tags = { "Organization" })
    @ApiResponse(responseCode = "204", description = "Organization successfully updated")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response updateOrganizationIdentities(Set<IdentityProviderActivationEntity> identityProviderActivations) {
        this.identityProviderActivationService.updateTargetIdp(
            GraviteeContext.getExecutionContext(),
            new ActivationTarget(GraviteeContext.getCurrentOrganization(), IdentityProviderActivationReferenceType.ORGANIZATION),
            identityProviderActivations
                .stream()
                .filter(ipa -> {
                    final IdentityProviderEntity idp = this.identityProviderService.findById(ipa.getIdentityProvider());
                    return GraviteeContext.getCurrentOrganization().equals(idp.getOrganization());
                })
                .map(IdentityProviderActivationEntity::getIdentityProvider)
                .collect(Collectors.toList())
        );
        return Response.noContent().build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions(
        @Permission(
            value = RolePermission.ORGANIZATION_POLICIES,
            acls = { RolePermissionAction.CREATE, RolePermissionAction.DELETE, RolePermissionAction.UPDATE }
        )
    )
    @Operation(summary = "Update organization", tags = { "Organization" })
    @ApiResponse(responseCode = "204", description = "Organization successfully updated")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response update(UpdateOrganizationEntity organizationEntity) {
        OrganizationEntity updatedOrganization = organizationService.updateOrganizationAndFlows(
            GraviteeContext.getCurrentOrganization(),
            organizationEntity
        );
        return Response.ok(updatedOrganization).status(204).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions(
        @Permission(
            value = RolePermission.ORGANIZATION_POLICIES,
            acls = { RolePermissionAction.CREATE, RolePermissionAction.DELETE, RolePermissionAction.UPDATE }
        )
    )
    @Operation(summary = "GET organization", tags = { "Organization" })
    @ApiResponse(
        responseCode = "200",
        description = "Organization",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrganizationEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response get() {
        return Response.ok(organizationService.findById(GraviteeContext.getCurrentOrganization())).build();
    }

    // Dynamic authentication provider endpoints
    @Path("auth/oauth2/{identity}")
    public OAuth2AuthenticationResource getOAuth2AuthenticationResource() {
        return resourceContext.getResource(OAuth2AuthenticationResource.class);
    }

    @Path("configuration")
    public OrganizationConfigurationResource getConfigurationResource() {
        return resourceContext.getResource(OrganizationConfigurationResource.class);
    }

    @Path("console")
    public ConsoleResource getConsoleResource() {
        return resourceContext.getResource(ConsoleResource.class);
    }

    @Path("environments")
    public EnvironmentsResource getEnvironmentsResource() {
        return resourceContext.getResource(EnvironmentsResource.class);
    }

    @Path("social-identities")
    public SocialIdentityProvidersResource getSocialIdentityProvidersResource() {
        return resourceContext.getResource(SocialIdentityProvidersResource.class);
    }

    @Path("search")
    public SearchResource getSearchResource() {
        return resourceContext.getResource(SearchResource.class);
    }

    @Path("settings")
    public ConsoleSettingsResource getConsoleSettingsResource() {
        return resourceContext.getResource(ConsoleSettingsResource.class);
    }

    @Path("users")
    public UsersResource getUsersResource() {
        return resourceContext.getResource(UsersResource.class);
    }

    @Path("user")
    public CurrentUserResource getCurrentUserResource() {
        return resourceContext.getResource(CurrentUserResource.class);
    }

    @Path("installation")
    public InstallationResource getInstallationResource() {
        return resourceContext.getResource(InstallationResource.class);
    }

    @Path("groups")
    public OrganizationGroupsResource getGroupsResource() {
        return resourceContext.getResource(OrganizationGroupsResource.class);
    }

    @Path("promotions")
    public PromotionsResource getPromotionsResource() {
        return resourceContext.getResource(PromotionsResource.class);
    }

    @Path("audit")
    public AuditResource getAuditResource() {
        return resourceContext.getResource(AuditResource.class);
    }
}
