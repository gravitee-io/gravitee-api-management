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
package io.gravitee.apim.core.plan.crud_service;

import io.gravitee.apim.core.plan.model.KafkaPortRange;
import java.util.List;
import java.util.Optional;

public interface KafkaPortRangeCrudService {
    KafkaPortRange create(KafkaPortRange portRange);

    KafkaPortRange update(KafkaPortRange portRange);

    Optional<KafkaPortRange> findByPlanId(String planId);

    void delete(String planId);

    void deleteByApiId(String apiId);

    /**
     * Returns all port-range records that conflict with the given candidate range within the same
     * {@code environment_id} scope. See
     * {@code io.gravitee.repository.management.api.KafkaPortRangeRepository#findConflicting} for
     * the exact overlap conditions (broker-range overlap, bootstrap-inside-range, bootstrap
     * collision).
     *
     * @param excludePlanId plan id to exclude from the check (the plan being updated); may be
     *                      {@code null} when creating a new plan.
     */
    List<KafkaPortRange> findConflicting(String environmentId, int bootstrapPort, int rangeStart, int rangeEnd, String excludePlanId);

    /**
     * Same as {@link #findConflicting} but acquires a row-level lock (JDBC {@code SELECT ... FOR
     * UPDATE}) on each matching row for the duration of the surrounding transaction. Callers must
     * invoke this inside an active transaction and must create the candidate row before the
     * transaction commits; otherwise the lock is released with no effect.
     *
     * <p>On MongoDB, row-level locks are not available outside of multi-document transactions and
     * this call silently degrades to {@link #findConflicting} — see the repository javadoc for
     * details.</p>
     */
    List<KafkaPortRange> findConflictingForUpdate(
        String environmentId,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId
    );
}
