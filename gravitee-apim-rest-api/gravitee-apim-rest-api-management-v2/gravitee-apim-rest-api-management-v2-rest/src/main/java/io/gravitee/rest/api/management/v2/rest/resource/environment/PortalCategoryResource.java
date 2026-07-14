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

import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_category.use_case.DeletePortalCategoryUseCase;
import io.gravitee.apim.core.portal_category.use_case.UpdatePortalCategoryUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalCategoryMapper;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalCategory;
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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.CustomLog;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class PortalCategoryResource extends AbstractResource {

    @Inject
    private UpdatePortalCategoryUseCase updatePortalCategoryUseCase;

    @Inject
    private DeletePortalCategoryUseCase deletePortalCategoryUseCase;

    private static final PortalCategoryMapper mapper = PortalCategoryMapper.INSTANCE;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public Response updatePortalCategory(
        @PathParam("categoryId") String categoryId,
        @Valid @NotNull final UpdatePortalCategory updatePortalCategory
    ) {
        var output = updatePortalCategoryUseCase.execute(
            new UpdatePortalCategoryUseCase.Input(
                GraviteeContext.getCurrentEnvironment(),
                PortalCategoryId.of(categoryId),
                mapper.map(updatePortalCategory)
            )
        );

        return Response.ok(mapper.map(output.portalCategory())).build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.DELETE) })
    public Response deletePortalCategory(@PathParam("categoryId") String categoryId) {
        deletePortalCategoryUseCase.execute(
            new DeletePortalCategoryUseCase.Input(GraviteeContext.getCurrentEnvironment(), PortalCategoryId.of(categoryId))
        );

        return Response.noContent().build();
    }
}
