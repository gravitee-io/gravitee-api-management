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
     * Keyset / seek pagination. Builds a {@code find()} query (not the aggregation pipeline).
     *
     * <p>When {@code sortByUpdatedAt} is {@code true}, sorts by {@code (updatedAt, _id)} and seeks
     * past {@code (cursor.updatedAt, cursor.id)} — used by the gateway-sync delta loop where the
     * {@code (environmentId, updatedAt, _id)} compound index can be used cleanly.
     *
     * <p>When {@code sortByUpdatedAt} is {@code false}, sorts by {@code _id} only and seeks past
     * {@code cursor.id} — used by the warmup load path where a {@code plans IN} filter dominates
     * selectivity and the {@code _id_} primary index makes the seek O(N) across all pages.
     */
    List<SubscriptionMongo> searchAfter(SubscriptionCriteria criteria, SubscriptionCursor after, int pageSize, boolean sortByUpdatedAt);

    Set<String> findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionCriteria criteria, Order order);
}
