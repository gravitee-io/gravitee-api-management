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
package io.gravitee.apim.core.log.model;

import java.util.Optional;

/**
 * Connection lifecycle statuses emitted by the native-Kafka reactor for a single API connection. Stored in the ES
 * {@code keyword_native-kafka_connection-status} additional-metric and projected into {@link NativeApiLog#getConnectionStatus()}.
 */
public enum NativeConnectionStatus {
    CONNECTED,
    CONNECTION_ERROR,
    SESSION_ERROR,
    INTERNAL_ERROR;

    /**
     * Lenient parse from a connectionStatus ES string. Returns {@link Optional#empty()} for {@code null} or unknown values so
     * presentation paths do not fail when the reactor evolves to emit a new status before the management API does.
     * Query-time filter validation is strict (handled by JAX-RS via {@link #valueOf(String)}); this method is intended
     * for the presentation/projection boundary only.
     */
    public static Optional<NativeConnectionStatus> fromString(String connectionStatus) {
        if (connectionStatus == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(connectionStatus));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
