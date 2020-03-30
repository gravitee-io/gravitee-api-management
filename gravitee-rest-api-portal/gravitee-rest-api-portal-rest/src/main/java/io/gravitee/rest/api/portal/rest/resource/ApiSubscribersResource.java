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
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.model.analytics.query.GroupByQuery;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.AnalyticsService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.rest.api.model.SubscriptionStatus.*;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSubscribersResource extends AbstractResource {

    @Inject
    private ApplicationMapper applicationMapper;

    @Inject
    private SubscriptionService subscriptionService;
    @Inject
    private AnalyticsService analyticsService;
    @Inject
    private ApplicationService applicationService;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getSubscriberApplicationsByApiId(@BeanParam PaginationParam paginationParam,
            @PathParam("apiId") String apiId) {
        String currentUser = getAuthenticatedUserOrNull();
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(currentUser);
        Optional<ApiEntity> optionalApi = userApis.stream().filter(a -> a.getId().equals(apiId)).findFirst();
        if (optionalApi.isPresent()) {

            SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
            subscriptionQuery.setApi(apiId);

            subscriptionQuery.setStatuses(Arrays.asList(ACCEPTED, PENDING, PAUSED));

            ApiEntity api = optionalApi.get();
            if(!api.getPrimaryOwner().getId().equals(currentUser) ) {
                Set<ApplicationListItem> userApplications = this.applicationService.findByUser(currentUser);
                if(userApplications == null || userApplications.isEmpty()) {
                    return createListResponse(Collections.emptyList(), paginationParam);
                }
                subscriptionQuery.setApplications(userApplications.stream().map(ApplicationListItem::getId).collect(Collectors.toList()));
            }


            Map<String, Long> nbHitsByApp = getNbHitsByApplication(apiId);

            Collection<SubscriptionEntity> subscriptions = subscriptionService.search(subscriptionQuery);
            List<Application> subscribersApplication = subscriptions.stream().map(SubscriptionEntity::getApplication)
                    .distinct()
                    .map(application -> applicationService.findById(application))
                    .map(application -> applicationMapper.convert(application, uriInfo))
                    .sorted((o1, o2) -> compareApp(nbHitsByApp, o1, o2))
                    .collect(Collectors.toList());
            return createListResponse(subscribersApplication, paginationParam);
        }
        throw new ApiNotFoundException(apiId);
    }

    private int compareApp(Map<String, Long> nbHitsByApp, Application o1, Application o2) {
        if (nbHitsByApp != null) {
            if(nbHitsByApp.get(o1.getId()) == null && nbHitsByApp.get(o2.getId()) == null) {
                return 0;
            }
            if(nbHitsByApp.get(o1.getId()) == null && nbHitsByApp.get(o2.getId()) != null) {
                return 1;
            }
            if(nbHitsByApp.get(o1.getId()) != null && nbHitsByApp.get(o2.getId()) == null) {
                return -1;
            }
            int compareTo = nbHitsByApp.get(o2.getId()).compareTo(nbHitsByApp.get(o1.getId()));
            if(compareTo != 0) {
                return compareTo;
            }
        }
        return o1.getName().compareTo(o2.getName());
    }

    private Map<String, Long> getNbHitsByApplication(String apiId) {
        GroupByQuery query = new GroupByQuery();
        Instant now = Instant.now();
        query.setField("application");
        query.setFrom(now.minus(7, ChronoUnit.DAYS).toEpochMilli());
        query.setTo(now.toEpochMilli());
        query.setInterval(43200000);
        query.setRootField("api");
        query.setRootIdentifier(apiId);

        try {
            final TopHitsAnalytics analytics = analyticsService.execute(query);
            if (analytics != null) {
                return analytics.getValues();
            }
        } catch (final Exception e) {
            // do nothing as the analytics errors should not break the portal
        }
        return null;
    }
}
