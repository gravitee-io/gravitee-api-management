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
package io.gravitee.rest.api.management.v2.rest.resource.category.api;

import io.gravitee.apim.core.category.use_case.UpdateCategoryApiOrderUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.CategoryApiMapper;
import io.gravitee.rest.api.management.v2.rest.model.UpdateCategoryApi;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Objects;

public class CategoryApiResource extends AbstractResource {

    @PathParam("categoryIdOrKey")
    String categoryIdOrKey;

    @PathParam("apiId")
    String apiId;

    @Inject
    UpdateCategoryApiOrderUseCase updateCategoryApiOrderUseCase;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CATEGORY, acls = { RolePermissionAction.UPDATE }) })
    public Response updateCategoryApi(@Valid UpdateCategoryApi updateCategoryApi) {
        if (Objects.isNull(updateCategoryApi)) {
            throw new BadRequestException("Order cannot be null");
        }

        var result = this.updateCategoryApiOrderUseCase.execute(
            new UpdateCategoryApiOrderUseCase.Input(
                GraviteeContext.getExecutionContext(),
                categoryIdOrKey,
                apiId,
                getAuthenticatedUser(),
                isAdmin(),
                updateCategoryApi.getOrder().intValue()
            )
        ).result();

        return Response.ok().entity(CategoryApiMapper.INSTANCE.map(result.apiCategoryOrder(), result.api())).build();
    }
}
