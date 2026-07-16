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

import io.gravitee.apim.core.portal_category.use_case.CreatePortalCategoryUseCase;
import io.gravitee.apim.core.portal_category.use_case.ListPortalCategoriesUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalCategoryMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalCategory;
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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lombok.CustomLog;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class PortalCategoriesResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreatePortalCategoryUseCase createPortalCategoryUseCase;

    @Inject
    private ListPortalCategoriesUseCase listPortalCategoriesUseCase;

    private static final PortalCategoryMapper mapper = PortalCategoryMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.READ) })
    public Response getPortalCategories() {
        var output = listPortalCategoriesUseCase.execute(new ListPortalCategoriesUseCase.Input(GraviteeContext.getCurrentEnvironment()));

        return Response.ok(mapper.mapList(output.portalCategories())).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = { RolePermissionAction.CREATE }) })
    public Response createPortalCategory(@Valid @NotNull final CreatePortalCategory createPortalCategory) {
        var output = createPortalCategoryUseCase.execute(
            new CreatePortalCategoryUseCase.Input(GraviteeContext.getCurrentEnvironment(), mapper.map(createPortalCategory))
        );

        return Response.created(this.getLocationHeader(output.portalCategory().getId().toString()))
            .entity(mapper.map(output.portalCategory()))
            .build();
    }

    @Path("{categoryId}")
    public PortalCategoryResource getPortalCategoryResource() {
        return resourceContext.getResource(PortalCategoryResource.class);
    }
}
