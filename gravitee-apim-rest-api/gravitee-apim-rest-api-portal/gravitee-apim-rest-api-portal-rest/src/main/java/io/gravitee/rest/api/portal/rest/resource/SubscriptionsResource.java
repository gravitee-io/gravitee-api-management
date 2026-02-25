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
package io.gravitee.rest.api.portal.rest.resource;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.subscription.model.SubscriptionConfiguration;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.use_case.CreateSubscriptionUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.PageEntity.PageRevisionId;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionMetadataQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.mapper.KeyMapper;
import io.gravitee.rest.api.portal.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.portal.rest.model.ApiLinks;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionConfigurationInput;
import io.gravitee.rest.api.portal.rest.model.SubscriptionInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateSubscriptionUseCase createSubscriptionUseCase;

    @Inject
    private PlanSearchService planSearchService;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private ApiMapper apiMapper;

    @Inject
    private KeyMapper keyMapper;

    @Inject
    private GraviteeMapper graviteeMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSubscription(@Valid @NotNull(message = "Input must not be null.") SubscriptionInput subscriptionInput) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String applicationId = subscriptionInput.getApplication();
        if (hasPermission(executionContext, RolePermission.APPLICATION_SUBSCRIPTION, applicationId, RolePermissionAction.CREATE)) {
            GenericPlanEntity plan = planSearchService.findById(executionContext, subscriptionInput.getPlan());

            var auditInfo = getAuditInfo();

            CreateSubscriptionUseCase.Output output = createSubscriptionUseCase.execute(
                CreateSubscriptionUseCase.Input.builder()
                    .referenceId(plan.getReferenceId())
                    .referenceType(SubscriptionReferenceType.valueOf(plan.getReferenceType().name()))
                    .planId(subscriptionInput.getPlan())
                    .applicationId(applicationId)
                    .requestMessage(subscriptionInput.getRequest())
                    .metadata(subscriptionInput.getMetadata())
                    .configuration(getSubscriptionConfiguration(subscriptionInput))
                    .apiKeyMode(getApiKeyMode(subscriptionInput))
                    .generalConditionsAccepted(subscriptionInput.getGeneralConditionsAccepted())
                    .generalConditionsContentRevision(getPageRevisionId(subscriptionInput))
                    .auditInfo(auditInfo)
                    .build()
            );

            // Fetch legacy subscription entity for response mapping
            SubscriptionEntity createdSubscription = subscriptionService.findById(output.subscription().getId());

            // For consumer convenience, fetch the keys just after the subscription has been created.
            List<Key> keys = apiKeyService
                .findBySubscription(executionContext, createdSubscription.getId())
                .stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .map(keyMapper::convert)
                .toList();

            final Subscription subscription = SubscriptionMapper.INSTANCE.map(createdSubscription);
            subscription.setKeys(keys);

            return Response.ok(subscription).build();
        }
        throw new ForbiddenAccessException();
    }

    private SubscriptionConfiguration getSubscriptionConfiguration(SubscriptionInput subscriptionInput) {
        SubscriptionConfiguration configuration = null;
        SubscriptionConfigurationInput inputConfiguration = subscriptionInput.getConfiguration();
        if (inputConfiguration != null) {
            var entrypointConfigNode = graviteeMapper.valueToTree(inputConfiguration.getEntrypointConfiguration());
            configuration = SubscriptionConfiguration.builder()
                .entrypointId(inputConfiguration.getEntrypointId())
                .channel(inputConfiguration.getChannel())
                .entrypointConfiguration(
                    entrypointConfigNode != null && !entrypointConfigNode.isNull() ? entrypointConfigNode.toString() : null
                )
                .build();
        }
        return configuration;
    }

    private static ApiKeyMode getApiKeyMode(SubscriptionInput subscriptionInput) {
        return subscriptionInput.getApiKeyMode() != null ? ApiKeyMode.valueOf(subscriptionInput.getApiKeyMode().name()) : null;
    }

    private static PageRevisionId getPageRevisionId(SubscriptionInput subscriptionInput) {
        PageRevisionId generalConditionsContentRevision = null;
        if (subscriptionInput.getGeneralConditionsContentRevision() != null) {
            var pageId = subscriptionInput.getGeneralConditionsContentRevision().getPageId();
            var revision = subscriptionInput.getGeneralConditionsContentRevision().getRevision();
            if (pageId == null || revision == null) {
                throw new IllegalArgumentException("generalConditionsContentRevision must contain pageId and revision fields");
            }
            generalConditionsContentRevision = new PageRevisionId(pageId, revision);
        }
        return generalConditionsContentRevision;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSubscriptions(
        @Deprecated @QueryParam("apiId") String apiId,
        @Deprecated @QueryParam("applicationId") String applicationId,
        @QueryParam("apiIds") List<String> apiIds,
        @QueryParam("applicationIds") List<String> applicationIds,
        @QueryParam("statuses") List<SubscriptionStatus> statuses,
        @BeanParam PaginationParam paginationParam
    ) {
        List<String> effectiveApiIds = resolveApiIds(apiId, apiIds);
        List<String> effectiveApplicationIds = resolveApplicationIds(applicationId, applicationIds);

        final SubscriptionQuery query = new SubscriptionQuery();
        if (effectiveApiIds != null && !effectiveApiIds.isEmpty()) {
            query.setApis(effectiveApiIds);
        }
        query.setStatuses(statuses);
        if (apiId == null || apiId.isEmpty()) {
            query.setReferenceType(GenericPlanEntity.ReferenceType.API);
        }

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (effectiveApplicationIds == null || effectiveApplicationIds.isEmpty()) {
            final Set<ApplicationListItem> applications = applicationService.findByUser(executionContext, getAuthenticatedUser());
            if (applications == null || applications.isEmpty()) {
                return createListResponse(executionContext, emptyList(), paginationParam, paginationParam.hasPagination());
            }
            query.setApplications(applications.stream().map(ApplicationListItem::getId).collect(toSet()));
        } else {
            for (String appId : effectiveApplicationIds) {
                if (!hasPermission(executionContext, RolePermission.APPLICATION_SUBSCRIPTION, appId, RolePermissionAction.READ)) {
                    throw new ForbiddenAccessException();
                }
            }
            query.setApplications(effectiveApplicationIds);
        }

        final Page<SubscriptionEntity> pagedResult = fetchSubscriptions(paginationParam, query);
        List<SubscriptionEntity> subscriptions = pagedResult.getContent();
        long totalElements = pagedResult.getTotalElements();

        if (subscriptions.isEmpty()) {
            return createListResponse(executionContext, subscriptions, paginationParam, null, paginationParam.hasPagination());
        }

        final List<Subscription> subscriptionList = subscriptions
            .stream()
            .map(SubscriptionMapper.INSTANCE::map)
            .collect(Collectors.toList());

        final Metadata metadata = buildSubscriptionMetadata(executionContext, subscriptions, totalElements);

        return createListResponse(executionContext, subscriptionList, paginationParam, metadata.toMap(), paginationParam.hasPagination());
    }

    private Metadata buildSubscriptionMetadata(
        ExecutionContext executionContext,
        List<SubscriptionEntity> subscriptions,
        long totalElements
    ) {
        SubscriptionMetadataQuery metadataQuery = new SubscriptionMetadataQuery(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            subscriptions
        )
            .withApis(true)
            .withApplications(true)
            .withPlans(true)
            .withSubscribers(true)
            .includeDetails()
            .fillApiMetadata((metadata, api) -> {
                String apisURL = PortalApiLinkHelper.apisURL(uriInfo.getBaseUriBuilder(), api.getId());
                ApiLinks apiLinks = apiMapper.computeApiLinks(apisURL, api.getUpdatedAt());
                metadata.put(api.getId(), "pictureUrl", apiLinks.getPicture());
                return api;
            });

        Metadata metadata = subscriptionService.getMetadata(executionContext, metadataQuery);

        if (metadata != null) {
            metadata.put("paginateMetaData", "totalElements", totalElements);
        }
        return metadata;
    }

    private static List<String> resolveApiIds(String apiId, List<String> apiIds) {
        if (apiIds != null && !apiIds.isEmpty()) {
            return apiIds;
        }
        if (apiId != null && !apiId.isBlank()) {
            return List.of(apiId.trim());
        }
        return emptyList();
    }

    private static List<String> resolveApplicationIds(String applicationId, List<String> applicationIds) {
        if (applicationIds != null && !applicationIds.isEmpty()) {
            return applicationIds;
        }
        if (applicationId != null && !applicationId.isBlank()) {
            return List.of(applicationId.trim());
        }
        return emptyList();
    }

    private Page<SubscriptionEntity> fetchSubscriptions(PaginationParam paginationParam, SubscriptionQuery query) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (!paginationParam.hasPagination()) {
            Collection<SubscriptionEntity> resp = subscriptionService.search(executionContext, query);
            return new Page<>(resp.stream().toList(), 0, resp.size(), resp.size());
        }

        Page<SubscriptionEntity> pagedSubscriptions = subscriptionService.search(
            executionContext,
            query,
            new PageableImpl(paginationParam.getPage(), paginationParam.getSize())
        );
        return Objects.requireNonNullElseGet(pagedSubscriptions, () -> new Page<>(emptyList(), 0, 0, 0));
    }

    @Path("{subscriptionId}")
    public SubscriptionResource getSubscriptionResource() {
        return resourceContext.getResource(SubscriptionResource.class);
    }
}
