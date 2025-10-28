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

import static io.gravitee.apim.core.utils.CollectionUtils.isNotEmpty;
import static io.gravitee.apim.core.utils.CollectionUtils.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.gravitee.apim.core.api.model.UpdateNativeApi;
import io.gravitee.apim.core.api.model.crd.IDExportStrategy;
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.apim.core.api.use_case.ExportApiCRDUseCase;
import io.gravitee.apim.core.api.use_case.ExportApiUseCase;
import io.gravitee.apim.core.api.use_case.GetApiDefinitionUseCase;
import io.gravitee.apim.core.api.use_case.GetExposedEntrypointsUseCase;
import io.gravitee.apim.core.api.use_case.MigrateApiUseCase;
import io.gravitee.apim.core.api.use_case.RollbackApiUseCase;
import io.gravitee.apim.core.api.use_case.UpdateFederatedApiUseCase;
import io.gravitee.apim.core.api.use_case.UpdateNativeApiUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.Excludable;
import io.gravitee.apim.core.promotion.use_case.CreatePromotionUseCase;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.exception.InvalidImageException;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiCRDMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.DuplicateApiMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.ImportExportApiMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.PromotionMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiCRD;
import io.gravitee.rest.api.management.v2.rest.model.ApiReview;
import io.gravitee.rest.api.management.v2.rest.model.ApiRollback;
import io.gravitee.rest.api.management.v2.rest.model.ApiTransferOwnership;
import io.gravitee.rest.api.management.v2.rest.model.DuplicateApiOptions;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.MigrationReportResponses;
import io.gravitee.rest.api.management.v2.rest.model.MigrationReportResponsesIssuesInner;
import io.gravitee.rest.api.management.v2.rest.model.MigrationStateType;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.PromotionRequest;
import io.gravitee.rest.api.management.v2.rest.model.SubscribersResponse;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiFederated;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV2;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV4;
import io.gravitee.rest.api.management.v2.rest.model.UpdateGenericApi;
import io.gravitee.rest.api.management.v2.rest.model.VerifyApiDeploymentResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.provider.YamlWriter;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.analytics.ApiAnalyticsResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.audit.ApiAuditsResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.debug.ApiDebugResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.event.ApiEventsResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.health.ApiHealthResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.log.ApiLogsResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.scoring.ApiScoringResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.specgen.ApiSpecGenResource;
import io.gravitee.rest.api.management.v2.rest.resource.documentation.ApiPagesResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.LifecycleAction;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import io.gravitee.rest.api.service.exceptions.InvalidLicenseException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.TransferOwnershipNotAllowedException;
import io.gravitee.rest.api.service.v4.ApiDuplicateService;
import io.gravitee.rest.api.service.v4.ApiImagesService;
import io.gravitee.rest.api.service.v4.ApiLicenseService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.gravitee.rest.api.service.v4.ApiWorkflowStateService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Defines the REST resources to manage API v4.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ApiResource extends AbstractResource {

    private static final String REVIEWS_ACTION_ASK = "ask";
    private static final String REVIEWS_ACTION_ACCEPT = "accept";
    private static final String REVIEWS_ACTION_REJECT = "reject";
    private static final Map<String, Excludable> MAPPING_EXCLUDE_EXPORT_PARAMS = Map.of(
        "groups",
        Excludable.GROUPS,
        "plans",
        Excludable.PLANS,
        "members",
        Excludable.MEMBERS,
        "pages",
        Excludable.PAGES_MEDIA,
        "metadata",
        Excludable.METADATA
    );

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ParameterService parameterService;

    @Inject
    private ApiStateService apiStateService;

    @Inject
    private ApiImagesService apiImagesService;

    @Inject
    private ExportApiUseCase exportApiUseCase;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApiLicenseService apiLicenseService;

    @Inject
    private ApiWorkflowStateService apiWorkflowStateService;

    @Inject
    private ApiDuplicatorService apiDuplicatorService;

    @Inject
    private ApiDuplicateService duplicateApiService;

    @Inject
    UpdateFederatedApiUseCase updateFederatedApiUseCase;

    @Inject
    ExportApiCRDUseCase exportApiCRDUseCase;

    @Inject
    private RollbackApiUseCase rollbackApiUseCase;

    @Inject
    private GetApiDefinitionUseCase getApiDefinitionUseCase;

    @Inject
    UpdateNativeApiUseCase updateNativeApiUseCase;

    @Inject
    GetExposedEntrypointsUseCase getExposedEntrypointsUseCase;

    @Inject
    MigrateApiUseCase migrateApiUseCase;

    @Inject
    CreatePromotionUseCase promotionUseCase;

    @Context
    protected UriInfo uriInfo;

    @Path("/plans")
    public ApiPlansResource getApiPlansResource() {
        return resourceContext.getResource(ApiPlansResource.class);
    }

    @Path("/subscriptions")
    public ApiSubscriptionsResource getApiSubscriptionsResource() {
        return resourceContext.getResource(ApiSubscriptionsResource.class);
    }

    @Path("/members")
    public ApiMembersResource getApiMembersResource() {
        return resourceContext.getResource(ApiMembersResource.class);
    }

    @Path("/primaryowner")
    public PrimaryOwnerResource getApiPrimaryOwner() {
        return resourceContext.getResource(PrimaryOwnerResource.class);
    }

    @Path("/logs")
    public ApiLogsResource getApiLogsResource() {
        return resourceContext.getResource(ApiLogsResource.class);
    }

    @Path("/analytics")
    public ApiAnalyticsResource getApiAnalyticsResource() {
        return resourceContext.getResource(ApiAnalyticsResource.class);
    }

    @Path("/health")
    public ApiHealthResource geApiHealthResource() {
        return resourceContext.getResource(ApiHealthResource.class);
    }

    @Path("/audits")
    public ApiAuditsResource getApiAuditsResource() {
        return resourceContext.getResource(ApiAuditsResource.class);
    }

    @Path("/events")
    public ApiEventsResource getApiEventsResource() {
        return resourceContext.getResource(ApiEventsResource.class);
    }

    @Path("/pages")
    public ApiPagesResource getApiPagesResource() {
        return resourceContext.getResource(ApiPagesResource.class);
    }

    @Path("/metadata")
    public ApiMetadataResource getApiMetadataResource() {
        return resourceContext.getResource(ApiMetadataResource.class);
    }

    @Path("/debug")
    public ApiDebugResource getApiDebugResource() {
        return resourceContext.getResource(ApiDebugResource.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiById(@PathParam("apiId") String apiId) {
        final GenericApiEntity apiEntity = getGenericApiEntityById(apiId, true);
        return apiResponse(apiEntity);
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
        @Valid @NotNull final UpdateGenericApi updateApi
    ) {
        return switch (updateApi.getDefinitionVersion()) {
            case V4 -> {
                final GenericApiEntity currentEntity = getGenericApiEntityById(apiId, false);
                evaluateIfMatch(headers, Long.toString(currentEntity.getUpdatedAt().getTime()));
                if (!(currentEntity instanceof ApiEntity) && !(currentEntity instanceof NativeApiEntity)) {
                    yield Response.status(Response.Status.BAD_REQUEST).entity(apiInvalid(apiId)).build();
                }
                yield apiResponse(updateApiV4(currentEntity, (UpdateApiV4) updateApi));
            }
            case V2 -> {
                final GenericApiEntity currentEntity = getGenericApiEntityById(apiId, false);
                evaluateIfMatch(headers, Long.toString(currentEntity.getUpdatedAt().getTime()));
                yield currentEntity instanceof io.gravitee.rest.api.model.api.ApiEntity
                    ? apiResponse(updateApiV2(currentEntity, (UpdateApiV2) updateApi))
                    : Response.status(Response.Status.BAD_REQUEST).entity(apiInvalid(apiId)).build();
            }
            case FEDERATED -> {
                var input = UpdateFederatedApiUseCase.Input.builder()
                    .apiToUpdate(ApiMapper.INSTANCE.mapToApiCore((UpdateApiFederated) updateApi, apiId))
                    .auditInfo(getAuditInfo())
                    .build();
                var output = updateFederatedApiUseCase.execute(input);

                yield apiResponse(
                    ApiAdapter.INSTANCE.toFederatedApiEntity(
                        ApiAdapter.INSTANCE.toRepository(output.updatedApi()),
                        output.primaryOwnerEntity()
                    )
                );
            }
            default -> throw new ApiDefinitionVersionNotSupportedException(updateApi.getDefinitionVersion().name());
        };
    }

    private GenericApiEntity updateApiV4(GenericApiEntity currentEntity, UpdateApiV4 updateApiV4) {
        return currentEntity instanceof NativeApiEntity nativeApi
            ? updateNativeApiV4(nativeApi, updateApiV4)
            : updateHttpApiV4(currentEntity, updateApiV4);
    }

    private NativeApiEntity updateNativeApiV4(NativeApiEntity currentEntity, UpdateApiV4 updateApiV4) {
        UpdateNativeApi apiToUpdate = ApiMapper.INSTANCE.mapToUpdateNativeApi(updateApiV4, currentEntity.getId());

        if (
            !hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_GATEWAY_DEFINITION,
                currentEntity.getId(),
                RolePermissionAction.UPDATE
            ) &&
            !Objects.equals(currentEntity.getPrimaryOwner().getId(), getAuthenticatedUser()) &&
            !isAdmin()
        ) {
            apiToUpdate.setListeners(currentEntity.getListeners());
        }

        var result = updateNativeApiUseCase.execute(new UpdateNativeApiUseCase.Input(apiToUpdate, getAuditInfo()));
        return ApiAdapter.INSTANCE.toNativeApiEntity(result.updatedApi());
    }

    private ApiEntity updateHttpApiV4(GenericApiEntity currentEntity, UpdateApiV4 updateApiV4) {
        UpdateApiEntity apiToUpdate = ApiMapper.INSTANCE.map(updateApiV4, currentEntity.getId());
        // Force listeners if user is not the primary_owner or an administrator
        if (
            !hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_GATEWAY_DEFINITION,
                currentEntity.getId(),
                RolePermissionAction.UPDATE
            ) &&
            !Objects.equals(currentEntity.getPrimaryOwner().getId(), getAuthenticatedUser()) &&
            !isAdmin()
        ) {
            apiToUpdate.setListeners(((ApiEntity) currentEntity).getListeners());
        }

        final ApiEntity updatedApi = apiServiceV4.update(
            GraviteeContext.getExecutionContext(),
            currentEntity.getId(),
            apiToUpdate,
            false, //TODO as plans should be updated in a separate endpoint, we don't need to check for plans. Once "/v4" resources in MAPI V1 are removed, we can remove this parameter.
            getAuthenticatedUser()
        );
        setPicturesUrl(updatedApi);
        return updatedApi;
    }

    private GenericApiEntity updateApiV2(GenericApiEntity currentEntity, UpdateApiV2 updateApiV2) {
        io.gravitee.rest.api.model.api.UpdateApiEntity apiToUpdate = ApiMapper.INSTANCE.map(updateApiV2);
        // Force context-path if user is not the primary_owner or an administrator
        if (
            !hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_GATEWAY_DEFINITION,
                currentEntity.getId(),
                RolePermissionAction.UPDATE
            ) &&
            !Objects.equals(currentEntity.getPrimaryOwner().getId(), getAuthenticatedUser()) &&
            !isAdmin()
        ) {
            apiToUpdate.getProxy().setVirtualHosts(((io.gravitee.rest.api.model.api.ApiEntity) currentEntity).getProxy().getVirtualHosts());
        }
        final io.gravitee.rest.api.model.api.ApiEntity updatedApi = apiService.update(
            GraviteeContext.getExecutionContext(),
            currentEntity.getId(),
            apiToUpdate,
            false
        );
        setPicturesUrl(updatedApi);
        return updatedApi;
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.DELETE) })
    public Response deleteApi(@PathParam("apiId") String apiId, @QueryParam("closePlans") boolean closePlans) {
        apiServiceV4.delete(GraviteeContext.getExecutionContext(), apiId, closePlans);

        return Response.noContent().build();
    }

    @POST
    @Path("/deployments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response deployApi(@PathParam("apiId") String apiId, @Valid final ApiDeploymentEntity apiDeploymentEntity) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        apiLicenseService.checkLicense(executionContext, apiId);
        GenericApiEntity apiEntity = apiStateService.deploy(executionContext, apiId, getAuthenticatedUser(), apiDeploymentEntity);
        return Response.accepted().tag(Long.toString(apiEntity.getUpdatedAt().getTime())).lastModified(apiEntity.getUpdatedAt()).build();
    }

    @GET
    @Path("/deployments/current")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApiDeployments(@PathParam("apiId") String apiId) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        apiLicenseService.checkLicense(executionContext, apiId);

        var output = getApiDefinitionUseCase.execute(new GetApiDefinitionUseCase.Input(apiId));
        if (output.apiDefinition() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    new Error()
                        .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
                        .message("Get current deployment for FEDERATED API is not supported")
                        .technicalCode("api.deployment.federated")
                )
                .build();
        }
        return Response.ok(output.apiDefinition()).build();
    }

    @GET
    @Path("/deployments/_verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response verifyApiDeployment(@PathParam("apiId") String apiId) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        try {
            apiLicenseService.checkLicense(executionContext, apiId);
            return Response.ok(new VerifyApiDeploymentResponse().ok(true)).build();
        } catch (ForbiddenFeatureException | InvalidLicenseException | TechnicalManagementException e) {
            return Response.ok(new VerifyApiDeploymentResponse().ok(false).reason(e.getMessage())).build();
        }
    }

    @GET
    @Path("/_export/definition")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response exportApiDefinition(
        @PathParam("apiId") String apiId,
        @QueryParam("excludeAdditionalData") Set<String> excludeAdditionalData
    ) {
        var input = ExportApiUseCase.Input.of(
            apiId,
            getAuditInfo(),
            stream(excludeAdditionalData).flatMap(ApiResource::mapExcludedExportParams).toList()
        );
        var export = exportApiUseCase.execute(input);

        return Response.ok(ImportExportApiMapper.INSTANCE.map(export.definition()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=%s".formatted(export.filename()))
            .build();
    }

    @GET
    @Path("/_export/crd")
    @Produces(YamlWriter.MEDIA_TYPE)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response exportApiCRD(@PathParam("apiId") String apiId) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();
        var input = new ExportApiCRDUseCase.Input(
            apiId,
            IDExportStrategy.GUID,
            AuditInfo.builder()
                .organizationId(executionContext.getOrganizationId())
                .environmentId(executionContext.getEnvironmentId())
                .actor(
                    AuditActor.builder()
                        .userId(userDetails.getUsername())
                        .userSource(userDetails.getSource())
                        .userSourceId(userDetails.getSourceId())
                        .build()
                )
                .build()
        );
        var output = exportApiCRDUseCase.execute(input);
        var spec = ApiCRDMapper.INSTANCE.map(output.spec());
        return Response.ok(new ApiCRD(spec)).build();
    }

    @POST
    @Path("/_duplicate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response duplicateAPI(@PathParam("apiId") String apiId, @Valid @NotNull DuplicateApiOptions duplicateOptions) {
        if (
            !hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_API,
                GraviteeContext.getCurrentEnvironment(),
                RolePermissionAction.CREATE
            ) ||
            !hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_DEFINITION, apiId, RolePermissionAction.READ)
        ) {
            throw new ForbiddenAccessException();
        }

        final GenericApiEntity currentEntity = getGenericApiEntityById(apiId, false);

        GenericApiEntity duplicate;

        return switch (currentEntity.getDefinitionVersion()) {
            case V1 -> Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    new Error()
                        .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
                        .message("Duplicating V1 API is not supported")
                        .technicalCode("api.duplicate.v1")
                )
                .build();
            case FEDERATED -> Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    new Error()
                        .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
                        .message("Duplicating FEDERATED API is not supported")
                        .technicalCode("api.duplicate.federated")
                )
                .build();
            case V4 -> {
                duplicate = duplicateApiService.duplicate(
                    GraviteeContext.getExecutionContext(),
                    (ApiEntity) currentEntity,
                    DuplicateApiMapper.INSTANCE.map(duplicateOptions)
                );
                yield apiResponse(duplicate);
            }
            case V2 -> {
                duplicate = apiDuplicatorService.duplicate(
                    GraviteeContext.getExecutionContext(),
                    (io.gravitee.rest.api.model.api.ApiEntity) currentEntity,
                    DuplicateApiMapper.INSTANCE.mapToV2(duplicateOptions)
                );
                yield apiResponse(duplicate);
            }
            case FEDERATED_AGENT -> Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    new Error()
                        .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
                        .message("Duplicating FEDERATED Agent is not supported")
                        .technicalCode("api.duplicate.federated")
                )
                .build();
        };
    }

    @POST
    @Path("/_start")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response startAPI(@Context HttpHeaders headers, @PathParam("apiId") String apiId) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        GenericApiEntity genericApiEntity = getGenericApiEntityById(apiId, false);

        apiLicenseService.checkLicense(executionContext, apiId);

        evaluateIfMatch(headers, Long.toString(genericApiEntity.getUpdatedAt().getTime()));

        checkApiLifeCycle(genericApiEntity, LifecycleAction.START);
        GenericApiEntity updatedApi = apiStateService.start(executionContext, genericApiEntity.getId(), getAuthenticatedUser());

        return Response.noContent().tag(Long.toString(updatedApi.getUpdatedAt().getTime())).lastModified(updatedApi.getUpdatedAt()).build();
    }

    @POST
    @Path("/_stop")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response stopAPI(@Context HttpHeaders headers, @PathParam("apiId") String apiId) {
        GenericApiEntity genericApiEntity = getGenericApiEntityById(apiId, false);
        evaluateIfMatch(headers, Long.toString(genericApiEntity.getUpdatedAt().getTime()));

        checkApiLifeCycle(genericApiEntity, LifecycleAction.STOP);
        GenericApiEntity updatedApi = apiStateService.stop(
            GraviteeContext.getExecutionContext(),
            genericApiEntity.getId(),
            getAuthenticatedUser()
        );

        return Response.noContent().tag(Long.toString(updatedApi.getUpdatedAt().getTime())).lastModified(updatedApi.getUpdatedAt()).build();
    }

    @Path("/scoring")
    public ApiScoringResource getApiScoringResource() {
        return resourceContext.getResource(ApiScoringResource.class);
    }

    @Path("/spec-gen")
    public ApiSpecGenResource getApiSpecGenResource() {
        return resourceContext.getResource(ApiSpecGenResource.class);
    }

    @POST
    @Path("/_transfer-ownership")
    @Permissions({ @Permission(value = RolePermission.API_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response transferOwnership(@PathParam("apiId") String apiId, ApiTransferOwnership apiTransferOwnership) {
        if (!apiSearchService.exists(apiId)) {
            throw new ApiNotFoundException(apiId);
        }

        List<RoleEntity> newRoles = new ArrayList<>();

        if (apiTransferOwnership.getPoRole() != null) {
            assertNoPrimaryOwnerReassignment(apiTransferOwnership.getPoRole());
            roleService
                .findByScopeAndName(RoleScope.API, apiTransferOwnership.getPoRole(), GraviteeContext.getCurrentOrganization())
                .ifPresent(newRoles::add);
        }

        membershipService.transferApiOwnership(
            GraviteeContext.getExecutionContext(),
            apiId,
            new MembershipService.MembershipMember(
                apiTransferOwnership.getUserId(),
                apiTransferOwnership.getUserReference(),
                apiTransferOwnership.getUserType() != null ? MembershipMemberType.valueOf(apiTransferOwnership.getUserType().name()) : null
            ),
            newRoles
        );
        return Response.noContent().build();
    }

    @GET
    @Path("picture")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApiPicture(@Context Request request, @PathParam("apiId") String apiId) {
        return imageResponse(request, apiImagesService.getApiPicture(GraviteeContext.getExecutionContext(), apiId));
    }

    @PUT
    @Path("picture")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response updateApiPicture(@PathParam("apiId") String apiId, String pictureContent) {
        try {
            ImageUtils.verify(pictureContent);
            apiImagesService.updateApiPicture(GraviteeContext.getExecutionContext(), apiId, pictureContent);
            return Response.noContent().build();
        } catch (InvalidImageException e) {
            log.warn("Error while parsing picture for api {}", apiId, e);
            throw new BadRequestException("Invalid image format");
        }
    }

    @DELETE
    @Path("picture")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response deleteApiPicture(@PathParam("apiId") String apiId) {
        apiImagesService.updateApiPicture(GraviteeContext.getExecutionContext(), apiId, null);
        return Response.noContent().build();
    }

    @GET
    @Path("background")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApiBackground(@Context Request request, @PathParam("apiId") String apiId) {
        return imageResponse(request, apiImagesService.getApiBackground(GraviteeContext.getExecutionContext(), apiId));
    }

    @PUT
    @Path("background")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response updateApiBackground(@PathParam("apiId") String apiId, String backgroundContent) {
        try {
            ImageUtils.verifyAndRescale(backgroundContent);
            apiImagesService.updateApiBackground(GraviteeContext.getExecutionContext(), apiId, backgroundContent);
            return Response.noContent().build();
        } catch (InvalidImageException e) {
            log.warn("Error while parsing background image for api {}", apiId, e);
            throw new BadRequestException("Invalid image format");
        }
    }

    @DELETE
    @Path("background")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response deleteApiBackground(@PathParam("apiId") String apiId) {
        apiImagesService.updateApiBackground(GraviteeContext.getExecutionContext(), apiId, null);
        return Response.noContent().build();
    }

    @GET
    @Path("subscribers")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public SubscribersResponse getApiSubscribers(
        @PathParam("apiId") String apiId,
        @QueryParam("name") String name,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        Set<String> applicationIds = subscriptionService
            .findByApi(executionContext, apiId)
            .stream()
            .map(SubscriptionEntity::getApplication)
            .collect(Collectors.toSet());

        if (applicationIds.isEmpty()) {
            return new SubscribersResponse().data(emptyList()).pagination(new Pagination()).links(null);
        }

        ApplicationQuery applicationQuery = new ApplicationQuery();
        applicationQuery.setIds(applicationIds);
        if (name != null && !name.isEmpty()) {
            applicationQuery.setName(name);
        }

        Sortable sortable = new SortableImpl("name", true);

        Page<ApplicationListItem> subscribersApplicationPage = applicationService.search(
            executionContext,
            applicationQuery,
            sortable,
            paginationParam.toPageable()
        );

        long totalCount = subscribersApplicationPage.getTotalElements();
        Integer pageItemsCount = Math.toIntExact(subscribersApplicationPage.getPageElements());
        return new SubscribersResponse()
            .data(ApplicationMapper.INSTANCE.mapToBaseApplicationList(subscribersApplicationPage.getContent()))
            .pagination(PaginationInfo.computePaginationInfo(totalCount, pageItemsCount, paginationParam))
            .links(computePaginationLinks(totalCount, paginationParam));
    }

    @POST
    @Path("/reviews/_ask")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response reviewsAsk(@Context HttpHeaders headers, @PathParam("apiId") String apiId, @Valid ApiReview apiReview) {
        GenericApiEntity genericApiEntity = getGenericApiEntityById(apiId, false);
        evaluateIfMatch(headers, Long.toString(genericApiEntity.getUpdatedAt().getTime()));

        checkApiReviewWorkflow(genericApiEntity, REVIEWS_ACTION_ASK);
        GenericApiEntity updatedApi = apiWorkflowStateService.askForReview(
            GraviteeContext.getExecutionContext(),
            apiId,
            getAuthenticatedUser(),
            ApiMapper.INSTANCE.map(apiReview)
        );

        return Response.noContent().tag(Long.toString(updatedApi.getUpdatedAt().getTime())).lastModified(updatedApi.getUpdatedAt()).build();
    }

    @POST
    @Path("/reviews/_accept")
    @Permissions({ @Permission(value = RolePermission.API_REVIEWS, acls = RolePermissionAction.UPDATE) })
    public Response reviewsAccept(@Context HttpHeaders headers, @PathParam("apiId") String apiId, @Valid ApiReview apiReview) {
        GenericApiEntity genericApiEntity = getGenericApiEntityById(apiId, false);
        evaluateIfMatch(headers, Long.toString(genericApiEntity.getUpdatedAt().getTime()));

        checkApiReviewWorkflow(genericApiEntity, REVIEWS_ACTION_ACCEPT);
        GenericApiEntity updatedApi = apiWorkflowStateService.acceptReview(
            GraviteeContext.getExecutionContext(),
            apiId,
            getAuthenticatedUser(),
            ApiMapper.INSTANCE.map(apiReview)
        );

        return Response.noContent().tag(Long.toString(updatedApi.getUpdatedAt().getTime())).lastModified(updatedApi.getUpdatedAt()).build();
    }

    @POST
    @Path("/reviews/_reject")
    @Permissions({ @Permission(value = RolePermission.API_REVIEWS, acls = RolePermissionAction.UPDATE) })
    public Response reviewsReject(@Context HttpHeaders headers, @PathParam("apiId") String apiId, @Valid ApiReview apiReview) {
        GenericApiEntity genericApiEntity = getGenericApiEntityById(apiId, false);
        evaluateIfMatch(headers, Long.toString(genericApiEntity.getUpdatedAt().getTime()));

        checkApiReviewWorkflow(genericApiEntity, REVIEWS_ACTION_REJECT);
        GenericApiEntity updatedApi = apiWorkflowStateService.rejectReview(
            GraviteeContext.getExecutionContext(),
            apiId,
            getAuthenticatedUser(),
            ApiMapper.INSTANCE.map(apiReview)
        );

        return Response.noContent().tag(Long.toString(updatedApi.getUpdatedAt().getTime())).lastModified(updatedApi.getUpdatedAt()).build();
    }

    @POST
    @Path("/_rollback")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response rollback(ApiRollback apiRollback) {
        if (apiRollback.getEventId() == null) {
            log.warn("Event ID is required");
            throw new BadRequestException("Event ID is required");
        }

        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        this.rollbackApiUseCase.execute(
            new RollbackApiUseCase.Input(
                apiRollback.getEventId(),
                AuditInfo.builder()
                    .organizationId(executionContext.getOrganizationId())
                    .environmentId(executionContext.getEnvironmentId())
                    .actor(
                        AuditActor.builder()
                            .userId(userDetails.getUsername())
                            .userSource(userDetails.getSource())
                            .userSourceId(userDetails.getSourceId())
                            .build()
                    )
                    .build()
            )
        );

        return Response.noContent().build();
    }

    @GET
    @Path("/exposedEntrypoints")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response exposedEntrypoints(@PathParam("apiId") String apiId) {
        var executionContext = GraviteeContext.getExecutionContext();

        var input = new GetExposedEntrypointsUseCase.Input(
            executionContext.getOrganizationId(),
            executionContext.getEnvironmentId(),
            apiId
        );
        var output = getExposedEntrypointsUseCase.execute(input);

        return Response.ok().entity(ApiMapper.INSTANCE.map(output.exposedEntrypoints())).build();
    }

    @POST
    @Path("/_migrate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public MigrationReportResponses migrateApi(@PathParam("apiId") String apiId, @QueryParam("mode") MigrateMode mode) {
        var upgradeMode = mode != null ? MigrateApiUseCase.Input.UpgradeMode.valueOf(mode.name()) : null;
        var output = migrateApiUseCase.execute(new MigrateApiUseCase.Input(apiId, upgradeMode, getAuditInfo()));
        return new MigrationReportResponses()
            .state(mapState(output.state()))
            .issues(
                stream(output.issues())
                    .map(issue -> new MigrationReportResponsesIssuesInner().message(issue.message()).state(mapState(issue.state())))
                    .toList()
            );
    }

    @POST
    @Path("_promote")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response promoteAPI(@RequestBody @Valid @NotNull final PromotionRequest promotionRequest, @PathParam("apiId") String apiId) {
        var input = new CreatePromotionUseCase.Input(apiId, PromotionMapper.INSTANCE.map(promotionRequest), getAuditInfo());
        var output = promotionUseCase.execute(input);
        return Response.ok(PromotionMapper.INSTANCE.map(output.promotion())).build();
    }

    private static MigrationStateType mapState(MigrationResult.State state) {
        return switch (state) {
            case MIGRATED -> MigrationStateType.MIGRATED;
            case MIGRATABLE -> MigrationStateType.MIGRATABLE;
            case IMPOSSIBLE -> MigrationStateType.IMPOSSIBLE;
            case CAN_BE_FORCED -> MigrationStateType.CAN_BE_FORCED;
        };
    }

    public enum MigrateMode {
        DRY_RUN,
        FORCE,
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

        switch (apiEntity) {
            case ApiEntity apiEntityV4 -> {
                apiEntityV4.setPictureUrl(pictureUrl);
                apiEntityV4.setPicture(null);
                apiEntityV4.setBackgroundUrl(backgroundUrl);
                apiEntityV4.setBackground(null);
            }
            case NativeApiEntity nativeApiEntityV4 -> {
                nativeApiEntityV4.setPictureUrl(pictureUrl);
                nativeApiEntityV4.setPicture(null);
                nativeApiEntityV4.setBackgroundUrl(backgroundUrl);
                nativeApiEntityV4.setBackground(null);
            }
            case io.gravitee.rest.api.model.api.ApiEntity apiEntityV2 -> {
                apiEntityV2.setPictureUrl(pictureUrl);
                apiEntityV2.setPicture(null);
                apiEntityV2.setBackgroundUrl(backgroundUrl);
                apiEntityV2.setBackground(null);
            }
            default -> {}
        }
    }

    private void filterSensitiveData(GenericApiEntity apiEntity) {
        switch (apiEntity) {
            case ApiEntity asApiEntity -> filterSensitiveData(asApiEntity);
            case NativeApiEntity asNativeApiEntity -> filterSensitiveData(asNativeApiEntity);
            case io.gravitee.rest.api.model.api.ApiEntity asApiEntityV2 -> filterSensitiveData(asApiEntityV2);
            case null, default -> {}
        }
    }

    private void filterSensitiveData(ApiEntity apiEntity) {
        List<Listener> listeners = apiEntity.getListeners();

        stream(listeners)
            .filter(listener -> ListenerType.HTTP == listener.getType())
            .findFirst()
            .ifPresent(first -> {
                if (first instanceof HttpListener httpListener) {
                    if (isNotEmpty(httpListener.getPaths())) {
                        var path = httpListener.getPaths().getFirst();
                        var filteredPath = new io.gravitee.definition.model.v4.listener.http.Path(path.getPath());
                        httpListener.setPaths(List.of(filteredPath));
                    }
                    httpListener.setPathMappings(null);
                }
            });
        apiEntity.setProperties(null);
        apiEntity.setServices(null);
        apiEntity.setResources(null);
        apiEntity.setResponseTemplates(null);
    }

    private void filterSensitiveData(NativeApiEntity apiEntity) {
        apiEntity.setProperties(null);
        apiEntity.setServices(null);
        apiEntity.setResources(null);
    }

    private void filterSensitiveData(io.gravitee.rest.api.model.api.ApiEntity apiEntity) {
        final Proxy filteredProxy = new Proxy();
        final VirtualHost virtualHost = apiEntity.getProxy().getVirtualHosts().getFirst();
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
                ) &&
                !WorkflowState.REVIEW_OK.equals(api.getWorkflowState())
            ) {
                throw new BadRequestException("API cannot be started without being reviewed");
            }
        }
    }

    private Response imageResponse(final Request request, InlinePictureEntity image) {
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

    private Response apiResponse(GenericApiEntity apiEntity) {
        boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);
        return Response.ok(ApiMapper.INSTANCE.map(apiEntity, uriInfo, isSynchronized))
            .tag(Long.toString(apiEntity.getUpdatedAt().getTime()))
            .lastModified(apiEntity.getUpdatedAt())
            .build();
    }

    private Error apiInvalid(String apiId) {
        return new Error()
            .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
            .message("Api [" + apiId + "] is not valid.")
            .putParametersItem("apiId", apiId)
            .technicalCode("api.invalid");
    }

    private void checkApiReviewWorkflow(final GenericApiEntity api, final String action) {
        if (ApiLifecycleState.ARCHIVED.equals(api.getLifecycleState())) {
            throw new BadRequestException("Deleted API cannot be reviewed.");
        }
        if (api.getWorkflowState() != null) {
            switch (api.getWorkflowState()) {
                case IN_REVIEW:
                    if (REVIEWS_ACTION_ASK.equals(action)) {
                        throw new BadRequestException("Review is still in progress.");
                    }
                    break;
                case DRAFT:
                    if (REVIEWS_ACTION_ACCEPT.equals(action) || REVIEWS_ACTION_REJECT.equals(action)) {
                        throw new BadRequestException("State invalid to accept/reject a review.");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static Stream<Excludable> mapExcludedExportParams(@Nullable String ex) {
        if (ex == null) {
            return Stream.empty();
        }
        var excludable = MAPPING_EXCLUDE_EXPORT_PARAMS.get(ex);
        if (excludable == null) {
            log.debug("Unknown excluded '{}'", ex);
        }
        return Stream.ofNullable(excludable);
    }

    private void assertNoPrimaryOwnerReassignment(String poRole) {
        if ("PRIMARY_OWNER".equals(poRole)) {
            throw new TransferOwnershipNotAllowedException(poRole);
        }
    }
}
