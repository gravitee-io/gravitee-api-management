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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.management.rest.resource.configuration.application.registration.ClientRegistrationProvidersResource;
import io.gravitee.rest.api.management.rest.resource.configuration.dictionary.DictionariesResource;
import io.gravitee.rest.api.management.rest.resource.configuration.identity.IdentityProvidersResource;
import io.gravitee.rest.api.management.rest.resource.organization.RoleScopesResource;
import io.gravitee.rest.api.management.rest.resource.quality.QualityRulesResource;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypesEntity;
import io.gravitee.rest.api.model.notification.NotifierEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.configuration.spel.SpelService;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Configuration")
public class EnvironmentConfigurationResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private NotifierService notifierService;

    @Inject
    private ApplicationTypeService applicationTypeService;

    @Inject
    private FlowService flowService;

    @Inject
    private SpelService spelService;

    @GET
    @Path("/hooks")
    @Operation(summary = "Get the list of available hooks")
    @Produces(MediaType.APPLICATION_JSON)
    public Hook[] getConfigurationHooks() {
        return Arrays.stream(PortalHook.values()).filter(h -> !h.isHidden()).toArray(Hook[]::new);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("notifiers")
    @Operation(summary = "List of available notifiers")
    @ApiResponse(
        responseCode = "200",
        description = "List of notifiers",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = NotifierEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_NOTIFICATION, acls = RolePermissionAction.READ) })
    public List<NotifierEntity> getPortalNotifiers() {
        return notifierService.list(NotificationReferenceType.PORTAL, GraviteeContext.getCurrentEnvironment());
    }

    @Path("categories")
    public CategoriesResource getCategoryResource() {
        return resourceContext.getResource(CategoriesResource.class);
    }

    @Path("groups")
    public GroupsResource getGroupResource() {
        return resourceContext.getResource(GroupsResource.class);
    }

    @Path("tags")
    @Deprecated
    public TagsResource getTagResource() {
        return resourceContext.getResource(TagsResource.class);
    }

    @Path("tenants")
    @Deprecated
    public TenantsResource getTenantsResource() {
        return resourceContext.getResource(TenantsResource.class);
    }

    @Path("metadata")
    public MetadataResource getMetadataResource() {
        return resourceContext.getResource(MetadataResource.class);
    }

    @Deprecated
    @Path("rolescopes")
    public RoleScopesResource getRoleScopesResource() {
        return resourceContext.getResource(RoleScopesResource.class);
    }

    @Path("notificationsettings")
    public PortalNotificationSettingsResource getNotificationSettingsResource() {
        return resourceContext.getResource(PortalNotificationSettingsResource.class);
    }

    @Path("top-apis")
    public TopApisResource getTopApisResource() {
        return resourceContext.getResource(TopApisResource.class);
    }

    @Path("plans")
    public PlansResource getPlansResource() {
        return resourceContext.getResource(PlansResource.class);
    }

    @Path("dictionaries")
    public DictionariesResource getDictionariesResource() {
        return resourceContext.getResource(DictionariesResource.class);
    }

    @Path("apiheaders")
    public ApiHeadersResource getApiHeadersResource() {
        return resourceContext.getResource(ApiHeadersResource.class);
    }

    @Deprecated
    @Path("identities")
    public IdentityProvidersResource getAuthenticationProvidersResource() {
        return resourceContext.getResource(IdentityProvidersResource.class);
    }

    @Path("applications/registration/providers")
    public ClientRegistrationProvidersResource getClientRegistrationProvidersResource() {
        return resourceContext.getResource(ClientRegistrationProvidersResource.class);
    }

    @GET
    @Path("applications/types")
    @Operation(summary = "Get the list of enabled application types")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEnabledApplicationTypes() {
        ApplicationTypesEntity enabledApplicationTypes = applicationTypeService.getEnabledApplicationTypes(
            GraviteeContext.getExecutionContext()
        );
        return Response.ok(enabledApplicationTypes.getData()).build();
    }

    @Path("entrypoints")
    @Deprecated
    public EntrypointsResource getEntryPointsResource() {
        return resourceContext.getResource(EntrypointsResource.class);
    }

    @Path("quality-rules")
    public QualityRulesResource getQualityRulesResource() {
        return resourceContext.getResource(QualityRulesResource.class);
    }

    @Path("dashboards")
    public DashboardsResource getDashboardsResource() {
        return resourceContext.getResource(DashboardsResource.class);
    }

    @Path("themes")
    public ThemesResource getThemesResource() {
        return resourceContext.getResource(ThemesResource.class);
    }

    @GET
    @Path("spel/grammar")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode getGrammar() {
        return spelService.getGrammar();
    }
}
