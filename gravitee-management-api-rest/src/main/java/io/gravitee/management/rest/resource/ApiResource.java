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

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.*;
import io.gravitee.management.model.api.*;
import io.gravitee.management.model.api.header.ApiHeaderEntity;
import io.gravitee.management.model.notification.NotifierEntity;
import io.gravitee.management.model.parameters.Key;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.exception.InvalidImageException;
import io.gravitee.management.rest.model.ApiStateEntity;
import io.gravitee.management.rest.resource.param.LifecycleActionParam;
import io.gravitee.management.rest.resource.param.LifecycleActionParam.LifecycleAction;
import io.gravitee.management.rest.resource.param.ReviewActionParam;
import io.gravitee.management.rest.resource.param.ReviewActionParam.ReviewAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.rest.utils.ImageUtils;
import io.gravitee.management.service.ApiMetadataService;
import io.gravitee.management.service.MessageService;
import io.gravitee.management.service.NotifierService;
import io.gravitee.management.service.ParameterService;
import io.gravitee.management.service.QualityMetricsService;
import io.gravitee.management.service.SwaggerService;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.swagger.annotations.*;
import org.glassfish.jersey.message.internal.HttpHeaderReader;
import org.glassfish.jersey.message.internal.MatchingEntityTag;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.format;

/**
 * Defines the REST resources to manage API.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API"})
public class ApiResource extends AbstractResource {

    @Context
    private UriInfo uriInfo;

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private NotifierService notifierService;
    @Autowired
    private QualityMetricsService qualityMetricsService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ParameterService parameterService;
    @Inject
    private SwaggerService swaggerService;

    @Autowired
    private ApiMetadataService apiMetadataService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the API's definition",
            notes = "User must have the READ permission on the API_DEFINITION to use this service on a private API.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API definition", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response get(
            @PathParam("api") String api) {
        ApiEntity apiEntity = apiService.findById(api);
        if (Visibility.PUBLIC.equals(apiEntity.getVisibility())
                || hasPermission(RolePermission.API_DEFINITION, api, RolePermissionAction.READ)) {
            setPicture(apiEntity);
            //TODO DBY: what is the purpose of this ?
            //apiEntity.setContextPath(apiEntity.getProxy().getContextPath());
            filterSensitiveData(apiEntity);

            return Response
                    .ok(apiEntity)
                    .tag(Long.toString(apiEntity.getUpdatedAt().getTime()))
                    .lastModified(apiEntity.getUpdatedAt())
                    .build();
        }
        throw new ForbiddenAccessException();
    }

    private void setPicture(final ApiEntity apiEntity) {
        final UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        final UriBuilder uriBuilder = ub.path("picture");
        if (apiEntity.getPicture() != null) {
            // force browser to get if updated
            uriBuilder.queryParam("hash", apiEntity.getPicture().hashCode());
        }
        apiEntity.setPictureUrl(uriBuilder.build().toString());
        apiEntity.setPicture(null);
    }

    @GET
    @Path("picture")
    @ApiOperation(value = "Get the API's picture",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API's picture"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response picture(
            @Context Request request,
            @PathParam("api") String api) throws ApiNotFoundException {
        ApiEntity apiEntity = apiService.findById(api);
        if (Visibility.PUBLIC.equals(apiEntity.getVisibility())
                || hasPermission(RolePermission.API_DEFINITION, api, RolePermissionAction.READ)) {

            CacheControl cc = new CacheControl();
            cc.setNoTransform(true);
            cc.setMustRevalidate(false);
            cc.setNoCache(false);
            cc.setMaxAge(86400);

            InlinePictureEntity image = apiService.getPicture(api);

            EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
            Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

            if (builder != null) {
                // Preconditions are not met, returning HTTP 304 'not-modified'
                return builder
                        .cacheControl(cc)
                        .build();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(image.getContent(), 0, image.getContent().length);

            return Response
                    .ok(baos)
                    .cacheControl(cc)
                    .tag(etag)
                    .type(image.getType())
                    .build();
        }
        throw new ForbiddenAccessException();
    }

    @POST
    @ApiOperation(
            value = "Manage the API's lifecycle",
            notes = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "API's picture"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE)
    })
    public Response doLifecycleAction(
            @Context HttpHeaders headers,
            @ApiParam(required = true, allowableValues = "START, STOP")
            @QueryParam("action") LifecycleActionParam action,
            @PathParam("api") String api) {
        final Response responseApi = get(api);
        Response.ResponseBuilder builder = evaluateIfMatch(headers, responseApi.getEntityTag().getValue());

        if (builder != null) {
            return builder.build();
        }

        final ApiEntity apiEntity = (ApiEntity) responseApi.getEntity();
        final ApiEntity updatedApi;
        switch (action.getAction()) {
            case START:
                checkAPILifeCycle(apiEntity, action.getAction());
                updatedApi = apiService.start(apiEntity.getId(), getAuthenticatedUser());
                break;
            case STOP:
                checkAPILifeCycle(apiEntity, action.getAction());
                updatedApi = apiService.stop(apiEntity.getId(), getAuthenticatedUser());
                break;
            default:
                updatedApi = null;
                break;
        }

        return Response
                .noContent()
                .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
                .lastModified(updatedApi.getUpdatedAt())
                .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update the API",
            notes = "User must have the MANAGE_API permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API successfully updated", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.API_GATEWAY_DEFINITION, acls = RolePermissionAction.UPDATE)
    })
    public Response update(
            @Context HttpHeaders headers,
            @ApiParam(name = "api", required = true) @Valid @NotNull final UpdateApiEntity apiToUpdate,
            @PathParam("api") String api) {
        final Response responseApi = get(api);
        Response.ResponseBuilder builder = evaluateIfMatch(headers, responseApi.getEntityTag().getValue());

        if (builder != null) {
            return builder.build();
        }

        try {
            ImageUtils.verify(apiToUpdate.getPicture());
        } catch (InvalidImageException e) {
            return Response.status(Status.BAD_REQUEST).entity("Invalid image format").build();
        }

        final ApiEntity currentApi = (ApiEntity) responseApi.getEntity();
        // Force context-path if user is not the primary_owner or an administrator
        if (!hasPermission(RolePermission.API_GATEWAY_DEFINITION, api, RolePermissionAction.UPDATE) &&
                !Objects.equals(currentApi.getPrimaryOwner().getId(), getAuthenticatedUser()) && !isAdmin()) {
            apiToUpdate.getProxy().setVirtualHosts(currentApi.getProxy().getVirtualHosts());
        }

        final ApiEntity updatedApi = apiService.update(api, apiToUpdate);
        setPicture(updatedApi);

        return Response
                .ok(updatedApi)
                .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
                .lastModified(updatedApi.getUpdatedAt())
                .build();
    }

    private Response.ResponseBuilder evaluateIfMatch(final HttpHeaders headers, final String etagValue) {
        String ifMatch = headers.getHeaderString(HttpHeaders.IF_MATCH);
        if (ifMatch == null || ifMatch.isEmpty()) {
            return null;
        }

        // Handle case for -gzip appended automatically (and sadly) by Apache
        ifMatch = ifMatch.replaceAll("-gzip", "");

        try {
            Set<MatchingEntityTag> matchingTags = HttpHeaderReader.readMatchingEntityTag(ifMatch);
            MatchingEntityTag ifMatchHeader = matchingTags.iterator().next();
            EntityTag eTag = new EntityTag(etagValue, ifMatchHeader.isWeak());

            return matchingTags != MatchingEntityTag.ANY_MATCH
                    && !matchingTags.contains(eTag) ? Response.status(Status.PRECONDITION_FAILED) : null;
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    @DELETE
    @ApiOperation(
            value = "Delete the API",
            notes = "User must have the DELETE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "API successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.DELETE)
    })
    public Response delete(@PathParam("api") String api) {
        apiService.delete(api);

        return Response.noContent().build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("deploy")
    @ApiOperation(
            value = "Deploy API to gateway instances",
            notes = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API successfully deployed", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE)
    })
    public Response deployAPI(@PathParam("api") String api) {
        try {
            ApiEntity apiEntity = apiService.deploy(api, getAuthenticatedUser(), EventType.PUBLISH_API);
            return Response
                    .ok(apiEntity)
                    .tag(Long.toString(apiEntity.getUpdatedAt().getTime()))
                    .lastModified(apiEntity.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("JsonProcessingException " + e).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("state")
    @ApiOperation(
            value = "Get the state of the API",
            notes = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API's state", response = ApiStateEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public ApiStateEntity isAPISynchronized(@PathParam("api") String api) {
        ApiEntity foundApi = apiService.findById(api);
        if (Visibility.PUBLIC.equals(foundApi.getVisibility())
                || hasPermission(RolePermission.API_DEFINITION, api, RolePermissionAction.READ)) {
            ApiStateEntity apiStateEntity = new ApiStateEntity();
            apiStateEntity.setApiId(api);
            setSynchronizationState(apiStateEntity);

            return apiStateEntity;
        }
        throw new ForbiddenAccessException();
    }

    @POST
    @Consumes
    @Produces(MediaType.APPLICATION_JSON)
    @Path("rollback")
    @ApiOperation(
            value = "Rollback API to a previous version",
            notes = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API successfully rollbacked", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE)
    })
    public Response rollback(
            @PathParam("api") String api,
            @ApiParam(name = "api", required = true) @Valid @NotNull final UpdateApiEntity apiEntity) {
        try {
            ApiEntity rollbackedApi = apiService.rollback(api, apiEntity);
            return Response
                    .ok(rollbackedApi)
                    .tag(Long.toString(rollbackedApi.getUpdatedAt().getTime()))
                    .lastModified(rollbackedApi.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("import")
    @ApiOperation(
            value = "Update the API with an existing API definition",
            notes = "User must have the MANAGE_API permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API successfully updated from API definition", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE)
    })
    public Response updateWithDefinition(
            @PathParam("api") String api,
            @ApiParam(name = "definition", required = true) String apiDefinition) {
        final ApiEntity apiEntity = (ApiEntity) get(api).getEntity();
        ApiEntity updatedApi = apiService.createOrUpdateWithDefinition(apiEntity, apiDefinition, getAuthenticatedUser());
        return Response
                .ok(updatedApi)
                .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
                .lastModified(updatedApi.getUpdatedAt())
                .build();
    }

    
    @POST
    @Path("import/swagger")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update the API with an existing Swagger descriptor",
            notes = "User must have the MANAGE_API permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API successfully updated from Swagger descriptor", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE)
    })
    public Response updateWithSwagger(
            @PathParam("api") String api,
            @ApiParam(name = "swagger", required = true) @Valid @NotNull ImportSwaggerDescriptorEntity swaggerDescriptor) {
        SwaggerApiEntity swaggerApiEntity = swaggerService.createAPI(swaggerDescriptor);
        final ApiEntity updatedApi = apiService.update(api, swaggerApiEntity, swaggerDescriptor);
        return Response
                .ok(updatedApi)
                .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
                .lastModified(updatedApi.getUpdatedAt())
                .build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("export")
    @ApiOperation(
            value = "Export the API definition in JSON format",
            notes = "User must have the MANAGE_API permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API definition", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ)
    })
    public Response exportDefinition(
            @PathParam("api") String api,
            @QueryParam("version") @DefaultValue("default") String version,
            @QueryParam("exclude") @DefaultValue("") String exclude) {
        final ApiEntity apiEntity = apiService.findById(api);
        final String apiDefinition = apiService.exportAsJson(api, version, exclude.split(","));
        return Response
                .ok(apiDefinition)
                .header(HttpHeaders.CONTENT_DISPOSITION, format("attachment;filename=%s", getExportFilename(apiEntity)))
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("notifiers")
    @Permissions({
            @Permission(value = RolePermission.API_NOTIFICATION, acls = RolePermissionAction.READ)
    })
    public List<NotifierEntity> getNotifiers(@PathParam("api") String api) {
        return notifierService.list(NotificationReferenceType.API, api);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("import-path-mappings")
    @ApiOperation(value = "Import path mappings from a page",
            notes = "User must have the MANAGE_API permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Path mappings successfully imported", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE)
    })
    public Response importPathMappingsFromPage(@PathParam("api") String api, @QueryParam("page") @NotNull String page) {
        final ApiEntity apiEntity = (ApiEntity) get(api).getEntity();
        ApiEntity updatedApi = apiService.importPathMappingsFromPage(apiEntity, page);
        return Response
                .ok(updatedApi)
                .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
                .lastModified(updatedApi.getUpdatedAt())
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("quality")
    @ApiOperation(value = "Get the quality metrics of the API")
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ)
    })
    public ApiQualityMetricsEntity getQualityMetrics(@PathParam("api") String api) {
        final ApiEntity apiEntity = (ApiEntity) get(api).getEntity();
        return qualityMetricsService.getMetrics(apiEntity);
    }


    @POST
    @Path("/messages")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.API_MESSAGE, acls = RolePermissionAction.CREATE)
    })
    public Response create(@PathParam("api") String api, final MessageEntity message) {
        return Response.ok(messageService.create(api, message)).build();
    }

    @GET
    @Path("headers")
    @ApiOperation(value = "Get the portal API headers values")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ApiHeaderEntity> getHeaders(@PathParam("api") String api) {
        return apiService.getPortalHeaders(api);
    }

    @POST
    @Path("reviews")
    @ApiOperation(
            value = "Manage the API's review state",
            notes = "User must have the API_DEFINITION or API_REVIEWS permission to use this service (depending on the action)")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Updated API"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.API_REVIEWS, acls = RolePermissionAction.UPDATE)
    })
    public Response doReviewAction(
            @Context HttpHeaders headers,
            @ApiParam(required = true, allowableValues = "ASK")
            @NotNull @Valid @QueryParam("action") ReviewActionParam action,
            @PathParam("api") String api,
            @ApiParam(name = "review") @Valid final ReviewEntity reviewEntity) {
        final Response responseApi = get(api);
        Response.ResponseBuilder builder = evaluateIfMatch(headers, responseApi.getEntityTag().getValue());
        if (builder != null) {
            return builder.build();
        }
        final ApiEntity apiEntity = (ApiEntity) responseApi.getEntity();
        final ApiEntity updatedApi;
        checkAPIReviewWorkflow(apiEntity, action.getAction());
        switch (action.getAction()) {
            case ASK:
                hasPermission(RolePermission.API_DEFINITION, api, RolePermissionAction.UPDATE);
                updatedApi = apiService.askForReview(apiEntity.getId(), getAuthenticatedUser(), reviewEntity);
                break;
            case ACCEPT:
                hasPermission(RolePermission.API_REVIEWS, api, RolePermissionAction.UPDATE);
                updatedApi = apiService.acceptReview(apiEntity.getId(), getAuthenticatedUser(), reviewEntity);
                break;
            case REJECT:
                hasPermission(RolePermission.API_REVIEWS, api, RolePermissionAction.UPDATE);
                updatedApi = apiService.rejectReview(apiEntity.getId(), getAuthenticatedUser(), reviewEntity);
                break;
            default:
                updatedApi = null;
                break;
        }
        return Response
                .noContent()
                .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
                .lastModified(updatedApi.getUpdatedAt())
                .build();
    }

    private void checkAPIReviewWorkflow(final ApiEntity api, final ReviewAction action) {
        if (ApiLifecycleState.ARCHIVED.equals(api.getLifecycleState())) {
            throw new BadRequestException("Deleted API can not be reviewed");
        }
        if (api.getWorkflowState() != null) {
            switch (api.getWorkflowState()) {
                case IN_REVIEW:
                    if (ReviewAction.ASK.equals(action)) {
                        throw new BadRequestException("Review is still in progress");
                    }
                    break;
                case DRAFT:
                    if (ReviewAction.ACCEPT.equals(action) || ReviewAction.REJECT.equals(action)) {
                        throw new BadRequestException("State invalid to accept/reject a review");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("duplicate")
    @ApiOperation(
            value = "Duplicate the API",
            notes = "User must have the MANAGE_API create permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API definition", response = ApiEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ),
            @Permission(value = RolePermission.MANAGEMENT_API, acls = RolePermissionAction.CREATE)
    })
    public Response duplicateAPI(@PathParam("api") String api, @ApiParam(name = "api", required = true)
            @Valid @NotNull final DuplicateApiEntity duplicateApiEntity) {
        get(api);
        return Response.ok(apiService.duplicate(api, duplicateApiEntity)).build();
    }

    @Path("keys")
    public ApiKeysResource getApiKeyResource() {
        return resourceContext.getResource(ApiKeysResource.class);
    }

    @Path("members")
    public ApiMembersResource getApiMembersResource() {
        return resourceContext.getResource(ApiMembersResource.class);
    }

    @Path("analytics")
    public ApiAnalyticsResource getApiAnalyticsResource() {
        return resourceContext.getResource(ApiAnalyticsResource.class);
    }

    @Path("logs")
    public ApiLogsResource getApiLogsResource() {
        return resourceContext.getResource(ApiLogsResource.class);
    }

    @Path("health")
    public ApiHealthResource getApiHealthResource() {
        return resourceContext.getResource(ApiHealthResource.class);
    }

    @Path("pages")
    public ApiPagesResource getApiPagesResource() {
        return resourceContext.getResource(ApiPagesResource.class);
    }

    @Path("events")
    public ApiEventsResource getApiEventsResource() {
        return resourceContext.getResource(ApiEventsResource.class);
    }

    @Path("plans")
    public ApiPlansResource getApiPlansResource() {
        return resourceContext.getResource(ApiPlansResource.class);
    }

    @Path("subscriptions")
    public ApiSubscriptionsResource getApiSubscriptionsResource() {
        return resourceContext.getResource(ApiSubscriptionsResource.class);
    }

    @Path("subscribers")
    public ApiSubscribersResource geApiSubscribersResource() {
        return resourceContext.getResource(ApiSubscribersResource.class);
    }

    @Path("metadata")
    public ApiMetadataResource getApiMetadataResource() {
        return resourceContext.getResource(ApiMetadataResource.class);
    }

    @Path("ratings")
    public ApiRatingResource getRatingResource() {
        return resourceContext.getResource(ApiRatingResource.class);
    }

    @Path("audit")
    public ApiAuditResource getApiAuditResource() {
        return resourceContext.getResource(ApiAuditResource.class);
    }

    @Path("notificationsettings")
    public ApiNotificationSettingsResource getNotificationSettingsResource() {
        return resourceContext.getResource(ApiNotificationSettingsResource.class);
    }

    @Path("alerts")
    public ApiAlertsResource getApiAlertsResource() {
        return resourceContext.getResource(ApiAlertsResource.class);
    }

    @Path("quality-rules")
    public ApiQualityRulesResource getApiQualityRulesResource() {
        return resourceContext.getResource(ApiQualityRulesResource.class);
    }

    private void setSynchronizationState(ApiStateEntity apiStateEntity) {
        if (apiService.isSynchronized(apiStateEntity.getApiId())) {
            apiStateEntity.setIsSynchronized(true);
        } else {
            apiStateEntity.setIsSynchronized(false);
        }
    }

    private void checkAPILifeCycle(ApiEntity api, LifecycleAction action) {
        if (ApiLifecycleState.ARCHIVED.equals(api.getLifecycleState())) {
            throw new BadRequestException("Deleted API can not be " + action.name().toLowerCase());
        }
        switch (api.getState()) {
            case STARTED:
                if (LifecycleAction.START.equals(action)) {
                    throw new BadRequestException("API is already started");
                }
                break;
            case STOPPED:
                if (LifecycleAction.STOP.equals(action)) {
                    throw new BadRequestException("API is already stopped");
                }

                final boolean apiReviewEnabled = parameterService.findAsBoolean(Key.API_REVIEW_ENABLED);
                if (apiReviewEnabled) {
                    if (api.getWorkflowState() != null && !WorkflowState.REVIEW_OK.equals(api.getWorkflowState())) {
                        throw new BadRequestException("API can not be started without being reviewed");
                    }
                }
                break;
            default:
                break;
        }
    }

    private String getExportFilename(ApiEntity apiEntity) {
        return format("%s-%s.json", apiEntity.getName(), apiEntity.getVersion())
                .trim()
                .toLowerCase()
                .replaceAll(" +", " ")
                .replaceAll(" ", "-")
                .replaceAll("[^\\w\\s\\.]", "-")
                .replaceAll("-+", "-");
    }

    private void filterSensitiveData(ApiEntity entity) {
        if (//try to display a public api as un unauthenticated user
                (!isAuthenticated() && Visibility.PUBLIC.equals(entity.getVisibility()))
                        || (!isAdmin() && !hasPermission(RolePermission.API_GATEWAY_DEFINITION, entity.getId(), RolePermissionAction.READ))) {
            entity.setProxy(null);
            entity.setPaths(null);
            entity.setProperties(null);
            entity.setServices(null);
            entity.setResources(null);
            entity.setPathMappings(null);
        }
    }
}
