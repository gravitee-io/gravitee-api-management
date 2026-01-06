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

package io.gravitee.apim.reporter.common.formatter.util;

import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.v4.common.Message;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ReportableSanitizationUtil {

    private ReportableSanitizationUtil() {
        // util class
    }

    public static void removeCustomMetricsWithNullValues(Metrics metrics) {
        removeNullsFromMap(metrics, Metrics::getCustomMetrics, Metrics::setCustomMetrics);
    }

    public static void removeCustomMetricsWithNullValues(io.gravitee.reporter.api.v4.metric.Metrics metrics) {
        removeNullsFromMap(
            metrics,
            io.gravitee.reporter.api.v4.metric.Metrics::getCustomMetrics,
            io.gravitee.reporter.api.v4.metric.Metrics::setCustomMetrics
        );
    }

    public static void removeCustomMetricsWithNullValues(MessageMetrics metrics) {
        removeNullsFromMap(metrics, MessageMetrics::getCustomMetrics, MessageMetrics::setCustomMetrics);
    }

    public static void removeMessageMetadataWithNullValues(Message message) {
        removeNullsFromMap(message, Message::getMetadata, Message::setMetadata);
    }

    private static <M, T> void removeNullsFromMap(M target, Function<M, Map<String, T>> getter, BiConsumer<M, Map<String, T>> setter) {
        if (target == null) {
            return;
        }

        final Map<String, T> map = getter.apply(target);
        if (map == null || map.isEmpty()) {
            return;
        }

        // Check for nulls without triggering NPE on maps that don't support nulls (like ConcurrentHashMap)
        boolean hasNulls = false;
        for (T value : map.values()) {
            if (value == null) {
                hasNulls = true;
                break;
            }
        }

        // Only allocate a new map if we actually found nulls
        if (hasNulls) {
            setter.accept(target, removeNullValueEntries(map));
        }
    }

    private static <T> Map<String, T> removeNullValueEntries(Map<String, T> map) {
        // Use a simple loop instead of Stream to reduce overhead and memory allocation
        Map<String, T> cleanMap = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, T> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                cleanMap.put(entry.getKey(), entry.getValue());
            }
        }
        return cleanMap;
    }
}
