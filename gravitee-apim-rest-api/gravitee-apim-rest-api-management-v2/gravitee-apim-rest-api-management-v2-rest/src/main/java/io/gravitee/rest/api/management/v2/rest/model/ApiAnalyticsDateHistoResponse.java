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
package io.gravitee.rest.api.management.v2.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST response for {@code GET /v2/apis/{apiId}/analytics?type=DATE_HISTO&field=<field>&interval=<ms>}.
 *
 * <pre>{@code
 * {
 *   "type": "DATE_HISTO",
 *   "timestamp": [1625000000000, 1625003600000],
 *   "values": [
 *     { "field": "200", "buckets": [120, 130], "metadata": { "name": "200" } },
 *     { "field": "500", "buckets": [2,   5  ], "metadata": { "name": "500" } }
 *   ]
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiAnalyticsDateHistoResponse {

    @JsonProperty("type")
    @Builder.Default
    private String type = "DATE_HISTO";

    /** Epoch-ms timestamp of each bucket start, in ascending order. */
    @JsonProperty("timestamp")
    private List<Long> timestamp;

    /** One series entry per distinct field value (e.g. one per HTTP status code). */
    @JsonProperty("values")
    private List<Bucket> values;

    /**
     * One time-series per distinct field value.
     *
     * @param field    the field value (e.g. "200", "500")
     * @param buckets  document count (or avg value) per time bucket, parallel to {@code timestamp}
     * @param metadata human-readable label, currently {@code {"name": "<value>"}}
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bucket {

        @JsonProperty("field")
        private String field;

        @JsonProperty("buckets")
        private List<Long> buckets;

        @JsonProperty("metadata")
        private Map<String, String> metadata;
    }
}
