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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.KafkaPortRange;
import java.util.List;

public interface KafkaPortRangeRepository extends CrudRepository<KafkaPortRange, String> {
    /**
     * Returns all port-range records that conflict with the given candidate range, scoped to the
     * same {@code environment_id + sharding_tag}. A conflict is any of:
     *
     * <ol>
     *   <li>existing broker range overlaps with {@code [rangeStart, rangeEnd]}</li>
     *   <li>new {@code bootstrapPort} falls inside an existing broker range</li>
     *   <li>an existing {@code bootstrapPort} falls inside {@code [rangeStart, rangeEnd]}</li>
     *   <li>an existing {@code bootstrapPort} equals {@code bootstrapPort}</li>
     * </ol>
     *
     * <p>The plan identified by {@code excludePlanId} is skipped (used when updating a plan, so the
     * plan's own row doesn't conflict with itself). Pass {@code null} to consider all rows.</p>
     *
     * <p>{@code shardingTag} {@code null} matches rows with {@code null} tag only — caller is
     * responsible for querying once per applicable tag when a plan is deployed to multiple.</p>
     */
    List<KafkaPortRange> findConflicting(
        String environmentId,
        String shardingTag,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId
    ) throws TechnicalException;

    /**
     * Same semantics as {@link #findConflicting(String, String, int, int, int, String)} but acquires
     * a row-level lock on each matching row for the duration of the current transaction (JDBC
     * {@code SELECT ... FOR UPDATE}).
     *
     * <p>Callers must invoke this inside an active transaction and follow up with a {@code create}
     * of the candidate row before the transaction commits. This prevents a TOCTOU race where two
     * concurrent plan saves both see "no conflict" and both persist.</p>
     *
     * <p><b>MongoDB limitation:</b> the MongoDB implementation delegates to the non-locking
     * {@link #findConflicting} — row-level locks are not available outside of MongoDB multi-document
     * transactions (which require a replica-set or sharded cluster plus explicit client-side
     * session management). Deployments that require strict concurrent-save protection on MongoDB
     * must enable multi-document transactions and wire this call through a ClientSession.</p>
     */
    List<KafkaPortRange> findConflictingForUpdate(
        String environmentId,
        String shardingTag,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId
    ) throws TechnicalException;

    /**
     * Deletes every port-range row for the given API, in one transactional call. Used when an API
     * is undeployed or deleted — frees all port allocations associated with that API.
     */
    void deleteByApiId(String apiId) throws TechnicalException;

    /**
     * Deletes every port-range row scoped to the given environment. Used during environment
     * cleanup.
     */
    void deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
