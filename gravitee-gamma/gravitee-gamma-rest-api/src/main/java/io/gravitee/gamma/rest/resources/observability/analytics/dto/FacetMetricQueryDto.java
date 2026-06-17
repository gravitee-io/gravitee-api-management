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

import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import java.util.List;

/**
 * Wire shape for a metric query on the facets and time-series endpoints.
 */
public record FacetMetricQueryDto(String name, List<String> measures, List<SortSpecDto> sorts) {
    public AnalyticsFacetMetricQuery toCore() {
        return new AnalyticsFacetMetricQuery(
            name,
            measures != null ? measures : List.of(),
            sorts != null ? sorts.stream().map(SortSpecDto::toCore).toList() : List.of()
        );
    }
}
