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
package io.gravitee.gamma.rest.resources.gravitee_plugin;

import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.ResourcePluginProvider;
import io.gravitee.gamma.rest.resources.gravitee_plugin.dto.PlatformPluginDto;
import io.gravitee.gamma.rest.resources.guardian.GuardiansResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.GraviteeLicenseFeature;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * The resource types a Guardian may hold, read from the installed resource plugins.
 *
 * <p>Organization-scoped, like the policies catalog: what the node has installed is the same for every environment.
 * Sorted the way every catalog is, see {@link PlatformPlugin#BY_NAME_THEN_ID}, so the screen does not have to.
 *
 * <p>Gated as the policies catalog is, by the {@code ORGANIZATION_POLICIES} read permission and the Guardians license
 * feature: a Guardian screen reads both catalogs, so one grant opens them both. No permission exists for resource
 * plugins, and the Guardian permission is an environment's while this path names none.
 *
 * @author GraviteeSource Team
 */
public class ResourcePluginResource {

    @Inject
    private ResourcePluginProvider resourcePluginProvider;

    @GET
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_POLICIES, acls = RolePermissionAction.READ) })
    @Produces(MediaType.APPLICATION_JSON)
    public List<PlatformPluginDto> getResources(@PathParam("orgId") String organizationId) {
        return resourcePluginProvider
            .findAll(organizationId)
            .stream()
            .sorted(PlatformPlugin.BY_NAME_THEN_ID)
            .map(PlatformPluginDto::from)
            .toList();
    }

    @GET
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_POLICIES, acls = RolePermissionAction.READ) })
    @Path("/{resourceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public PlatformPluginDto getResource(@PathParam("orgId") String organizationId, @PathParam("resourceId") String resourceId) {
        return PlatformPluginDto.from(resourcePluginProvider.findById(organizationId, resourceId));
    }

    @GET
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_POLICIES, acls = RolePermissionAction.READ) })
    @Path("/{resourceId}/schema")
    @Produces(MediaType.APPLICATION_JSON)
    public String getResourceSchema(@PathParam("resourceId") String resourceId) {
        return resourcePluginProvider.getSchema(resourceId);
    }

    @GET
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_POLICIES, acls = RolePermissionAction.READ) })
    @Path("/{resourceId}/documentation")
    @Produces(MediaType.TEXT_PLAIN)
    public String getResourceDocumentation(@PathParam("resourceId") String resourceId) {
        return resourcePluginProvider.getDocumentation(resourceId);
    }
}
