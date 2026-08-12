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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;

/**
 * Field resolver for the native Kafka <b>event metrics</b>, stored in the {@code event-metrics} data
 * stream (one document per dimension key, flushed every 5 seconds by the native Kafka reactor).
 *
 * <p>Two specifics compared to the other resolvers:
 *
 * <ol>
 *   <li><b>Documents are typed.</b> A single index holds four document shapes discriminated by
 *       {@code doc-type} ({@code api}, {@code application}, {@code topic}, {@code operation}), each
 *       carrying its own measure fields. Since one analytics query may mix metrics of different
 *       types, the type has to be applied <em>per metric</em> — see {@link #docType(Metric)}, which
 *       the query adapters turn into a {@code filter} sub-aggregation.
 *   <li><b>Dimension field names differ from {@code v4-metrics}.</b> The application id is
 *       {@code app-id} here (it is {@code application-id} in the connection index), so this resolver
 *       cannot delegate to {@code RequestV2MetricsV4Fields}.
 * </ol>
 *
 * <p>Note that {@code doc-type: api} documents carry <b>neither {@code plan-id} nor {@code app-id}</b>:
 * the reactor nulls them for the metrics recorded before a connection is authenticated (active
 * connections, downstream authentication failures). Faceting those two metrics by application or
 * plan therefore yields nothing, which is why the analytics catalog only declares {@code API} for them.
 *
 * @author GraviteeSource Team
 */
public class EventMetricsFieldResolver implements FieldResolver {

    public static final String DOC_TYPE_FIELD = "doc-type";

    public static final String DOC_TYPE_API = "api";
    public static final String DOC_TYPE_APPLICATION = "application";
    public static final String DOC_TYPE_TOPIC = "topic";
    public static final String DOC_TYPE_OPERATION = "operation";

    private static final String API_ID = "api-id";
    private static final String PLAN_ID = "plan-id";
    private static final String APP_ID = "app-id";
    private static final String TOPIC = "topic";
    private static final String OPERATION = "operation";

    /**
     * Elasticsearch field backing each metric.
     *
     * <p>Three families, each with its own reading:
     *
     * <ul>
     *   <li><b>Counters</b> ({@code *-increment}) are reset at every 5s flush, so summing them over a
     *       window gives the exact total for that window — no interpolation involved.
     *   <li><b>Gauges</b> ({@code *-active-connections}) are point-in-time values, hence MAX rather
     *       than SUM: adding two samples of the same connection count would be meaningless.
     *   <li><b>Durations</b> ({@code *-durations-nanos}) are the <em>sum</em> of the durations seen in
     *       the window, not individual samples. See {@link #durationSampleCountField(Metric)}.
     * </ul>
     *
     * <p>The operation family names phases of one Kafka request rather than sides of the proxy path:
     * {@code upstream} is the gateway handling the request, {@code endpoint} the broker round-trip,
     * {@code downstream} the gateway handling the response. The topic family, by contrast, does use
     * downstream/upstream as client side and broker side.
     */
    @Override
    public String fromMetric(Metric metric) {
        return switch (metric) {
            case NATIVE_MESSAGES_PRODUCED_DOWNSTREAM -> "downstream-publish-messages-count-increment";
            case NATIVE_MESSAGES_PRODUCED_UPSTREAM -> "upstream-publish-messages-count-increment";
            case NATIVE_MESSAGES_CONSUMED_DOWNSTREAM -> "downstream-subscribe-messages-count-increment";
            case NATIVE_MESSAGES_CONSUMED_UPSTREAM -> "upstream-subscribe-messages-count-increment";
            case NATIVE_BYTES_PRODUCED_DOWNSTREAM -> "downstream-publish-message-bytes-increment";
            case NATIVE_BYTES_PRODUCED_UPSTREAM -> "upstream-publish-message-bytes-increment";
            case NATIVE_BYTES_CONSUMED_DOWNSTREAM -> "downstream-subscribe-message-bytes-increment";
            case NATIVE_BYTES_CONSUMED_UPSTREAM -> "upstream-subscribe-message-bytes-increment";
            case NATIVE_ACTIVE_CONNECTIONS_DOWNSTREAM -> "downstream-active-connections";
            case NATIVE_ACTIVE_CONNECTIONS_UPSTREAM -> "upstream-active-connections";
            case NATIVE_AUTHENTICATIONS_SUCCESS_DOWNSTREAM -> "downstream-authentication-successes-count-increment";
            case NATIVE_AUTHENTICATIONS_SUCCESS_UPSTREAM -> "upstream-authentication-successes-count-increment";
            case NATIVE_AUTHENTICATIONS_FAILURE_DOWNSTREAM -> "downstream-authentication-failures-count-increment";
            case NATIVE_AUTHENTICATIONS_FAILURE_UPSTREAM -> "upstream-authentication-failures-count-increment";
            case NATIVE_OPERATIONS_RECEIVED -> "upstream-count-increment";
            case NATIVE_OPERATIONS_FORWARDED -> "endpoint-upstream-count-increment";
            case NATIVE_OPERATIONS_ANSWERED -> "endpoint-downstream-count-increment";
            case NATIVE_OPERATIONS_COMPLETED -> "downstream-count-increment";
            case NATIVE_OPERATION_GATEWAY_REQUEST_DURATION -> "upstream-durations-nanos";
            case NATIVE_OPERATION_BROKER_DURATION -> "endpoint-durations-nanos";
            case NATIVE_OPERATION_GATEWAY_RESPONSE_DURATION -> "downstream-durations-nanos";
            default -> throw new UnsupportedOperationException("EventMetricsFieldResolver does not support metric " + metric);
        };
    }

    /**
     * Document type carrying the metric, applied as a {@code term} filter around its aggregations.
     * Without it, faceting a {@code topic} metric would also open buckets for {@code api} documents
     * (which have no {@code topic}) and vice versa.
     */
    public String docType(Metric metric) {
        return switch (metric) {
            case
                NATIVE_MESSAGES_PRODUCED_DOWNSTREAM,
                NATIVE_MESSAGES_PRODUCED_UPSTREAM,
                NATIVE_MESSAGES_CONSUMED_DOWNSTREAM,
                NATIVE_MESSAGES_CONSUMED_UPSTREAM,
                NATIVE_BYTES_PRODUCED_DOWNSTREAM,
                NATIVE_BYTES_PRODUCED_UPSTREAM,
                NATIVE_BYTES_CONSUMED_DOWNSTREAM,
                NATIVE_BYTES_CONSUMED_UPSTREAM -> DOC_TYPE_TOPIC;
            case
                NATIVE_ACTIVE_CONNECTIONS_DOWNSTREAM,
                NATIVE_ACTIVE_CONNECTIONS_UPSTREAM,
                NATIVE_AUTHENTICATIONS_FAILURE_DOWNSTREAM -> DOC_TYPE_API;
            case
                NATIVE_AUTHENTICATIONS_SUCCESS_DOWNSTREAM,
                NATIVE_AUTHENTICATIONS_SUCCESS_UPSTREAM,
                NATIVE_AUTHENTICATIONS_FAILURE_UPSTREAM -> DOC_TYPE_APPLICATION;
            case
                NATIVE_OPERATIONS_RECEIVED,
                NATIVE_OPERATIONS_FORWARDED,
                NATIVE_OPERATIONS_ANSWERED,
                NATIVE_OPERATIONS_COMPLETED,
                NATIVE_OPERATION_GATEWAY_REQUEST_DURATION,
                NATIVE_OPERATION_BROKER_DURATION,
                NATIVE_OPERATION_GATEWAY_RESPONSE_DURATION -> DOC_TYPE_OPERATION;
            default -> throw new UnsupportedOperationException("EventMetricsFieldResolver does not support metric " + metric);
        };
    }

    /**
     * True when the metric is an accumulated duration rather than a plain counter. The reactor sums
     * durations over each 5s window, so the only meaningful average is
     * {@code sum(durations) / sum(samples)} — a percentile would describe the distribution of the
     * per-window sums, not of the operation latencies.
     */
    public boolean isAccumulatedDuration(Metric metric) {
        return switch (metric) {
            case
                NATIVE_OPERATION_GATEWAY_REQUEST_DURATION,
                NATIVE_OPERATION_BROKER_DURATION,
                NATIVE_OPERATION_GATEWAY_RESPONSE_DURATION -> true;
            default -> false;
        };
    }

    /**
     * Counter incremented at the very point where the matching duration is sampled, hence the correct
     * denominator of the derived average. The three durations do <b>not</b> share a denominator:
     * the gateway-request duration is recorded when the request is forwarded to the broker, the
     * broker duration when its response comes back, and the gateway-response duration when the
     * response is handed to the client.
     */
    public String durationSampleCountField(Metric metric) {
        return switch (metric) {
            case NATIVE_OPERATION_GATEWAY_REQUEST_DURATION -> "endpoint-upstream-count-increment";
            case NATIVE_OPERATION_BROKER_DURATION -> "endpoint-downstream-count-increment";
            case NATIVE_OPERATION_GATEWAY_RESPONSE_DURATION -> "downstream-count-increment";
            default -> throw new UnsupportedOperationException("Metric " + metric + " is not an accumulated duration");
        };
    }

    /**
     * Filters and facets name the same five dimensions and resolve to the same fields, so both go
     * through here. They arrive as two unrelated enums ({@code Filter.Name} and {@code Facet}), which
     * Java cannot switch over jointly — matching on the name is what lets the mapping live in one
     * place instead of drifting between two identical switches. Nothing is lost by it: the switch
     * already had a {@code default} branch, so it never had exhaustiveness checking to begin with.
     *
     * @param kind "filter" or "facet", so the failure says which side of the query is at fault
     */
    private String fromDimension(String dimension, String kind) {
        return switch (dimension) {
            case "API" -> API_ID;
            case "APPLICATION" -> APP_ID;
            case "PLAN" -> PLAN_ID;
            case "NATIVE_TOPIC" -> TOPIC;
            case "NATIVE_OPERATION" -> OPERATION;
            default -> throw new UnsupportedOperationException(
                "EventMetricsFieldResolver does not support " +
                    kind +
                    " '" +
                    dimension +
                    "' — supported dimensions: API, APPLICATION, PLAN, NATIVE_TOPIC, NATIVE_OPERATION"
            );
        };
    }

    @Override
    public String fromFilter(Filter filter) {
        return fromDimension(filter.name().name(), "filter");
    }

    @Override
    public String fromFacet(Facet facet) {
        return fromDimension(facet.name(), "facet");
    }
}
