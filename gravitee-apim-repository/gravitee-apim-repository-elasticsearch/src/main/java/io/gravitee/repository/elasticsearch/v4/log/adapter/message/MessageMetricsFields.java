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
package io.gravitee.repository.elasticsearch.v4.log.adapter.message;

import lombok.NoArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class MessageMetricsFields {

    public static final String TIMESTAMP = "@timestamp";
    public static final String API_ID = "api-id";
    public static final String API_NAME = "api-name";
    public static final String REQUEST_ID = "request-id";
    public static final String CLIENT_IDENTIFIER = "client-identifier";
    public static final String CORRELATION_ID = "correlation-id";
    public static final String OPERATION = "operation";
    public static final String CONNECTOR_TYPE = "connector-type";
    public static final String CONNECTOR_ID = "connector-id";
    public static final String GATEWAY = "gateway";
    public static final String GATEWAY_LATENCY_MS = "gateway-latency-ms";
    public static final String CONTENT_LENGTH = "content-length";
    public static final String ERROR = "error";
    public static final String COUNT = "count";
    public static final String COUNT_INCREMENT = "count-increment";
    public static final String ERROR_COUNT = "error-count";
    public static final String ERROR_COUNT_INCREMENT = "error-count-increment";
    public static final String CUSTOM = "custom";
    public static final String ADDITIONAL_METRICS = "additional-metrics";
}
