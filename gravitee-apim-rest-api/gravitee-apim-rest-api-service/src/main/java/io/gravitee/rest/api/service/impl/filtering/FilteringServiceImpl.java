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

import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.filtering.FilterableItem;
import io.gravitee.rest.api.model.filtering.FilteredEntities;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.filtering.FilteringService;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.*;
import java.util.function.Function;
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

    /**
     * Filters and sorts input entities by number of subscriptions.
     *
     * @param items    Can be {@link ApiEntity} or {@link ApplicationListItem}
     * @param excluded If set to true, only entities without subscriptions are returned. Else, only entities with subscriptions are returned.
     * @return a {@link FilteredEntities} object with the filtered and sorted list of items and a metadata map.
     */
    @Override
    public <T extends FilterableItem> FilteredEntities<T> getEntitiesOrderByNumberOfSubscriptions(
        Collection<T> items,
        Boolean excluded,
        boolean isAsc
    ) {
        if (items == null || items.isEmpty()) {
            return new FilteredEntities<>(Collections.emptyList(), new HashMap<>());
        }

        Function<SubscriptionEntity, String> getItemFunction;
        if (items.toArray()[0] instanceof ApiEntity) {
            getItemFunction = (SubscriptionEntity sub) -> sub.getApi();
        } else if (items.toArray()[0] instanceof ApplicationListItem) {
            getItemFunction = (SubscriptionEntity sub) -> sub.getApplication();
        } else {
            throw new IllegalStateException("Only ApiEntity and ApplicationListItem are allowed");
        }

        //find all subscribed items
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApis(items.stream().map(FilterableItem::getId).collect(Collectors.toList()));
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));

        // group by items
        Map<String, Long> subscribedItemsWithCount = subscriptionService
            .search(subscriptionQuery)
            .stream()
            .collect(Collectors.groupingBy(getItemFunction, Collectors.counting()));

        // link an item with its nb of subscriptions
        Map<FilterableItem, Long> itemsWithCount = new HashMap<>();
        Map<String, Map<String, Object>> itemsMetadata = new HashMap<>();
        Map<String, Object> subscriptionsMetadata = new HashMap<>();
        itemsMetadata.put("subscriptions", subscriptionsMetadata);
        items.forEach(
            item -> {
                Long itemSubscriptionsCount = subscribedItemsWithCount.get(item.getId());
                if ((excluded == null) || (!excluded && itemSubscriptionsCount != null) || (excluded && itemSubscriptionsCount == null)) {
                    //creation of a map which will be sorted to retrieve items in the right order
                    itemsWithCount.put(item, itemSubscriptionsCount == null ? 0L : itemSubscriptionsCount);

                    //creation of a metadata map
                    subscriptionsMetadata.put(item.getId(), itemSubscriptionsCount == null ? 0L : itemSubscriptionsCount);
                }
            }
        );

        // order the list
        Comparator<Map.Entry<FilterableItem, Long>> comparingByValue = Map.Entry.comparingByValue();
        if (!isAsc) {
            comparingByValue = comparingByValue.reversed();
        }

        return new FilteredEntities(
            itemsWithCount
                .entrySet()
                .stream()
                .sorted(
                    Map.Entry
                        .<FilterableItem, Long>comparingByValue()
                        .reversed()
                        .thenComparing(
                            Map.Entry.comparingByKey(Comparator.comparing(FilterableItem::getName, String.CASE_INSENSITIVE_ORDER))
                        )
                )
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()),
            itemsMetadata
        );
    }

    @Override
    public FilteredEntities<ApiEntity> filterApis(
        final Collection<ApiEntity> apis,
        final FilterType filterType,
        final FilterType excludedFilterType
    ) {
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
                    return getEntitiesOrderByNumberOfSubscriptions(apis, excluded, false);
                case FEATURED:
                    return getTopApis(apis, excluded);
                case ALL:
                default:
                    break;
            }
        }

        // No category was applied but at least, the list is ordered
        return new FilteredEntities<>(
            apis
                .stream()
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .collect(Collectors.toList()),
            null
        );
    }

    private FilteredEntities<ApiEntity> getTopApis(Collection<ApiEntity> apis, boolean excluded) {
        Map<String, Integer> topApiIdAndOrderMap = topApiService
            .findAll()
            .stream()
            .collect(Collectors.toMap(TopApiEntity::getApi, TopApiEntity::getOrder));

        if (topApiIdAndOrderMap.isEmpty()) {
            if (excluded) {
                return new FilteredEntities<>(
                    apis.stream().sorted(Comparator.comparing(ApiEntity::getName)).collect(Collectors.toList()),
                    null
                );
            } else {
                return new FilteredEntities<>(Collections.emptyList(), null);
            }
        } else if (excluded) {
            return new FilteredEntities<>(
                apis
                    .stream()
                    .filter(api -> (!topApiIdAndOrderMap.containsKey(api.getId())))
                    .sorted(Comparator.comparing(ApiEntity::getName))
                    .collect(Collectors.toList()),
                null
            );
        } else {
            return new FilteredEntities<>(
                apis
                    .stream()
                    .filter(api -> topApiIdAndOrderMap.containsKey(api.getId()))
                    .sorted(Comparator.comparing(o -> topApiIdAndOrderMap.get(o.getId())))
                    .collect(Collectors.toList()),
                null
            );
        }
    }

    private FilteredEntities<ApiEntity> getRatedApis(Collection<ApiEntity> apis, boolean excluded) {
        //keep apis with ratings
        Map<ApiEntity, RatingSummaryEntity> ratings = new HashMap<>();
        // APIPortal: should create a specific service to retrieve all the information
        // in one call to the repository
        apis.forEach(
            api -> {
                RatingSummaryEntity apiRatingSummary = ratingService.findSummaryByApi(api.getId());
                if (apiRatingSummary != null && apiRatingSummary.getNumberOfRatings() > 0) {
                    ratings.put(api, apiRatingSummary);
                }
            }
        );

        if (excluded) {
            return new FilteredEntities<>(apis.stream().filter(api -> !ratings.containsKey(api)).collect(Collectors.toList()), null);
        } else {
            //sort apis by ratings, nb of ratings, and name
            return new FilteredEntities<>(
                ratings
                    .entrySet()
                    .stream()
                    .sorted(
                        (e1, e2) -> {
                            RatingSummaryEntity o1 = e1.getValue();
                            RatingSummaryEntity o2 = e2.getValue();
                            int averageRateComparaison = Double.compare(o2.getAverageRate(), o1.getAverageRate());
                            if (averageRateComparaison != 0) {
                                return averageRateComparaison;
                            }
                            int nbRatingsComparaison = Integer.compare(o2.getNumberOfRatings(), o1.getNumberOfRatings());
                            if (nbRatingsComparaison != 0) {
                                return nbRatingsComparaison;
                            }
                            return String.CASE_INSENSITIVE_ORDER.compare(e1.getKey().getName(), e2.getKey().getName());
                        }
                    )
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList()),
                null
            );
        }
    }

    private FilteredEntities<ApiEntity> getCurrentUserSubscribedApis(Collection<ApiEntity> apis, boolean excluded) {
        //get Current user applications
        List<String> currentUserApplicationsId = applicationService
            .findByUser(GraviteeContext.getExecutionContext(), getAuthenticatedUser().getUsername())
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
                .filter(api -> (!excluded && subscribedApis.contains(api.getId())) || (excluded && !subscribedApis.contains(api.getId())))
                .sorted((a1, a2) -> String.CASE_INSENSITIVE_ORDER.compare(a1.getName(), a2.getName()))
                .collect(Collectors.toList()),
            null
        );
    }
}
