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

import static java.lang.String.format;

import io.gravitee.apim.core.api_key.use_case.RevokeApiSubscriptionApiKeyUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.apim.core.subscription.use_case.AcceptSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.CloseSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.DeleteSubscriptionSpecUseCase;
import io.gravitee.apim.core.subscription.use_case.ImportSubscriptionSpecUseCase;
import io.gravitee.apim.core.subscription.use_case.RejectSubscriptionUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.*;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.TransferSubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionMetadataQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSubscriptionsResource extends AbstractResource {

    private final SubscriptionMapper subscriptionMapper = SubscriptionMapper.INSTANCE;
    private final PageMapper pageMapper = PageMapper.INSTANCE;
    private final PlanMapper planMapper = PlanMapper.INSTANCE;

    private final ApiMapper apiMapper = ApiMapper.INSTANCE;
    private final ApplicationMapper applicationMapper = ApplicationMapper.INSTANCE;
    private final UserMapper userMapper = UserMapper.INSTANCE;
    private static final String EXPAND_API = "api";
    private static final String EXPAND_PLAN = "plan";
    private static final String EXPAND_APPLICATION = "application";
    private static final String EXPAND_SUBSCRIBED_BY = "subscribedBy";

    @Inject
    private CloseSubscriptionUseCase closeSubscriptionUsecase;

    @Inject
    private AcceptSubscriptionUseCase acceptSubscriptionUsecase;

    @Inject
    private RejectSubscriptionUseCase rejectSubscriptionUseCase;

    @Inject
    private RevokeApiSubscriptionApiKeyUseCase revokeApiSubscriptionApiKeyUsecase;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private PlanSearchService planSearchService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ParameterService parameterService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private UserService userService;

    @Inject
    private SecurityContext securityContext;

    @Inject
    private ImportSubscriptionSpecUseCase importSubscriptionSpecUseCase;

    @Inject
    private DeleteSubscriptionSpecUseCase deleteSubscriptionSpecUseCase;

    @PathParam("apiId")
    private String apiId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.READ }) })
    public SubscriptionsResponse getApiSubscriptions(
        @QueryParam("applicationIds") Set<String> applicationIds,
        @QueryParam("planIds") Set<String> planIds,
        @QueryParam("statuses") @DefaultValue("ACCEPTED") Set<SubscriptionStatus> statuses,
        @QueryParam("apiKey") String apiKey,
        @QueryParam("expands") Set<String> expands,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        final Page<SubscriptionEntity> subscriptionPage = getSubscriptionEntityPage(
            applicationIds,
            planIds,
            statuses,
            apiKey,
            paginationParam,
            GraviteeContext.getExecutionContext()
        );

        final List<Subscription> subscriptions = subscriptionMapper.mapToList(subscriptionPage.getContent());
        expandData(subscriptions, expands);

        long totalCount = subscriptionPage.getTotalElements();
        Integer pageItemsCount = Math.toIntExact(subscriptionPage.getPageElements());
        return new SubscriptionsResponse()
            .data(subscriptions)
            .pagination(PaginationInfo.computePaginationInfo(totalCount, pageItemsCount, paginationParam))
            .links(computePaginationLinks(totalCount, paginationParam));
    }

    @GET
    @Path("/_export")
    @Produces("text/csv")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public Response exportApiSubscriptions(
        @QueryParam("applicationIds") Set<String> applicationIds,
        @QueryParam("planIds") Set<String> planIds,
        @QueryParam("statuses") @DefaultValue("ACCEPTED") Set<SubscriptionStatus> statuses,
        @QueryParam("apiKey") String apiKey,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final Page<SubscriptionEntity> subscriptionPage = getSubscriptionEntityPage(
            applicationIds,
            planIds,
            statuses,
            apiKey,
            paginationParam,
            executionContext
        );

        final Map<String, Map<String, Object>> metadata;

        if (subscriptionPage.getPageElements() > 0) {
            final SubscriptionMetadataQuery subscriptionMetadataQuery = new SubscriptionMetadataQuery(
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                subscriptionPage.getContent()
            )
                .withApis(true)
                .withApplications(true)
                .withPlans(true);

            metadata = subscriptionService.getMetadata(executionContext, subscriptionMetadataQuery).toMap();
        } else {
            metadata = new HashMap<>();
        }

        return Response
            .ok(subscriptionService.exportAsCsv(subscriptionPage.getContent(), metadata))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                format("attachment;filename=subscriptions-%s-%s.csv", apiId, System.currentTimeMillis())
            )
            .build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.CREATE }) })
    public Response createApiSubscription(@Valid @NotNull CreateSubscription createSubscription) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        if (
            StringUtils.isNotEmpty(createSubscription.getCustomApiKey()) &&
            !parameterService.findAsBoolean(executionContext, Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED, ParameterReferenceType.ENVIRONMENT)
        ) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(subscriptionInvalid("You are not allowed to provide a custom API Key"))
                .build();
        }

        final NewSubscriptionEntity newSubscriptionEntity = subscriptionMapper.map(createSubscription);
        SubscriptionEntity created = subscriptionService.create(
            executionContext,
            newSubscriptionEntity,
            createSubscription.getCustomApiKey()
        );
        var subscription = subscriptionMapper.map(created);

        if (created.getStatus() == io.gravitee.rest.api.model.SubscriptionStatus.PENDING) {
            var result = acceptSubscriptionUsecase.execute(
                AcceptSubscriptionUseCase.Input.builder().subscriptionId(created.getId()).apiId(apiId).auditInfo(getAuditInfo()).build()
            );
            subscription = subscriptionMapper.map(result.subscription());
        }

        return Response.created(this.getLocationHeader(created.getId())).entity(subscription).build();
    }

    @POST
    @Path("_verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.CREATE) })
    public Response verifyCreateApiSubscription(@Valid @NotNull VerifySubscription verifySubscriptionSubscription) {
        boolean canCreate = apiKeyService.canCreate(
            GraviteeContext.getExecutionContext(),
            verifySubscriptionSubscription.getApiKey(),
            apiId,
            verifySubscriptionSubscription.getApplicationId()
        );
        return Response.ok(VerifySubscriptionResponse.builder().ok(canCreate).build()).build();
    }

    private Page<SubscriptionEntity> getSubscriptionEntityPage(
        Set<String> applicationIds,
        Set<String> planIds,
        Set<SubscriptionStatus> statuses,
        String apiKey,
        PaginationParam paginationParam,
        ExecutionContext executionContext
    ) {
        final SubscriptionQuery subscriptionQuery = SubscriptionQuery
            .builder()
            .apis(List.of(apiId))
            .applications(applicationIds)
            .plans(planIds)
            .statuses(subscriptionMapper.mapToStatusSet(statuses))
            .apiKey(apiKey)
            .build();

        final Page<SubscriptionEntity> subscriptionPage = subscriptionService.search(
            executionContext,
            subscriptionQuery,
            paginationParam.toPageable(),
            false,
            false
        );
        return subscriptionPage;
    }

    @GET
    @Path("/{subscriptionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.READ }) })
    public Response getApiSubscription(@PathParam("subscriptionId") String subscriptionId, @QueryParam("expands") Set<String> expands) {
        final SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!subscriptionEntity.getApi().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }

        final Subscription subscription = subscriptionMapper.map(subscriptionEntity);
        expandData(subscription, expands);

        return Response.ok(subscription).build();
    }

    @PUT
    @Path("/spec/_import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importSubscriptionSpec(@Valid SubscriptionCRDSpec spec) {
        return Response.ok(importSubscriptionSpecUseCase.execute(new ImportSubscriptionSpecUseCase.Input(getAuditInfo(), spec))).build();
    }

    @DELETE
    @Path("/spec/{id}")
    public Response deleteSubscriptionSpec(@PathParam("id") String id) {
        deleteSubscriptionSpecUseCase.execute(new DeleteSubscriptionSpecUseCase.Input(getAuditInfo(), id));
        return Response.noContent().build();
    }

    @PUT
    @Path("/{subscriptionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response updateApiSubscription(
        @PathParam("subscriptionId") String subscriptionId,
        @Valid @NotNull UpdateSubscription updateSubscription
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!subscriptionEntity.getApi().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }

        final UpdateSubscriptionEntity updateSubscriptionEntity = subscriptionMapper.map(updateSubscription, subscriptionId);
        return Response
            .status(Response.Status.OK)
            .entity(subscriptionMapper.map(subscriptionService.update(executionContext, updateSubscriptionEntity)))
            .build();
    }

    @POST
    @Path("/{subscriptionId}/_accept")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response acceptApiSubscription(
        @PathParam("subscriptionId") String subscriptionId,
        @Valid @NotNull AcceptSubscription acceptSubscription
    ) {
        var input = new AcceptSubscriptionUseCase.Input(
            apiId,
            subscriptionId,
            acceptSubscription.getStartingAt() != null ? acceptSubscription.getStartingAt().toZonedDateTime() : null,
            acceptSubscription.getEndingAt() != null ? acceptSubscription.getEndingAt().toZonedDateTime() : null,
            acceptSubscription.getReason(),
            acceptSubscription.getCustomApiKey(),
            getAuditInfo()
        );

        return Response.ok().entity(subscriptionMapper.map(acceptSubscriptionUsecase.execute(input).subscription())).build();
    }

    @POST
    @Path("/{subscriptionId}/_reject")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response rejectApiSubscription(
        @PathParam("subscriptionId") String subscriptionId,
        @Valid @NotNull RejectSubscription rejectSubscription
    ) {
        var input = new RejectSubscriptionUseCase.Input(apiId, subscriptionId, rejectSubscription.getReason(), getAuditInfo());
        return Response.ok().entity(subscriptionMapper.map(rejectSubscriptionUseCase.execute(input).subscription())).build();
    }

    @POST
    @Path("/{subscriptionId}/_close")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response closeApiSubscription(@PathParam("subscriptionId") String subscriptionId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final var user = getAuthenticatedUserDetails();

        var result = closeSubscriptionUsecase.execute(
            CloseSubscriptionUseCase.Input
                .builder()
                .subscriptionId(subscriptionId)
                .apiId(apiId)
                .auditInfo(
                    AuditInfo
                        .builder()
                        .organizationId(executionContext.getOrganizationId())
                        .environmentId(executionContext.getEnvironmentId())
                        .actor(
                            AuditActor
                                .builder()
                                .userId(user.getUsername())
                                .userSource(user.getSource())
                                .userSourceId(user.getSourceId())
                                .build()
                        )
                        .build()
                )
                .build()
        );
        return Response.ok(subscriptionMapper.map(result.subscription())).build();
    }

    @POST
    @Path("/{subscriptionId}/_pause")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response pauseApiSubscription(@PathParam("subscriptionId") String subscriptionId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!subscriptionEntity.getApi().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }

        return Response.ok(subscriptionMapper.map(subscriptionService.pause(executionContext, subscriptionId))).build();
    }

    @POST
    @Path("/{subscriptionId}/_resume")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response resumeApiSubscription(@PathParam("subscriptionId") String subscriptionId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!subscriptionEntity.getApi().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }

        return Response.ok(subscriptionMapper.map(subscriptionService.resume(executionContext, subscriptionId))).build();
    }

    @POST
    @Path("/{subscriptionId}/_transfer")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response transferApiSubscription(
        @PathParam("subscriptionId") String subscriptionId,
        @Valid @NotNull TransferSubscription transferSubscription
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!subscriptionEntity.getApi().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }

        final TransferSubscriptionEntity transferSubscriptionEntity = subscriptionMapper.map(transferSubscription, subscriptionId);
        return Response
            .ok(subscriptionMapper.map(subscriptionService.transfer(executionContext, transferSubscriptionEntity, getAuthenticatedUser())))
            .build();
    }

    private void expandData(Subscription subscription, Set<String> expands) {
        expandData(List.of(subscription), expands);
    }

    private void expandData(List<Subscription> subscriptions, Set<String> expands) {
        if (expands == null || expands.isEmpty()) {
            return;
        }

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        if (expands.contains(EXPAND_API)) {
            final BaseApi baseApi = apiMapper.map(apiSearchService.findGenericById(executionContext, apiId));
            subscriptions.forEach(subscription -> subscription.setApi(baseApi));
        }

        if (expands.contains(EXPAND_PLAN)) {
            final Set<String> planIds = subscriptions
                .stream()
                .map(subscription -> (subscription.getPlan()).getId())
                .collect(Collectors.toSet());
            final Collection<BasePlan> plans = planMapper.mapToBasePlans(planSearchService.findByIdIn(executionContext, planIds));
            plans.forEach(plan ->
                subscriptions
                    .stream()
                    .filter(subscription -> subscription.getPlan().getId().equals(plan.getId()))
                    .forEach(subscription -> subscription.setPlan(plan))
            );
        }

        if (expands.contains(EXPAND_APPLICATION)) {
            final Set<String> applicationIds = subscriptions
                .stream()
                .map(subscription -> (subscription.getApplication()).getId())
                .collect(Collectors.toSet());
            final Collection<BaseApplication> applications = applicationMapper.mapToBaseApplicationList(
                applicationService.findByIds(executionContext, applicationIds)
            );
            applications.forEach(application ->
                subscriptions
                    .stream()
                    .filter(subscription -> subscription.getApplication().getId().equals(application.getId()))
                    .forEach(subscription -> subscription.setApplication(application))
            );
        }

        if (expands.contains(EXPAND_SUBSCRIBED_BY)) {
            final Set<String> userIds = subscriptions
                .stream()
                .map(subscription -> subscription.getSubscribedBy().getId())
                .collect(Collectors.toSet());
            final Collection<BaseUser> users = userMapper.mapToBaseUserList(userService.findByIds(executionContext, userIds));
            users.forEach(user ->
                subscriptions
                    .stream()
                    .filter(subscription -> subscription.getSubscribedBy().getId().equals(user.getId()))
                    .forEach(subscription -> subscription.setSubscribedBy(user))
            );
        }
    }

    @GET
    @Path("/{subscriptionId}/api-keys")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public Response getApiSubscriptionApiKeys(
        @PathParam("subscriptionId") String subscriptionId,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        final SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!subscriptionEntity.getApi().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }

        final List<ApiKey> apiKeys = subscriptionMapper.mapToApiKeyList(
            apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), subscriptionId)
        );

        final List<ApiKey> apiKeysSubList = computePaginationData(apiKeys, paginationParam);

        return Response
            .ok(
                new SubscriptionApiKeysResponse()
                    .data(apiKeysSubList)
                    .pagination(PaginationInfo.computePaginationInfo(apiKeys.size(), apiKeysSubList.size(), paginationParam))
                    .links(computePaginationLinks(apiKeys.size(), paginationParam))
            )
            .build();
    }

    @POST
    @Path("/{subscriptionId}/api-keys/_renew")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response renewApiSubscriptionApiKeys(
        @PathParam("subscriptionId") String subscriptionId,
        @Valid @NotNull RenewApiKey renewApiKey
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            StringUtils.isNotEmpty(renewApiKey.getCustomApiKey()) &&
            !parameterService.findAsBoolean(executionContext, Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED, ParameterReferenceType.ENVIRONMENT)
        ) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(subscriptionInvalid("You are not allowed to provide a custom API Key"))
                .build();
        }

        final SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!subscriptionEntity.getApi().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }

        checkApplicationDoesntUseSharedApiKey(executionContext, subscriptionEntity.getApplication());

        final ApiKeyEntity apiKeyEntity = apiKeyService.renew(executionContext, subscriptionEntity, renewApiKey.getCustomApiKey());

        return Response.ok(subscriptionMapper.mapToApiKey(apiKeyEntity)).build();
    }

    @PUT
    @Path("/{subscriptionId}/api-keys/{apiKeyId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response updateApiSubscriptionApiKey(
        @PathParam("subscriptionId") String subscriptionId,
        @PathParam("apiKeyId") String apiKeyId,
        @Valid @NotNull UpdateApiKey updateApiKey
    ) {
        final SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!subscriptionEntity.getApi().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        checkApplicationDoesntUseSharedApiKey(executionContext, subscriptionEntity.getApplication());

        final ApiKeyEntity apiKeyEntity = apiKeyService.findById(executionContext, apiKeyId);

        if (!apiKeyEntity.getSubscriptionIds().contains(subscriptionId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(apiKeyNotFoundError(apiKeyId)).build();
        }

        apiKeyEntity.setExpireAt(updateApiKey.getExpireAt() != null ? Date.from(updateApiKey.getExpireAt().toInstant()) : null);

        return Response.ok(subscriptionMapper.mapToApiKey(apiKeyService.update(executionContext, apiKeyEntity))).build();
    }

    @POST
    @Path("/{subscriptionId}/api-keys/{apiKeyId}/_revoke")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response revokeApiSubscriptionApiKey(
        @PathParam("subscriptionId") String subscriptionId,
        @PathParam("apiKeyId") String apiKeyId
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final var user = getAuthenticatedUserDetails();

        var result = revokeApiSubscriptionApiKeyUsecase.execute(
            new RevokeApiSubscriptionApiKeyUseCase.Input(
                apiKeyId,
                apiId,
                subscriptionId,
                AuditInfo
                    .builder()
                    .organizationId(executionContext.getOrganizationId())
                    .environmentId(executionContext.getEnvironmentId())
                    .actor(
                        AuditActor
                            .builder()
                            .userId(user.getUsername())
                            .userSource(user.getSource())
                            .userSourceId(user.getSourceId())
                            .build()
                    )
                    .build()
            )
        );

        return Response.ok(subscriptionMapper.mapToApiKey(result.apiKey())).build();
    }

    @POST
    @Path("/{subscriptionId}/api-keys/{apiKeyId}/_reactivate")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response reactivateApiSubscriptionApiKey(
        @PathParam("subscriptionId") String subscriptionId,
        @PathParam("apiKeyId") String apiKeyId
    ) {
        final SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!subscriptionEntity.getApi().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApiKeyEntity apiKeyEntity = apiKeyService.findById(executionContext, apiKeyId);

        if (!apiKeyEntity.getSubscriptionIds().contains(subscriptionId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(apiKeyNotFoundError(apiKeyId)).build();
        }

        checkApplicationDoesntUseSharedApiKey(executionContext, apiKeyEntity.getApplication().getId());
        apiKeyEntity = apiKeyService.reactivate(executionContext, apiKeyEntity);

        return Response.ok(subscriptionMapper.mapToApiKey(apiKeyEntity)).build();
    }

    private Error subscriptionNotFoundError(String subscriptionId) {
        return new Error()
            .httpStatus(Response.Status.NOT_FOUND.getStatusCode())
            .message("Subscription [" + subscriptionId + "] cannot be found.")
            .putParametersItem("subscription", subscriptionId)
            .technicalCode("subscription.notFound");
    }

    private Error apiKeyNotFoundError(String apiKeyId) {
        return new Error()
            .httpStatus(Response.Status.NOT_FOUND.getStatusCode())
            .message("No API Key can be found.")
            .putParametersItem("apiKeyId", apiKeyId)
            .technicalCode("apiKey.notFound");
    }

    private Error subscriptionInvalid(String message) {
        return new Error().httpStatus(Response.Status.BAD_REQUEST.getStatusCode()).message(message).technicalCode("subscription.invalid");
    }

    private void checkApplicationDoesntUseSharedApiKey(ExecutionContext executionContext, String applicationId) {
        final ApplicationEntity applicationEntity = applicationService.findById(executionContext, applicationId);

        if (applicationEntity.hasApiKeySharedMode()) {
            throw new InvalidApplicationApiKeyModeException(
                String.format(
                    "Invalid operation for API Key mode [%s] of application [%s].",
                    applicationEntity.getApiKeyMode(),
                    applicationEntity.getId()
                )
            );
        }
    }
}
