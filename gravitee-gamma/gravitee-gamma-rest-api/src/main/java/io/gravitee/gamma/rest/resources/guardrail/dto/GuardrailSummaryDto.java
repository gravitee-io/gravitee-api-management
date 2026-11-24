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
package io.gravitee.gamma.rest.resources.guardrail.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailLifecycleState;
import java.time.Instant;
import java.util.Set;

/**
 * One row of the Guardrail list: what a list screen shows, and nothing it does not.
 *
 * <p>The guardrail policy sequence and the resources are deliberately absent. They are the bulk of a
 * Guardrail and a list of twenty would carry twenty copies of them, while no list column renders one;
 * the whole definition stays on {@code GET /guardrails/{guardrailId}}.
 *
 * <p>{@code guardrailPolicyCount} replaces them for the one thing the list does say about the sequence —
 * how long it is. Without it the console would have to read every Guardrail in full to render a
 * column, which is the cost this type exists to avoid.
 *
 * <p>Timestamps are ISO 8601 in UTC, for the reasons given on {@link GuardrailDto}.
 *
 * @author GraviteeSource Team
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuardrailSummaryDto(
    String id,
    String name,
    String description,
    io.gravitee.definition.model.v4.guardrail.Guardrail.Phase phase,
    Set<ApiType> apiTypes,
    GuardrailLifecycleState lifecycleState,
    int guardrailPolicyCount,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant updatedAt
) {
    public static GuardrailSummaryDto from(Guardrail guardrail) {
        return new GuardrailSummaryDto(
            guardrail.getId(),
            guardrail.getName(),
            guardrail.getDescription(),
            guardrail.getPhase(),
            guardrail.getApiTypes(),
            guardrail.getLifecycleState(),
            guardrail.getPolicies() == null ? 0 : guardrail.getPolicies().size(),
            guardrail.getCreatedAt(),
            guardrail.getUpdatedAt()
        );
    }
}
