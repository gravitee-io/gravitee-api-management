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
package io.gravitee.apim.core.analytics_engine.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The measures of each group of a {@link GroupedMeasuresRequest}, by group key. */
public record GroupedMeasuresResponse(Map<String, MeasuresResponse> groups) {
    /** Joins the answers of several engines to one request: a group carries the metrics every engine returned for it. */
    public static GroupedMeasuresResponse merge(List<GroupedMeasuresResponse> responses) {
        var metricsByGroup = new LinkedHashMap<String, List<MetricMeasuresResponse>>();
        responses
            .stream()
            .filter(Objects::nonNull)
            .forEach(response ->
                response
                    .groups()
                    .forEach((group, measures) ->
                        metricsByGroup.computeIfAbsent(group, key -> new ArrayList<>()).addAll(measures.metrics())
                    )
            );
        var groups = new LinkedHashMap<String, MeasuresResponse>();
        metricsByGroup.forEach((group, metrics) -> groups.put(group, new MeasuresResponse(metrics)));
        return new GroupedMeasuresResponse(groups);
    }
}
