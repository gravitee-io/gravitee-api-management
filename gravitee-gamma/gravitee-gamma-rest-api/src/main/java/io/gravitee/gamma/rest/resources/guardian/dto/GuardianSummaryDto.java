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
package io.gravitee.gamma.rest.resources.guardian.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * One row of the Guardian list: what a list screen shows, and nothing it does not.
 *
 * <p>The guardrail sequence and the resources are deliberately absent. They are the bulk of a
 * Guardian and a list of twenty would carry twenty copies of them, while no list column renders one;
 * the whole definition stays on {@code GET /guardians/{guardianId}}.
 *
 * <p>{@code guardrailCount} replaces them for the one thing the list does say about the sequence —
 * how long it is. Without it the console would have to read every Guardian in full to render a
 * column, which is the cost this type exists to avoid.
 *
 * <p>Timestamps are ISO 8601 in UTC, for the reasons given on {@link GuardianDto}.
 *
 * @author GraviteeSource Team
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuardianSummaryDto(
    String id,
    String name,
    String description,
    io.gravitee.definition.model.v4.guardian.Guardian.Phase phase,
    Set<ApiType> apiTypes,
    GuardianLifecycleState lifecycleState,
    int guardrailCount,
    @JsonFormat(shape = JsonFormat.Shape.STRING) ZonedDateTime createdAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) ZonedDateTime updatedAt
) {
    public static GuardianSummaryDto from(Guardian guardian) {
        return new GuardianSummaryDto(
            guardian.getId(),
            guardian.getName(),
            guardian.getDescription(),
            guardian.getPhase(),
            guardian.getApiTypes(),
            guardian.getLifecycleState(),
            guardian.getPolicies() == null ? 0 : guardian.getPolicies().size(),
            toUtc(guardian.getCreatedAt()),
            toUtc(guardian.getUpdatedAt())
        );
    }

    private static ZonedDateTime toUtc(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.withZoneSameInstant(ZoneOffset.UTC);
    }
}
