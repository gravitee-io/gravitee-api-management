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
package io.gravitee.repository.bridge.client.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.bridge.client.utils.BodyCodecs;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpSubscriptionRepository extends AbstractRepository implements SubscriptionRepository {

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
        return blockingGet(
            post("/subscriptions/_search", BodyCodecs.page(Subscription.class))
                .addQueryParam("page", Integer.toString(pageable.pageNumber()))
                .addQueryParam("size", Integer.toString(pageable.pageSize()))
                .send(criteria)
        )
            .payload();
    }

    @Override
    public List<Subscription> search(SubscriptionCriteria criteria) throws TechnicalException {
        return blockingGet(post("/subscriptions/_search", BodyCodecs.list(Subscription.class)).send(criteria)).payload();
    }

    @Override
    public Set<String> findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionCriteria criteria) {
        throw new IllegalStateException();
    }

    @Override
    public Set<Subscription> findAll() throws TechnicalException {
        throw new IllegalStateException();
    }
}
