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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.rest.api.model.Visibility.PUBLIC;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.use_case.VerifyApiPathsUseCase;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.rest.model.Pageable;
import io.gravitee.rest.api.management.rest.model.PagedResult;
import io.gravitee.rest.api.management.rest.model.wrapper.ApiListItemPagedResult;
import io.gravitee.rest.api.management.rest.resource.param.ApisParam;
import io.gravitee.rest.api.management.rest.resource.param.VerifyApiParam;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiCRDEntity;
import io.gravitee.rest.api.model.api.ApiCRDStatusEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.ApiListItem;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.api.ApiValidationResult;
import io.gravitee.rest.api.model.api.NewApiEntity;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApiCRDService;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApiValidationService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.Hook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "APIs")
public class ApisResource extends AbstractResource {

    @Inject
    protected ApiDuplicatorService apiDuplicatorService;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiService apiService;

    @Inject
    private ApiCRDService apiCRDService;

    @Inject
    @Qualifier("apiV2ValidationService")
    private ApiValidationService apiValidationService;

    @Inject
    private SwaggerService swaggerService;

    @Inject
    private TopApiService topApiService;

    @Inject
    private RatingService ratingService;

    @Inject
    private CategoryService categoryService;

    @Inject
    private FlowService flowService;

    @Inject
    private VerifyApiPathsUseCase verifyApiPathsUsecase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List APIs", description = "List all the APIs accessible to the current user.")
    @ApiResponse(
        responseCode = "200",
        description = "List accessible APIs for current user",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ApiListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Collection<ApiListItem> getApis(@BeanParam final ApisParam apisParam, @QueryParam("ids") final List<String> ids) {
        return getApis(apisParam, ids, null).getData();
    }

    @GET
    @Path("_paged")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List APIs with pagination",
        description = "List all the APIs accessible to the current user with pagination.",
        operationId = "getApisPaged"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Page of APIs for current user",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiListItemPagedResult.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ApiListItemPagedResult getApis(
        @BeanParam final ApisParam apisParam,
        @QueryParam("ids") final List<String> ids,
        @Valid @BeanParam Pageable pageable
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final ApiQuery apiQuery = new ApiQuery();
        if (ids != null && !ids.isEmpty()) {
            apiQuery.setIds(ids);
        }
        if (apisParam.getGroup() != null) {
            apiQuery.setGroups(singletonList(apisParam.getGroup()));
        }
        apiQuery.setContextPath(apisParam.getContextPath());
        apiQuery.setLabel(apisParam.getLabel());
        apiQuery.setVersion(apisParam.getVersion());
        apiQuery.setName(apisParam.getName());
        apiQuery.setTag(apisParam.getTag());
        apiQuery.setState(apisParam.getState());
        apiQuery.setCrossId(apisParam.getCrossId());
        if (apisParam.getCategory() != null) {
            apiQuery.setCategory(categoryService.findById(apisParam.getCategory(), executionContext.getEnvironmentId()).getId());
        }

        Sortable sortable = null;
        if (apisParam.getOrder() != null) {
            sortable = new SortableImpl(apisParam.getOrder().getField(), apisParam.getOrder().isOrder());
        }

        io.gravitee.rest.api.model.common.Pageable commonPageable = null;

        if (pageable != null) {
            commonPageable = pageable.toPageable();
        }

        final Page<ApiEntity> apis;

        if (isAdmin()) {
            apis = apiService.search(executionContext, apiQuery, sortable, commonPageable);
        } else {
            if (apisParam.isPortal() || apisParam.isTop()) {
                apiQuery.setLifecycleStates(singletonList(PUBLISHED));
            }
            if (isAuthenticated()) {
                apis =
                    apiService.findByUser(
                        executionContext,
                        getAuthenticatedUser(),
                        apiQuery,
                        sortable,
                        commonPageable,
                        !apisParam.isPortal()
                    );
            } else {
                apiQuery.setVisibility(PUBLIC);
                apis = apiService.search(executionContext, apiQuery, sortable, commonPageable);
            }
        }

        final boolean isRatingServiceEnabled = ratingService.isEnabled(executionContext);

        if (apisParam.isTop()) {
            final List<String> visibleApis = apis.getContent().stream().map(ApiEntity::getId).collect(toList());
            return new ApiListItemPagedResult(
                topApiService
                    .findAll(executionContext)
                    .stream()
                    .filter(topApi -> visibleApis.contains(topApi.getApi()))
                    .map(topApiEntity -> apiService.findById(executionContext, topApiEntity.getApi()))
                    .map(apiEntity -> this.convert(apiEntity, isRatingServiceEnabled))
                    .collect(toList()),
                apis.getPageNumber(),
                (int) apis.getPageElements(),
                (int) apis.getTotalElements()
            );
        }

        return new ApiListItemPagedResult(
            apis.getContent().stream().map(apiEntity -> this.convert(apiEntity, isRatingServiceEnabled)).collect(toList()),
            apis.getPageNumber(),
            (int) apis.getPageElements(),
            (int) apis.getTotalElements()
        );
    }

    /**
     * Create a new API for the authenticated user.
     * @param newApiEntity
     * @return
     */
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

    @POST
    @Path("import")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create an API by importing an API definition",
        description = "Create an API by importing an existing API definition in JSON format."
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response importApiDefinition(
        @RequestBody(required = true) @Valid @NotNull JsonNode apiDefinition,
        @QueryParam("definitionVersion") @DefaultValue("2.0.0") String definitionVersion
    ) {
        return importApiDefinitionOrUrl(apiDefinition, definitionVersion);
    }

    @POST
    @Path("import-url")
    @Consumes({ MediaType.TEXT_PLAIN })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create an API by importing a URL pointing to an API definition",
        description = "Create an API by importing an existing API definition via a URL"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response importApiDefinitionUrl(
        @RequestBody(required = true) @Valid @NotNull String apiDefinitionOrUrl,
        @QueryParam("definitionVersion") @DefaultValue("2.0.0") String definitionVersion
    ) {
        return importApiDefinitionOrUrl(apiDefinitionOrUrl, definitionVersion);
    }

    @PUT
    @Path("import-crd")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create an API by importing an API CRD definition",
        description = "Create an API by importing an API CRD definition"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response importApiDefinitionCrd(
        @RequestBody(required = true) @Valid @NotNull ApiCRDEntity api,
        @QueryParam("dryRun") boolean dryRun
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApiValidationResult<ApiCRDEntity> validationResult = apiValidationService.validateAndSanitizeApiDefinitionCRD(
            executionContext,
            api
        );

        if (dryRun) {
            return Response
                .ok(
                    new ApiCRDStatusEntity(
                        executionContext.getOrganizationId(),
                        executionContext.getEnvironmentId(),
                        api.getId(),
                        api.getCrossId(),
                        api.getState(),
                        validationResult,
                        api.planIdMapping()
                    )
                )
                .build();
        }

        if (CollectionUtils.isNotEmpty(validationResult.getSevere())) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).entity(validationResult).build();
        }

        ApiCRDStatusEntity response = apiCRDService.importApiDefinitionCRD(executionContext, validationResult.getSanitizedApi());
        response.setErrors(validationResult);

        return Response.ok(response).build();
    }

    @Deprecated(since = "3.18.0", forRemoval = true)
    @POST
    @Path("import")
    @Consumes({ MediaType.TEXT_PLAIN })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Deprecated - Create an API by importing an API definition",
        description = "Old endpoint to create an API by importing an existing API definition via a URL - prefer `/apis/import-url`"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response deprecatedImportApiDefinitionUrl(
        @RequestBody(required = true) @Valid @NotNull String apiDefinitionOrUrl,
        @QueryParam("definitionVersion") @DefaultValue("2.0.0") String definitionVersion
    ) {
        return importApiDefinitionOrUrl(apiDefinitionOrUrl, definitionVersion);
    }

    private Response importApiDefinitionOrUrl(Object apiDefinitionOrUrl, String definitionVersion) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApiEntity imported = apiDuplicatorService.createWithImportedDefinition(executionContext, apiDefinitionOrUrl);

        if (
            DefinitionVersion.valueOfLabel(definitionVersion).equals(DefinitionVersion.V2) &&
            DefinitionVersion.V1.getLabel().equals(imported.getGraviteeDefinitionVersion())
        ) {
            return Response.ok(apiService.migrate(executionContext, imported.getId())).build();
        }

        return Response.ok(imported).build();
    }

    @POST
    @Path("import/swagger")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create an API definition from a Swagger descriptor")
    @ApiResponse(
        responseCode = "201",
        description = "API definition from Swagger descriptor",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE) })
    public Response importSwaggerApi(
        @Parameter(name = "swagger", required = true) @Valid @NotNull ImportSwaggerDescriptorEntity swaggerDescriptor,
        @QueryParam("definitionVersion") @DefaultValue("2.0.0") String definitionVersion
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final SwaggerApiEntity swaggerApiEntity = swaggerService.createAPI(
            executionContext,
            swaggerDescriptor,
            DefinitionVersion.valueOfLabel(definitionVersion)
        );
        final ApiEntity api = apiService.createFromSwagger(executionContext, swaggerApiEntity, getAuthenticatedUser(), swaggerDescriptor);
        return Response
            .created(URI.create(this.uriInfo.getRequestUri().getRawPath().replaceAll("import/swagger", "") + api.getId()))
            .entity(api)
            .build();
    }

    @POST
    @Path("verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check if an API match the following criteria", deprecated = true)
    @ApiResponse(
        responseCode = "200",
        description = "No API match the following criteria",
        content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
    )
    @ApiResponse(responseCode = "400", description = "API already exist with the following criteria")
    @Permissions(
        { @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.CREATE, RolePermissionAction.UPDATE }) }
    )
    @Deprecated
    public Response verifyApi(@Valid VerifyApiParam verifyApiParam) {
        try {
            verifyApiPathsUsecase.execute(
                new VerifyApiPathsUseCase.Request(
                    verifyApiParam.getApiId(),
                    List.of(io.gravitee.apim.core.api.model.Path.builder().path(verifyApiParam.getContextPath()).build())
                )
            );
            return Response.ok("API context [" + verifyApiParam.getContextPath() + "] is available").build();
        } catch (InvalidPathsException e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/hooks")
    @Operation(summary = "Get the list of available hooks")
    @Produces(MediaType.APPLICATION_JSON)
    public Hook[] getApiHooks() {
        return Arrays.stream(ApiHook.values()).filter(h -> !h.isHidden()).toArray(Hook[]::new);
    }

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Search for API using the search engine")
    @ApiResponse(
        responseCode = "200",
        description = "List accessible APIs for current user",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ApiListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response searchApis(@Parameter(name = "q", required = true) @NotNull @QueryParam("q") String query) {
        try {
            return Response.ok().entity(this.searchPagedApis(query, new ApisOrderParam("name"), true, null).getData()).build();
        } catch (TechnicalManagementException te) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(te).build();
        }
    }

    @POST
    @Path("_search/_paged")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Search for API using the search engine")
    @ApiResponse(
        responseCode = "200",
        description = "List accessible APIs for current user",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiListItem.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public PagedResult<ApiListItem> searchPagedApis(
        @Parameter(name = "q", required = true) @NotNull @QueryParam("q") String query,
        @Parameter(
            name = "order",
            schema = @Schema(
                implementation = String.class,
                description = "By default, sort is ASC. If *field* starts with '-', the order sort is DESC. Currently, only **name** and **paths** are supported"
            )
        ) @QueryParam("order") final ApisOrderParam apisOrderParam,
        @Parameter(
            name = "manageOnly",
            description = "By default only APIs that the user can manage are returned. If set to false, all APIs that the user can view are returned."
        ) @QueryParam("manageOnly") @DefaultValue("true") final boolean manageOnly,
        @Valid @BeanParam Pageable pageable
    ) {
        Map<String, Object> filters = new HashMap<>();

        io.gravitee.rest.api.model.common.Pageable commonPageable = null;

        if (pageable != null) {
            commonPageable = pageable.toPageable();
        }

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (!isAdmin()) {
            filters.put("api", apiAuthorizationService.findIdsByUser(executionContext, getAuthenticatedUser(), manageOnly));
        }

        final boolean isRatingServiceEnabled = ratingService.isEnabled(executionContext);

        final Page<ApiEntity> apis = apiService.search(
            executionContext,
            query,
            filters,
            apisOrderParam != null ? apisOrderParam.toSortable() : null,
            commonPageable
        );

        return new PagedResult<>(
            apis.getContent().stream().map(apiEntity -> this.convert(apiEntity, isRatingServiceEnabled)).collect(toList()),
            apis.getPageNumber(),
            (int) apis.getPageElements(),
            (int) apis.getTotalElements()
        );
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("import")
    @Operation(
        summary = "Update the API from the API definition",
        description = "Update the API from the API definition in JSON format either with json or via an URL"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully updated from API definition",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response updateWithDefinition(@RequestBody(required = true) @Valid @NotNull Object apiDefinitionOrUrl) {
        ApiEntity updatedApi = apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), apiDefinitionOrUrl);
        return Response
            .ok(updatedApi)
            .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
            .lastModified(updatedApi.getUpdatedAt())
            .build();
    }

    @Path("{api}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }

    @Path("{api}/media")
    public ApiMediaResource getApiMediaResource() {
        return resourceContext.getResource(ApiMediaResource.class);
    }

    private ApiListItem convert(ApiEntity api, boolean isRatingServiceEnabled) {
        final ApiListItem apiItem = new ApiListItem();

        apiItem.setId(api.getId());
        apiItem.setName(api.getName());
        apiItem.setVersion(api.getVersion());
        apiItem.setDescription(api.getDescription());

        final UriBuilder ub = uriInfo.getBaseUriBuilder();
        final UriBuilder uriBuilder = ub
            .path("organizations")
            .path(GraviteeContext.getCurrentOrganization())
            .path("environments")
            .path(GraviteeContext.getCurrentEnvironment())
            .path("apis")
            .path(api.getId())
            .path("picture");

        // force browser to get if updated
        uriBuilder.queryParam("hash", api.getUpdatedAt().getTime());

        apiItem.setPictureUrl(uriBuilder.build().toString());
        apiItem.setCategories(api.getCategories());
        apiItem.setCreatedAt(api.getCreatedAt());
        apiItem.setUpdatedAt(api.getUpdatedAt());
        apiItem.setLabels(api.getLabels());
        apiItem.setPrimaryOwner(api.getPrimaryOwner());

        apiItem.setHasHealthCheckEnabled(apiService.hasHealthCheckEnabled(api, false));

        if (api.getVisibility() != null) {
            apiItem.setVisibility(Visibility.valueOf(api.getVisibility().toString()));
        }

        if (api.getState() != null) {
            apiItem.setState(Lifecycle.State.valueOf(api.getState().toString()));
        }

        if (api.getProxy() != null) {
            apiItem.setVirtualHosts(api.getProxy().getVirtualHosts());
        }

        if (isRatingServiceEnabled) {
            final RatingSummaryEntity ratingSummary = ratingService.findSummaryByApi(GraviteeContext.getExecutionContext(), api.getId());
            apiItem.setRate(ratingSummary.getAverageRate());
            apiItem.setNumberOfRatings(ratingSummary.getNumberOfRatings());
        }
        apiItem.setTags(api.getTags());

        if (api.getLifecycleState() != null) {
            apiItem.setLifecycleState(ApiLifecycleState.valueOf(api.getLifecycleState().toString()));
        }

        if (api.getWorkflowState() != null) {
            apiItem.setWorkflowState(WorkflowState.valueOf(api.getWorkflowState().toString()));
        }

        // Issue https://github.com/gravitee-io/issues/issues/3356
        if (api.getProxy().getVirtualHosts() != null && !api.getProxy().getVirtualHosts().isEmpty()) {
            apiItem.setContextPath(api.getProxy().getVirtualHosts().get(0).getPath());
        }

        if (api.getDefinitionContext() != null && !api.getDefinitionContext().getOrigin().isEmpty()) {
            apiItem.setDefinitionContext(api.getDefinitionContext());
        }

        if (api.getDefinitionVersion() != null) {
            apiItem.setGraviteeDefinitionVersion(api.getDefinitionVersion().getLabel());
        }

        return apiItem;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("schema")
    @Operation(summary = "Get the API configuration schema")
    @ApiResponse(
        responseCode = "200",
        description = "API definition",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "string"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public Response getApiFlowSchemaForm() {
        return Response.ok(flowService.getApiFlowSchemaForm()).build();
    }
}
