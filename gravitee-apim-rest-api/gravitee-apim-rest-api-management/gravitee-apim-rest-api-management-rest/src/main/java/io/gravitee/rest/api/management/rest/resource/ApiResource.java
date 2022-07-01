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

import static java.lang.String.format;
import static java.util.Collections.singletonList;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.exception.InvalidImageException;
import io.gravitee.rest.api.management.rest.resource.param.LifecycleAction;
import io.gravitee.rest.api.management.rest.resource.param.ReviewAction;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.DuplicateApiEntity;
import io.gravitee.rest.api.model.api.RollbackApiEntity;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.model.api.header.ApiHeaderEntity;
import io.gravitee.rest.api.model.notification.NotifierEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionRequestEntity;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.ApiExportService;
import io.gravitee.rest.api.service.DebugApiService;
import io.gravitee.rest.api.service.JsonPatchService;
import io.gravitee.rest.api.service.MessageService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.QualityMetricsService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.message.internal.HttpHeaderReader;
import org.glassfish.jersey.message.internal.MatchingEntityTag;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Defines the REST resources to manage API.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "APIs")
public class ApiResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private NotifierService notifierService;

    @Inject
    private QualityMetricsService qualityMetricsService;

    @Inject
    private MessageService messageService;

    @Inject
    private ParameterService parameterService;

    @Inject
    private SwaggerService swaggerService;

    @Inject
    private PromotionService promotionService;

    @Inject
    protected ApiDuplicatorService apiDuplicatorService;

    @Inject
    protected JsonPatchService jsonPatchService;

    @Inject
    protected ApiExportService apiExportService;

    @Inject
    protected DebugApiService debugApiService;

    @PathParam("api")
    @Parameter(name = "api", required = true, description = "The ID of the API")
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get the API",
        description = "User must have the READ permission on the API_DEFINITION to use this service on a private API."
    )
    @ApiResponse(
        responseCode = "200",
        description = "API definition",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getApi() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApiEntity apiEntity = apiService.findById(executionContext, api);

        if (!canManageApi(apiEntity)) {
            throw new ForbiddenAccessException();
        }

        if (hasPermission(executionContext, RolePermission.API_DEFINITION, api, RolePermissionAction.READ)) {
            setPictures(apiEntity);
        } else {
            filterSensitiveData(apiEntity);
        }
        return Response.ok(apiEntity).tag(Long.toString(apiEntity.getUpdatedAt().getTime())).lastModified(apiEntity.getUpdatedAt()).build();
    }

    private void setPictures(final ApiEntity apiEntity) {
        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder().path("picture");
        // force browser to get if updated
        uriBuilder.queryParam("hash", apiEntity.getUpdatedAt().getTime());
        apiEntity.setPictureUrl(uriBuilder.build().toString());
        apiEntity.setPicture(null);

        uriBuilder = uriInfo.getAbsolutePathBuilder().path("background");
        // force browser to get if updated
        uriBuilder.queryParam("hash", apiEntity.getUpdatedAt().getTime());
        apiEntity.setBackgroundUrl(uriBuilder.build().toString());
        apiEntity.setBackground(null);
    }

    @GET
    @Path("picture")
    @Operation(summary = "Get the API's picture", description = "User must have the READ permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "API's picture",
        content = @Content(mediaType = "*/*", schema = @Schema(type = "string", format = "binary"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getApiPicture(@Context Request request) throws ApiNotFoundException {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        return getImageResponse(executionContext, request, apiService.getPicture(executionContext, api));
    }

    @GET
    @Path("background")
    @Operation(summary = "Get the API's background", description = "User must have the READ permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "API's background",
        content = @Content(mediaType = "*/*", schema = @Schema(type = "string", format = "binary"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getApiBackground(@Context Request request) throws ApiNotFoundException {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        return getImageResponse(executionContext, request, apiService.getBackground(executionContext, api));
    }

    private Response getImageResponse(final ExecutionContext executionContext, final Request request, InlinePictureEntity image) {
        canReadApi(executionContext, api);
        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        if (image == null || image.getContent() == null) {
            return Response.ok().cacheControl(cc).build();
        }
        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder.cacheControl(cc).build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response.ok(baos).cacheControl(cc).tag(etag).type(image.getType()).build();
    }

    @POST
    @Operation(summary = "Manage the API's lifecycle", description = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response doApiLifecycleAction(
        @Context HttpHeaders headers,
        @Parameter(required = true) @QueryParam("action") LifecycleAction action
    ) {
        final Response responseApi = getApi();
        Response.ResponseBuilder builder = evaluateIfMatch(headers, responseApi.getEntityTag().getValue());

        if (builder != null) {
            return builder.build();
        }

        if (action == null) {
            throw new BadRequestException("Valid LifecycleAction is required");
        }

        final ApiEntity apiEntity = (ApiEntity) responseApi.getEntity();
        ApiEntity updatedApi = null;
        switch (action) {
            case START:
                checkApiLifeCycle(apiEntity, action);
                updatedApi = apiService.start(GraviteeContext.getExecutionContext(), apiEntity.getId(), getAuthenticatedUser());
                break;
            case STOP:
                checkApiLifeCycle(apiEntity, action);
                updatedApi = apiService.stop(GraviteeContext.getExecutionContext(), apiEntity.getId(), getAuthenticatedUser());
                break;
        }

        return Response.noContent().tag(Long.toString(updatedApi.getUpdatedAt().getTime())).lastModified(updatedApi.getUpdatedAt()).build(); //NOSONAR `updatedApi` can't be null
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update the API", description = "User must have the MANAGE_API permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "API successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.API_GATEWAY_DEFINITION, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response updateApi(
        @Context HttpHeaders headers,
        @Parameter(name = "api", required = true) @Valid @NotNull final UpdateApiEntity apiToUpdate
    ) {
        final Response responseApi = getApi();
        Response.ResponseBuilder builder = evaluateIfMatch(headers, responseApi.getEntityTag().getValue());

        if (builder != null) {
            return builder.build();
        }

        try {
            ImageUtils.verify(apiToUpdate.getPicture());
            ImageUtils.verify(apiToUpdate.getBackground());
        } catch (InvalidImageException e) {
            throw new BadRequestException("Invalid image format");
        }

        final ApiEntity currentApi = (ApiEntity) responseApi.getEntity();
        // Force context-path if user is not the primary_owner or an administrator
        if (
            !hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_GATEWAY_DEFINITION,
                api,
                RolePermissionAction.UPDATE
            ) &&
            !Objects.equals(currentApi.getPrimaryOwner().getId(), getAuthenticatedUser()) &&
            !isAdmin()
        ) {
            apiToUpdate.getProxy().setVirtualHosts(currentApi.getProxy().getVirtualHosts());
        }

        final ApiEntity updatedApi = apiService.update(GraviteeContext.getExecutionContext(), api, apiToUpdate, true);
        setPictures(updatedApi);

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

            return matchingTags != MatchingEntityTag.ANY_MATCH && !matchingTags.contains(eTag)
                ? Response.status(Status.PRECONDITION_FAILED)
                : null;
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    @DELETE
    @Operation(summary = "Delete the API", description = "User must have the DELETE permission to use this service")
    @ApiResponse(responseCode = "204", description = "API successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.DELETE) })
    public Response deleteApi() {
        apiService.delete(GraviteeContext.getExecutionContext(), api);

        return Response.noContent().build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("deploy")
    @Operation(
        summary = "Deploy API to gateway instances",
        description = "User must have the MANAGE_LIFECYCLE permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully deployed",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response deployApi(@Parameter(name = "apiDeployment") @Valid final ApiDeploymentEntity apiDeploymentEntity) {
        try {
            ApiEntity apiEntity = apiService.deploy(
                GraviteeContext.getExecutionContext(),
                api,
                getAuthenticatedUser(),
                EventType.PUBLISH_API,
                apiDeploymentEntity
            );
            return Response
                .ok(apiEntity)
                .tag(Long.toString(apiEntity.getUpdatedAt().getTime()))
                .lastModified(apiEntity.getUpdatedAt())
                .build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("JsonProcessingException " + e).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("_debug")
    @Operation(
        summary = "Debug an API on gateway instances",
        description = "User must have the UPDATE permission on API_DEFINITION to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Debug session successfully started",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EventEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response debugAPI(@Parameter(name = "request") @Valid final DebugApiEntity debugApiEntity) {
        EventEntity apiEntity = debugApiService.debug(GraviteeContext.getExecutionContext(), api, getAuthenticatedUser(), debugApiEntity);
        return Response.ok(apiEntity).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("state")
    @Operation(summary = "Get the state of the API", description = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "API's state",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiStateEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ApiStateEntity isApiSynchronized() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        canReadApi(executionContext, api);
        ApiStateEntity apiStateEntity = new ApiStateEntity();
        apiStateEntity.setApiId(api);
        setSynchronizationState(executionContext, apiStateEntity);
        return apiStateEntity;
    }

    @POST
    @Consumes
    @Produces(MediaType.APPLICATION_JSON)
    @Path("rollback")
    @Operation(
        summary = "Rollback API to a previous version",
        description = "User must have the MANAGE_LIFECYCLE permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully rollbacked",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response rollbackApi(@Parameter(name = "api", required = true) @Valid @NotNull final RollbackApiEntity apiEntity) {
        try {
            ApiEntity rollbackedApi = apiService.rollback(GraviteeContext.getExecutionContext(), api, apiEntity);
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
    @Deprecated
    @Produces(MediaType.APPLICATION_JSON)
    @Path("import")
    @Operation(
        summary = "Update the API with an existing API definition in JSON format either with json or via an URL",
        description = "User must have the MANAGE_API permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully updated from API definition",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response updateApiWithDefinition(@RequestBody @Valid @NotNull Object apiDefinitionOrUrl) {
        ApiEntity updatedApi = apiDuplicatorService.createWithImportedDefinition(GraviteeContext.getExecutionContext(), apiDefinitionOrUrl);
        return Response
            .ok(updatedApi)
            .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
            .lastModified(updatedApi.getUpdatedAt())
            .build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("import")
    @Operation(
        summary = "Update the API with an existing API definition in JSON format either with json or via an URL",
        description = "User must have the MANAGE_API permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully updated from API definition",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response updateWithDefinitionPUT(@RequestBody @Valid @NotNull Object apiDefinitionOrUrl) {
        final ApiEntity apiEntity = (ApiEntity) getApi().getEntity();

        ApiEntity updatedApi = apiDuplicatorService.updateWithImportedDefinition(
            GraviteeContext.getExecutionContext(),
            apiEntity.getId(),
            apiDefinitionOrUrl
        );
        return Response
            .ok(updatedApi)
            .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
            .lastModified(updatedApi.getUpdatedAt())
            .build();
    }

    @POST
    @Deprecated
    @Path("import/swagger")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Deprecated, use PUT method instead. Update the API with an existing Swagger descriptor",
        description = "User must have the MANAGE_API permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully updated from Swagger descriptor",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response updateApiWithSwagger(
        @Parameter(name = "swagger", required = true) @Valid @NotNull ImportSwaggerDescriptorEntity swaggerDescriptor
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        SwaggerApiEntity swaggerApiEntity = swaggerService.createAPI(executionContext, swaggerDescriptor);
        final ApiEntity updatedApi = apiService.updateFromSwagger(executionContext, api, swaggerApiEntity, swaggerDescriptor);
        return Response
            .ok(updatedApi)
            .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
            .lastModified(updatedApi.getUpdatedAt())
            .build();
    }

    @PUT
    @Path("import/swagger")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update the API with an existing Swagger descriptor",
        description = "User must have the MANAGE_API permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully updated from Swagger descriptor",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response updateApiWithSwaggerPUT(
        @Parameter(name = "swagger", required = true) @Valid @NotNull ImportSwaggerDescriptorEntity swaggerDescriptor,
        @QueryParam("definitionVersion") @DefaultValue("1.0.0") String definitionVersion
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        SwaggerApiEntity swaggerApiEntity = swaggerService.createAPI(
            executionContext,
            swaggerDescriptor,
            DefinitionVersion.valueOfLabel(definitionVersion)
        );
        final ApiEntity updatedApi = apiService.updateFromSwagger(executionContext, api, swaggerApiEntity, swaggerDescriptor);
        return Response
            .ok(updatedApi)
            .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
            .lastModified(updatedApi.getUpdatedAt())
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("export")
    @Operation(
        summary = "Export the API definition in JSON format",
        description = "User must have the MANAGE_API permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API definition",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response exportApiDefinition(
        @QueryParam("version") @DefaultValue("default") String version,
        @QueryParam("exclude") @DefaultValue("") String exclude
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final ApiEntity apiEntity = apiService.findById(executionContext, api);
        final String apiDefinition = apiExportService.exportAsJson(executionContext, api, version, exclude.split(","));
        return Response
            .ok(apiDefinition)
            .header(HttpHeaders.CONTENT_DISPOSITION, format("attachment;filename=%s", getExportFilename(apiEntity)))
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("notifiers")
    @Operation(
        summary = "List available notifiers for API",
        description = "User must have the API_NOTIFICATION[READ] permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.API_NOTIFICATION, acls = RolePermissionAction.READ) })
    public List<NotifierEntity> getApiNotifiers() {
        return notifierService.list(NotificationReferenceType.API, api);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("import-path-mappings")
    @Operation(summary = "Import path mappings from a page", description = "User must have the MANAGE_API permission to use this service")
    @ApiResponse(
        responseCode = "201",
        description = "Path mappings successfully imported",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response importApiPathMappingsFromPage(
        @QueryParam("page") @NotNull String page,
        @QueryParam("definitionVersion") @DefaultValue("1.0.0") String definitionVersion
    ) {
        final ApiEntity apiEntity = (ApiEntity) getApi().getEntity();
        ApiEntity updatedApi = apiService.importPathMappingsFromPage(
            GraviteeContext.getExecutionContext(),
            apiEntity,
            page,
            DefinitionVersion.valueOfLabel(definitionVersion)
        );
        return Response
            .ok(updatedApi)
            .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
            .lastModified(updatedApi.getUpdatedAt())
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("quality")
    @Operation(summary = "Get the quality metrics of the API")
    public ApiQualityMetricsEntity getApiQualityMetrics() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        canReadApi(executionContext, api);
        final ApiEntity apiEntity = apiService.findById(executionContext, api);
        return qualityMetricsService.getMetrics(executionContext, apiEntity);
    }

    @POST
    @Path("/messages")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Send a message to existing consumers of an API",
        description = "User must have the API_MESSAGE[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Message successfully sent, the resulting integer is the number of recipients that were found",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Integer.class))
    )
    @Permissions({ @Permission(value = RolePermission.API_MESSAGE, acls = RolePermissionAction.CREATE) })
    public Response createApiMessage(final MessageEntity message) {
        return Response.ok(messageService.create(GraviteeContext.getExecutionContext(), api, message)).build();
    }

    @GET
    @Path("headers")
    @Operation(summary = "Get the portal API headers values")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ApiHeaderEntity> getPortalApiHeaders() {
        return apiService.getPortalHeaders(GraviteeContext.getExecutionContext(), api);
    }

    @POST
    @Path("reviews")
    @Operation(
        summary = "Manage the API's review state",
        description = "User must have the API_DEFINITION[UPDATE] or API_REVIEWS[UPDATE] permission to use this service (depending on the action)"
    )
    @ApiResponse(responseCode = "204", description = "Updated API")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.API_REVIEWS, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response doApiReviewAction(
        @Context HttpHeaders headers,
        @Parameter(required = true) @NotNull @Valid @QueryParam("action") ReviewAction action,
        @Parameter(name = "review") @Valid final ReviewEntity reviewEntity
    ) {
        final Response responseApi = getApi();
        Response.ResponseBuilder builder = evaluateIfMatch(headers, responseApi.getEntityTag().getValue());
        if (builder != null) {
            return builder.build();
        }

        final ApiEntity apiEntity = (ApiEntity) responseApi.getEntity();
        ApiEntity updatedApi = null;
        checkApiReviewWorkflow(apiEntity, action);
        switch (action) {
            case ASK:
                hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_DEFINITION, api, RolePermissionAction.UPDATE);
                updatedApi =
                    apiService.askForReview(GraviteeContext.getExecutionContext(), apiEntity.getId(), getAuthenticatedUser(), reviewEntity);
                break;
            case ACCEPT:
                hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_REVIEWS, api, RolePermissionAction.UPDATE);
                updatedApi =
                    apiService.acceptReview(GraviteeContext.getExecutionContext(), apiEntity.getId(), getAuthenticatedUser(), reviewEntity);
                break;
            case REJECT:
                hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_REVIEWS, api, RolePermissionAction.UPDATE);
                updatedApi =
                    apiService.rejectReview(GraviteeContext.getExecutionContext(), apiEntity.getId(), getAuthenticatedUser(), reviewEntity);
                break;
        }

        return Response.noContent().tag(Long.toString(updatedApi.getUpdatedAt().getTime())).lastModified(updatedApi.getUpdatedAt()).build(); //NOSONAR `updatedApi` can't be null
    }

    private void checkApiReviewWorkflow(final ApiEntity api, final ReviewAction action) {
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
    @Operation(summary = "Duplicate the API", description = "User must have the MANAGE_API create permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "API definition",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ),
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE),
        }
    )
    public Response duplicateAPI(@Parameter(name = "api", required = true) @Valid @NotNull final DuplicateApiEntity duplicateApiEntity) {
        final ApiEntity apiEntity = (ApiEntity) getApi().getEntity(); // call this method to check READ permission on source API.
        return Response.ok(apiDuplicatorService.duplicate(GraviteeContext.getExecutionContext(), apiEntity, duplicateApiEntity)).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("_migrate")
    @Operation(
        summary = "Migrate the API definition to be used with Policy Studio",
        description = "User must have the MANAGE_API create permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API definition",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ),
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE),
        }
    )
    public Response migrateAPI() {
        return Response.ok(apiService.migrate(GraviteeContext.getExecutionContext(), this.api)).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("_promote")
    @Operation(
        summary = "Promote the API to another environment",
        description = "User must have the API_DEFINITION update permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Promotion request",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PromotionEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response promoteAPI(@RequestBody @Valid @NotNull final PromotionRequestEntity promotionRequest) {
        return Response
            .ok(
                promotionService.promote(
                    GraviteeContext.getExecutionContext(),
                    GraviteeContext.getCurrentEnvironment(),
                    this.api,
                    promotionRequest,
                    getAuthenticatedUser()
                )
            )
            .build();
    }

    @Path("members")
    public ApiMembersResource getApiMembersResource() {
        return resourceContext.getResource(ApiMembersResource.class);
    }

    @Path("groups")
    public ApiGroupsResource getApiGroupsResource() {
        return resourceContext.getResource(ApiGroupsResource.class);
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

    @Path("definition")
    public ApiDefinitionResource getApiDefinition() {
        return resourceContext.getResource(ApiDefinitionResource.class);
    }

    private void setSynchronizationState(final ExecutionContext executionContext, ApiStateEntity apiStateEntity) {
        apiStateEntity.setIsSynchronized(apiService.isSynchronized(executionContext, apiStateEntity.getApiId()));
    }

    private void checkApiLifeCycle(ApiEntity api, LifecycleAction action) {
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

                final boolean apiReviewEnabled = parameterService.findAsBoolean(
                    GraviteeContext.getExecutionContext(),
                    Key.API_REVIEW_ENABLED,
                    ParameterReferenceType.ENVIRONMENT
                );
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
        final Proxy filteredProxy = new Proxy();
        final VirtualHost virtualHost = entity.getProxy().getVirtualHosts().get(0);
        virtualHost.setHost(null);
        filteredProxy.setVirtualHosts(singletonList(virtualHost));

        entity.setProxy(filteredProxy);
        entity.setPaths(null);
        entity.setProperties(null);
        entity.setServices(null);
        entity.setResources(null);
        entity.setPathMappings(null);
        entity.setResponseTemplates(null);
    }
}
