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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

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
    private ApiMapper apiMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSubscription(@Valid @NotNull(message = "Input must not be null.") SubscriptionInput subscriptionInput) {
        if (hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, subscriptionInput.getApplication(), RolePermissionAction.CREATE)) {
            NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
            newSubscriptionEntity.setApplication(subscriptionInput.getApplication());
            newSubscriptionEntity.setPlan(subscriptionInput.getPlan());
            newSubscriptionEntity.setRequest(subscriptionInput.getRequest());

            SubscriptionEntity createdSubscription = subscriptionService.create(newSubscriptionEntity);

            return Response
                    .ok(subscriptionMapper.convert(createdSubscription))
                    .build();
        }
        throw new ForbiddenAccessException();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSubscriptions(@QueryParam("apiId") String apiId, @QueryParam("applicationId") String applicationId,
                                     @QueryParam("statuses") List<SubscriptionStatus> statuses, @BeanParam PaginationParam paginationParam) {

        final boolean withoutPagination = paginationParam.getSize() != null && paginationParam.getSize().equals(-1);
        final SubscriptionQuery query = new SubscriptionQuery();
        query.setApi(apiId);
        query.setApplication(applicationId);

        final Map<String, Map<String, Object>> metadata = new HashMap<>();
        if (applicationId == null) {
            final Set<ApplicationListItem> applications = applicationService.findByUser(getAuthenticatedUser());
            if (applications == null || applications.isEmpty()) {
                return createListResponse(emptyList(), paginationParam, !withoutPagination);
            }
            query.setApplications(applications.stream().map(ApplicationListItem::getId).collect(toSet()));
            applications.forEach(application -> {
                final Map<String, Object> m = new HashMap<>();
                m.put("name", application.getName());
                metadata.put(application.getId(), m);
            });
        } else if (!hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, applicationId, RolePermissionAction.READ)) {
            throw new ForbiddenAccessException();
        }

        if (statuses != null && !statuses.isEmpty()) {
            query.setStatuses(statuses);
        }

        final Collection<SubscriptionEntity> subscriptions;
        if (withoutPagination) {
            subscriptions = subscriptionService.search(query);
        } else {
            final Page<SubscriptionEntity> pagedSubscriptions = subscriptionService.search(query, new PageableImpl(paginationParam.getPage(), paginationParam.getSize()));
            if (pagedSubscriptions == null) {
                subscriptions = emptyList();
            } else {
                subscriptions = pagedSubscriptions.getContent();
            }
        }

        final List<Subscription> subscriptionList = subscriptions
                .stream()
                .map(subscriptionMapper::convert)
                .collect(Collectors.toList());

        subscriptionList.forEach(subscription -> {
            final ApiEntity api = apiService.findById(subscription.getApi());
            if (api != null) {
                final Map<String, Object> m = new HashMap<>();
                m.put("name", api.getName());
                m.put("pictureUrl", apiMapper.computeApiLinks(PortalApiLinkHelper.apisURL(uriInfo.getBaseUriBuilder(), api.getId())).getPicture());
                m.put("version", api.getVersion());
                metadata.put(api.getId(), m);
            }
            final PlanEntity plan = planService.findById(subscription.getPlan());
            if (plan != null) {
                final Map<String, Object> m = new HashMap<>();
                m.put("name", plan.getName());
                metadata.put(plan.getId(), m);
            }
            final UserEntity user = userService.findById(subscription.getSubscribedBy());
            if (user != null) {
                final Map<String, Object> m = new HashMap<>();
                m.put("name", user.getDisplayName());
                metadata.put(user.getId(), m);
            }
        });

        return createListResponse(subscriptionList, paginationParam, metadata, !withoutPagination);
    }

    @Path("{subscriptionId}")
    public SubscriptionResource getSubscriptionResource() {
        return resourceContext.getResource(SubscriptionResource.class);
    }

}
