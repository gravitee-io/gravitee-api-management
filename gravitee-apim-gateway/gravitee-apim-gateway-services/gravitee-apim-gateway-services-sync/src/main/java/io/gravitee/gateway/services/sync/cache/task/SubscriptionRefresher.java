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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionConfiguration;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import java.util.concurrent.Callable;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Slf4j
public abstract class SubscriptionRefresher implements Callable<Result<Boolean>> {

    private SubscriptionRepository subscriptionRepository;

    protected SubscriptionService subscriptionService;

    private ObjectMapper objectMapper;

    protected Result<Boolean> doRefresh(SubscriptionCriteria criteria) {
        return doRefresh(criteria, false);
    }

    protected Result<Boolean> doRefresh(SubscriptionCriteria criteria, boolean forceDispatch) {
        log.debug("Refresh subscriptions");

        try {
            subscriptionRepository
                .search(criteria)
                .stream()
                .map(this::convertModelSubscriptionToCache)
                .map(s -> {
                    s.setForceDispatch(forceDispatch);
                    return s;
                })
                .forEach(this::handleSubscription);
            return Result.success(true);
        } catch (Exception ex) {
            return Result.failure(ex);
        }
    }

    protected abstract void handleSubscription(Subscription subscription);

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
        if (subscriptionModel.getConsumerStatus() != null) {
            subscription.setConsumerStatus(Subscription.ConsumerStatus.valueOf(subscriptionModel.getConsumerStatus().name()));
        }
        if (subscriptionModel.getType() != null) {
            subscription.setType(Subscription.Type.valueOf(subscriptionModel.getType().name().toUpperCase()));
        }
        if (subscriptionModel.getConfiguration() != null) {
            try {
                SubscriptionConfiguration subscriptionConfiguration = objectMapper.readValue(
                    subscriptionModel.getConfiguration(),
                    SubscriptionConfiguration.class
                );
                subscription.setConfiguration(subscriptionConfiguration);
            } catch (Exception e) {
                log.error("Unable to parse subscription configuration.", e);
            }
        }
        subscription.setMetadata(subscriptionModel.getMetadata());
        return subscription;
    }
}
