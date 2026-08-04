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
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import java.util.List;

/**
 * Wire shape for a dashboard. {@code environmentId} and {@code createdBy} are intentionally omitted
 * — redundant with the URL scope / not needed by the UI. {@code widgets} is returned verbatim
 * (opaque, never parsed). {@code version} must be present even though nothing enforces it yet —
 * OBS-16/17 need it on the write path and reads are the only place it can be handed out.
 */
public record DashboardDto(
    String id,
    String title,
    String description,
    List<DashboardFilterDto> filters,
    DashboardTimeRangeDto timeRange,
    JsonNode widgets,
    Integer version,
    Long createdAt,
    Long updatedAt
) {
    public static DashboardDto from(Dashboard dashboard) {
        return new DashboardDto(
            dashboard.id(),
            dashboard.title(),
            dashboard.description(),
            dashboard.filters().stream().map(DashboardFilterDto::from).toList(),
            DashboardTimeRangeDto.from(dashboard.timeRange()),
            dashboard.widgets(),
            dashboard.version(),
            dashboard.createdAt() == null ? null : dashboard.createdAt().toEpochMilli(),
            dashboard.updatedAt() == null ? null : dashboard.updatedAt().toEpochMilli()
        );
    }
}
