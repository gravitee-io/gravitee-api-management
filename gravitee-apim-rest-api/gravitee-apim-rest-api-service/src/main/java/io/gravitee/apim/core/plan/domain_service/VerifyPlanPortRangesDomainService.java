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
package io.gravitee.apim.core.plan.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.plan.crud_service.KafkaPortRangeCrudService;
import io.gravitee.apim.core.plan.exception.PlanInvalidException;
import io.gravitee.apim.core.plan.exception.PortRangeConflictException;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Validates a Kafka plan's port allocation before it is persisted.
 *
 * <p>Checks internal consistency of the candidate allocation ({@code bootstrapPort},
 * {@code [rangeStart, rangeEnd]}) and queries the {@code kafka_port_ranges} table for conflicts
 * with already-deployed plans sharing the same {@code environment_id + sharding_tag} scope.</p>
 *
 * <p>Follows the pattern of {@link io.gravitee.apim.core.api.domain_service.VerifyApiHostsDomainService}.</p>
 */
@CustomLog
@RequiredArgsConstructor
@DomainService
public class VerifyPlanPortRangesDomainService {

    static final int MIN_PORT = 1024;
    static final int MAX_PORT = 65535;

    private final KafkaPortRangeCrudService kafkaPortRangeCrudService;

    /**
     * Runs the full set of port-routing checks. Throws on first failure.
     *
     * @param environmentId  scoping key — conflicts only compared within the same environment
     * @param shardingTag    scoping key — {@code null} = default tag; matches other null-tagged plans only
     * @param planId         id of the plan being saved; excluded from its own conflict check on update. Pass {@code null} for create.
     * @param bootstrapPort  candidate bootstrap port
     * @param rangeStart     candidate broker range start (inclusive)
     * @param rangeEnd       candidate broker range end (inclusive)
     *
     * @throws PlanInvalidException        if values are out of bounds, or {@code bootstrapPort} falls inside {@code [rangeStart, rangeEnd]}
     * @throws PortRangeConflictException  if the candidate allocation overlaps with an existing plan
     */
    public void verify(String environmentId, String shardingTag, String planId, int bootstrapPort, int rangeStart, int rangeEnd) {
        ensurePortInBounds(bootstrapPort, "bootstrapPort");
        ensurePortInBounds(rangeStart, "brokerRangeStart");
        ensurePortInBounds(rangeEnd, "brokerRangeEnd");

        if (rangeStart > rangeEnd) {
            throw new PlanInvalidException("brokerRangeStart (" + rangeStart + ") must be <= brokerRangeEnd (" + rangeEnd + ")");
        }

        if (bootstrapPort >= rangeStart && bootstrapPort <= rangeEnd) {
            throw new PlanInvalidException(
                "bootstrapPort (" + bootstrapPort + ") must not fall within brokerRange [" + rangeStart + "-" + rangeEnd + "]"
            );
        }

        // Locking variant: rows returned (or the index range scanned, depending on DB) are held for
        // the duration of the surrounding @UseCase / @Transactional boundary. A concurrent save that
        // would conflict blocks here until we commit, then re-runs the conflict check and fails
        // cleanly — preventing the TOCTOU race where two saves both observe "no conflict" and both
        // persist.
        var conflicts = kafkaPortRangeCrudService.findConflictingForUpdate(
            environmentId,
            shardingTag,
            bootstrapPort,
            rangeStart,
            rangeEnd,
            planId
        );
        if (!conflicts.isEmpty()) {
            var first = conflicts.get(0);
            throw new PortRangeConflictException(first.getPlanId(), first.getApiId(), rangeStart, rangeEnd, bootstrapPort);
        }
    }

    private static void ensurePortInBounds(int port, String fieldName) {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new PlanInvalidException(fieldName + " (" + port + ") must be in range [" + MIN_PORT + "-" + MAX_PORT + "]");
        }
    }
}
