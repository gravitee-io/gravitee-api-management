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
package io.gravitee.gateway.services.subscriptionscache.repository;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionRepositoryWrapper implements SubscriptionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionRepositoryWrapper.class);

    private final SubscriptionRepository wrapped;
    private final Ehcache cache;

    public SubscriptionRepositoryWrapper(SubscriptionRepository wrapped, Ehcache cache) {
        this.wrapped = wrapped;
        this.cache = cache;
    }

    @Override
    public Optional<Subscription> findById(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Subscription create(Subscription item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Subscription update(Subscription item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Page<Subscription> search(SubscriptionCriteria criteria, Pageable pageable) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public List<Subscription> search(SubscriptionCriteria criteria) throws TechnicalException {
        String key = criteria.getPlans().iterator().next() + '-' + criteria.getClientId();
        Element element = this.cache.get(key);
        return (element != null) ?
                Collections.singletonList((Subscription) element.getObjectValue()):
                null;
    }
}
