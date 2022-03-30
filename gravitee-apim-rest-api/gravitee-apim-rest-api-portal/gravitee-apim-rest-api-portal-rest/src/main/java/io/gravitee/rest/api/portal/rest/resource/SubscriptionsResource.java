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
package io.gravitee.rest.api.portal.rest.resource;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.PageEntity.PageRevisionId;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionMetadataQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.mapper.KeyMapper;
import io.gravitee.rest.api.portal.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.portal.rest.model.ApiLinks;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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
    private SubscriptionMapper subscriptionMapper;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private PlanService planService;

    @Inject
    private UserService userService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private ApiMapper apiMapper;

    @Inject
    private KeyMapper keyMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSubscription(@Valid @NotNull(message = "Input must not be null.") SubscriptionInput subscriptionInput) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            hasPermission(
                executionContext,
                RolePermission.APPLICATION_SUBSCRIPTION,
                subscriptionInput.getApplication(),
                RolePermissionAction.CREATE
            )
        ) {
            NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
            newSubscriptionEntity.setApplication(subscriptionInput.getApplication());
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
            SubscriptionEntity createdSubscription = subscriptionService.create(executionContext, newSubscriptionEntity);

            // For consumer convenience, fetch the keys just after the subscription has been created.
            List<Key> keys = apiKeyService
                .findBySubscription(executionContext, createdSubscription.getId())
                .stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .map(keyMapper::convert)
                .collect(Collectors.toList());

            final Subscription subscription = subscriptionMapper.convert(createdSubscription);
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

        final Collection<SubscriptionEntity> subscriptions = fetchSubscriptions(paginationParam, query);

        if (subscriptions.isEmpty()) {
            return createListResponse(executionContext, subscriptions, paginationParam, null, paginationParam.hasPagination());
        }

        final List<Subscription> subscriptionList = subscriptions.stream().map(subscriptionMapper::convert).collect(Collectors.toList());

        SubscriptionMetadataQuery metadataQuery = new SubscriptionMetadataQuery(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            subscriptions
        )
            .withApis(true)
            .withApplications(applicationId == null)
            .withPlans(true)
            .withSubscribers(true)
            .includeDetails()
            .fillApiMetadata(
                (metadata, api) -> {
                    String apisURL = PortalApiLinkHelper.apisURL(uriInfo.getBaseUriBuilder(), api.getId());
                    ApiLinks apiLinks = apiMapper.computeApiLinks(apisURL, api.getUpdatedAt());
                    metadata.put(api.getId(), "pictureUrl", apiLinks.getPicture());
                    return api;
                }
            );

        Metadata metadata = subscriptionService.getMetadata(executionContext, metadataQuery);

        return createListResponse(executionContext, subscriptionList, paginationParam, metadata.toMap(), paginationParam.hasPagination());
    }

    private Collection<SubscriptionEntity> fetchSubscriptions(PaginationParam paginationParam, SubscriptionQuery query) {
        if (paginationParam.hasPagination()) {
            return subscriptionService.search(GraviteeContext.getExecutionContext(), query);
        } else {
            final Page<SubscriptionEntity> pagedSubscriptions = subscriptionService.search(
                GraviteeContext.getExecutionContext(),
                query,
                new PageableImpl(paginationParam.getPage(), paginationParam.getSize())
            );
            if (pagedSubscriptions == null) {
                return emptyList();
            } else {
                return pagedSubscriptions.getContent();
            }
        }
    }

    @Path("{subscriptionId}")
    public SubscriptionResource getSubscriptionResource() {
        return resourceContext.getResource(SubscriptionResource.class);
    }
}
