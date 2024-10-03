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
package io.gravitee.rest.api.service.impl.filtering;

import io.gravitee.apim.core.category.use_case.GetCategoryApisUseCase;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.filtering.FilteringService;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    ApiSearchService apiSearchService;

    @Autowired
    ApiCategoryService apiCategoryService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    ApiAuthorizationService apiAuthorizationService;

    @Autowired
    GetCategoryApisUseCase getCategoryApisUseCase;

    @Override
    public Collection<String> getApisOrderByNumberOfSubscriptions(Collection<String> apis, boolean excluded) {
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));
        subscriptionQuery.setApis(apis);
        Set<String> apisOrderByNumberOfSubscriptions = subscriptionService.findReferenceIdsOrderByNumberOfSubscriptions(
            subscriptionQuery,
            Order.DESC
        );

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
    public Collection<String> getApplicationsOrderByNumberOfSubscriptions(Collection<String> ids, Order order) {
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));
        subscriptionQuery.setApplications(ids);
        Set<String> ranking = subscriptionService.findReferenceIdsOrderByNumberOfSubscriptions(subscriptionQuery, order);
        // add apis already sorted by name to apis sorted by subscriptions
        ranking.addAll(ids);
        return ranking;
    }

    @Override
    public Collection<String> filterApis(
        ExecutionContext executionContext,
        final Set<String> apis,
        final FilterType filterType,
        final FilterType excludedFilterType
    ) {
        if (apis == null || apis.isEmpty()) {
            return apis;
        }

        final FilterType filter = excludedFilterType == null ? filterType : excludedFilterType;
        if (filter == null) {
            return apis;
        }

        final boolean excluded = excludedFilterType != null;
        switch (filter) {
            case MINE:
                if (isAuthenticated()) {
                    return getCurrentUserSubscribedApis(executionContext, apis, excluded);
                } else {
                    return Collections.emptyList();
                }
            case STARRED:
                if (ratingService.isEnabled(executionContext)) {
                    return getRatedApis(executionContext, apis, excluded);
                } else {
                    return Collections.emptyList();
                }
            case TRENDINGS:
                return getApisOrderByNumberOfSubscriptions(apis, excluded);
            case FEATURED:
                return getTopApis(executionContext, apis, excluded);
            case ALL:
            default:
                break;
        }
        // Apis is already ordered
        return apis;
    }

    @Override
    public Collection<String> filterApis(
        ExecutionContext executionContext,
        String userId,
        FilterType filterType,
        FilterType excludedFilterType,
        ApiQuery apiQuery
    ) {
        Set<String> apis = this.apiAuthorizationService.findAccessibleApiIdsForUser(executionContext, userId, apiQuery);
        return this.filterApis(executionContext, apis, filterType, excludedFilterType);
    }

    @Override
    public Collection<String> searchApis(ExecutionContext executionContext, String userId, String query) throws TechnicalException {
        List<String> apiIds = apiAuthorizationService
            .findAccessibleApiIdsForUser(executionContext, userId, new ApiQuery())
            .stream()
            .toList();

        Map<String, Object> filters = new HashMap<>();
        filters.put("api", apiIds);
        return apiSearchService.searchIds(executionContext, query, filters, null);
    }

    @Override
    public Collection<String> searchApisWithCategory(ExecutionContext executionContext, String userId, String query, String category)
        throws TechnicalException {
        var categoryApisOutput = getCategoryApisUseCase.execute(
            new GetCategoryApisUseCase.Input(executionContext, category, userId, false, true)
        );

        List<String> apiIds = categoryApisOutput.results().stream().map(result -> result.api().getId()).collect(Collectors.toList());

        Map<String, Object> filters = new HashMap<>();
        filters.put("api", apiIds);

        return apiSearchService
            .searchIds(executionContext, query, filters, null)
            .stream()
            .sorted(Comparator.comparingInt(apiIds::indexOf))
            .collect(Collectors.toList());
    }

    @Override
    public Set<CategoryEntity> listCategories(
        ExecutionContext executionContext,
        String userId,
        FilterType filterType,
        FilterType excludedFilterType
    ) {
        Set<String> apisForUser = this.apiAuthorizationService.findAccessibleApiIdsForUser(executionContext, userId);
        Collection<String> apis = this.filterApis(executionContext, apisForUser, filterType, excludedFilterType);
        return this.apiCategoryService.listCategories(apis, executionContext.getEnvironmentId());
    }

    private Collection<String> getTopApis(ExecutionContext executionContext, Set<String> apis, boolean excluded) {
        Map<String, Integer> topApiIdAndOrderMap = topApiService
            .findAll(executionContext)
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

    private Collection<String> getRatedApis(ExecutionContext executionContext, Set<String> apis, boolean excluded) {
        Set<String> ratings = ratingService.findReferenceIdsOrderByRate(executionContext, apis);

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

    private Collection<String> getCurrentUserSubscribedApis(ExecutionContext executionContext, Set<String> apis, boolean excluded) {
        //get Current user applications
        Set<String> currentUserApplicationsId = applicationService.findIdsByUser(executionContext, getAuthenticatedUser().getUsername());

        //find all subscribed apis for these applications
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApplications(currentUserApplicationsId);
        List<String> subscribedApis = subscriptionService
            .search(executionContext, subscriptionQuery)
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
