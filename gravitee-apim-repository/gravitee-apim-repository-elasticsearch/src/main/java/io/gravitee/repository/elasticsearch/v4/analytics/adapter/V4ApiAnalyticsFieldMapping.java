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
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Maps API analytics field names (PRD) to v4-metrics Elasticsearch index field names.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class V4ApiAnalyticsFieldMapping {

    private static final Map<String, String> API_TO_ES = Map.ofEntries(
        Map.entry("status", "status"),
        Map.entry("mapped-status", "status"),
        Map.entry("application", "application-id"),
        Map.entry("plan", "plan-id"),
        Map.entry("host", "host"),
        Map.entry("uri", "uri"),
        Map.entry("gateway-latency-ms", "gateway-response-time-ms"),
        Map.entry("gateway-response-time-ms", "gateway-response-time-ms"),
        Map.entry("endpoint-response-time-ms", "endpoint-response-time-ms"),
        Map.entry("request-content-length", "request-content-length")
    );

    public static String toEsField(String apiField) {
        return API_TO_ES.getOrDefault(apiField, apiField);
    }
}
