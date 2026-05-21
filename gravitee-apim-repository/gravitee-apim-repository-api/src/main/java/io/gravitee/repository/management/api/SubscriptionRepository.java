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
import io.gravitee.repository.management.api.search.SubscriptionCursor;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    /**
     * Same filters as {@link #search(SubscriptionCriteria)} but skips the implicit {@code createdAt desc}
     * default sort. Use this for index-friendly queries where order is irrelevant — notably range scans on
     * {@code endingAt} that would otherwise be blocked by the sort stage (ESR violation).
     */
    List<Subscription> searchUnordered(SubscriptionCriteria criteria) throws TechnicalException;

    /**
     * Keyset / seek pagination over subscriptions matching {@code criteria}. Returns up to
     * {@code pageSize} subscriptions positioned strictly after {@code after} (or starting from
     * the beginning when {@code after} is {@code null}). A short page (size {@code < pageSize})
     * signals exhaustion.
     *
     * <p>Sort and seek mode are controlled by {@code sortable.field()}:
     * <ul>
     *   <li>{@code "updatedAt"} (or {@code null}) — sort {@code (updatedAt ASC, id ASC)}, seek
     *       past {@code (after.updatedAt, after.id)}. Used by the gateway-sync delta loop.</li>
     *   <li>{@code "id"} — sort {@code id ASC}, seek past {@code after.id}. Used by the
     *       gateway-sync warmup load where a {@code plans IN} filter dominates selectivity.</li>
     * </ul>
     * {@code sortable.order()} is ignored — ASC is enforced.
     *
     * <p>Implementations <b>must</b> reject criteria including {@code planSecurityTypes} or
     * {@code excludedApis} with {@link TechnicalException} — these filters are not supported by
     * this narrow path (use {@link #search(SubscriptionCriteria, Sortable)} instead).
     *
     * @throws TechnicalException if {@code criteria} carries an unsupported filter or if
     *         {@code sortable.field()} is not one of the documented values.
     */
    List<Subscription> searchAfter(SubscriptionCriteria criteria, Sortable sortable, SubscriptionCursor after, int pageSize)
        throws TechnicalException;

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

    Set<Subscription> findByReferenceIdAndReferenceType(String referenceId, SubscriptionReferenceType referenceType)
        throws TechnicalException;

    Optional<Subscription> findByIdAndReferenceIdAndReferenceType(
        String subscriptionId,
        String referenceId,
        SubscriptionReferenceType referenceType
    ) throws TechnicalException;
}
