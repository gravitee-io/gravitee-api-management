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
package io.gravitee.gateway.services.sync.subscriptions.task;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Subscription.Status.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionRefresher implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionRefresher.class);

    private static final int TIMEFRAME_BEFORE_DELAY = 10 * 60 * 1000;
    private static final int TIMEFRAME_AFTER_DELAY = 1 * 60 * 1000;

    private ClusterManager clusterManager;

    private SubscriptionRepository subscriptionRepository;

    private Map<String, Object> cache;

    private final Api api;

    private Collection<String> plans;

    private long lastRefreshAt = -1;

    private long minTime;

    private long maxTime;

    private long avgTime;

    private long totalTime;

    private long count;

    private long errorsCount;

    private Throwable lastException;

    private boolean distributed;

    private static final List<Subscription.Status> REFRESH_STATUS = Arrays.asList(
            Subscription.Status.ACCEPTED, CLOSED, PAUSED);

    public SubscriptionRefresher(final Api api) {
        this.api = api;
    }

    public void initialize() {
        this.plans = api.getPlans()
                .stream()
                .filter(plan -> io.gravitee.repository.management.model.Plan.PlanSecurityType.OAUTH2.name()
                        .equalsIgnoreCase(plan.getSecurity()) ||
                                io.gravitee.repository.management.model.Plan.PlanSecurityType.JWT.name()
                                        .equalsIgnoreCase(plan.getSecurity()))
                .map(Plan::getId)
                .collect(Collectors.toList());
    }

    @Override
    public void run() {
        if (!plans.isEmpty() && (clusterManager.isMasterNode() || (!clusterManager.isMasterNode() && !distributed))) {
            long start = System.currentTimeMillis();
            long nextLastRefreshAt = System.currentTimeMillis();
            LOGGER.debug("Refresh subscriptions for API id[{}] name[{}]", api.getId(), api.getName());

            final SubscriptionCriteria.Builder criteriaBuilder = new SubscriptionCriteria.Builder()
                    .plans(plans);

            if (lastRefreshAt == -1) {
                criteriaBuilder.status(Subscription.Status.ACCEPTED);
            } else {
                criteriaBuilder
                        .statuses(REFRESH_STATUS)
                        .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
                        .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY);
            }

            try {
                subscriptionRepository
                        .search(criteriaBuilder.build())
                        .forEach(this::saveOrUpdate);

                lastRefreshAt = nextLastRefreshAt;
            } catch (Exception ex) {
                errorsCount++;
                LOGGER.error("Unexpected error while refreshing subscriptions", ex);
                lastException = ex;
            }

            count++;

            long end = System.currentTimeMillis();

            long diff = end - start;
            totalTime += diff;

            if (count == 1) {
                minTime = diff;
            } else {
                if (diff > maxTime) {
                    maxTime = diff;
                }

                if (diff < minTime) {
                    minTime = diff;
                }
            }

            avgTime = totalTime / count;
        }
    }

    private void saveOrUpdate(Subscription subscription) {
        String key = subscription.getApi() + '-' + subscription.getClientId();

        Object element = cache.get(subscription.getId());

        if ((CLOSED.equals(subscription.getStatus()) || PAUSED.equals(subscription.getStatus()))
                && element != null) {
            cache.remove(subscription.getId());
            String oldKey = (String) element;
            Subscription eltSubscription = (Subscription) cache.get(oldKey);
            if (eltSubscription != null && eltSubscription.getId().equals(subscription.getId())) {
                cache.remove(oldKey);
            }
        } else if (ACCEPTED.equals(subscription.getStatus())){
            LOGGER.debug("Cache a subscription: plan[{}] application[{}] client_id[{}]", subscription.getPlan(), subscription.getApplication(), subscription.getClientId());
            cache.put(subscription.getId(), key);

            // Delete useless information to preserve memory
            subscription.setGeneralConditionsContentPageId(null);
            subscription.setRequest(null);
            subscription.setReason(null);
            subscription.setSubscribedBy(null);
            subscription.setProcessedBy(null);

            cache.put(key, subscription);

            if (element != null) {
                final String oldKey = (String) element;
                if (!oldKey.equals(key)) {
                    cache.remove(oldKey);
                }
            }
        }
    }

    public Api getApi() {
        return api;
    }

    public long getLastRefreshAt() {
        return lastRefreshAt;
    }

    public long getCount() {
        return count;
    }

    public long getMinTime() {
        return minTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public long getAvgTime() {
        return avgTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public long getErrorsCount() {
        return errorsCount;
    }

    public Throwable getLastException() {
        return lastException;
    }

    public void setSubscriptionRepository(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public void setCache(Map<String, Object> cache) {
        this.cache = cache;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public void setDistributed(boolean distributed) {
        this.distributed = distributed;
    }
}
