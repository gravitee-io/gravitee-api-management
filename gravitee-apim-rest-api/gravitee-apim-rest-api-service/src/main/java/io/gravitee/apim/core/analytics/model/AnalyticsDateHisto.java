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
package io.gravitee.apim.core.analytics.model;

import java.util.List;
import java.util.Map;

/**
 * Domain model for a date-histogram analytics result.
 *
 * <p>Separates the interface contract from the repository {@code DateHistoAggregate} model,
 * following the same pattern as {@link AnalyticsStats} and {@link AnalyticsGroupBy}.</p>
 *
 * @param timestamps ordered epoch-ms bucket-start values
 * @param buckets    one entry per distinct field value; counts are parallel to {@code timestamps}
 */
public record AnalyticsDateHisto(List<Long> timestamps, List<AnalyticsDateHisto.Bucket> buckets) {
    /**
     * One time-series per distinct field value (e.g. per HTTP status code).
     *
     * @param field    the field value (e.g. "200", "500")
     * @param counts   document count per time bucket, parallel to {@link AnalyticsDateHisto#timestamps}
     * @param metadata human-readable label, currently {@code {"name": "<value>"}}
     */
    public record Bucket(String field, List<Long> counts, Map<String, String> metadata) {}
}
