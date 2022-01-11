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
package io.gravitee.repository.mongodb.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.mongodb.management.internal.model.SubscriptionMongo;
import io.gravitee.repository.mongodb.management.internal.plan.SubscriptionMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoSubscriptionRepository implements SubscriptionRepository {

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private SubscriptionMongoRepository internalSubscriptionRepository;

    @Override
    public Page<Subscription> search(SubscriptionCriteria criteria, Pageable pageable) throws TechnicalException {
        Page<SubscriptionMongo> subscriptionsMongo = internalSubscriptionRepository.search(criteria, pageable);

        List<Subscription> content = mapper.collection2list(subscriptionsMongo.getContent(), SubscriptionMongo.class, Subscription.class);
        return new Page<>(
            content,
            subscriptionsMongo.getPageNumber(),
            (int) subscriptionsMongo.getPageElements(),
            subscriptionsMongo.getTotalElements()
        );
    }

    @Override
    public List<Subscription> search(SubscriptionCriteria criteria) throws TechnicalException {
        Page<SubscriptionMongo> subscriptionsMongo = internalSubscriptionRepository.search(criteria, null);

        return mapper.collection2list(subscriptionsMongo.getContent(), SubscriptionMongo.class, Subscription.class);
    }

    @Override
    public Set<String> findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionCriteria criteria, Order order) {
        return internalSubscriptionRepository.findReferenceIdsOrderByNumberOfSubscriptions(criteria, order);
    }

    @Override
    public Optional<Subscription> findById(String subscription) throws TechnicalException {
        SubscriptionMongo planMongo = internalSubscriptionRepository.findById(subscription).orElse(null);
        return Optional.ofNullable(map(planMongo));
    }

    @Override
    public Subscription create(Subscription subscription) throws TechnicalException {
        SubscriptionMongo subscriptionMongo = map(subscription);
        subscriptionMongo = internalSubscriptionRepository.insert(subscriptionMongo);
        return map(subscriptionMongo);
    }

    @Override
    public Subscription update(Subscription subscription) throws TechnicalException {
        if (subscription == null || subscription.getId() == null) {
            throw new IllegalStateException("Subscription to update must have an id");
        }

        SubscriptionMongo subscriptionMongo = internalSubscriptionRepository.findById(subscription.getId()).orElse(null);

        if (subscriptionMongo == null) {
            throw new IllegalStateException(String.format("No subscription found with id [%s]", subscription.getId()));
        }

        subscriptionMongo = map(subscription);
        subscriptionMongo = internalSubscriptionRepository.save(subscriptionMongo);
        return map(subscriptionMongo);
    }

    @Override
    public void delete(String plan) throws TechnicalException {
        internalSubscriptionRepository.deleteById(plan);
    }

    private SubscriptionMongo map(Subscription subscription) {
        return (subscription == null) ? null : mapper.map(subscription, SubscriptionMongo.class);
    }

    private Subscription map(SubscriptionMongo subscriptionMongo) {
        return (subscriptionMongo == null) ? null : mapper.map(subscriptionMongo, Subscription.class);
    }

    @Override
    public Set<Subscription> findAll() throws TechnicalException {
        return internalSubscriptionRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
    }
}
