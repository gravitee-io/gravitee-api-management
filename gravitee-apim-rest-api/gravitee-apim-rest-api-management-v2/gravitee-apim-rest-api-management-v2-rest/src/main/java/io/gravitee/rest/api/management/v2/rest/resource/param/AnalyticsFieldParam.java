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
package io.gravitee.rest.api.management.v2.rest.resource.param;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum AnalyticsFieldParam {
    STATUS("status"),
    MAPPED_STATUS("mapped-status"),
    APPLICATION("application"),
    PLAN("plan"),
    HOST("host"),
    URI("uri"),
    GATEWAY_LATENCY_MS("gateway-latency-ms"),
    GATEWAY_RESPONSE_TIME_MS("gateway-response-time-ms"),
    ENDPOINT_RESPONSE_TIME_MS("endpoint-response-time-ms"),
    REQUEST_CONTENT_LENGTH("request-content-length");

    private static final Map<String, AnalyticsFieldParam> BY_VALUE = Stream
        .of(values())
        .collect(Collectors.toMap(AnalyticsFieldParam::getValue, Function.identity()));

    private final String value;

    AnalyticsFieldParam(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isSupported(String value) {
        return BY_VALUE.containsKey(value);
    }
}
