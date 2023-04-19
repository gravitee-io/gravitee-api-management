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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import com.google.common.base.Strings;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiSearchQuery;
import io.gravitee.rest.api.management.v2.rest.model.ApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiV4;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.management.v2.rest.security.Permission;
import io.gravitee.rest.api.management.v2.rest.security.Permissions;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.query.QueryBuilder;

import javax.validation.*;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Objects;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/environments/{envId}/apis")
public class ApisResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.CREATE }) })
    public Response createApi(@Valid @NotNull final CreateApiV4 api) {
        // NOTE: Only for V4 API. V2 API is planned to be supported in the future.
        NewApiEntity newApiEntity = ApiMapper.INSTANCE.convert(api);
        ApiEntity newApi = apiServiceV4.create(GraviteeContext.getExecutionContext(), newApiEntity, getAuthenticatedUser());
        return Response.created(this.getLocationHeader(newApi.getId())).entity(ApiMapper.INSTANCE.convert(newApi)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public ApisResponse getApis(@BeanParam @Valid PaginationParam paginationParam) {
        Page<GenericApiEntity> apis = apiServiceV4.findAll(
            GraviteeContext.getExecutionContext(),
            getAuthenticatedUser(),
            isAdmin(),
            paginationParam.toPageable()
        );

        return new ApisResponse()
            .data(ApiMapper.INSTANCE.convert(apis.getContent()))
            .pagination(
                computePaginationInfo(Math.toIntExact(apis.getTotalElements()), Math.toIntExact(apis.getPageElements()), paginationParam)
            )
            .links(computePaginationLinks(Math.toIntExact(apis.getTotalElements()), paginationParam));
    }

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public ApisResponse searchApis(@BeanParam @Valid PaginationParam paginationParam, @QueryParam("orderBy") String apiOrderBy, final @Valid @NotNull ApiSearchQuery apiSearchQuery) {
        QueryBuilder<ApiEntity> apiQueryBuilder = QueryBuilder.create(ApiEntity.class);

        if (!Strings.isNullOrEmpty(apiSearchQuery.getQuery())) {
            apiQueryBuilder.setQuery(apiSearchQuery.getQuery());
        }

        if (Objects.nonNull(apiSearchQuery.getIds()) && !apiSearchQuery.getIds().isEmpty()) {
            apiQueryBuilder.addFilter("api", apiSearchQuery.getIds());
        }

        Sortable sortable = null;
        if (Objects.nonNull(apiOrderBy)) {
            boolean isAsc = !apiOrderBy.startsWith("-");
            String field = apiOrderBy.replace("-", "");

            sortable = new SortableImpl(field, isAsc);
        }

        Page<GenericApiEntity> apis = apiSearchService.search(
            GraviteeContext.getExecutionContext(),
            getAuthenticatedUser(),
            isAdmin(),
            apiQueryBuilder,
            paginationParam.toPageable(),
            sortable
        );

        return new ApisResponse()
            .data(ApiMapper.INSTANCE.convert(apis.getContent()))
            .pagination(
                computePaginationInfo(Math.toIntExact(apis.getTotalElements()), Math.toIntExact(apis.getPageElements()), paginationParam)
            )
            .links(computePaginationLinks(Math.toIntExact(apis.getTotalElements()), paginationParam));
    }

    @Path("{apiId}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }
}
