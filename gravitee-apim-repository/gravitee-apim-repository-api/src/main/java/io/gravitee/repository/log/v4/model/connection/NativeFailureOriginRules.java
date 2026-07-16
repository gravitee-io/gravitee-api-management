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
package io.gravitee.repository.log.v4.model.connection;

import java.util.Set;

/**
 * Single home of the native Kafka error-key classification rules, localizing which side of the
 * gateway proxying path a failure sits on. Error keys are the Kafka protocol error names
 * ({@code org.apache.kafka.common.protocol.Errors}) reported by the native gateway; the connection
 * status is the {@link NativeApiMetricKeys#CONNECTION_STATUS} additional metric.
 *
 * <p>Consumed by the gamma observability logs backend to derive the {@code failureOrigin} response
 * field, and by the Elasticsearch metrics query adapter to translate the {@code FAILURE_ORIGIN}
 * filter — both must stay in lockstep, which is why the vocabulary lives here.
 *
 * <p>Classification order (first match wins): client-side keys → broker-side keys/prefixes →
 * {@code UNKNOWN_SERVER_ERROR} or {@code INTERNAL_ERROR} status → fallback on the failure phase
 * ({@code CONNECTION_ERROR} status → client side, anything else → broker side). A log without an
 * error key is a failure only when the status is {@code INTERNAL_ERROR}.
 */
public final class NativeFailureOriginRules {

    /** Authentication / SASL handshake failures happen between the client and the gateway. */
    public static final Set<String> CLIENT_SIDE_ERROR_KEYS = Set.of(
        "SASL_AUTHENTICATION_FAILED",
        "ILLEGAL_SASL_STATE",
        "UNSUPPORTED_SASL_MECHANISM",
        "SECURITY_DISABLED"
    );

    /**
     * Broker-domain failures: availability, topics/partitions, consumer groups, coordination.
     * Genuinely side-ambiguous keys (NETWORK_EXCEPTION, REQUEST_TIMED_OUT) are deliberately NOT
     * listed — they classify as undetermined unless the document carries an explicit
     * {@link NativeApiMetricKeys#FAILURE_SIDE}.
     */
    public static final Set<String> BROKER_SIDE_ERROR_KEYS = Set.of(
        "UNKNOWN_TOPIC_OR_PARTITION",
        "REBALANCE_IN_PROGRESS",
        "ILLEGAL_GENERATION",
        "MEMBER_ID_REQUIRED",
        "INVALID_TXN_STATE",
        "NOT_COORDINATOR",
        "NOT_LEADER_OR_FOLLOWER",
        "LEADER_NOT_AVAILABLE"
    );

    public static final Set<String> BROKER_SIDE_ERROR_KEY_PREFIXES = Set.of(
        "BROKER_",
        "COORDINATOR_",
        "TOPIC_",
        "GROUP_",
        "REBALANCE_",
        "OFFSET_"
    );

    /** Kafka's catch-all error, reported by the gateway for unclassified internal throwables. */
    public static final String UNKNOWN_SERVER_ERROR_KEY = "UNKNOWN_SERVER_ERROR";

    public static final String INTERNAL_ERROR_STATUS = "INTERNAL_ERROR";
    public static final String CONNECTION_ERROR_STATUS = "CONNECTION_ERROR";

    /** {@link NativeApiMetricKeys#FAILURE_SIDE} values written by the gateway. */
    public static final String FAILURE_SIDE_DOWNSTREAM = "DOWNSTREAM";
    public static final String FAILURE_SIDE_UPSTREAM = "UPSTREAM";
    public static final String FAILURE_SIDE_INTERNAL = "INTERNAL";

    private NativeFailureOriginRules() {}
}
