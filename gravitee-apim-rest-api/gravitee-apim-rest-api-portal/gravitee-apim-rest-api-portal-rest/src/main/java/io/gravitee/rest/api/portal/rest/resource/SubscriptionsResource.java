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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.PageEntity.PageRevisionId;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
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

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

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
            NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
            newSubscriptionEntity.setApplication(applicationId);
            newSubscriptionEntity.setPlan(subscriptionInput.getPlan());
            newSubscriptionEntity.setRequest(subscriptionInput.getRequest());
            newSubscriptionEntity.setGeneralConditionsAccepted(subscriptionInput.getGeneralConditionsAccepted());
            if (subscriptionInput.getGeneralConditionsContentRevision() != null) {
                final PageRevisionId generalConditionsContentRevision = new PageRevisionId(
                    subscriptionInput.getGeneralConditionsContentRevision().getPageId(),
                    subscriptionInput.getGeneralConditionsContentRevision().getRevision()
                );
                newSubscriptionEntity.setGeneralConditionsContentRevision(generalConditionsContentRevision);
            }
            newSubscriptionEntity.setMetadata(subscriptionInput.getMetadata());
            SubscriptionConfigurationInput inputConfiguration = subscriptionInput.getConfiguration();
            if (inputConfiguration != null) {
                SubscriptionConfigurationEntity subscriptionConfigurationEntity = new SubscriptionConfigurationEntity();
                subscriptionConfigurationEntity.setEntrypointId(inputConfiguration.getEntrypointId());
                subscriptionConfigurationEntity.setChannel(inputConfiguration.getChannel());
                if (inputConfiguration.getEntrypointConfiguration() != null) {
                    subscriptionConfigurationEntity.setEntrypointConfiguration(
                        graviteeMapper.valueToTree(inputConfiguration.getEntrypointConfiguration())
                    );
                }
                newSubscriptionEntity.setConfiguration(subscriptionConfigurationEntity);
            }
            if (subscriptionInput.getApiKeyMode() != null) {
                newSubscriptionEntity.setApiKeyMode(ApiKeyMode.valueOf(subscriptionInput.getApiKeyMode().name()));
            }
            SubscriptionEntity createdSubscription = subscriptionService.create(executionContext, newSubscriptionEntity);

            // For consumer convenience, fetch the keys just after the subscription has been
            // created.
            List<Key> keys = apiKeyService
                .findBySubscription(executionContext, createdSubscription.getId())
                .stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .map(keyMapper::convert)
                .collect(Collectors.toList());

            final Subscription subscription = SubscriptionMapper.INSTANCE.map(createdSubscription);
            subscription.setKeys(keys);

            return Response.ok(subscription).build();
        }
        throw new ForbiddenAccessException();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSubscriptions(
        @QueryParam("apiId") String apiId,
        @QueryParam("applicationId") String applicationId,
        @QueryParam("statuses") List<SubscriptionStatus> statuses,
        @BeanParam PaginationParam paginationParam
    ) {
        final SubscriptionQuery query = new SubscriptionQuery();
        query.setApi(apiId);
        query.setApplication(applicationId);
        query.setStatuses(statuses);
        if (apiId == null || apiId.isEmpty()) {
            query.setReferenceType(GenericPlanEntity.ReferenceType.API);
        }

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (applicationId == null) {
            final Set<ApplicationListItem> applications = applicationService.findByUser(executionContext, getAuthenticatedUser());
            if (applications == null || applications.isEmpty()) {
                return createListResponse(executionContext, emptyList(), paginationParam, paginationParam.hasPagination());
            }
            query.setApplications(applications.stream().map(ApplicationListItem::getId).collect(toSet()));
        } else if (!hasPermission(executionContext, RolePermission.APPLICATION_SUBSCRIPTION, applicationId, RolePermissionAction.READ)) {
            throw new ForbiddenAccessException();
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
        return createListResponse(executionContext, subscriptionList, paginationParam, metadata.toMap(), paginationParam.hasPagination());
    }

    private Page<SubscriptionEntity> fetchSubscriptions(PaginationParam paginationParam, SubscriptionQuery query) {
        if (!paginationParam.hasPagination()) {
            Collection<SubscriptionEntity> resp = subscriptionService.search(GraviteeContext.getExecutionContext(), query);
            return new Page<>(resp.stream().toList(), 0, resp.size(), resp.size());
        } else {
            final Page<SubscriptionEntity> pagedSubscriptions = subscriptionService.search(
                GraviteeContext.getExecutionContext(),
                query,
                new PageableImpl(paginationParam.getPage(), paginationParam.getSize())
            );
            return Objects.requireNonNullElseGet(pagedSubscriptions, () -> new Page<>(emptyList(), 0, 0, 0));
        }
    }

    @Path("{subscriptionId}")
    public SubscriptionResource getSubscriptionResource() {
        return resourceContext.getResource(SubscriptionResource.class);
    }
}
