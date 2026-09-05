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
import java.util.List;
import java.util.Map;

/**
 * The same measures computed once per group of documents, in a single request. Each group selects its documents
 * with its own filters on top of the request filters, and is answered under its key.
 *
 * @param groups the filters of each group, by group key; the insertion order is kept in the answer
 */
public record GroupedMeasuresRequest(
    TimeRange timeRange,
    List<Filter> filters,
    List<MetricMeasuresRequest> metrics,
    Map<String, List<Filter>> groups
) {
    public GroupedMeasuresRequest emptyMetrics() {
        return new GroupedMeasuresRequest(timeRange, filters, new ArrayList<>(), groups);
    }
}
