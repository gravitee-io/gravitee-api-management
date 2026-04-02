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
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST response for {@code GET /v2/apis/{apiId}/analytics?type=GROUP_BY&field=<field>}.
 *
 * <pre>{@code
 * {
 *   "type": "GROUP_BY",
 *   "values":   { "200": 10000, "404": 1500, "500": 845 },
 *   "metadata": { "200": { "name": "200" }, "404": { "name": "404" } }
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiAnalyticsGroupByResponse {

    @JsonProperty("type")
    @Builder.Default
    private String type = "GROUP_BY";

    /** Document count per distinct field value. */
    @JsonProperty("values")
    private Map<String, Long> values;

    /** Human-readable labels per field value. Currently {@code {"name": "<value>"}}; enrichment deferred to M2. */
    @JsonProperty("metadata")
    private Map<String, Map<String, String>> metadata;
}
