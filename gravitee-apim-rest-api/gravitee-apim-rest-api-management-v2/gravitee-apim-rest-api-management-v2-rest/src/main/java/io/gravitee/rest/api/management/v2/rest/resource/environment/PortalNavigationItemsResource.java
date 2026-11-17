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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import io.gravitee.apim.core.portal.domain_service.DefaultPortalDomainService;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.use_case.BulkCreatePortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.CreatePortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.ImportPortalNavigationUseCase;
import io.gravitee.apim.core.portal_page.use_case.ListPortalNavigationItemsForPortalUseCase;
import io.gravitee.apim.core.portal_page.use_case.SeedDefaultPagesForPortalNavigationItemsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalNavigationItemsMapper;
import io.gravitee.rest.api.management.v2.rest.model.BaseCreatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.BaseCreatePortalNavigationItems;
import io.gravitee.rest.api.management.v2.rest.model.ImportPortalNavigationRequest;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemApiSummary;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemsResponse;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemsResponseMetadata;
import io.gravitee.rest.api.management.v2.rest.model.SeedDefaultPagesRequest;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class PortalNavigationItemsResource extends AbstractResource {

    @Inject
    private DefaultPortalDomainService defaultPortalDomainService;

    private PortalId resolvePortalId() {
        var portalId = uriInfo.getPathParameters().getFirst("portalId");
        return portalId == null
            ? defaultPortalDomainService.getDefault(GraviteeContext.getCurrentEnvironment()).getId()
            : PortalMapper.INSTANCE.toPortalId(portalId);
    }

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreatePortalNavigationItemUseCase createPortalNavigationItemUseCase;

    @Inject
    private BulkCreatePortalNavigationItemUseCase bulkCreatePortalNavigationItemUseCase;

    @Inject
    private ListPortalNavigationItemsForPortalUseCase listPortalNavigationItemsForPortalUseCase;

    @Inject
    private SeedDefaultPagesForPortalNavigationItemsUseCase seedDefaultPagesForPortalNavigationItemsUseCase;

    @Inject
    private ImportPortalNavigationUseCase importPortalNavigationUseCase;

    private final PortalNavigationItemsMapper mapper = PortalNavigationItemsMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.READ) })
    public PortalNavigationItemsResponse getPortalNavigationItems(
        @QueryParam("area") @DefaultValue("TOP_NAVBAR") String area,
        @QueryParam("parentId") String parentId,
        @QueryParam("loadChildren") @DefaultValue("true") boolean loadChildren,
        @QueryParam("includes") Set<String> includes
    ) {
        final PortalArea portalArea;
        try {
            portalArea = PortalArea.valueOf(area);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid value '" + area + "' for query parameter 'area'");
        }

        boolean includeApis = includes != null && includes.contains("apis");

        var result = listPortalNavigationItemsForPortalUseCase.execute(
            new ListPortalNavigationItemsForPortalUseCase.Input(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                resolvePortalId(),
                portalArea,
                Optional.ofNullable(parentId).map(PortalNavigationItemId::of),
                loadChildren,
                includeApis
            )
        );

        var response = new PortalNavigationItemsResponse().items(mapper.map(result.items()));

        Map<String, PortalNavigationItemApiSummary> apisMetadata = mapper.mapApisMetadata(result.apis());
        if (!apisMetadata.isEmpty()) {
            response.metadata(new PortalNavigationItemsResponseMetadata().apis(apisMetadata));
        }

        return response;
    }

    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPortalNavigationItem(@Valid @NotNull final BaseCreatePortalNavigationItem createPortalNavigationItem) {
        final var executionContext = GraviteeContext.getExecutionContext();

        final var output = createPortalNavigationItemUseCase.execute(
            new CreatePortalNavigationItemUseCase.Input(
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                mapper.map(createPortalNavigationItem),
                new NavigationItemReference.PortalReference(resolvePortalId())
            )
        );

        return Response.created(this.getLocationHeader(output.item().getId().toString())).entity(mapper.map(output.item())).build();
    }

    @Path("_bulk")
    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPortalNavigationItemsInBulk(
        @Valid @NotNull final BaseCreatePortalNavigationItems createPortalNavigationItemsRequest
    ) {
        final var executionContext = GraviteeContext.getExecutionContext();

        final var output = bulkCreatePortalNavigationItemUseCase.execute(
            new BulkCreatePortalNavigationItemUseCase.Input(
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                mapper.mapCreatePortalNavigationItems(createPortalNavigationItemsRequest.getItems()),
                resolvePortalId()
            )
        );

        return Response.ok(new PortalNavigationItemsResponse().items(mapper.map(output.items()))).build();
    }

    @Path("_import")
    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importPortalNavigation(@Valid @NotNull final ImportPortalNavigationRequest importPortalNavigationRequest) {
        final var executionContext = GraviteeContext.getExecutionContext();

        final var output = importPortalNavigationUseCase.execute(
            mapper.map(
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                importPortalNavigationRequest,
                new NavigationItemReference.PortalReference(resolvePortalId())
            )
        );

        return Response.ok(mapper.map(output)).build();
    }

    @Path("_default-pages")
    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = { RolePermissionAction.CREATE }) })
    @Consumes(MediaType.APPLICATION_JSON)
    public Response seedDefaultPages(@Valid @NotNull final SeedDefaultPagesRequest seedDefaultPagesRequest) {
        final var executionContext = GraviteeContext.getExecutionContext();

        seedDefaultPagesForPortalNavigationItemsUseCase.execute(
            mapper.mapSeedDefaultPagesInput(
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                seedDefaultPagesRequest,
                resolvePortalId()
            )
        );

        return Response.noContent().build();
    }

    @Path("{navId}")
    public PortalNavigationItemResource getPortalNavigationItemResource() {
        return resourceContext.getResource(PortalNavigationItemResource.class);
    }
}
