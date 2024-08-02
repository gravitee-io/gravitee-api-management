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
package io.gravitee.repository.noop.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpSubscriptionRepository extends AbstractNoOpManagementRepository<Subscription, String> implements SubscriptionRepository {

    @Override
    public Page<Subscription> search(SubscriptionCriteria criteria, Sortable sortable, Pageable pageable) throws TechnicalException {
        return new Page<>(List.of(), 0, 0, 0L);
    }

    @Override
    public List<Subscription> search(SubscriptionCriteria criteria, Sortable sortable) throws TechnicalException {
        return List.of();
    }

    @Override
    public List<Subscription> search(SubscriptionCriteria criteria) throws TechnicalException {
        return List.of();
    }

    @Override
    public List<Subscription> findByIdIn(Collection<String> ids) throws TechnicalException {
        return List.of();
    }

    @Override
    public Set<String> findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionCriteria criteria, Order order) throws TechnicalException {
        return Set.of();
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        return List.of();
    }
}
