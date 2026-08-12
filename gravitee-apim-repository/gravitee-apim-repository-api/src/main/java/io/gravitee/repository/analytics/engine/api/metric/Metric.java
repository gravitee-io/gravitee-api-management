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
package io.gravitee.repository.analytics.engine.api.metric;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum Metric {
    HTTP_REQUESTS,
    HTTP_ERRORS,
    HTTP_ERROR_RATE,
    HTTP_REQUEST_CONTENT_LENGTH,
    HTTP_RESPONSE_CONTENT_LENGTH,
    HTTP_ENDPOINT_RESPONSE_TIME,
    HTTP_GATEWAY_RESPONSE_TIME,
    HTTP_GATEWAY_LATENCY,
    LLM_PROMPT_TOKEN_SENT,
    LLM_PROMPT_TOKEN_RECEIVED,
    LLM_PROMPT_TOKEN_SENT_COST,
    LLM_PROMPT_TOKEN_RECEIVED_COST,
    LLM_PROMPT_TOTAL_TOKEN,
    LLM_PROMPT_TOKEN_TOTAL_COST,
    MESSAGE_PAYLOAD_SIZE,
    MESSAGES,
    MESSAGE_ERRORS,
    MESSAGE_GATEWAY_LATENCY,
    SUBSCRIPTIONS,
    APIS,
    EDGE_DETECTION_COUNT,
    EDGE_TOKENS_IN,
    EDGE_TOKENS_OUT,
    EDGE_HEARTBEAT_COUNT,
    NATIVE_CONNECTIONS_SUMMARY,

    // Native Kafka event metrics — served from the `event-metrics` data stream, where the native
    // reactor flushes accumulated counters every 5s. One metric per Elasticsearch field: the
    // downstream/upstream split is materialized as distinct fields, not as a dimension, so it
    // cannot be a facet (same modelling as LLM_PROMPT_TOKEN_SENT / _RECEIVED).
    NATIVE_MESSAGES_PRODUCED_DOWNSTREAM,
    NATIVE_MESSAGES_PRODUCED_UPSTREAM,
    NATIVE_MESSAGES_CONSUMED_DOWNSTREAM,
    NATIVE_MESSAGES_CONSUMED_UPSTREAM,
    NATIVE_BYTES_PRODUCED_DOWNSTREAM,
    NATIVE_BYTES_PRODUCED_UPSTREAM,
    NATIVE_BYTES_CONSUMED_DOWNSTREAM,
    NATIVE_BYTES_CONSUMED_UPSTREAM,
    NATIVE_ACTIVE_CONNECTIONS_DOWNSTREAM,
    NATIVE_ACTIVE_CONNECTIONS_UPSTREAM,
    NATIVE_AUTHENTICATIONS_SUCCESS_DOWNSTREAM,
    NATIVE_AUTHENTICATIONS_SUCCESS_UPSTREAM,
    NATIVE_AUTHENTICATIONS_FAILURE_DOWNSTREAM,
    NATIVE_AUTHENTICATIONS_FAILURE_UPSTREAM,
    NATIVE_OPERATIONS_RECEIVED,
    NATIVE_OPERATIONS_FORWARDED,
    NATIVE_OPERATIONS_ANSWERED,
    NATIVE_OPERATIONS_COMPLETED,
    NATIVE_OPERATION_GATEWAY_REQUEST_DURATION,
    NATIVE_OPERATION_BROKER_DURATION,
    NATIVE_OPERATION_GATEWAY_RESPONSE_DURATION,
}
