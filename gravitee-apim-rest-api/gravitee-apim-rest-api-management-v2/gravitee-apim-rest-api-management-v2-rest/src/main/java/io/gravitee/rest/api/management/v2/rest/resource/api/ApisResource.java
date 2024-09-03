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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DEFINITION_VERSION;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TYPE_VALUE;

import com.google.common.base.Strings;
import io.gravitee.common.data.domain.Order;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.exception.InvalidImageException;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.ImportExportApiMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiSearchQuery;
import io.gravitee.rest.api.management.v2.rest.model.ApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.ApiSortByParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.management.v2.rest.security.Permission;
import io.gravitee.rest.api.management.v2.rest.security.Permissions;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/environments/{envId}/apis")
@Slf4j
public class ApisResource extends AbstractResource {

    private static final String EXPAND_DEPLOYMENT_STATE = "deploymentState";

    @Context
    private ResourceContext resourceContext;

    @Context
    protected UriInfo uriInfo;

    @Inject
    private ApiImportExportService apiImportExportService;

    @Inject
    private ApiStateService apiStateService;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.CREATE }) })
    public Response createApi(@Valid @NotNull final CreateApiV4 api) {
        // NOTE: Only for V4 API. V2 API is planned to be supported in the future.
        NewApiEntity newApiEntity = ApiMapper.INSTANCE.map(api);
        ApiEntity newApi = apiServiceV4.create(GraviteeContext.getExecutionContext(), newApiEntity, getAuthenticatedUser());

        boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), newApi);
        return Response
            .created(this.getLocationHeader(newApi.getId()))
            .entity(ApiMapper.INSTANCE.map(newApi, uriInfo, isSynchronized))
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public ApisResponse getApis(@BeanParam @Valid PaginationParam paginationParam, @QueryParam("expands") Set<String> expands) {
        Page<GenericApiEntity> apis = apiServiceV4.findAll(
            GraviteeContext.getExecutionContext(),
            getAuthenticatedUser(),
            isAdmin(),
            expands,
            new SortableImpl("name", true),
            paginationParam.toPageable()
        );

        long totalCount = apis.getTotalElements();
        Integer pageItemsCount = Math.toIntExact(apis.getPageElements());
        return new ApisResponse()
            .data(
                ApiMapper.INSTANCE.map(
                    apis.getContent(),
                    uriInfo,
                    api -> {
                        if (expands == null || expands.isEmpty() || !expands.contains(EXPAND_DEPLOYMENT_STATE)) {
                            return null;
                        }
                        return apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), api);
                    }
                )
            )
            .pagination(PaginationInfo.computePaginationInfo(totalCount, pageItemsCount, paginationParam))
            .links(computePaginationLinks(totalCount, paginationParam));
    }

    @POST
    @Path("/_import/definition")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE) })
    public Response createApiWithDefinition(@Valid ExportApiV4 apiToImport) {
        verifyImage(apiToImport.getApiPicture(), "picture");
        verifyImage(apiToImport.getApiBackground(), "background");

        ExportApiEntity exportApiEntity = ImportExportApiMapper.INSTANCE.map(apiToImport);
        GenericApiEntity fromExportedApi = apiImportExportService.createFromExportedApi(
            GraviteeContext.getExecutionContext(),
            exportApiEntity,
            getAuthenticatedUser()
        );

        boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), fromExportedApi);

        return Response
            .created(this.getLocationHeader(fromExportedApi.getId()))
            .entity(ApiMapper.INSTANCE.map(fromExportedApi, uriInfo, isSynchronized))
            .build();
    }

    private static void verifyImage(String imageContent, String imageUsage) {
        try {
            ImageUtils.verify(imageContent);
        } catch (InvalidImageException e) {
            log.warn("Error while parsing {} while importing api", imageUsage, e);
            throw new BadRequestException("Invalid image format for api " + imageUsage);
        }
    }

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public ApisResponse searchApis(
        @BeanParam @Valid PaginationParam paginationParam,
        @BeanParam ApiSortByParam apiSortByParam,
        final @Valid @NotNull ApiSearchQuery apiSearchQuery,
        @QueryParam("expands") Set<String> expands
    ) {
        apiSortByParam.validate();

        QueryBuilder<ApiEntity> apiQueryBuilder = QueryBuilder.create(ApiEntity.class);

        if (!Strings.isNullOrEmpty(apiSearchQuery.getQuery())) {
            apiQueryBuilder.setQuery(apiSearchQuery.getQuery());
        }

        if (Objects.nonNull(apiSearchQuery.getIds()) && !apiSearchQuery.getIds().isEmpty()) {
            apiQueryBuilder.addFilter(FIELD_TYPE_VALUE, apiSearchQuery.getIds());
        }

        if (Objects.nonNull(apiSearchQuery.getDefinitionVersion())) {
            apiQueryBuilder.addFilter(
                FIELD_DEFINITION_VERSION,
                ApiMapper.INSTANCE.mapDefinitionVersion(apiSearchQuery.getDefinitionVersion()).getLabel()
            );
        }

        if (Objects.nonNull(apiSortByParam)) {
            apiQueryBuilder.setSort(apiSortByParam.toSortable());
        }

        boolean expandDeploymentState = Objects.nonNull(expands) && expands.contains(EXPAND_DEPLOYMENT_STATE);

        Page<GenericApiEntity> apis = apiSearchService.search(
            GraviteeContext.getExecutionContext(),
            getAuthenticatedUser(),
            isAdmin(),
            apiQueryBuilder,
            paginationParam.toPageable(),
            expandDeploymentState
        );

        long totalCount = apis.getTotalElements();
        Integer pageItemsCount = Math.toIntExact(apis.getPageElements());
        return new ApisResponse()
            .data(
                ApiMapper.INSTANCE.map(
                    apis.getContent(),
                    uriInfo,
                    api -> expandDeploymentState ? apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), api) : null
                )
            )
            .pagination(PaginationInfo.computePaginationInfo(totalCount, pageItemsCount, paginationParam))
            .links(computePaginationLinks(totalCount, paginationParam));
    }

    @Path("{apiId}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }
}
