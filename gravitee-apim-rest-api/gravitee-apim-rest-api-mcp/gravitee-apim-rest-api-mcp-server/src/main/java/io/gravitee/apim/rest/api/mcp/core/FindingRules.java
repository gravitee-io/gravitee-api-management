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
package io.gravitee.apim.rest.api.mcp.core;

import io.gravitee.apim.rest.api.mcp.core.model.Finding;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Decides what counts as a change worth reporting between a time range and its baseline.
 */
public final class FindingRules {

    /** Below this volume a rate is noise. */
    static final long MIN_REQUESTS = 20;
    static final double SERVER_ERROR_RATE_PCT = 1;
    static final double SERVER_ERROR_RATE_INCREASE_PCT = 1;
    static final double CLIENT_ERROR_RATE_PCT = 5;
    static final double CLIENT_ERROR_RATE_INCREASE_PCT = 5;
    static final double LATENCY_FACTOR = 1.5;
    static final double LATENCY_INCREASE_MS = 100;
    static final double TRAFFIC_DROP_FACTOR = 0.5;

    private FindingRules() {}

    public static List<Finding.Kind> evaluate(TrafficStats current, TrafficStats baseline) {
        var kinds = new ArrayList<Finding.Kind>();
        if (current.requests() >= MIN_REQUESTS) {
            if (
                current.serverErrorRatePct() >= SERVER_ERROR_RATE_PCT &&
                current.serverErrorRatePct() - baseline.serverErrorRatePct() >= SERVER_ERROR_RATE_INCREASE_PCT
            ) {
                kinds.add(Finding.Kind.SERVER_ERROR_RATE_UP);
            }
            if (
                current.clientErrorRatePct() >= CLIENT_ERROR_RATE_PCT &&
                current.clientErrorRatePct() - baseline.clientErrorRatePct() >= CLIENT_ERROR_RATE_INCREASE_PCT
            ) {
                kinds.add(Finding.Kind.CLIENT_ERROR_RATE_UP);
            }
            if (isSlower(current.responseTimeP95Ms(), baseline.responseTimeP95Ms())) {
                kinds.add(Finding.Kind.LATENCY_UP);
            }
        }
        if (baseline.requests() >= MIN_REQUESTS && current.requests() <= baseline.requests() * TRAFFIC_DROP_FACTOR) {
            kinds.add(Finding.Kind.TRAFFIC_DROP);
        }
        return kinds;
    }

    public static String describe(Finding.Kind kind, TrafficStats current, TrafficStats baseline) {
        return switch (kind) {
            case SERVER_ERROR_RATE_UP -> String.format(
                Locale.ROOT,
                "5xx rate %.2f%% of %d requests, up from %.2f%%",
                current.serverErrorRatePct(),
                current.requests(),
                baseline.serverErrorRatePct()
            );
            case CLIENT_ERROR_RATE_UP -> String.format(
                Locale.ROOT,
                "4xx rate %.2f%% of %d requests, up from %.2f%%",
                current.clientErrorRatePct(),
                current.requests(),
                baseline.clientErrorRatePct()
            );
            case LATENCY_UP -> String.format(
                Locale.ROOT,
                "P95 response time %.0f ms, up from %.0f ms",
                current.responseTimeP95Ms(),
                baseline.responseTimeP95Ms()
            );
            case TRAFFIC_DROP -> String.format(Locale.ROOT, "%d requests, down from %d", current.requests(), baseline.requests());
        };
    }

    private static boolean isSlower(Double current, Double baseline) {
        return (current != null && baseline != null && current >= baseline * LATENCY_FACTOR && current - baseline >= LATENCY_INCREASE_MS);
    }
}
