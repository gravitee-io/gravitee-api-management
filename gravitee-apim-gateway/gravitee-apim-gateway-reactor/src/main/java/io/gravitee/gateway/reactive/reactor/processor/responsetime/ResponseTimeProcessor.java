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
import io.reactivex.rxjava3.core.Completable;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTimeProcessor implements Processor {

    @Override
    public String getId() {
        return "processor-response-time";
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            Metrics metrics = ctx.metrics();
            metrics.setStatus(ctx.response().status());

            final long requestStartNs = metrics.getRequestStartNs();
            if (requestStartNs > 0) {
                // Monotonic clock: measures the elapsed time regardless of any wall-clock adjustment, and at a
                // resolution the gateway's own overhead — a couple of milliseconds — is actually visible at. Setting
                // the nanoseconds derives the milliseconds.
                final long gatewayResponseTimeNs = System.nanoTime() - requestStartNs;
                metrics.setGatewayResponseTimeNs(gatewayResponseTimeNs);
                metrics.setGatewayLatencyNs(gatewayResponseTimeNs - endpointResponseTimeNs(metrics));
            } else {
                // No monotonic origin (a request that did not go through the HTTP layer): fall back to the wall clock.
                final long gatewayResponseTimeInMs = System.currentTimeMillis() - metrics.timestamp().toEpochMilli();
                metrics.setGatewayResponseTimeMs(gatewayResponseTimeInMs);
                if (metrics.getEndpointResponseTimeMs() > -1) {
                    metrics.setGatewayLatencyMs(gatewayResponseTimeInMs - metrics.getEndpointResponseTimeMs());
                }
            }
        });
    }

    /**
     * The endpoint response time in nanoseconds.
     * <p>
     * Not every reactor reports one: the v2 emulation reactor only sets the millisecond field, and it runs behind the
     * very same platform chain as the v4 one, so its value has to be converted rather than ignored — otherwise its
     * latency would come out as the whole response time instead of a difference.
     * <p>
     * A request that never reached an endpoint leaves the millisecond field at its {@code 0} default, which charges
     * the whole time to the gateway, as it always has.
     */
    private static long endpointResponseTimeNs(final Metrics metrics) {
        final long endpointResponseTimeNs = metrics.getEndpointResponseTimeNs();
        return endpointResponseTimeNs > -1 ? endpointResponseTimeNs : TimeUnit.MILLISECONDS.toNanos(metrics.getEndpointResponseTimeMs());
    }
}
