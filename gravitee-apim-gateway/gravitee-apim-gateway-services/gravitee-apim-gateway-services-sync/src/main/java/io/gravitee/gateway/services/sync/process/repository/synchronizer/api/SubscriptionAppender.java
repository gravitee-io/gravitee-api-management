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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;
import static io.gravitee.repository.management.model.Subscription.Status.CLOSED;
import static io.gravitee.repository.management.model.Subscription.Status.PAUSED;
import static io.gravitee.repository.management.model.Subscription.Status.PENDING;
import static java.util.stream.Collectors.groupingBy;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class SubscriptionAppender {

    private static final List<String> INITIAL_STATUS = List.of(ACCEPTED.name());
    private static final List<String> INCREMENTAL_STATUS = List.of(ACCEPTED.name(), CLOSED.name(), PAUSED.name(), PENDING.name());
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final ApiProductRegistry apiProductRegistry;

    /**
     * Fetching subscriptions for given deployables
     * @param deployables the deployables to update
     * @return the deployables updated with subscriptions
     */
    public List<ApiReactorDeployable> appends(
        final boolean initialSync,
        final List<ApiReactorDeployable> deployables,
        final Set<String> environments
    ) {
        final Map<String, ApiReactorDeployable> deployableByApi = deployables
            .stream()
            .collect(Collectors.toMap(ApiReactorDeployable::apiId, d -> d));

        List<String> apiPlans = collectApiPlans(deployableByApi);
        List<String> allPlans = new ArrayList<>(apiPlans);

        deployableByApi.forEach((apiId, deployable) -> {
            Set<String> envs = environments != null && !environments.isEmpty()
                ? environments
                : Optional.ofNullable(deployable.reactableApi())
                    .map(a -> a.getEnvironmentId())
                    .map(Set::of)
                    .orElse(Set.of());
            Set<String> apiProductPlans = collectApiProductPlans(apiId, envs);
            deployable.subscribablePlans().addAll(apiProductPlans);
            deployable.apiKeyPlans().addAll(apiProductPlans);
            allPlans.addAll(apiProductPlans);
        });

        if (!allPlans.isEmpty()) {
            Map<String, List<Subscription>> subscriptionsByApi = loadSubscriptions(initialSync, allPlans, environments);
            subscriptionsByApi.forEach((api, subscriptions) -> {
                ApiReactorDeployable deployable = deployableByApi.get(api);
                if (deployable == null) {
                    log.warn(
                        "Cannot find api {} for subscriptions [{}]",
                        api,
                        subscriptions.stream().map(Subscription::getId).collect(Collectors.joining(","))
                    );
                } else {
                    deployable.subscriptions(subscriptions);
                }
            });
        }
        return deployables;
    }

    private List<String> collectApiPlans(Map<String, ApiReactorDeployable> deployableByApi) {
        return deployableByApi.values().stream().map(ApiReactorDeployable::subscribablePlans).flatMap(Collection::stream).toList();
    }

    private Set<String> collectApiProductPlans(String apiId, Set<String> envs) {
        return envs
            .stream()
            .flatMap(envId -> apiProductRegistry.getApiProductPlanEntriesForApi(apiId, envId).stream())
            .map(e -> e.plan().getId())
            .collect(Collectors.toSet());
    }

    protected Map<String, List<Subscription>> loadSubscriptions(
        final boolean initialSync,
        final List<String> plans,
        final Set<String> environments
    ) {
        SubscriptionCriteria.SubscriptionCriteriaBuilder criteriaBuilder = SubscriptionCriteria.builder()
            .plans(plans)
            .environments(environments);
        if (initialSync) {
            criteriaBuilder.statuses(INITIAL_STATUS).endingAtAfter(Instant.now().toEpochMilli()).includeWithoutEnd(true);
        } else {
            criteriaBuilder.statuses(INCREMENTAL_STATUS);
        }

        try {
            return subscriptionRepository
                .search(criteriaBuilder.build(), new SortableBuilder().field("updatedAt").order(Order.ASC).build())
                .stream()
                .flatMap(subscription -> subscriptionMapper.to(subscription).stream())
                .peek(subscriptionConverted -> subscriptionConverted.setForceDispatch(true))
                .collect(groupingBy(Subscription::getApi));
        } catch (Exception ex) {
            throw new SyncException("Error occurred when retrieving subscriptions", ex);
        }
    }
}
