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

import java.util.Arrays;
import java.util.Optional;

public enum ApiAnalyticsField {
    STATUS("status", "status", FieldType.KEYWORD),
    MAPPED_STATUS("mapped-status", "mapped-status", FieldType.KEYWORD),
    APPLICATION("application", "application", FieldType.KEYWORD),
    PLAN("plan", "plan", FieldType.KEYWORD),
    HOST("host", "host", FieldType.KEYWORD),
    URI("uri", "uri", FieldType.KEYWORD),
    GATEWAY_LATENCY_MS("gateway-latency-ms", "gateway-latency-ms", FieldType.NUMERIC),
    GATEWAY_RESPONSE_TIME_MS("gateway-response-time-ms", "gateway-response-time-ms", FieldType.NUMERIC),
    ENDPOINT_RESPONSE_TIME_MS("endpoint-response-time-ms", "endpoint-response-time-ms", FieldType.NUMERIC),
    REQUEST_CONTENT_LENGTH("request-content-length", "request-content-length", FieldType.NUMERIC);

    private final String prdName;
    private final String esFieldName;
    private final FieldType fieldType;

    ApiAnalyticsField(String prdName, String esFieldName, FieldType fieldType) {
        this.prdName = prdName;
        this.esFieldName = esFieldName;
        this.fieldType = fieldType;
    }

    public String prdName() {
        return prdName;
    }

    public String esFieldName() {
        return esFieldName;
    }

    public FieldType fieldType() {
        return fieldType;
    }

    public static Optional<ApiAnalyticsField> fromPrdName(String prdName) {
        if (prdName == null || prdName.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.prdName.equals(prdName)).findFirst();
    }

    public enum FieldType {
        KEYWORD,
        NUMERIC,
    }
}
