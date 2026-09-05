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
package io.gravitee.repository.analytics.engine.api.query;

import java.util.List;
import java.util.Map;

/**
 * The same measures computed once per group of documents, in a single request. Each group is named by the caller
 * and selects its documents with its own filters, on top of the query filters shared by every group; a group whose
 * filters match no document still comes back, with no measure.
 *
 * @param groups the filters of each group, by group key; the insertion order is the order of the answer
 */
public record GroupedMeasuresQuery(
    TimeRange timeRange,
    List<Filter> filters,
    List<MetricMeasuresQuery> metrics,
    Map<String, List<Filter>> groups
) implements Query {}
