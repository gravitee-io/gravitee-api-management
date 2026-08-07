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

import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRange;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRangeType;

/**
 * Wire shape for a dashboard's time range. {@code type} is lowercase ({@code relative} /
 * {@code absolute}) to match the frontend's discriminated union.
 */
public record DashboardTimeRangeDto(String type, String period, Long from, Long to) {
    public static DashboardTimeRangeDto from(TimeRange timeRange) {
        if (timeRange == null) {
            return null;
        }
        return new DashboardTimeRangeDto(timeRange.type().name().toLowerCase(), timeRange.period(), timeRange.from(), timeRange.to());
    }

    public TimeRange toCore() {
        if (type == null || type.isBlank()) {
            throw new InvalidDashboardException("Dashboard time range type is required");
        }
        TimeRangeType timeRangeType;
        try {
            timeRangeType = TimeRangeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDashboardException("Unsupported dashboard time range type '" + type + "'");
        }
        return new TimeRange(timeRangeType, period, from, to);
    }
}
