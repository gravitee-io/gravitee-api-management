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
package io.gravitee.apim.rest.api.mcp.core.model;

/**
 * Traffic over one time range. Rates are percentages of all requests; response times are 95th percentiles in milliseconds.
 *
 * @param responseTimeP95Ms time to answer the consumer, backend included; null without traffic
 * @param endpointResponseTimeP95Ms time spent waiting for the backend; null without traffic
 */
public record TrafficStats(
    long requests,
    long clientErrors,
    long serverErrors,
    double clientErrorRatePct,
    double serverErrorRatePct,
    Double responseTimeP95Ms,
    Double endpointResponseTimeP95Ms
) {
    public static final TrafficStats NONE = of(0, 0, 0, null, null);

    public static TrafficStats of(
        long requests,
        long clientErrors,
        long serverErrors,
        Double responseTimeP95Ms,
        Double endpointResponseTimeP95Ms
    ) {
        return new TrafficStats(
            requests,
            clientErrors,
            serverErrors,
            rate(clientErrors, requests),
            rate(serverErrors, requests),
            round(responseTimeP95Ms),
            round(endpointResponseTimeP95Ms)
        );
    }

    private static double rate(long part, long total) {
        return total == 0 ? 0 : Math.round((part * 10_000.0) / total) / 100.0;
    }

    private static Double round(Double value) {
        return value == null ? null : Math.round(value * 10) / 10.0;
    }
}
