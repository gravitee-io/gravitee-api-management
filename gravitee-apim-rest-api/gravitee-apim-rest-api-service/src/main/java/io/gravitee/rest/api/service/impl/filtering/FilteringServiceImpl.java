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
package io.gravitee.rest.api.service.impl.filtering;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.filtering.FilteringService;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class FilteringServiceImpl extends AbstractService implements FilteringService {

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    RatingService ratingService;

    @Autowired
    TopApiService topApiService;

    @Autowired
    ApplicationService applicationService;

    @Autowired
    ApiService apiService;

    @Override
    public Collection<String> getApisOrderByNumberOfSubscriptions(Collection<String> apis, boolean excluded) {
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));
        subscriptionQuery.setApis(apis);
        Set<String> apisOrderByNumberOfSubscriptions = subscriptionService.findReferenceIdsOrderByNumberOfSubscriptions(subscriptionQuery);

        if (excluded) {
            // remove apis with subscriptions to apis already sorted by name
            apis.removeAll(apisOrderByNumberOfSubscriptions);
            return apis;
        } else {
            // add apis already sorted by name to apis sorted by subscriptions
            apisOrderByNumberOfSubscriptions.addAll(apis);
            return apisOrderByNumberOfSubscriptions;
        }
    }

    @Override
    public Collection<String> getApplicationsOrderByNumberOfSubscriptions(Collection<String> ids) {
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));
        subscriptionQuery.setApplications(ids);
        Set<String> ranking = subscriptionService.findReferenceIdsOrderByNumberOfSubscriptions(subscriptionQuery);
        // add apis already sorted by name to apis sorted by subscriptions
        ranking.addAll(ids);
        return ranking;
    }

    @Override
    public Collection<String> filterApis(final Set<String> apis, final FilterType filterType, final FilterType excludedFilterType) {
        final FilterType filter = excludedFilterType == null ? filterType : excludedFilterType;
        final boolean excluded = excludedFilterType != null;
        if (filter != null) {
            switch (filter) {
                case MINE:
                    if (isAuthenticated()) {
                        return getCurrentUserSubscribedApis(apis, excluded);
                    } else {
                        return Collections.emptyList();
                    }
                case STARRED:
                    if (ratingService.isEnabled()) {
                        return getRatedApis(apis, excluded);
                    } else {
                        return Collections.emptyList();
                    }
                case TRENDINGS:
                    return getApisOrderByNumberOfSubscriptions(apis, excluded);
                case FEATURED:
                    return getTopApis(apis, excluded);
                case ALL:
                default:
                    break;
            }
        }

        // Apis is already ordered
        return apis;
    }

    @Override
    public Collection<String> filterApis(String userId, FilterType filterType, FilterType excludedFilterType, ApiQuery apiQuery) {
        Set<String> apis = this.apiService.findPublishedIdsByUser(userId);
        return this.filterApis(apis, filterType, excludedFilterType);
    }

    @Override
    public Collection<String> searchApis(String userId, String query) throws TechnicalException {
        Set<String> apis = apiService.findPublishedIdsByUser(userId);

        Map<String, Object> filters = new HashMap<>();
        filters.put("api", apis);

        return apiService.searchIds(query, filters);
    }

    @Override
    public Set<CategoryEntity> listCategories(String userId, FilterType filterType, FilterType excludedFilterType) {
        Set<String> apisForUser = this.apiService.findPublishedIdsByUser(userId);
        Collection<String> apis = this.filterApis(apisForUser, filterType, excludedFilterType);
        return this.apiService.listCategories(apis, GraviteeContext.getCurrentEnvironment());
    }

    private Collection<String> getTopApis(Set<String> apis, boolean excluded) {
        Map<String, Integer> topApiIdAndOrderMap = topApiService
            .findAll()
            .stream()
            .collect(Collectors.toMap(TopApiEntity::getApi, TopApiEntity::getOrder));

        if (topApiIdAndOrderMap.isEmpty()) {
            if (excluded) {
                return apis;
            } else {
                return Collections.emptyList();
            }
        } else if (excluded) {
            return apis.stream().filter(api -> (!topApiIdAndOrderMap.containsKey(api))).collect(Collectors.toList());
        } else {
            return apis
                .stream()
                .filter(topApiIdAndOrderMap::containsKey)
                .sorted(Comparator.comparing(topApiIdAndOrderMap::get))
                .collect(Collectors.toList());
        }
    }

    private Collection<String> getRatedApis(Set<String> apis, boolean excluded) {
        Set<String> ratings = ratingService.findReferenceIdsOrderByRate(apis);

        if (excluded) {
            // remove apis rated to apis already sorted by name
            apis.removeAll(ratings);
            return apis;
        } else {
            // add apis already sorted by name to apis sorted by rate
            ratings.addAll(apis);
            return ratings;
        }
    }

    private Collection<String> getCurrentUserSubscribedApis(Set<String> apis, boolean excluded) {
        //get Current user applications
        List<String> currentUserApplicationsId = applicationService
            .findByUser(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                getAuthenticatedUser().getUsername()
            )
            .stream()
            .map(ApplicationListItem::getId)
            .collect(Collectors.toList());

        //find all subscribed apis for these applications
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApplications(currentUserApplicationsId);
        List<String> subscribedApis = subscriptionService
            .search(subscriptionQuery)
            .stream()
            .map(SubscriptionEntity::getApi)
            .distinct()
            .collect(Collectors.toList());

        //filter apis list with subscribed apis list
        return apis
            .stream()
            .filter(api -> (!excluded && subscribedApis.contains(api)) || (excluded && !subscribedApis.contains(api)))
            .collect(Collectors.toList());
    }
}
