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
package io.gravitee.apim.core.plan.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.Map;

/**
 * Thrown when a plan's port allocation ({@code bootstrapPort}, {@code [brokerRangeStart,
 * brokerRangeEnd]}) overlaps with an already-registered plan in the same
 * {@code environment_id + sharding_tag} scope.
 */
public class PortRangeConflictException extends ValidationDomainException {

    public PortRangeConflictException(String conflictingPlanId, String conflictingApiId, int rangeStart, int rangeEnd, int bootstrapPort) {
        super(
            String.format(
                "Port range [%d-%d] (bootstrap %d) conflicts with plan '%s' on API '%s'",
                rangeStart,
                rangeEnd,
                bootstrapPort,
                conflictingPlanId,
                conflictingApiId
            ),
            Map.of(
                "conflictingPlanId",
                conflictingPlanId,
                "conflictingApiId",
                conflictingApiId,
                "rangeStart",
                String.valueOf(rangeStart),
                "rangeEnd",
                String.valueOf(rangeEnd),
                "bootstrapPort",
                String.valueOf(bootstrapPort)
            )
        );
    }
}
