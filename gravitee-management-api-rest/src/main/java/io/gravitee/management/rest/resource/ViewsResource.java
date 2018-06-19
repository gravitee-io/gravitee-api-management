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
import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.enhancer.ViewEnhancer;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ViewService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Views"})
public class ViewsResource extends AbstractResource  {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private ViewService viewService;

    @Autowired
    private ViewEnhancer viewEnhancer;

    @Autowired
    private ApiService apiService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ViewEntity> list(@QueryParam("all") boolean all)  {
        Set<ApiEntity> apis;
        if (isAdmin()) {
            apis = apiService.findAll();
        } else if (isAuthenticated()) {
            apis = apiService.findByUser(getAuthenticatedUser(), null);
        } else {
            apis = apiService.findByVisibility(Visibility.PUBLIC);
        }

        boolean viewAll = (all && hasPermission(RolePermission.PORTAL_VIEW, RolePermissionAction.UPDATE, RolePermissionAction.CREATE, RolePermissionAction.DELETE));

        return viewService.findAll()
                .stream()
                .filter(v -> viewAll || !v.isHidden())
                .sorted(Comparator.comparingInt(ViewEntity::getOrder))
                .map(v -> viewEnhancer.enhance(apis).apply(v))
                .collect(Collectors.toList());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/default")
    public ViewEntity getDefault() {
        List<ViewEntity> views = this.list(false);
        return views.
                stream().
                filter(ViewEntity::isDefaultView).
                findFirst().
                orElse(views.
                        stream().
                        findFirst().
                        orElse(null));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_VIEW, acls = RolePermissionAction.CREATE)
    })
    public ViewEntity create(@Valid @NotNull final NewViewEntity view) {
        return viewService.create(view);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_VIEW, acls = RolePermissionAction.UPDATE)
    })
    public List<ViewEntity> update(@Valid @NotNull final List<UpdateViewEntity> views) {
        return viewService.update(views);
    }

    @Path("{id}")
    public ViewResource getViewResource() {
        return resourceContext.getResource(ViewResource.class);
    }
}
