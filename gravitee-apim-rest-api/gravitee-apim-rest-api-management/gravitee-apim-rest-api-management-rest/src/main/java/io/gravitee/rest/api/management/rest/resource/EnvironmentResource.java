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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.PermissionMap;
import io.gravitee.rest.api.management.rest.resource.auth.OAuth2AuthenticationResource;
import io.gravitee.rest.api.management.rest.resource.organization.CurrentUserResource;
import io.gravitee.rest.api.management.rest.resource.organization.UsersResource;
import io.gravitee.rest.api.management.rest.resource.search.SearchResource;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private IdentityProviderService identityProviderService;

    @Inject
    private IdentityProviderActivationService identityProviderActivationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get an Environment", tags = { "Environment" })
    @ApiResponse(
        responseCode = "200",
        description = "Found Environment",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EnvironmentEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getEnvironment() {
        return Response.ok(environmentService.findById(GraviteeContext.getCurrentEnvironment())).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/identities")
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_IDENTITY_PROVIDER_ACTIVATION, acls = READ))
    @Operation(
        summary = "Get the list of identity provider activations for current environment",
        description = "User must have the ENVIRONMENT_IDENTITY_PROVIDER_ACTIVATION[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List identity provider activations for current environment",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = IdentityProviderActivationEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Set<IdentityProviderActivationEntity> getIdentityProviderActivations() {
        return identityProviderActivationService.findAllByTarget(
            new IdentityProviderActivationService.ActivationTarget(
                GraviteeContext.getCurrentEnvironment(),
                IdentityProviderActivationReferenceType.ENVIRONMENT
            )
        );
    }

    @PUT
    @Path("/identities")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_IDENTITY_PROVIDER_ACTIVATION, acls = { CREATE, DELETE, UPDATE }))
    @Operation(summary = "Update available environment identities", tags = { "Environment" })
    @ApiResponse(responseCode = "204", description = "Environment successfully updated")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response updateEnvironmentIdentities(List<IdentityProviderActivationEntity> identityProviderActivations) {
        this.identityProviderActivationService.updateTargetIdp(
                GraviteeContext.getExecutionContext(),
                new IdentityProviderActivationService.ActivationTarget(
                    GraviteeContext.getCurrentEnvironment(),
                    IdentityProviderActivationReferenceType.ENVIRONMENT
                ),
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/permissions")
    @Operation(summary = "Get permissions for the current user on the environment")
    @ApiResponse(
        responseCode = "200",
        description = "Current user permissions on environment",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PermissionMap.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getEnvironmentPermissions() {
        Map<String, char[]> permissions = new HashMap<>();
        if (isAuthenticated()) {
            final String username = getAuthenticatedUser();
            // Search for the environment in any  case to make sure it exists
            // We use the env resolved from context to make sure the hrid has been resolved
            final EnvironmentEntity environmentEntity = environmentService.findById(GraviteeContext.getCurrentEnvironment());
            if (isAdmin()) {
                final char[] rights = new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() };
                for (EnvironmentPermission perm : EnvironmentPermission.values()) {
                    permissions.put(perm.getName(), rights);
                }
            } else {
                permissions.putAll(
                    membershipService.getUserMemberPermissions(GraviteeContext.getExecutionContext(), environmentEntity, username)
                );
            }
        }
        return Response.ok(permissions).build();
    }

    @Path("alerts")
    public AlertsResource getAlertsResource() {
        return resourceContext.getResource(AlertsResource.class);
    }

    @Path("apis")
    public ApisResource getApisResource() {
        return resourceContext.getResource(ApisResource.class);
    }

    @Path("applications")
    public ApplicationsResource getApplicationsResource() {
        return resourceContext.getResource(ApplicationsResource.class);
    }

    @Path("configuration")
    public EnvironmentConfigurationResource getConfigurationResource() {
        return resourceContext.getResource(EnvironmentConfigurationResource.class);
    }

    @Deprecated
    @Path("user")
    public CurrentUserResource getCurrentUserResource() {
        return resourceContext.getResource(CurrentUserResource.class);
    }

    @Path("subscriptions")
    public SubscriptionsResource getSubscriptionsResource() {
        return resourceContext.getResource(SubscriptionsResource.class);
    }

    @Path("audit")
    public AuditResource getAuditResource() {
        return resourceContext.getResource(AuditResource.class);
    }

    @Path("portal")
    public PortalResource getPortalResource() {
        return resourceContext.getResource(PortalResource.class);
    }

    // Dynamic authentication provider endpoints
    @Deprecated
    @Path("auth/oauth2/{identity}")
    public OAuth2AuthenticationResource getOAuth2AuthenticationResource() {
        return resourceContext.getResource(OAuth2AuthenticationResource.class);
    }

    @Deprecated
    @Path("users")
    public UsersResource getUsersResource() {
        return resourceContext.getResource(UsersResource.class);
    }

    @Deprecated
    @Path("search")
    public SearchResource getSearchResource() {
        return resourceContext.getResource(SearchResource.class);
    }

    @Path("fetchers")
    public FetchersResource getFetchersResource() {
        return resourceContext.getResource(FetchersResource.class);
    }

    @Path("policies")
    public PoliciesResource getPoliciesResource() {
        return resourceContext.getResource(PoliciesResource.class);
    }

    @Path("resources")
    public ResourcesResource getResourcesResource() {
        return resourceContext.getResource(ResourcesResource.class);
    }

    @Path("services-discovery")
    public ServicesDiscoveryResource getServicesDiscoveryResource() {
        return resourceContext.getResource(ServicesDiscoveryResource.class);
    }

    @Path("connectors")
    public ConnectorsResource getConnectorsResource() {
        return resourceContext.getResource(ConnectorsResource.class);
    }

    @Path("instances")
    public InstancesResource getInstancesResource() {
        return resourceContext.getResource(InstancesResource.class);
    }

    @Path("platform")
    public PlatformResource getPlatformResource() {
        return resourceContext.getResource(PlatformResource.class);
    }

    @Path("messages")
    public MessagesResource getMessagesResource() {
        return resourceContext.getResource(MessagesResource.class);
    }

    @Path("tickets")
    public PlatformTicketsResource getPlatformTicketsResource() {
        return resourceContext.getResource(PlatformTicketsResource.class);
    }

    @Path("entrypoints")
    public PortalEntrypointsResource getPortalEntryPointsResource() {
        return resourceContext.getResource(PortalEntrypointsResource.class);
    }

    @Path("notifiers")
    public NotifiersResource getNotifiersResource() {
        return resourceContext.getResource(NotifiersResource.class);
    }

    @Path("settings")
    public PortalSettingsResource getPortalSettingsResource() {
        return resourceContext.getResource(PortalSettingsResource.class);
    }

    @Path("analytics")
    public EnvironmentAnalyticsResource getEnvironmentAnalyticsResource() {
        return resourceContext.getResource(EnvironmentAnalyticsResource.class);
    }

    @Path("promotion-targets")
    public PromotionTargetsResource getPromotionTargetsResource() {
        return resourceContext.getResource(PromotionTargetsResource.class);
    }

    @Path("restrictedDomains")
    public RestrictedDomainsResource getRestrictedDomainsResource() {
        return resourceContext.getResource(RestrictedDomainsResource.class);
    }

    @Path("flows")
    public EnvironmentFlowsResource getEnvironmentFlowsResource() {
        return resourceContext.getResource(EnvironmentFlowsResource.class);
    }
}
