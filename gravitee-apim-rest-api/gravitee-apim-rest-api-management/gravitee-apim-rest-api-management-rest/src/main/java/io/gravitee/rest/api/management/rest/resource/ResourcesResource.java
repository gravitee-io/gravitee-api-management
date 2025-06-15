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

import static java.util.Collections.emptyList;

import io.gravitee.common.http.MediaType;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.ResourceListItem;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ResourceService;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Plugins")
public class ResourcesResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private LicenseManager licenseManager;

    @Inject
    private ResourceService resourceService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List resource plugins", description = "User must have the MANAGEMENT_API[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of resources",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ResourceListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public Collection<ResourceListItem> getResources(@QueryParam("expand") List<String> expand) {
        License license = licenseManager.getOrganizationLicenseOrPlatform(
            GraviteeContext.getCurrentOrganization() != null
                ? GraviteeContext.getCurrentOrganization()
                : GraviteeContext.getDefaultOrganization()
        );

        expand = expand == null ? emptyList() : expand;
        boolean includeSchema = expand.contains("schema");
        boolean includeIcon = expand.contains("icon");

        Stream<ResourceListItem> stream = resourceService
            .findAll()
            .stream()
            .map(resource -> convert(resource, license::isFeatureEnabled, includeSchema, includeIcon));

        return stream.sorted(Comparator.comparing(ResourceListItem::getName)).collect(Collectors.toList());
    }

    @Path("{resource}")
    public ResourceResource getResourceResource() {
        return resourceContext.getResource(ResourceResource.class);
    }

    private ResourceListItem convert(
        PlatformPluginEntity resource,
        java.util.function.Function<String, Boolean> featureEnabled,
        boolean includeSchema,
        boolean includeIcon
    ) {
        ResourceListItem item = new ResourceListItem();

        item.setId(resource.getId());
        item.setName(resource.getName());
        item.setDescription(resource.getDescription());
        item.setVersion(resource.getVersion());
        item.setDeployed(resource.isDeployed() && featureEnabled.apply(resource.getFeature()));
        item.setFeature(resource.getFeature());

        if (includeSchema) {
            item.setSchema(resourceService.getSchema(item.getId()));
        }
        if (includeIcon) {
            item.setIcon(resourceService.getIcon(item.getId()));
        }

        return item;
    }
}
