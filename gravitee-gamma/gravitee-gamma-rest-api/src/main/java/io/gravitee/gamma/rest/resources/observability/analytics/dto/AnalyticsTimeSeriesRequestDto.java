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
package io.gravitee.gamma.rest.resources.observability.analytics.dto;

import io.gravitee.gamma.rest.resources.observability.logs.dto.FilterConditionDto;
import io.gravitee.gamma.rest.resources.observability.logs.dto.SearchLogsRequestDto.TimeRangeDto;
import java.util.List;

/**
 * POST body for {@code /observability/analytics/time-series}.
 */
public record AnalyticsTimeSeriesRequestDto(
    TimeRangeDto timeRange,
    List<FilterConditionDto> filters,
    Long interval,
    List<FacetMetricQueryDto> metrics,
    List<String> by,
    Integer facetSize,
    List<NumberRangeDto> ranges
) {}
