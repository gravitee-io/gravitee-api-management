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

import static io.gravitee.rest.api.model.WorkflowReferenceType.API;
import static io.gravitee.rest.api.model.WorkflowType.REVIEW;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.rest.api.exception.InvalidImageException;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.ImportExportApiMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.MemberMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.MetadataMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.PageMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.PlanMapper;
import io.gravitee.rest.api.management.v2.rest.model.BaseApi;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.LifecycleAction;
import io.gravitee.rest.api.management.v2.rest.security.Permission;
import io.gravitee.rest.api.management.v2.rest.security.Permissions;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.v4.ApiImagesService;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;

/**
 * Defines the REST resources to manage API v4.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ParameterService parameterService;

    @Inject
    private ApiStateService apiStateService;

    @Inject
    private ApiImagesService apiImagesService;

    @Inject
    private ApiImportExportService apiImportExportService;

    @Inject
    private WorkflowService workflowService;

    @Context
    protected UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiById(@PathParam("apiId") String apiId) {
        final GenericApiEntity apiEntity = getGenericApiEntityById(apiId, true);
        return Response
            .ok(ApiMapper.INSTANCE.convert(apiEntity, uriInfo))
            .tag(Long.toString(apiEntity.getUpdatedAt().getTime()))
            .lastModified(apiEntity.getUpdatedAt())
            .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions(
        {
            @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.API_GATEWAY_DEFINITION, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response updateApi(
        @Context HttpHeaders headers,
        @PathParam("apiId") String apiId,
        @Valid @NotNull final UpdateApiEntity apiToUpdate
    ) {
        if (!apiId.equals(apiToUpdate.getId())) {
            throw new BadRequestException("'apiId' is not the same as the API in payload");
        }

        final GenericApiEntity currentEntity = getGenericApiEntityById(apiId, true);
        evaluateIfMatch(headers, Long.toString(currentEntity.getUpdatedAt().getTime()));

        try {
            ImageUtils.verify(apiToUpdate.getPicture());
            ImageUtils.verify(apiToUpdate.getBackground());
        } catch (InvalidImageException e) {
            throw new BadRequestException("Invalid image format");
        }

        // Force listeners if user is not the primary_owner or an administrator
        if (
            currentEntity instanceof ApiEntity &&
            !hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_GATEWAY_DEFINITION,
                apiId,
                RolePermissionAction.UPDATE
            ) &&
            !Objects.equals(currentEntity.getPrimaryOwner().getId(), getAuthenticatedUser()) &&
            !isAdmin()
        ) {
            apiToUpdate.setListeners(((ApiEntity) currentEntity).getListeners());
        }

        final ApiEntity updatedApi = apiServiceV4.update(
            GraviteeContext.getExecutionContext(),
            apiId,
            apiToUpdate,
            true,
            getAuthenticatedUser()
        );
        setPicturesUrl(updatedApi);

        return Response
            .ok(ApiMapper.INSTANCE.convert(updatedApi, uriInfo))
            .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
            .lastModified(updatedApi.getUpdatedAt())
            .build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.DELETE) })
    public Response deleteApi(@PathParam("apiId") String apiId, @QueryParam("closePlans") boolean closePlans) {
        apiServiceV4.delete(GraviteeContext.getExecutionContext(), apiId, closePlans);

        return Response.noContent().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/deployments")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response deployApi(@PathParam("apiId") String apiId, @Valid final ApiDeploymentEntity apiDeploymentEntity) {
        try {
            GenericApiEntity apiEntity = apiStateService.deploy(
                GraviteeContext.getExecutionContext(),
                apiId,
                getAuthenticatedUser(),
                apiDeploymentEntity
            );
            return Response
                .accepted()
                .tag(Long.toString(apiEntity.getUpdatedAt().getTime()))
                .lastModified(apiEntity.getUpdatedAt())
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("JsonProcessingException " + e).build();
        }
    }

    @Path("/_export/definition")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response exportApiDefinition(@Context HttpHeaders headers, @PathParam("apiId") String apiId) {
        ExportApiEntity exportApiEntity = apiImportExportService.exportApi(
            GraviteeContext.getExecutionContext(),
            apiId,
            getAuthenticatedUser()
        );

        return Response
            .ok(ImportExportApiMapper.INSTANCE.map(exportApiEntity))
            .header(HttpHeaders.CONTENT_DISPOSITION, format("attachment;filename=%s", getExportFilename(exportApiEntity.getApiEntity())))
            .build();
    }

    @Path("/_start")
    @POST
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response startAPI(@Context HttpHeaders headers, @PathParam("apiId") String apiId) {
        GenericApiEntity genericApiEntity = getGenericApiEntityById(apiId, false);
        evaluateIfMatch(headers, Long.toString(genericApiEntity.getUpdatedAt().getTime()));

        checkApiLifeCycle(genericApiEntity, LifecycleAction.START);
        ApiEntity updatedApi = apiStateService.start(
            GraviteeContext.getExecutionContext(),
            genericApiEntity.getId(),
            getAuthenticatedUser()
        );

        return Response.noContent().tag(Long.toString(updatedApi.getUpdatedAt().getTime())).lastModified(updatedApi.getUpdatedAt()).build();
    }

    @Path("/_stop")
    @POST
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response stopAPI(@Context HttpHeaders headers, @PathParam("apiId") String apiId) {
        GenericApiEntity genericApiEntity = getGenericApiEntityById(apiId, false);
        evaluateIfMatch(headers, Long.toString(genericApiEntity.getUpdatedAt().getTime()));

        checkApiLifeCycle(genericApiEntity, LifecycleAction.STOP);
        ApiEntity updatedApi = apiStateService.stop(
            GraviteeContext.getExecutionContext(),
            genericApiEntity.getId(),
            getAuthenticatedUser()
        );

        return Response.noContent().tag(Long.toString(updatedApi.getUpdatedAt().getTime())).lastModified(updatedApi.getUpdatedAt()).build();
    }

    @GET
    @Path("picture")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApiPicture(@Context Request request, @Context HttpHeaders headers, @PathParam("apiId") String apiId) {
        return getImageResponse(request, apiImagesService.getApiPicture(GraviteeContext.getExecutionContext(), apiId));
    }

    @GET
    @Path("background")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApiBackground(@Context Request request, @Context HttpHeaders headers, @PathParam("apiId") String apiId) {
        return getImageResponse(request, apiImagesService.getApiBackground(GraviteeContext.getExecutionContext(), apiId));
    }

    private GenericApiEntity getGenericApiEntityById(String apiId, boolean prepareData) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        GenericApiEntity apiEntity = apiSearchService.findGenericById(executionContext, apiId);

        if (!canManageApi(apiEntity)) {
            throw new ForbiddenAccessException();
        }

        if (prepareData) {
            prepareDataForResponse(apiId, executionContext, apiEntity);
        }
        return apiEntity;
    }

    private void prepareDataForResponse(String apiId, ExecutionContext executionContext, GenericApiEntity apiEntity) {
        if (hasPermission(executionContext, RolePermission.API_DEFINITION, apiId, RolePermissionAction.READ)) {
            setPicturesUrl(apiEntity);
        } else {
            filterSensitiveData(apiEntity);
        }
    }

    private void setPicturesUrl(final GenericApiEntity apiEntity) {
        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder().path("picture");
        // force browser to get if updated
        uriBuilder.queryParam("hash", apiEntity.getUpdatedAt().getTime());
        String pictureUrl = uriBuilder.build().toString();

        uriBuilder = uriInfo.getAbsolutePathBuilder().path("background");
        // force browser to get if updated
        uriBuilder.queryParam("hash", apiEntity.getUpdatedAt().getTime());
        String backgroundUrl = uriBuilder.build().toString();

        if (apiEntity.getDefinitionVersion() == DefinitionVersion.V4) {
            ApiEntity apiEntityV4 = (ApiEntity) apiEntity;
            apiEntityV4.setPictureUrl(pictureUrl);
            apiEntityV4.setPicture(null);
            apiEntityV4.setBackgroundUrl(backgroundUrl);
            apiEntityV4.setBackground(null);
        }
        if (apiEntity.getDefinitionVersion() == DefinitionVersion.V2) {
            io.gravitee.rest.api.model.api.ApiEntity apiEntityV2 = (io.gravitee.rest.api.model.api.ApiEntity) apiEntity;
            apiEntityV2.setPictureUrl(pictureUrl);
            apiEntityV2.setPicture(null);
            apiEntityV2.setBackgroundUrl(backgroundUrl);
            apiEntityV2.setBackground(null);
        }
    }

    private void filterSensitiveData(GenericApiEntity apiEntity) {
        if (apiEntity.getDefinitionVersion() == DefinitionVersion.V4) {
            filterSensitiveData((ApiEntity) apiEntity);
        }
        if (apiEntity.getDefinitionVersion() == DefinitionVersion.V2) {
            filterSensitiveData((io.gravitee.rest.api.model.api.ApiEntity) apiEntity);
        }
    }

    private void filterSensitiveData(ApiEntity apiEntity) {
        List<Listener> listeners = apiEntity.getListeners();

        if (listeners != null) {
            Optional<Listener> first = listeners.stream().filter(listener -> ListenerType.HTTP == listener.getType()).findFirst();
            if (first.isPresent()) {
                HttpListener httpListener = (HttpListener) first.get();
                if (httpListener.getPaths() != null && !httpListener.getPaths().isEmpty()) {
                    io.gravitee.definition.model.v4.listener.http.Path path = httpListener.getPaths().get(0);
                    io.gravitee.definition.model.v4.listener.http.Path filteredPath =
                        new io.gravitee.definition.model.v4.listener.http.Path(path.getPath());
                    httpListener.setPaths(List.of(filteredPath));
                }
                httpListener.setPathMappings(null);
            }
        }
        apiEntity.setProperties(null);
        apiEntity.setServices(null);
        apiEntity.setResources(null);
        apiEntity.setResponseTemplates(null);
    }

    private void filterSensitiveData(io.gravitee.rest.api.model.api.ApiEntity apiEntity) {
        final Proxy filteredProxy = new Proxy();
        final VirtualHost virtualHost = apiEntity.getProxy().getVirtualHosts().get(0);
        virtualHost.setHost(null);
        filteredProxy.setVirtualHosts(singletonList(virtualHost));

        apiEntity.setProxy(filteredProxy);
        apiEntity.setPaths(null);
        apiEntity.setProperties(null);
        apiEntity.setServices(null);
        apiEntity.setResources(null);
        apiEntity.setPathMappings(null);
        apiEntity.setResponseTemplates(null);
    }

    private void checkApiLifeCycle(GenericApiEntity api, LifecycleAction action) {
        if (ApiLifecycleState.ARCHIVED.equals(api.getLifecycleState())) {
            var actionKeyword = LifecycleAction.START.equals(action) ? "started" : "stopped";
            throw new BadRequestException("Deleted API cannot be " + actionKeyword);
        }
        if (Lifecycle.State.STARTED.equals(api.getState()) && LifecycleAction.START.equals(action)) {
            throw new BadRequestException("API is already started");
        }
        if (Lifecycle.State.STOPPED.equals(api.getState())) {
            if (LifecycleAction.STOP.equals(action)) {
                throw new BadRequestException("API is already stopped");
            }

            if (
                parameterService.findAsBoolean(
                    GraviteeContext.getExecutionContext(),
                    Key.API_REVIEW_ENABLED,
                    ParameterReferenceType.ENVIRONMENT
                )
            ) {
                final List<Workflow> workflows = workflowService.findByReferenceAndType(API, api.getId(), REVIEW);

                workflows.forEach(workflow -> {
                    WorkflowState workflowState = WorkflowState.valueOf(workflow.getState());
                    if (!WorkflowState.REVIEW_OK.equals(workflowState)) {
                        throw new BadRequestException("API cannot be started without being reviewed");
                    }
                });
            }
        }
    }

    private String getExportFilename(GenericApiEntity apiEntity) {
        return format("%s-%s.%s", apiEntity.getName(), apiEntity.getApiVersion(), "json")
            .trim()
            .toLowerCase()
            .replaceAll(" +", " ")
            .replaceAll(" ", "-")
            .replaceAll("[^\\w\\s\\.]", "-")
            .replaceAll("-+", "-");
    }

    private Response getImageResponse(final Request request, InlinePictureEntity image) {
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
}
