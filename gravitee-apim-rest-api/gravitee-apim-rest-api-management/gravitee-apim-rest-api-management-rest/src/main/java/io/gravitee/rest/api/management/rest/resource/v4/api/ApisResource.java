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
package io.gravitee.rest.api.management.rest.resource.v4.api;

import static io.gravitee.rest.api.model.Visibility.PUBLIC;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.Pageable;
import io.gravitee.rest.api.management.rest.model.wrapper.ApiListItemPagedResult;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.resource.param.ApisParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.api.ApiListItem;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.rest.api.service.v4.ApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "APIs")
public class ApisResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiService apiService;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create an API", description = "User must have API_PUBLISHER or ADMIN role to create an API.")
    @ApiResponse(
        responseCode = "201",
        description = "API successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE) })
    public Response createApi(@Parameter(name = "api", required = true) @Valid @NotNull final NewApiEntity newApiEntity)
        throws ApiAlreadyExistsException {
        ApiEntity newApi = apiService.create(GraviteeContext.getExecutionContext(), newApiEntity, getAuthenticatedUser());
        if (newApi != null) {
            return Response.created(this.getLocationHeader(newApi.getId())).entity(newApi).build();
        }

        return Response.serverError().build();
    }

    @Path("{api}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }
}
