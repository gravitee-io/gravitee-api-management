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
package io.gravitee.apim.reporter.common.formatter.elasticsearch;

import io.gravitee.reporter.api.v4.log.Log;
import io.gravitee.reporter.api.v4.metric.Metrics;

/**
 * A message API reports a connection twice: when it opens and when it ends.
 *
 * <p>Written under the request id, the end would overwrite the opening, which an append-only index (a data stream)
 * refuses. So the opening is written as a document of its own, and both are flagged so that readers can tell them
 * apart.
 *
 * <p>A {@link Log} has no API type: an opening is recognised by {@code requestEnded} alone, which only the message
 * API opening leaves unset — the platform reporter sets it before reporting. For the same reason, the end of a
 * {@link Log} cannot be told from a proxy request and is not flagged.
 */
public final class ConnectionPhase {

    public static final String OPEN = "open";
    public static final String END = "end";

    private static final String MESSAGE_API_TYPE = "message";
    private static final String OPENING_ID_SUFFIX = ":" + OPEN;

    private ConnectionPhase() {}

    public static boolean isOpening(Metrics metrics) {
        return isMessageApi(metrics) && !metrics.isRequestEnded();
    }

    public static boolean isEnd(Metrics metrics) {
        return isMessageApi(metrics) && metrics.isRequestEnded();
    }

    public static boolean isOpening(Log log) {
        return !log.isRequestEnded();
    }

    public static String openingDocumentId(String requestId) {
        return requestId + OPENING_ID_SUFFIX;
    }

    private static boolean isMessageApi(Metrics metrics) {
        return MESSAGE_API_TYPE.equals(metrics.getApiType());
    }
}
