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
package io.gravitee.repository.redis.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.redis.management.internal.SubscriptionRedisRepository;
import io.gravitee.repository.redis.management.model.RedisSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisSubscriptionRepository implements SubscriptionRepository {

    @Autowired
    private SubscriptionRedisRepository subscriptionRedisRepository;

    @Override
    public Page<Subscription> search(SubscriptionCriteria criteria, Pageable pageable) throws TechnicalException {
        Page<RedisSubscription> pagedSubscriptions = subscriptionRedisRepository.search(criteria, pageable);

        return new Page<>(
                pagedSubscriptions.getContent().stream().map(this::convert).collect(Collectors.toList()),
                pagedSubscriptions.getPageNumber(), (int) pagedSubscriptions.getTotalElements(),
                pagedSubscriptions.getTotalElements());
    }

    @Override
    public List<Subscription> search(SubscriptionCriteria criteria) throws TechnicalException {
        Page<RedisSubscription> pagedSubscriptions = subscriptionRedisRepository.search(criteria, null);

        return pagedSubscriptions.getContent()
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Subscription> findById(String subscription) throws TechnicalException {
        RedisSubscription redisSubscription = subscriptionRedisRepository.find(subscription);
        return Optional.ofNullable(convert(redisSubscription));
    }

    @Override
    public Subscription create(Subscription subscription) throws TechnicalException {
        RedisSubscription redisSubscription = subscriptionRedisRepository.saveOrUpdate(convert(subscription));
        return convert(redisSubscription);
    }

    @Override
    public Subscription update(Subscription subscription) throws TechnicalException {
        if (subscription == null || subscription.getId() == null) {
            throw new IllegalStateException("Subscription to update must have an id");
        }

        RedisSubscription redisSubscription = subscriptionRedisRepository.find(subscription.getId());

        if (redisSubscription == null) {
            throw new IllegalStateException(String.format("No subscription found with id [%s]", subscription.getId()));
        }

        RedisSubscription redisSubscriptionUpdated = subscriptionRedisRepository.saveOrUpdate(convert(subscription));
        return convert(redisSubscriptionUpdated);
    }

    @Override
    public void delete(String subscription) throws TechnicalException {
        subscriptionRedisRepository.delete(subscription);
    }

    private Subscription convert(RedisSubscription redisSubscription) {
        if (redisSubscription == null) {
            return null;
        }

        Subscription subscription = new Subscription();
        subscription.setId(redisSubscription.getId());
        subscription.setStatus(Subscription.Status.valueOf(redisSubscription.getStatus()));
        subscription.setApplication(redisSubscription.getApplication());
        subscription.setApi(redisSubscription.getApi());
        subscription.setClientId(redisSubscription.getClientId());
        subscription.setPlan(redisSubscription.getPlan());
        subscription.setProcessedBy(redisSubscription.getProcessedBy());
        subscription.setReason(redisSubscription.getReason());
        subscription.setSubscribedBy(redisSubscription.getSubscribedBy());

        if (redisSubscription.getProcessedAt() != 0) {
            subscription.setProcessedAt(new Date(redisSubscription.getProcessedAt()));
        }
        if (redisSubscription.getStartingAt() != 0) {
            subscription.setStartingAt(new Date(redisSubscription.getStartingAt()));
        }
        if (redisSubscription.getEndingAt() != 0) {
            subscription.setEndingAt(new Date(redisSubscription.getEndingAt()));
        }

        subscription.setCreatedAt(new Date(redisSubscription.getCreatedAt()));
        subscription.setUpdatedAt(new Date(redisSubscription.getUpdatedAt()));

        if (redisSubscription.getClosedAt() != 0) {
            subscription.setClosedAt(new Date(redisSubscription.getClosedAt()));
        }

        return subscription;
    }

    private RedisSubscription convert(Subscription subscription) {
        RedisSubscription redisSubscription = new RedisSubscription();
        redisSubscription.setId(subscription.getId());
        redisSubscription.setStatus(subscription.getStatus().name());
        redisSubscription.setReason(subscription.getReason());
        redisSubscription.setProcessedBy(subscription.getProcessedBy());
        redisSubscription.setApplication(subscription.getApplication());
        redisSubscription.setApi(subscription.getApi());
        redisSubscription.setClientId(subscription.getClientId());
        redisSubscription.setPlan(subscription.getPlan());
        redisSubscription.setSubscribedBy(subscription.getSubscribedBy());

        if (subscription.getProcessedAt() != null) {
            redisSubscription.setProcessedAt(subscription.getProcessedAt().getTime());
        }
        if (subscription.getStartingAt() != null) {
            redisSubscription.setStartingAt(subscription.getStartingAt().getTime());
        }
        if (subscription.getEndingAt() != null) {
            redisSubscription.setEndingAt(subscription.getEndingAt().getTime());
        }
        if (subscription.getCreatedAt() != null) {
            redisSubscription.setCreatedAt(subscription.getCreatedAt().getTime());
        }
        if (subscription.getUpdatedAt() != null) {
            redisSubscription.setUpdatedAt(subscription.getUpdatedAt().getTime());
        }

        return redisSubscription;
    }
}
