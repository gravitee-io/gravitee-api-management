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
package inmemory;

import io.gravitee.apim.core.plan.crud_service.KafkaPortRangeCrudService;
import io.gravitee.apim.core.plan.model.KafkaPortRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class KafkaPortRangeCrudServiceInMemory implements KafkaPortRangeCrudService, InMemoryAlternative<KafkaPortRange> {

    private final List<KafkaPortRange> storage = new ArrayList<>();

    @Override
    public KafkaPortRange create(KafkaPortRange portRange) {
        storage.add(portRange);
        return portRange;
    }

    @Override
    public KafkaPortRange update(KafkaPortRange portRange) {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).getPlanId().equals(portRange.getPlanId())) {
                storage.set(i, portRange);
                return portRange;
            }
        }
        throw new IllegalStateException("No kafka port range found for plan " + portRange.getPlanId());
    }

    @Override
    public Optional<KafkaPortRange> findByPlanId(String planId) {
        return storage
            .stream()
            .filter(r -> r.getPlanId().equals(planId))
            .findFirst();
    }

    @Override
    public void delete(String planId) {
        storage.removeIf(r -> r.getPlanId().equals(planId));
    }

    @Override
    public void deleteByApiId(String apiId) {
        storage.removeIf(r -> apiId.equals(r.getApiId()));
    }

    @Override
    public List<KafkaPortRange> findConflicting(
        String environmentId,
        String shardingTag,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId
    ) {
        return storage
            .stream()
            .filter(r -> environmentId.equals(r.getEnvironmentId()))
            .filter(r -> Objects.equals(shardingTag, r.getShardingTag()))
            .filter(r -> excludePlanId == null || !excludePlanId.equals(r.getPlanId()))
            .filter(
                r ->
                    // broker-range overlap
                    (r.getRangeStart() <= rangeEnd && r.getRangeEnd() >= rangeStart) ||
                    // new bootstrap inside existing range
                    (bootstrapPort >= r.getRangeStart() && bootstrapPort <= r.getRangeEnd()) ||
                    // existing bootstrap inside new range
                    (r.getBootstrapPort() >= rangeStart && r.getBootstrapPort() <= rangeEnd) ||
                    // bootstrap collision
                    r.getBootstrapPort() ==
                    bootstrapPort
            )
            .toList();
    }

    @Override
    public List<KafkaPortRange> findConflictingForUpdate(
        String environmentId,
        String shardingTag,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId
    ) {
        // In-memory has no transaction/locking semantics; the FOR UPDATE variant is observably
        // equivalent to the non-locking query for unit tests.
        return findConflicting(environmentId, shardingTag, bootstrapPort, rangeStart, rangeEnd, excludePlanId);
    }

    @Override
    public void initWith(List<KafkaPortRange> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<KafkaPortRange> storage() {
        return storage;
    }
}
