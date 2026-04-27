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
package io.gravitee.apim.infra.query_service.tracing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TempoSearchResponse(List<TraceResult> traces) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TraceResult(
        String traceID,
        String rootServiceName,
        String rootTraceName,
        long startTimeUnixNano,
        long durationMs,
        SpanSet spanSet
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpanSet(List<Span> spans) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Span(String spanID, List<KeyValue> attributes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeyValue(String key, Value value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(String stringValue, Long intValue, Boolean boolValue, Double doubleValue) {
        public String asString() {
            if (stringValue != null) return stringValue;
            if (intValue != null) return intValue.toString();
            if (boolValue != null) return boolValue.toString();
            if (doubleValue != null) return doubleValue.toString();
            return null;
        }
    }
}
