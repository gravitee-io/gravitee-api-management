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
package io.gravitee.gateway.reactive.reactor.processor.responsetime;

import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.reactivex.rxjava3.core.Completable;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTimeProcessor implements Processor {

    private final Timer gatewayResponseTimer;

    /**
     * Creates a ResponseTimeProcessor without Micrometer percentile metrics.
     */
    public ResponseTimeProcessor() {
        this(null);
    }

    /**
     * Creates a ResponseTimeProcessor that records gateway response times to Micrometer
     * with P50, P75, P95, and P99 percentile distribution for Prometheus exposition.
     *
     * @param meterRegistry the Micrometer MeterRegistry to register the timer with, or null to disable
     */
    public ResponseTimeProcessor(MeterRegistry meterRegistry) {
        if (meterRegistry != null) {
            this.gatewayResponseTimer = Timer.builder("gateway_response_time")
                .description("Gateway response time with percentile distribution (P95/P99)")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(meterRegistry);
        } else {
            this.gatewayResponseTimer = null;
        }
    }

    @Override
    public String getId() {
        return "processor-response-time";
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            Metrics metrics = ctx.metrics();
            // Compute response-time and add it to the metrics
            long gatewayResponseTimeInMs = System.currentTimeMillis() - metrics.timestamp().toEpochMilli();
            metrics.setStatus(ctx.response().status());
            metrics.setGatewayResponseTimeMs(gatewayResponseTimeInMs);
            if (metrics.getEndpointResponseTimeMs() > -1) {
                metrics.setGatewayLatencyMs(gatewayResponseTimeInMs - metrics.getEndpointResponseTimeMs());
            }

            // Record to Micrometer timer for Prometheus percentile metrics (P95/P99)
            if (gatewayResponseTimer != null) {
                gatewayResponseTimer.record(gatewayResponseTimeInMs, TimeUnit.MILLISECONDS);
            }
        });
    }
}
