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
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * A Guardian as the console reads it: its whole definition, plus what the store owns.
 *
 * <p>{@code environmentId} is the one field of the domain model left out — it is already in the path.
 * The deployment stamp is converted to a {@link ZonedDateTime} like the other two timestamps, so one
 * response does not mix formats.
 *
 * <p>The three timestamps are ISO 8601 in UTC. Both halves of that are deliberate. The shape needs
 * {@code @JsonFormat}: the inherited rest-api mapper enables {@code WRITE_DATES_AS_TIMESTAMPS}, which
 * would otherwise write a fractional {@code <epoch_seconds>.<nanos>} number. The zone needs
 * {@link ZoneOffset#UTC}: read back with {@code ZoneId.systemDefault()} the offset would depend on the
 * server's time zone, so the same instant would be written differently on two nodes.
 *
 * @author GraviteeSource Team
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuardianDto(
    String id,
    String name,
    String description,
    io.gravitee.definition.model.v4.guardian.Guardian.Phase phase,
    Set<ApiType> apiTypes,
    List<Step> policies,
    List<Resource> resources,
    GuardianLifecycleState lifecycleState,
    @JsonFormat(shape = JsonFormat.Shape.STRING) ZonedDateTime deployedAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) ZonedDateTime createdAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) ZonedDateTime updatedAt
) {
    public static GuardianDto from(Guardian guardian) {
        return new GuardianDto(
            guardian.getId(),
            guardian.getName(),
            guardian.getDescription(),
            guardian.getPhase(),
            guardian.getApiTypes(),
            guardian.getPolicies(),
            guardian.getResources(),
            guardian.getLifecycleState(),
            toZonedDateTime(guardian.getDeployedAt()),
            toUtc(guardian.getCreatedAt()),
            toUtc(guardian.getUpdatedAt())
        );
    }

    private static ZonedDateTime toZonedDateTime(Date date) {
        return date == null ? null : ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    /** The same instant, read in UTC, so the offset written does not depend on the server's zone. */
    private static ZonedDateTime toUtc(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.withZoneSameInstant(ZoneOffset.UTC);
    }
}
