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
package io.gravitee.gamma.rest.core.observability.dashboard.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import java.time.Instant;
import java.util.List;

/**
 * A dashboard saved by a Gamma Observability user, served by the read endpoints in this ticket
 * (OBS-15) and written by OBS-16/17. Distinct from the legacy v2 analytics dashboard and from
 * {@code CustomDashboard} — see OBS-13 for the rationale.
 *
 * <p>{@code widgets} is deliberately opaque: never parsed, only returned verbatim (see
 * {@code GammaDashboard.widgets}).
 *
 * @author GraviteeSource Team
 */
public record Dashboard(
    String id,
    String environmentId,
    String title,
    String description,
    List<DashboardFilter> filters,
    TimeRange timeRange,
    JsonNode widgets,
    Integer version,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {
    public Dashboard {
        requireNonBlank(id, "id");
        requireNonBlank(environmentId, "environmentId");
        requireNonBlank(title, "title");
        filters = filters == null ? List.of() : List.copyOf(filters);
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidDashboardException("Dashboard " + fieldName + " is required");
        }
    }
}
