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
package io.gravitee.repository.mongodb.management.internal.plan;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.SubscriptionCursor;
import io.gravitee.repository.management.api.search.SubscriptionSearchSort;
import io.gravitee.repository.mongodb.management.internal.model.SubscriptionMongo;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface SubscriptionMongoRepositoryCustom {
    Page<SubscriptionMongo> search(SubscriptionCriteria criteria, Sortable sortable, Pageable pageable);

    List<SubscriptionMongo> searchUnordered(SubscriptionCriteria criteria);

    /**
     * Keyset / seek pagination. Builds a {@code find()} query (not the aggregation pipeline). The
     * {@code sort} mode drives both the sort order and the seek predicate so they always match:
     *
     * <ul>
     *   <li>{@code UPDATED_AT} — sorts by {@code (updatedAt, _id)} and seeks past
     *       {@code (cursor.updatedAt, cursor.id)} — the gateway-sync delta loop, served cleanly by
     *       the {@code (environmentId, updatedAt, _id)} compound index.</li>
     *   <li>{@code PLAN_ID} — sorts by {@code (plan, _id)} and seeks past {@code (cursor.plan, cursor.id)}
     *       — the warmup path, where the {@code {plan,_id}} index scans each plan's range in order
     *       instead of an {@code _id}-ordered walk over unrelated rows.</li>
     *   <li>{@code ID} — sorts by {@code _id} only and seeks past {@code cursor.id} — the legacy /
     *       repository-bridge fallback when no plan marker is available.</li>
     * </ul>
     */
    List<SubscriptionMongo> searchAfter(SubscriptionCriteria criteria, SubscriptionCursor after, int pageSize, SubscriptionSearchSort sort);

    Set<String> findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionCriteria criteria, Order order);
}
