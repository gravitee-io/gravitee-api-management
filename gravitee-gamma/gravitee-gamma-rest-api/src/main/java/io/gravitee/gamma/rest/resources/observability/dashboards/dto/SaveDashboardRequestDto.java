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
package io.gravitee.gamma.rest.resources.observability.dashboards.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardContent;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardFilter;
import java.util.List;

/**
 * Request shape shared by POST and PUT (OBS-16). {@code id} is read on POST only (client-supplied,
 * AGENTS.md §9) — PUT takes the id from the path. Server-owned fields ({@code version},
 * {@code createdAt}, {@code updatedAt}, {@code createdBy}, {@code environmentId}) are not declared
 * here and are silently dropped by Jackson ({@code GraviteeMapper} disables
 * {@code FAIL_ON_UNKNOWN_PROPERTIES}), so a client sending them cannot influence the write.
 *
 * <p>{@code version} stays out of this shape on purpose (OBS-17): the revision a PUT is based on
 * travels in the {@code If-Match} header, not the body, so the body remains a pure description of
 * the dashboard's content and the concurrency token is expressed the way HTTP already expresses it.
 */
public record SaveDashboardRequestDto(
    String id,
    String title,
    String description,
    List<SaveDashboardFilterDto> filters,
    DashboardTimeRangeDto timeRange,
    JsonNode widgets
) {
    public DashboardContent toContent() {
        return new DashboardContent(title, description, toCoreFilters(), timeRange == null ? null : timeRange.toCore(), widgets);
    }

    /**
     * A literal {@code null} element in the JSON array deserializes to a null entry — reject it as a
     * 400 rather than letting it surface as an NPE-driven 500.
     */
    private List<DashboardFilter> toCoreFilters() {
        if (filters == null) {
            return List.of();
        }
        return filters
            .stream()
            .map(filter -> {
                if (filter == null) {
                    throw new InvalidDashboardException("Dashboard filters must not contain null entries");
                }
                return filter.toCore();
            })
            .toList();
    }
}
