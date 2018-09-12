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

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.ImportSwaggerDescriptorEntity;
import io.gravitee.management.model.RatingSummaryEntity;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.api.ApiListItem;
import io.gravitee.management.model.api.ApiQuery;
import io.gravitee.management.model.api.NewApiEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.resource.param.ApisParam;
import io.gravitee.management.rest.resource.param.VerifyApiParam;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.RatingService;
import io.gravitee.management.service.SwaggerService;
import io.gravitee.management.service.TopApiService;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.management.service.notification.ApiHook;
import io.gravitee.management.service.notification.Hook;
import io.gravitee.repository.exceptions.TechnicalException;
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
import java.util.stream.Collectors;

import static io.gravitee.management.model.Visibility.PUBLIC;
import static io.gravitee.repository.management.model.View.ALL_ID;
import static java.util.stream.Collectors.toList;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/apis")
@Api(tags = {"API"})
public class ApisResource extends AbstractResource {

    @Context
    private UriInfo uriInfo;
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "List APIs",
            notes = "List all the APIs accessible to the current user or only public APIs for non authenticated users.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List accessible APIs for current user", response = ApiListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<ApiListItem> listApis(@BeanParam final ApisParam apisParam) {

        final ApiQuery apiQuery = new ApiQuery();
        if (apisParam.getGroup() != null) {
            apiQuery.setGroups(Collections.singletonList(apisParam.getGroup()));
        }
        apiQuery.setContextPath(apisParam.getContextPath());
        apiQuery.setLabel(apisParam.getLabel());
        apiQuery.setVersion(apisParam.getVersion());
        apiQuery.setName(apisParam.getName());
        apiQuery.setTag(apisParam.getTag());
        apiQuery.setState(apisParam.getState());
        if (!ALL_ID.equals(apisParam.getView())) {
            apiQuery.setView(apisParam.getView());
        }

        final Collection<ApiEntity> apis;
        if (isAdmin()) {
            apis = apiService.search(apiQuery);
        } else {
            if (isAuthenticated()) {
                apis = apiService.findByUser(getAuthenticatedUser(), apiQuery);
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
                .map(this::setManageable)
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
            @Permission(value = RolePermission.MANAGEMENT_API, acls = RolePermissionAction.CREATE)
    })
    public Response createApi(
            @ApiParam(name = "api", required = true)
            @Valid @NotNull final NewApiEntity newApiEntity) throws ApiAlreadyExistsException {
        ApiEntity newApi = apiService.create(newApiEntity, getAuthenticatedUser());
        if (newApi != null) {
            return Response
                    .created(URI.create("/apis/" + newApi.getId()))
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
            @Permission(value = RolePermission.MANAGEMENT_API, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.MANAGEMENT_API, acls = RolePermissionAction.UPDATE)
    })
    public Response importDefinition(
            @ApiParam(name = "definition", required = true) @Valid @NotNull String apiDefinition) {
        return Response.ok(apiService.createOrUpdateWithDefinition(
                null, apiDefinition, getAuthenticatedUser())).build();
    }

    @POST
    @Path("import/swagger")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create an API definition from a Swagger descriptor")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API definition from Swagger descriptor", response = NewApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_API, acls = RolePermissionAction.CREATE)
    })
    public NewApiEntity importSwagger(
            @ApiParam(name = "swagger", required = true) @Valid @NotNull ImportSwaggerDescriptorEntity swaggerDescriptor) {
        return swaggerService.prepare(swaggerDescriptor);
    }

    @POST
    @Path("verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check if an API match the following criteria")
    @ApiResponses({
            @ApiResponse(code = 200, message = "No API match the following criteria"),
            @ApiResponse(code = 400, message = "API already exist with the following criteria")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_API, acls = RolePermissionAction.CREATE)
    })
    public Response verify(@Valid VerifyApiParam verifyApiParam) {
        try {
            // TODO : create verify service to query repository with criteria
            apiService.checkContextPath(verifyApiParam.getContextPath());
            return Response.ok().entity("API context [" + verifyApiParam.getContextPath() + "] is available").build();
        } catch (TechnicalException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("The api context path [" + verifyApiParam.getContextPath() + "] already exists.").build();
        }
    }

    @GET
    @Path("/hooks")
    @ApiOperation("Get the list of available hooks")
    @Produces(MediaType.APPLICATION_JSON)
    public Hook[] getHooks() {
        return Arrays.stream(ApiHook.values()).filter(h -> !h.isHidden()).toArray(Hook[]::new);
    }

    @POST
    @Path("_search")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Search for API using the search engine")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List accessible APIs for current user", response = ApiListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response searchApis(
            @ApiParam(name = "q", required = true)
            @NotNull @QueryParam("q") String query) {
        try {
            final Collection<ApiEntity> apis;
            if (isAdmin()) {
                apis = apiService.search(new ApiQuery());
            } else {
                if (isAuthenticated()) {
                    apis = apiService.findByUser(getAuthenticatedUser(), new ApiQuery());
                } else {
                    ApiQuery apiQuery = new ApiQuery();
                    apiQuery.setVisibility(PUBLIC);
                    apis = apiService.search(apiQuery);
                }
            }

            Map<String, Object> filters = new HashMap<>();
            filters.put("api", apis.stream().map(ApiEntity::getId).collect(Collectors.toSet()));

            return Response.ok().entity(apiService.search(query, filters)
                    .stream()
                    .map(this::convert)
                    .map(this::setManageable)
                    .collect(toList())).build();
        } catch (TechnicalException te) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(te).build();
        }
    }

    @Path("{api}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }

    private ApiListItem convert(ApiEntity api) {
        final ApiListItem apiItem = new ApiListItem();

        apiItem.setId(api.getId());
        apiItem.setName(api.getName());
        apiItem.setVersion(api.getVersion());
        apiItem.setDescription(api.getDescription());

        final UriBuilder ub = uriInfo.getBaseUriBuilder();
        final UriBuilder uriBuilder = ub.path("apis").path(api.getId()).path("picture");
        if (api.getPicture() != null) {
            // force browser to get if updated
            uriBuilder.queryParam("hash", api.getPicture().hashCode());
        }
        apiItem.setPictureUrl(uriBuilder.build().toString());
        apiItem.setViews(api.getViews());
        apiItem.setCreatedAt(api.getCreatedAt());
        apiItem.setUpdatedAt(api.getUpdatedAt());
        apiItem.setLabels(api.getLabels());
        apiItem.setViews(api.getViews());
        apiItem.setPrimaryOwner(api.getPrimaryOwner());

        if (api.getVisibility() != null) {
            apiItem.setVisibility(io.gravitee.management.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        if (api.getState() != null) {
            apiItem.setState(Lifecycle.State.valueOf(api.getState().toString()));
        }

        if (api.getProxy() != null) {
            apiItem.setContextPath(api.getProxy().getContextPath());
        }

        if (ratingService.isEnabled()) {
            final RatingSummaryEntity ratingSummary = ratingService.findSummaryByApi(api.getId());
            apiItem.setRate(ratingSummary.getAverageRate());
            apiItem.setNumberOfRatings(ratingSummary.getNumberOfRatings());
        }
        apiItem.setTags(api.getTags());

        return apiItem;
    }

    private ApiListItem setManageable(ApiListItem api) {
        api.setManageable(isAuthenticated() &&
                (isAdmin() || hasPermission(RolePermission.API_GATEWAY_DEFINITION, api.getId(), RolePermissionAction.READ))
        );
        return api;
    }
}
