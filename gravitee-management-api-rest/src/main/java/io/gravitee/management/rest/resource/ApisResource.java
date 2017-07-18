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
import io.gravitee.management.model.*;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.resource.param.VerifyApiParam;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.SwaggerService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private UserService userService;

    @Inject
    private SwaggerService swaggerService;

    @Inject
    private MembershipService membershipService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "List APIs",
            notes = "List all the APIs accessible to the current user or only public APIs for non authenticated users.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List accessible APIs for current user", response = ApiListItem.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<ApiListItem> listApis(@QueryParam("view") final String view, @QueryParam("group") final String group) {
        Set<ApiEntity> apis;
        if (isAdmin()) {
            apis = group != null
                    ? apiService.findByGroup(group)
                    : apiService.findAll();
        } else if (isAuthenticated()) {
            apis = apiService.findByUser(getAuthenticatedUsername());
        } else {
            apis = apiService.findByVisibility(Visibility.PUBLIC);
        }

        return apis.stream()
                .filter(apiEntity -> view == null || (apiEntity.getViews() != null && apiEntity.getViews().contains(view)))
                .filter(apiEntity -> group == null || (apiEntity.getGroup() != null && apiEntity.getGroup().getId().equals(group)))
                .map(this::convert)
                .map(this::setManageable)
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .collect(Collectors.toList());
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
        ApiEntity newApi = apiService.create(newApiEntity, getAuthenticatedUsername());
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
                null, apiDefinition, getAuthenticatedUsername())).build();
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

        final UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        final UriBuilder uriBuilder = ub.path(api.getId()).path("picture");
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

        if (api.getVisibility() != null) {
            apiItem.setVisibility(io.gravitee.management.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        if (api.getState() != null) {
            apiItem.setState(Lifecycle.State.valueOf(api.getState().toString()));
        }

        if (api.getProxy() != null) {
            apiItem.setContextPath(api.getProxy().getContextPath());
        }

        // Add primary owner
        Collection<MemberEntity> members = membershipService.getMembers(MembershipReferenceType.API, api.getId(), RoleScope.API, SystemRole.PRIMARY_OWNER.name());
        if (! members.isEmpty()) {
            MemberEntity primaryOwner = members.iterator().next();
            UserEntity user = userService.findByName(primaryOwner.getUsername(), false);

            PrimaryOwnerEntity owner = new PrimaryOwnerEntity();
            owner.setUsername(user.getUsername());
            owner.setEmail(user.getEmail());
            owner.setFirstname(user.getFirstname());
            owner.setLastname(user.getLastname());
            apiItem.setPrimaryOwner(owner);
        }

        return apiItem;
    }

    private ApiListItem setManageable(ApiListItem api) {
        api.setManageable(isAuthenticated() &&
                        (isAdmin() || hasPermission(RolePermission.API_GATEWAY_DEFINITION, api.getId(), RolePermissionAction.READ))
        );
        return api;
    }
}
