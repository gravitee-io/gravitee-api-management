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
package fixtures.core.model;

import io.gravitee.apim.core.log.model.NativeApiLog;
import io.gravitee.apim.core.log.model.NativeConnectionStatus;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetrics;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public final class NativeApiLogFixtures {

    public static final String API_ID = "api-1";
    public static final String REQUEST_ID = "request-42";
    public static final String TRANSACTION_ID = "txn-1";
    public static final String APPLICATION_ID = "app-1";
    public static final String PLAN_ID = "plan-1";
    public static final String SUBSCRIPTION_ID = "sub-1";
    public static final String CLIENT_IDENTIFIER = "client-fp-A";
    public static final String ENTRYPOINT_ID = "native-kafka";
    public static final String GATEWAY = "gw-1";
    public static final String REMOTE_ADDRESS = "10.0.0.1";
    public static final String LOCAL_ADDRESS = "10.0.0.2";
    public static final String HOST = "broker.example.com";
    public static final String ERROR_KEY = "KAFKA_CONSUMER_LEFT_GROUP";
    public static final String MESSAGE = "Consumer left the group";

    public static final String STATUS_CONNECTED = "CONNECTED";
    public static final String STATUS_CONNECTION_ERROR = "CONNECTION_ERROR";

    public static final String CLIENT_ID = "consumer-app-1-A";
    public static final String BROKER_ID = "broker-1";
    public static final long CONNECTION_DURATION_MS = 1_800_000L;

    public static final String TIMESTAMP_ISO = "2026-01-01T00:00:00.000Z";
    public static final Instant FROM = Instant.parse(TIMESTAMP_ISO);
    public static final Instant TO = FROM.plus(Duration.ofHours(1));
    public static final OffsetDateTime TIMESTAMP_UTC = FROM.atOffset(ZoneOffset.UTC);
    public static final long FROM_MILLIS = FROM.toEpochMilli();
    public static final long TO_MILLIS = TO.toEpochMilli();

    public static final Map<String, Long> CONNECTION_STATUS_COUNTS = Map.of(STATUS_CONNECTED, 7L, STATUS_CONNECTION_ERROR, 3L);

    /** Raw ES {@code additional-metrics} map matching the fields in {@link #buildNativeApiErrorLog} — used by repository / source-mapper tests. */
    public static final Map<String, Object> RAW_ADDITIONAL_METRICS = Map.of(
        NativeApiMetricKeys.CLIENT_ID,
        CLIENT_ID,
        NativeApiMetricKeys.BROKER_ID,
        BROKER_ID,
        NativeApiMetricKeys.CONNECTION_STATUS,
        STATUS_CONNECTION_ERROR,
        NativeApiMetricKeys.CONNECTION_DURATION_MS,
        CONNECTION_DURATION_MS
    );

    private NativeApiLogFixtures() {}

    /** {@link NativeApiLog} populated only with fields visible to {@code API_NATIVE_LOG} (LIST view). */
    public static NativeApiLog buildNativeApiLog(String apiId, String requestId) {
        return NativeApiLog.builder()
            .apiId(apiId)
            .requestId(requestId)
            .transactionId(TRANSACTION_ID)
            .timestamp(TIMESTAMP_ISO)
            .applicationId(APPLICATION_ID)
            .planId(PLAN_ID)
            .clientIdentifier(CLIENT_IDENTIFIER)
            .entrypointId(ENTRYPOINT_ID)
            .connectionStatus(NativeConnectionStatus.CONNECTION_ERROR)
            .connectionDurationMs(CONNECTION_DURATION_MS)
            .build();
    }

    /** {@link NativeApiLog} populated on every field — visible to {@code API_NATIVE_ANALYTICS} (DETAIL view). */
    public static NativeApiLog buildNativeApiErrorLog(String apiId, String requestId) {
        return buildNativeApiLog(apiId, requestId)
            .toBuilder()
            .subscriptionId(SUBSCRIPTION_ID)
            .gateway(GATEWAY)
            .remoteAddress(REMOTE_ADDRESS)
            .localAddress(LOCAL_ADDRESS)
            .host(HOST)
            .errorKey(ERROR_KEY)
            .message(MESSAGE)
            .clientId(CLIENT_ID)
            .brokerId(BROKER_ID)
            .build();
    }

    /** {@link NativeApiMetrics} repository entity matching {@link #buildNativeApiErrorLog} — same shape, same constants. */
    public static NativeApiMetrics buildNativeApiMetrics(String apiId, String requestId) {
        return NativeApiMetrics.builder()
            .apiId(apiId)
            .requestId(requestId)
            .transactionId(TRANSACTION_ID)
            .timestamp(TIMESTAMP_ISO)
            .applicationId(APPLICATION_ID)
            .planId(PLAN_ID)
            .clientIdentifier(CLIENT_IDENTIFIER)
            .subscriptionId(SUBSCRIPTION_ID)
            .entrypointId(ENTRYPOINT_ID)
            .gateway(GATEWAY)
            .remoteAddress(REMOTE_ADDRESS)
            .localAddress(LOCAL_ADDRESS)
            .host(HOST)
            .errorKey(ERROR_KEY)
            .message(MESSAGE)
            .additionalMetrics(RAW_ADDITIONAL_METRICS)
            .build();
    }
}
