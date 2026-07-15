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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.gamma.rest.core.observability.logs.model.FailureOrigin;
import io.gravitee.repository.log.v4.model.connection.NativeFailureOriginRules;
import java.util.Locale;

/**
 * Derives the {@link FailureOrigin} of a native Kafka log entry from its error key and connection
 * status. The classification vocabulary (key sets, prefixes, decision order) is shared with the
 * Elasticsearch {@code FAILURE_ORIGIN} filter translation through
 * {@link NativeFailureOriginRules} — keep both renditions in lockstep.
 *
 * @author GraviteeSource Team
 */
public final class FailureOriginClassifier {

    private FailureOriginClassifier() {}

    /**
     * Localizes a failure from the log's error key, refined by the reported connection status.
     * A log with no error key is a failure only when the gateway flagged the connection
     * {@code INTERNAL_ERROR}; otherwise it is {@link FailureOrigin#NONE}.
     */
    public static FailureOrigin classify(String errorKey, String connectionStatus) {
        boolean internalStatus = NativeFailureOriginRules.INTERNAL_ERROR_STATUS.equals(connectionStatus);

        if (errorKey == null || errorKey.isBlank()) {
            return internalStatus ? FailureOrigin.GATEWAY_INTERNAL : FailureOrigin.NONE;
        }

        var key = errorKey.trim().toUpperCase(Locale.ROOT);

        if (NativeFailureOriginRules.CLIENT_SIDE_ERROR_KEYS.contains(key)) {
            return FailureOrigin.CLIENT_TO_GATEWAY;
        }
        if (
            NativeFailureOriginRules.BROKER_SIDE_ERROR_KEYS.contains(key) ||
            NativeFailureOriginRules.BROKER_SIDE_ERROR_KEY_PREFIXES.stream().anyMatch(key::startsWith)
        ) {
            return FailureOrigin.GATEWAY_TO_BROKER;
        }
        if (NativeFailureOriginRules.UNKNOWN_SERVER_ERROR_KEY.equals(key) || internalStatus) {
            return FailureOrigin.GATEWAY_INTERNAL;
        }
        // Unclassified key: fall back on the phase the failure was reported in — connection
        // establishment sits on the client side, an established session interacts with the broker.
        if (NativeFailureOriginRules.CONNECTION_ERROR_STATUS.equals(connectionStatus)) {
            return FailureOrigin.CLIENT_TO_GATEWAY;
        }
        return FailureOrigin.GATEWAY_TO_BROKER;
    }
}
