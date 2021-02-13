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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.rest.resource.param.ApisParam;
import io.gravitee.rest.api.management.rest.resource.param.VerifyApiParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.*;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.Hook;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.*;

import static io.gravitee.rest.api.model.Visibility.PUBLIC;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"APIs"})
public class ApisResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiService apiService;
    @Inject
    private SwaggerService swaggerService;
    @Inject
    private TopApiService topApiService;
    @Inject
    private RatingService ratingService;
    @Inject
    private VirtualHostService virtualHostService;
    @Inject
    private CategoryService categoryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "List APIs",
            notes = "List all the APIs accessible to the current user or only public APIs for non authenticated users.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List accessible APIs for current user", response = ApiListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<ApiListItem> getApis(@BeanParam final ApisParam apisParam) {

        final ApiQuery apiQuery = new ApiQuery();
        if (apisParam.getGroup() != null) {
            apiQuery.setGroups(singletonList(apisParam.getGroup()));
        }
        apiQuery.setContextPath(apisParam.getContextPath());
        apiQuery.setLabel(apisParam.getLabel());
        apiQuery.setVersion(apisParam.getVersion());
        apiQuery.setName(apisParam.getName());
        apiQuery.setTag(apisParam.getTag());
        apiQuery.setState(apisParam.getState());
        if (apisParam.getCategory() != null) {
            apiQuery.setCategory(categoryService.findById(apisParam.getCategory()).getId());
        }

        final Collection<ApiEntity> apis;
        if (isAdmin()) {
            apis = apiService.search(apiQuery);
        } else {
            if (apisParam.isPortal() || apisParam.isTop()) {
                apiQuery.setLifecycleStates(singletonList(PUBLISHED));
            }
            if (isAuthenticated()) {
                apis = apiService.findByUser(getAuthenticatedUser(), apiQuery, false);
            } else {
                apiQuery.setVisibility(PUBLIC);
                apis = apiService.search(apiQuery);
            }
        }

        if (apisParam.isTop()) {
            final List<String> visibleApis = apis.stream().map(ApiEntity::getId).collect(toList());
            return topApiService.findAll().stream()
                    .filter(topApi -> visibleApis.contains(topApi.getApi()))
                    .map(topApiEntity -> apiService.findById(topApiEntity.getApi()))
                    .map(this::convert)
                    .collect(toList());
        }

        return apis.stream()
                .map(this::convert)
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .collect(toList());
    }

    /**
     * Create a new API for the authenticated user.
     * @param newApiEntity
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Create an API",
            notes = "User must have API_PUBLISHER or ADMIN role to create an API.")
    @ApiResponses({
            @ApiResponse(code = 201, message = "API successfully created"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE)
    })
    public Response createApi(
            @ApiParam(name = "api", required = true)
            @Valid @NotNull final NewApiEntity newApiEntity) throws ApiAlreadyExistsException {
        ApiEntity newApi = apiService.create(newApiEntity, getAuthenticatedUser());
        if (newApi != null) {

            return Response
                    .created(this.getLocationHeader(newApi.getId()))
                    .entity(newApi)
                    .build();
        }

        return Response.serverError().build();
    }

    @POST
    @Path("import")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Create an API by importing an API definition",
            notes = "Create an API by importing an existing API definition in JSON format")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API successfully created"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.UPDATE)
    })
    public Response importApiDefinition(
            @ApiParam(name = "definition", required = true) @Valid @NotNull String apiDefinition) {

        return Response.ok(apiService.createWithImportedDefinition(null, apiDefinition, getAuthenticatedUser())).build();
    }

    @POST
    @Path("import/swagger")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create an API definition from a Swagger descriptor")
    @ApiResponses({
            @ApiResponse(code = 201, message = "API definition from Swagger descriptor", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE)
    })
    public Response importSwaggerApi(
            @ApiParam(name = "swagger", required = true) @Valid @NotNull ImportSwaggerDescriptorEntity swaggerDescriptor) {
        final ApiEntity api = apiService.create(swaggerService.createAPI(swaggerDescriptor), getAuthenticatedUser(), swaggerDescriptor);
        return Response
                .created(URI.create(this.uriInfo.getRequestUri().getRawPath().replaceAll("import/swagger", "") + api.getId()))
                .entity(api)
                .build();
    }

    @POST
    @Path("verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check if an API match the following criteria")
    @ApiResponses({
            @ApiResponse(code = 200, message = "No API match the following criteria"),
            @ApiResponse(code = 400, message = "API already exist with the following criteria")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE)
    })
    public Response verifyApi(@Valid VerifyApiParam verifyApiParam) {
        // TODO : create verify service to query repository with criteria
        virtualHostService.validate(Collections.singletonList(new VirtualHost(verifyApiParam.getContextPath())));
        return Response.ok().entity("API context [" + verifyApiParam.getContextPath() + "] is available").build();
    }

    @GET
    @Path("/hooks")
    @ApiOperation(value = "Get the list of available hooks")
    @Produces(MediaType.APPLICATION_JSON)
    public Hook[] getApiHooks() {
        return Arrays.stream(ApiHook.values()).filter(h -> !h.isHidden()).toArray(Hook[]::new);
    }

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Search for API using the search engine")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List accessible APIs for current user", response = ApiListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response searchApis(@ApiParam(name = "q", required = true) @NotNull @QueryParam("q") String query) {
        try {
            final ApiQuery apiQuery = new ApiQuery();
            Map<String, Object> filters = new HashMap<>();
            
            if (!isAdmin()) {
                final Collection<ApiEntity> apis = apiService.findByUser(getAuthenticatedUser(), apiQuery, false);
                filters.put("api", apis.stream().map(ApiEntity::getId).collect(toSet()));
            }

            return Response.ok().entity(apiService.search(query, filters)
                    .stream()
                    .map(this::convert)
                    .collect(toList())).build();
        } catch (TechnicalException te) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(te).build();
        }
    }

    @Path("{api}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }

    @Path("{api}/media")
    public ApiMediaResource getApiMediaResource() {
        return resourceContext.getResource(ApiMediaResource.class);
    }

    private ApiListItem convert(ApiEntity api) {
        final ApiListItem apiItem = new ApiListItem();

        apiItem.setId(api.getId());
        apiItem.setName(api.getName());
        apiItem.setVersion(api.getVersion());
        apiItem.setDescription(api.getDescription());

        final UriBuilder ub = uriInfo.getBaseUriBuilder();
        final UriBuilder uriBuilder = ub.path("organizations").path(GraviteeContext.getCurrentOrganization())
                .path("environments").path(GraviteeContext.getCurrentEnvironment())
                .path("apis").path(api.getId()).path("picture");
        if (api.getPicture() != null) {
            // force browser to get if updated
            uriBuilder.queryParam("hash", api.getPicture().hashCode());
        }
        apiItem.setPictureUrl(uriBuilder.build().toString());
        apiItem.setCategories(api.getCategories());
        apiItem.setCreatedAt(api.getCreatedAt());
        apiItem.setUpdatedAt(api.getUpdatedAt());
        apiItem.setLabels(api.getLabels());
        apiItem.setPrimaryOwner(api.getPrimaryOwner());

        if (api.getVisibility() != null) {
            apiItem.setVisibility(io.gravitee.rest.api.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        if (api.getState() != null) {
            apiItem.setState(Lifecycle.State.valueOf(api.getState().toString()));
        }

        if (api.getProxy() != null) {
            apiItem.setVirtualHosts(api.getProxy().getVirtualHosts());
        }

        if (ratingService.isEnabled()) {
            final RatingSummaryEntity ratingSummary = ratingService.findSummaryByApi(api.getId());
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
        if (api.getProxy().getVirtualHosts() != null &&
                !api.getProxy().getVirtualHosts().isEmpty()) {
            apiItem.setContextPath(api.getProxy().getVirtualHosts().get(0).getPath());
        }

        return apiItem;
    }
}
