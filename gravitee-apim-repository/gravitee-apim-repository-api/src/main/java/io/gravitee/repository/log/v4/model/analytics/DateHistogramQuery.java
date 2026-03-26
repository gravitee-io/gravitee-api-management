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
package io.gravitee.repository.log.v4.model.analytics;

import java.time.Duration;
import java.time.Instant;

/**
 * Query for DATE_HISTO analytics: date_histogram aggregation with a nested terms sub-agg.
 *
 * @param apiId    the API identifier
 * @param field    the field for the nested terms sub-aggregation (e.g., "status")
 * @param interval the histogram bucket interval
 * @param from     start of time range
 * @param to       end of time range
 */
public record DateHistogramQuery(String apiId, String field, Duration interval, Instant from, Instant to) {}
