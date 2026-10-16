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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidUrlFormatException;
import io.gravitee.apim.core.portal_page.model.OpenApiConfiguration;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.RedocConfiguration;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import io.gravitee.apim.core.portal_page.use_case.DeletePortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.FetchPortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetApiPortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageContentUseCase;
import io.gravitee.apim.core.portal_page.use_case.UpdatePortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.UpdatePortalPageContentConfigurationUseCase;
import io.gravitee.apim.core.portal_page.use_case.UpdatePortalPageContentUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalNavigationItemsMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalPageContentMapper;
import io.gravitee.rest.api.management.v2.rest.model.BaseUpdatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.FetchPortalNavigationItemResponse;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageContent;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageOpenApiConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalPageContent;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

public class ApiDocumentationNavigationItemResource extends AbstractResource {

    @Inject
    private GetApiPortalNavigationItemUseCase getApiPortalNavigationItemUseCase;

    @Inject
    private UpdatePortalNavigationItemUseCase updatePortalNavigationItemUseCase;

    @Inject
    private DeletePortalNavigationItemUseCase deletePortalNavigationItemUseCase;

    @Inject
    private FetchPortalNavigationItemUseCase fetchPortalNavigationItemUseCase;

    @Inject
    private GetPortalPageContentUseCase getPortalPageContentUseCase;

    @Inject
    private UpdatePortalPageContentUseCase updatePortalPageContentUseCase;

    @Inject
    private UpdatePortalPageContentConfigurationUseCase updatePortalPageContentConfigurationUseCase;

    @Inject
    private ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;

    private final PortalNavigationItemsMapper mapper = PortalNavigationItemsMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.READ }) })
    public PortalNavigationItem getApiPortalNavigationItem(@PathParam("apiId") String apiId, @PathParam("navId") String navigationItemId) {
        var output = getApiPortalNavigationItemUseCase.execute(
            new GetApiPortalNavigationItemUseCase.Input(
                GraviteeContext.getCurrentEnvironment(),
                apiId,
                PortalNavigationItemId.of(navigationItemId)
            )
        );
        return mapper.map(output.item());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public PortalNavigationItem updateApiPortalNavigationItem(
        @PathParam("apiId") String apiId,
        @PathParam("navId") String navigationItemId,
        @QueryParam("propagatePublishToChildren") @DefaultValue("false") boolean propagatePublishToChildren,
        @Valid @NotNull final BaseUpdatePortalNavigationItem updatePortalNavigationItem
    ) {
        var environmentId = GraviteeContext.getCurrentEnvironment();
        var toUpdate = mapper.map(updatePortalNavigationItem);
        apiOwnedNavigationDomainService.requireOwnedItem(environmentId, apiId, PortalNavigationItemId.of(navigationItemId));
        if (toUpdate.getParentId() != null) {
            apiOwnedNavigationDomainService.requireOwnedItem(environmentId, apiId, toUpdate.getParentId());
        }

        var output = updatePortalNavigationItemUseCase.execute(
            new UpdatePortalNavigationItemUseCase.Input(
                GraviteeContext.getCurrentOrganization(),
                environmentId,
                navigationItemId,
                toUpdate,
                propagatePublishToChildren,
                true
            )
        );
        return mapper.map(output.updatedItem());
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.DELETE }) })
    public Response deleteApiPortalNavigationItem(@PathParam("apiId") String apiId, @PathParam("navId") String navigationItemId) {
        var environmentId = GraviteeContext.getCurrentEnvironment();
        var itemId = PortalNavigationItemId.of(navigationItemId);
        apiOwnedNavigationDomainService.requireOwnedItem(environmentId, apiId, itemId);

        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(GraviteeContext.getCurrentOrganization(), environmentId, itemId)
        );
        return Response.noContent().build();
    }

    @Path("_fetch")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public FetchPortalNavigationItemResponse fetchApiPortalNavigationItem(
        @PathParam("apiId") String apiId,
        @PathParam("navId") String navigationItemId
    ) {
        var environmentId = GraviteeContext.getCurrentEnvironment();
        apiOwnedNavigationDomainService.requireOwnedItem(environmentId, apiId, PortalNavigationItemId.of(navigationItemId));

        var output = fetchPortalNavigationItemUseCase.execute(new FetchPortalNavigationItemUseCase.Input(environmentId, navigationItemId));
        return mapper.map(output);
    }

    @Path("content")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.READ }) })
    public PortalPageContent getApiPortalNavigationPageContent(
        @PathParam("apiId") String apiId,
        @PathParam("navId") String navigationItemId
    ) {
        final var executionContext = GraviteeContext.getExecutionContext();
        var page = apiOwnedNavigationDomainService.requireOwnedPage(
            executionContext.getEnvironmentId(),
            apiId,
            PortalNavigationItemId.of(navigationItemId)
        );

        var output = getPortalPageContentUseCase.execute(
            new GetPortalPageContentUseCase.Input(
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                page.getPortalPageContentId()
            )
        );
        return PortalPageContentMapper.INSTANCE.map(output.content());
    }

    @Path("content")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public PortalPageContent updateApiPortalNavigationPageContent(
        @PathParam("apiId") String apiId,
        @PathParam("navId") String navigationItemId,
        @Valid @NotNull final UpdatePortalPageContent updatePortalPageContent
    ) {
        var update = PortalPageContentMapper.INSTANCE.map(updatePortalPageContent);
        if (update.getConfiguration() != null) {
            requireHttpTryItUrl(update.getConfiguration());
        }
        var environmentId = GraviteeContext.getCurrentEnvironment();
        var page = apiOwnedNavigationDomainService.requireOwnedPage(environmentId, apiId, PortalNavigationItemId.of(navigationItemId));

        var output = updatePortalPageContentUseCase.execute(
            new UpdatePortalPageContentUseCase.Input(
                GraviteeContext.getCurrentOrganization(),
                environmentId,
                page.getPortalPageContentId().json(),
                update
            )
        );
        return PortalPageContentMapper.INSTANCE.map(output.portalPageContent());
    }

    @Path("content/configuration")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public PortalPageContent updateApiPortalNavigationPageContentConfiguration(
        @PathParam("apiId") String apiId,
        @PathParam("navId") String navigationItemId,
        @Valid @NotNull final PortalPageOpenApiConfiguration configuration
    ) {
        var viewerSettings = PortalPageContentMapper.INSTANCE.mapToConfiguration(configuration);
        requireHttpTryItUrl(viewerSettings);
        var environmentId = GraviteeContext.getCurrentEnvironment();
        var page = apiOwnedNavigationDomainService.requireOwnedPage(environmentId, apiId, PortalNavigationItemId.of(navigationItemId));

        var output = updatePortalPageContentConfigurationUseCase.execute(
            new UpdatePortalPageContentConfigurationUseCase.Input(
                GraviteeContext.getCurrentOrganization(),
                environmentId,
                page.getPortalPageContentId().json(),
                viewerSettings
            )
        );
        return PortalPageContentMapper.INSTANCE.map(output.portalPageContent());
    }

    /**
     * The API-scoped content and configuration writes open the base URL to API users, so they only take what the dialogs
     * allow: empty, or an http(s) URL. The environment-level endpoints are left as they were, relative URLs included.
     */
    private static void requireHttpTryItUrl(OpenApiConfiguration viewerSettings) {
        var tryItUrl = switch (viewerSettings) {
            case SwaggerUiConfiguration swagger -> swagger.tryItUrl();
            case RedocConfiguration redoc -> redoc.tryItUrl();
        };
        if (!tryItUrl.isEmpty() && !isHttpUrl(tryItUrl)) {
            throw new InvalidUrlFormatException();
        }
    }

    private static boolean isHttpUrl(String url) {
        try {
            var uri = new URI(url);
            var isHttpScheme = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            return isHttpScheme && uri.getRawAuthority() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
