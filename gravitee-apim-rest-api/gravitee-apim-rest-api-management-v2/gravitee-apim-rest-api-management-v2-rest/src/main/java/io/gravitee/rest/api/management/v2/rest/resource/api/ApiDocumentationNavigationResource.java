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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.use_case.CreatePortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.ImportPortalNavigationUseCase;
import io.gravitee.apim.core.portal_page.use_case.ListApiDocumentationUseCase;
import io.gravitee.apim.core.portal_page.use_case.ListApiPublishLocationsUseCase;
import io.gravitee.apim.core.portal_page.use_case.PublishApiToPortalUseCase;
import io.gravitee.apim.core.portal_page.use_case.UnpublishApiFromPortalUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalNavigationItemsMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiPortalNavigationItemsResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiPortalPublication;
import io.gravitee.rest.api.management.v2.rest.model.ApiPortalPublishLocationsResponse;
import io.gravitee.rest.api.management.v2.rest.model.BaseCreatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.ImportPortalNavigationRequest;
import io.gravitee.rest.api.management.v2.rest.model.PublishApiToPortal;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

public class ApiDocumentationNavigationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ListApiDocumentationUseCase listApiDocumentationUseCase;

    @Inject
    private ListApiPublishLocationsUseCase listApiPublishLocationsUseCase;

    @Inject
    private CreatePortalNavigationItemUseCase createPortalNavigationItemUseCase;

    @Inject
    private ImportPortalNavigationUseCase importPortalNavigationUseCase;

    @Inject
    private PublishApiToPortalUseCase publishApiToPortalUseCase;

    @Inject
    private UnpublishApiFromPortalUseCase unpublishApiFromPortalUseCase;

    @Inject
    private ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;

    @Inject
    private ApiCrudService apiCrudService;

    private final PortalNavigationItemsMapper mapper = PortalNavigationItemsMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.READ }) })
    public ApiPortalNavigationItemsResponse getApiPortalNavigationItems(@PathParam("apiId") String apiId) {
        requireApi(apiId);
        var output = listApiDocumentationUseCase.execute(
            new ListApiDocumentationUseCase.Input(GraviteeContext.getCurrentEnvironment(), apiId)
        );
        return mapper.map(output);
    }

    @Path("_publish-locations")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.READ }) })
    public ApiPortalPublishLocationsResponse getApiPortalPublishLocations(@PathParam("apiId") String apiId) {
        requireApi(apiId);
        var output = listApiPublishLocationsUseCase.execute(
            new ListApiPublishLocationsUseCase.Input(GraviteeContext.getCurrentEnvironment())
        );
        return mapper.map(output);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.CREATE }) })
    public Response createApiPortalNavigationItem(
        @PathParam("apiId") String apiId,
        @Valid @NotNull final BaseCreatePortalNavigationItem createPortalNavigationItem
    ) {
        requireApi(apiId);
        var executionContext = GraviteeContext.getExecutionContext();
        var itemToCreate = apiOwnedNavigationDomainService.claimForApi(
            executionContext.getEnvironmentId(),
            apiId,
            mapper.map(createPortalNavigationItem)
        );

        var output = createPortalNavigationItemUseCase.execute(
            new CreatePortalNavigationItemUseCase.Input(
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                itemToCreate
            )
        );

        return Response.created(this.getLocationHeader(output.item().getId().toString())).entity(mapper.map(output.item())).build();
    }

    @Path("_publish")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public ApiPortalPublication publishApiToPortal(
        @PathParam("apiId") String apiId,
        @Valid @NotNull final PublishApiToPortal publishApiToPortal
    ) {
        requireApi(apiId);
        var executionContext = GraviteeContext.getExecutionContext();
        var output = publishApiToPortalUseCase.execute(
            new PublishApiToPortalUseCase.Input(
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                apiId,
                PortalNavigationItemId.of(publishApiToPortal.getSectionId().toString())
            )
        );
        return mapper.mapPublication(new ListApiDocumentationUseCase.Publication(output.listing(), output.section()));
    }

    @Path("_unpublish")
    @POST
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public Response unpublishApiFromPortal(@PathParam("apiId") String apiId) {
        requireApi(apiId);
        unpublishApiFromPortalUseCase.execute(new UnpublishApiFromPortalUseCase.Input(GraviteeContext.getCurrentEnvironment(), apiId));
        return Response.noContent().build();
    }

    @Path("_import")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.CREATE }) })
    public Response importApiPortalNavigation(
        @PathParam("apiId") String apiId,
        @Valid @NotNull final ImportPortalNavigationRequest importPortalNavigationRequest
    ) {
        requireApi(apiId);
        var executionContext = GraviteeContext.getExecutionContext();
        var input = mapper.map(executionContext.getOrganizationId(), executionContext.getEnvironmentId(), importPortalNavigationRequest);
        if (input.parentId() != null) {
            apiOwnedNavigationDomainService.requireOwnedItem(executionContext.getEnvironmentId(), apiId, input.parentId());
        }

        var output = importPortalNavigationUseCase.execute(
            input.toBuilder().reference(new NavigationItemReference.ApiReference(apiId)).build()
        );

        return Response.ok(mapper.map(output)).build();
    }

    /**
     * Called by each operation, after its permission check: at the point where this resource is reached the
     * check has not run yet, and answering there would tell a caller without rights which APIs exist.
     */
    private void requireApi(String apiId) {
        apiCrudService
            .findById(apiId)
            .filter(api -> api.belongsToEnvironment(GraviteeContext.getCurrentEnvironment()))
            .orElseThrow(() -> new ApiNotFoundException(apiId));
    }

    @Path("{navId}")
    public ApiDocumentationNavigationItemResource getApiDocumentationNavigationItemResource() {
        return resourceContext.getResource(ApiDocumentationNavigationItemResource.class);
    }
}
