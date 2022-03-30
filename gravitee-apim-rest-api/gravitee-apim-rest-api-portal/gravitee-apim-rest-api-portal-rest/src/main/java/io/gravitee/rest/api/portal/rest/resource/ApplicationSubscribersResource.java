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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.model.analytics.query.GroupByQuery;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.AnalyticsService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationSubscribersResource extends AbstractResource {

    @Inject
    private ApiMapper apiMapper;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private AnalyticsService analyticsService;

    @Inject
    private ApplicationService applicationService;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getSubscriberApisByApplicationId(
        @BeanParam PaginationParam paginationParam,
        @PathParam("applicationId") String applicationId,
        @QueryParam("statuses") List<SubscriptionStatus> statuses
    ) {
        String currentUser = getAuthenticatedUserOrNull();
        Collection<ApplicationListItem> userApplications = applicationService.findByUser(
            GraviteeContext.getExecutionContext(),
            currentUser
        );
        Optional<ApplicationListItem> optionalApplication = userApplications
            .stream()
            .filter(a -> a.getId().equals(applicationId))
            .findFirst();
        if (optionalApplication.isPresent()) {
            SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
            subscriptionQuery.setApplication(applicationId);

            subscriptionQuery.setStatuses(statuses);

            ApplicationListItem application = optionalApplication.get();
            if (!application.getPrimaryOwner().getId().equals(currentUser)) {
                Set<ApiEntity> userApis = this.apiService.findPublishedByUser(GraviteeContext.getExecutionContext(), currentUser);
                if (userApis == null || userApis.isEmpty()) {
                    return createListResponse(Collections.emptyList(), paginationParam);
                }
                subscriptionQuery.setApis(userApis.stream().map(ApiEntity::getId).collect(Collectors.toList()));
            }

            Map<String, Long> nbHitsByApp = getNbHitsByApplication(applicationId);

            Collection<SubscriptionEntity> subscriptions = subscriptionService.search(
                GraviteeContext.getExecutionContext(),
                subscriptionQuery
            );
            List<Api> subscribersApis = subscriptions
                .stream()
                .map(SubscriptionEntity::getApi)
                .distinct()
                .map(api -> apiService.findById(GraviteeContext.getExecutionContext(), api))
                .map(api1 -> apiMapper.convert(GraviteeContext.getExecutionContext(), api1))
                .sorted((o1, o2) -> compareApp(nbHitsByApp, o1, o2))
                .collect(Collectors.toList());
            return createListResponse(subscribersApis, paginationParam);
        }
        throw new ApplicationNotFoundException(applicationId);
    }

    private int compareApp(Map<String, Long> nbHitsByApp, Api o1, Api o2) {
        if (nbHitsByApp.get(o1.getId()) == null && nbHitsByApp.get(o2.getId()) == null) {
            return 0;
        }
        if (nbHitsByApp.get(o1.getId()) == null && nbHitsByApp.get(o2.getId()) != null) {
            return 1;
        }
        if (nbHitsByApp.get(o1.getId()) != null && nbHitsByApp.get(o2.getId()) == null) {
            return -1;
        }
        int compareTo = nbHitsByApp.get(o2.getId()).compareTo(nbHitsByApp.get(o1.getId()));
        if (compareTo != 0) {
            return compareTo;
        }
        return o1.getName().compareTo(o2.getName());
    }

    private Map<String, Long> getNbHitsByApplication(String applicationId) {
        GroupByQuery query = new GroupByQuery();
        Instant now = Instant.now();
        query.setField("api");
        query.setFrom(now.minus(7, ChronoUnit.DAYS).toEpochMilli());
        query.setTo(now.toEpochMilli());
        query.setInterval(43200000);
        query.setRootField("application");
        query.setRootIdentifier(applicationId);

        TopHitsAnalytics analytics = analyticsService.execute(GraviteeContext.getExecutionContext(), query);
        if (analytics != null) {
            return analytics.getValues();
        }
        return null;
    }
}
