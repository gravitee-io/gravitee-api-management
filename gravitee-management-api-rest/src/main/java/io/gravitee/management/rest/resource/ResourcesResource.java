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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.ResourceListItem;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.model.platform.plugin.PluginEntity;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ResourceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/resources")
@Api(tags = {"Plugins"})
public class ResourcesResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ResourceService resourceService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List resource plugins",
            notes = "User must have the MANAGEMENT_API[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of resources", response = ResourceListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_API, acls = RolePermissionAction.READ)
    })
    public Collection<ResourceListItem> listResources(@QueryParam("expand") List<String> expand) {
        Stream<ResourceListItem> stream = resourceService.findAll().stream().map(this::convert);

        if(expand!=null && !expand.isEmpty()) {
            for (String s : expand) {
                switch (s) {
                    case "schema":
                        stream = stream.map(resourceListItem -> {
                            resourceListItem.setSchema(resourceService.getSchema(resourceListItem.getId()));
                            return resourceListItem;
                        });
                        break;
                    default: break;
                }
            }
        }

        return stream
                .sorted(Comparator.comparing(ResourceListItem::getName))
                .collect(Collectors.toList());
    }

    @Path("{resource}")
    public ResourceResource getResourceResource() {
        return resourceContext.getResource(ResourceResource.class);
    }

    private ResourceListItem convert(PluginEntity resource) {
        ResourceListItem item = new ResourceListItem();

        item.setId(resource.getId());
        item.setName(resource.getName());
        item.setDescription(resource.getDescription());
        item.setVersion(resource.getVersion());

        return item;
    }
}
