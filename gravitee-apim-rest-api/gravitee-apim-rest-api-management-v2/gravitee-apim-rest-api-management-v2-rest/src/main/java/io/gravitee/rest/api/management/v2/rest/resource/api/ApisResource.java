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
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.api.use_case.CreateV4ApiUseCase;
import io.gravitee.apim.core.api.use_case.ImportApiCRDUseCase;
import io.gravitee.apim.core.api.use_case.ImportApiDefinitionUseCase;
import io.gravitee.apim.core.api.use_case.OAIToImportApiUseCase;
import io.gravitee.apim.core.api.use_case.ValidateApiCRDUseCase;
import io.gravitee.apim.core.api.use_case.VerifyApiHostsUseCase;
import io.gravitee.apim.core.api.use_case.VerifyApiPathsUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.exception.InvalidImageException;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.ImportExportApiMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiCRDSpec;
import io.gravitee.rest.api.management.v2.rest.model.ApiSearchQuery;
import io.gravitee.rest.api.management.v2.rest.model.ApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ImportSwaggerDescriptor;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiHosts;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiHostsResponse;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiPaths;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiPathsResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.ApiSortByParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.gravitee.rest.api.service.v4.exception.InvalidPathException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
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
@Slf4j
@Path("/apis")
public class ApisResource extends AbstractResource {

    private static final String EXPAND_DEPLOYMENT_STATE = "deploymentState";

    @Context
    private ResourceContext resourceContext;

    @Context
    protected UriInfo uriInfo;

    @Inject
    private ApiStateService apiStateService;

    @Inject
    private VerifyApiPathsUseCase verifyApiPathsUsecase;

    @Inject
    private VerifyApiHostsUseCase verifyApiHostsUseCase;

    @Path("{apiId}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }

    @Inject
    private ApiStateDomainService apiStateDomainService;

    @Inject
    private CreateV4ApiUseCase createV4ApiUseCase;

    @Inject
    private ImportApiCRDUseCase importCRDUseCase;

    @Inject
    private ValidateApiCRDUseCase validateApiCRDUseCase;

    @Inject
    private ImportApiDefinitionUseCase importApiDefinitionUseCase;

    @Inject
    private OAIToImportApiUseCase oaiToImportApiUseCase;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.CREATE }) })
    public Response createApi(@Valid @NotNull final CreateApiV4 api) {
        // NOTE: Only for V4 API. V2 API is planned to be supported in the future.
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        AuditInfo audit = AuditInfo
            .builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor
                    .builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();
        var output = createV4ApiUseCase.execute(new CreateV4ApiUseCase.Input(ApiMapper.INSTANCE.map(api), audit));

        boolean isSynchronized = apiStateDomainService.isSynchronized(output.api(), audit);
        return Response
            .created(this.getLocationHeader(output.api().getId()))
            .entity(ApiMapper.INSTANCE.map(output.api(), uriInfo, isSynchronized))
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

    @PUT
    @Path("/_import/crd")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE) })
    public Response createApiWithCRD(@Valid ApiCRDSpec crd, @QueryParam("dryRun") boolean dryRun) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        var input = new ImportApiCRDUseCase.Input(
            AuditInfo
                .builder()
                .organizationId(executionContext.getOrganizationId())
                .environmentId(executionContext.getEnvironmentId())
                .actor(
                    AuditActor
                        .builder()
                        .userId(userDetails.getUsername())
                        .userSource(userDetails.getSource())
                        .userSourceId(userDetails.getSourceId())
                        .build()
                )
                .build(),
            ApiMapper.INSTANCE.map(crd)
        );

        return dryRun
            ? Response.ok(validateApiCRDUseCase.execute(input).status()).build()
            : Response.ok(importCRDUseCase.execute(input).status()).build();
    }

    @POST
    @Path("/_import/swagger")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE) })
    public Response createApiFromSwagger(@Valid @NotNull ImportSwaggerDescriptor descriptor) {
        try {
            var userDetails = getAuthenticatedUserDetails();
            var executionContext = GraviteeContext.getExecutionContext();
            var audit = AuditInfo
                .builder()
                .organizationId(executionContext.getOrganizationId())
                .environmentId(executionContext.getEnvironmentId())
                .actor(
                    AuditActor
                        .builder()
                        .userId(userDetails.getUsername())
                        .userSource(userDetails.getSource())
                        .userSourceId(userDetails.getSourceId())
                        .build()
                )
                .build();
            var importSwaggerDescriptor = ImportSwaggerDescriptorEntity
                .builder()
                .payload(descriptor.getPayload())
                .withDocumentation(Boolean.TRUE.equals(descriptor.getWithDocumentation()))
                .build();

            OAIToImportApiUseCase.Output importOutput = oaiToImportApiUseCase.execute(
                OAIToImportApiUseCase.Input
                    .builder()
                    .importSwaggerDescriptor(importSwaggerDescriptor)
                    .auditInfo(audit)
                    .withDocumentation(Boolean.TRUE.equals(descriptor.getWithDocumentation()))
                    .withOASValidationPolicy(Boolean.TRUE.equals(descriptor.getWithOASValidationPolicy()))
                    .build()
            );

            boolean isSynchronized = apiStateDomainService.isSynchronized(importOutput.apiWithFlows(), audit);

            return Response
                .created(this.getLocationHeader(importOutput.apiWithFlows().getId()))
                .entity(ApiMapper.INSTANCE.map(importOutput.apiWithFlows(), uriInfo, isSynchronized))
                .build();
        } catch (InvalidPathsException e) {
            throw new InvalidPathException("Cannot import API with invalid paths", e);
        }
    }

    @POST
    @Path("/_import/definition")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE) })
    public Response createApiWithDefinition(@Valid ExportApiV4 apiToImport) {
        verifyImage(apiToImport.getApiPicture(), "picture");
        verifyImage(apiToImport.getApiBackground(), "background");

        ImportDefinition importDefinition = ImportExportApiMapper.INSTANCE.toImportDefinition(apiToImport);

        try {
            var userDetails = getAuthenticatedUserDetails();
            var executionContext = GraviteeContext.getExecutionContext();
            var audit = AuditInfo
                .builder()
                .organizationId(executionContext.getOrganizationId())
                .environmentId(executionContext.getEnvironmentId())
                .actor(
                    AuditActor
                        .builder()
                        .userId(userDetails.getUsername())
                        .userSource(userDetails.getSource())
                        .userSourceId(userDetails.getSourceId())
                        .build()
                )
                .build();
            ImportApiDefinitionUseCase.Output output = importApiDefinitionUseCase.execute(
                new ImportApiDefinitionUseCase.Input(importDefinition, audit)
            );

            boolean isSynchronized = apiStateDomainService.isSynchronized(output.apiWithFlows(), audit);

            return Response
                .created(this.getLocationHeader(output.apiWithFlows().getId()))
                .entity(ApiMapper.INSTANCE.map(output.apiWithFlows(), uriInfo, isSynchronized))
                .build();
        } catch (InvalidPathsException e) {
            throw new InvalidPathException("Cannot import API with invalid paths", e);
        }
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
        @QueryParam("expands") Set<String> expands,
        @QueryParam("manageOnly") @DefaultValue("true") boolean manageOnly
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

        apiQueryBuilder.setSort(apiSortByParam.toSortable());

        boolean expandDeploymentState = Objects.nonNull(expands) && expands.contains(EXPAND_DEPLOYMENT_STATE);

        Page<GenericApiEntity> apis = apiSearchService.search(
            GraviteeContext.getExecutionContext(),
            getAuthenticatedUser(),
            isAdmin(),
            apiQueryBuilder,
            paginationParam.toPageable(),
            expandDeploymentState,
            manageOnly
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

    @POST
    @Path("_verify/paths")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public Response verifyPaths(VerifyApiPaths verifyPayload) {
        try {
            verifyApiPathsUsecase.execute(
                new VerifyApiPathsUseCase.Request(
                    verifyPayload.getApiId(),
                    verifyPayload
                        .getPaths()
                        .stream()
                        .map(p ->
                            io.gravitee.apim.core.api.model.Path
                                .builder()
                                .path(p.getPath())
                                .host(p.getHost())
                                .overrideAccess(Boolean.TRUE.equals(p.getOverrideAccess()))
                                .build()
                        )
                        .toList()
                )
            );

            return Response.accepted(VerifyApiPathsResponse.builder().ok(true).build()).build();
        } catch (InvalidPathsException e) {
            return Response.accepted(VerifyApiPathsResponse.builder().ok(false).reason(e.getMessage()).build()).build();
        }
    }

    @POST
    @Path("_verify/hosts")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public Response verifyHosts(VerifyApiHosts verifyPayload) {
        var executionContext = GraviteeContext.getExecutionContext();
        try {
            verifyApiHostsUseCase.execute(
                new VerifyApiHostsUseCase.Input(executionContext.getEnvironmentId(), verifyPayload.getApiId(), verifyPayload.getHosts())
            );
            return Response.accepted(VerifyApiHostsResponse.builder().ok(true).build()).build();
        } catch (Exception e) {
            return Response.accepted(VerifyApiHostsResponse.builder().ok(false).reason(e.getMessage()).build()).build();
        }
    }
}
