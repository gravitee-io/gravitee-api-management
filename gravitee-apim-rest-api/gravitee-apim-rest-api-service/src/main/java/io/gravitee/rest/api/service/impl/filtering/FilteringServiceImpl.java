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
import io.gravitee.rest.api.model.filtering.FilteredEntities;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.*;
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
    public FilteredEntities<String> getApisOrderByNumberOfSubscriptions(Set<String> apis, boolean excluded) {
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));
        subscriptionQuery.setApis(apis);
        Set<String> apisOrderByNumberOfSubscriptions = subscriptionService.computeRanking(subscriptionQuery);

        if (excluded) {
            // remove apis with subscriptions to apis already sorted by name
            apis.removeAll(apisOrderByNumberOfSubscriptions);
            return new FilteredEntities<>(apis, null);
        } else {
            // add apis already sorted by name to apis sorted by subscriptions
            apisOrderByNumberOfSubscriptions.addAll(apis);
            return new FilteredEntities(apisOrderByNumberOfSubscriptions, null);
        }
    }

    @Override
    public FilteredEntities<String> getApplicationsOrderByNumberOfSubscriptions(Set<String> ids) {
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));
        subscriptionQuery.setApplications(ids);
        return new FilteredEntities(subscriptionService.computeRanking(subscriptionQuery), null);
    }

    @Override
    public FilteredEntities<String> filterApis(final Set<String> apis, final FilterType filterType, final FilterType excludedFilterType) {
        final FilterType filter = excludedFilterType == null ? filterType : excludedFilterType;
        final boolean excluded = excludedFilterType != null;
        if (filter != null) {
            switch (filter) {
                case MINE:
                    if (isAuthenticated()) {
                        return getCurrentUserSubscribedApis(apis, excluded);
                    } else {
                        return new FilteredEntities<>(Collections.emptyList(), null);
                    }
                case STARRED:
                    if (ratingService.isEnabled()) {
                        return getRatedApis(apis, excluded);
                    } else {
                        return new FilteredEntities<>(Collections.emptyList(), null);
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
        return new FilteredEntities<>(apis, null);
    }

    @Override
    public FilteredEntities<String> filterApis(String userId, FilterType filterType, FilterType excludedFilterType, ApiQuery apiQuery) {
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
        FilteredEntities<String> apis = this.filterApis(apisForUser, filterType, excludedFilterType);
        return this.apiService.listCategories(apis.getFilteredItems());
    }

    private FilteredEntities<String> getTopApis(Set<String> apis, boolean excluded) {
        Map<String, Integer> topApiIdAndOrderMap = topApiService
            .findAll()
            .stream()
            .collect(Collectors.toMap(TopApiEntity::getApi, TopApiEntity::getOrder));

        if (topApiIdAndOrderMap.isEmpty()) {
            if (excluded) {
                return new FilteredEntities<>(apis, null);
            } else {
                return new FilteredEntities<>(Collections.emptyList(), null);
            }
        } else if (excluded) {
            return new FilteredEntities<>(
                apis.stream().filter(api -> (!topApiIdAndOrderMap.containsKey(api))).collect(Collectors.toList()),
                null
            );
        } else {
            return new FilteredEntities<>(
                apis
                    .stream()
                    .filter(api -> topApiIdAndOrderMap.containsKey(api))
                    .sorted(Comparator.comparing(o -> topApiIdAndOrderMap.get(o)))
                    .collect(Collectors.toList()),
                null
            );
        }
    }

    private FilteredEntities<String> getRatedApis(Set<String> apis, boolean excluded) {
        Set<String> ratings = ratingService.computeRanking(apis);

        if (excluded) {
            // remove apis rated to apis already sorted by name
            apis.removeAll(ratings);
            return new FilteredEntities<>(apis, null);
        } else {
            // add apis already sorted by name to apis sorted by rate
            ratings.addAll(apis);
            return new FilteredEntities(ratings, null);
        }
    }

    private FilteredEntities<String> getCurrentUserSubscribedApis(Set<String> apis, boolean excluded) {
        //get Current user applications
        List<String> currentUserApplicationsId = applicationService
            .findByUser(getAuthenticatedUser().getUsername())
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
        return new FilteredEntities<>(
            apis
                .stream()
                .filter(api -> (!excluded && subscribedApis.contains(api)) || (excluded && !subscribedApis.contains(api)))
                // .sorted((a1, a2) -> String.CASE_INSENSITIVE_ORDER.compare(a1.getName(), a2.getName()))
                .collect(Collectors.toList()),
            null
        );
    }
}
