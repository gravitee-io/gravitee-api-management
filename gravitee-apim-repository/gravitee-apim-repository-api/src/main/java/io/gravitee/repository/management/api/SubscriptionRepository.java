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
package io.gravitee.repository.management.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Subscription repository API.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SubscriptionRepository extends CrudRepository<Subscription, String> {
    Page<Subscription> search(SubscriptionCriteria criteria, Sortable sortable, Pageable pageable) throws TechnicalException;

    List<Subscription> search(SubscriptionCriteria criteria, Sortable sortable) throws TechnicalException;

    List<Subscription> search(SubscriptionCriteria criteria) throws TechnicalException;

    List<Subscription> findByIdIn(Collection<String> ids) throws TechnicalException;

    Set<String> findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionCriteria criteria, Order order) throws TechnicalException;

    /**
     * Delete subscription by environment ID
     *
     * @param environmentId The environment ID
     * @return List of deleted IDs for subscriptions
     * @throws TechnicalException
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
