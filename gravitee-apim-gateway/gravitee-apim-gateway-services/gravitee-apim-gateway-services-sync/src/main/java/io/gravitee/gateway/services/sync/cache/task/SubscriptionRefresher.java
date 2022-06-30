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
package io.gravitee.gateway.services.sync.cache.task;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class SubscriptionRefresher implements Callable<Result<Boolean>> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private SubscriptionRepository subscriptionRepository;

    private SubscriptionService subscriptionService;

    protected Result<Boolean> doRefresh(SubscriptionCriteria criteria) {
        logger.debug("Refresh subscriptions");

        try {
            subscriptionRepository.search(criteria).stream().map(this::convertModelSubscriptionToCache).forEach(subscriptionService::save);
            return Result.success(true);
        } catch (Exception ex) {
            return Result.failure(ex);
        }
    }

    public void setSubscriptionRepository(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public void setSubscriptionService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    private Subscription convertModelSubscriptionToCache(io.gravitee.repository.management.model.Subscription subscriptionModel) {
        Subscription subscription = new Subscription();
        subscription.setApi(subscriptionModel.getApi());
        subscription.setApplication(subscriptionModel.getApplication());
        subscription.setClientId(subscriptionModel.getClientId());
        subscription.setStartingAt(subscriptionModel.getStartingAt());
        subscription.setEndingAt(subscriptionModel.getEndingAt());
        subscription.setId(subscriptionModel.getId());
        subscription.setPlan(subscriptionModel.getPlan());
        if (subscriptionModel.getStatus() != null) {
            subscription.setStatus(subscriptionModel.getStatus().name());
        }
        return subscription;
    }
}
