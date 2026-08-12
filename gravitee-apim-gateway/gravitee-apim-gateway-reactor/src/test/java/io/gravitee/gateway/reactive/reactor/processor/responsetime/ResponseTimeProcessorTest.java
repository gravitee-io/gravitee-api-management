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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.reactor.processor.AbstractProcessorTest;
import io.gravitee.reporter.api.v4.metric.Metrics;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ResponseTimeProcessorTest extends AbstractProcessorTest {

    private static final long ELAPSED_NS = MILLISECONDS.toNanos(50);
    private static final long ENDPOINT_RESPONSE_TIME_NS = MILLISECONDS.toNanos(30);

    @Test
    void should_add_response_time_to_metric() {
        ResponseTimeProcessor responseTimeProcessor = new ResponseTimeProcessor();
        ctx.metrics().setEndpointResponseTimeMs(100);
        responseTimeProcessor.execute(ctx).test().assertResult();
        assertThat(ctx.metrics().getGatewayResponseTimeMs()).isLessThanOrEqualTo(
            System.currentTimeMillis() - ctx.metrics().getEndpointResponseTimeMs()
        );
        assertThat(ctx.metrics().getGatewayLatencyMs()).isEqualTo(
            ctx.metrics().getGatewayResponseTimeMs() - ctx.metrics().getEndpointResponseTimeMs()
        );
    }

    @Test
    void should_measure_in_nanoseconds_and_derive_the_milliseconds() {
        final Metrics metrics = ctx.metrics();
        metrics.setRequestStartNs(System.nanoTime() - ELAPSED_NS);
        metrics.setEndpointResponseTimeNs(ENDPOINT_RESPONSE_TIME_NS);

        new ResponseTimeProcessor().execute(ctx).test().assertResult();

        assertThat(metrics.getGatewayResponseTimeNs()).isGreaterThanOrEqualTo(ELAPSED_NS);
        assertThat(metrics.getGatewayLatencyNs()).isEqualTo(metrics.getGatewayResponseTimeNs() - ENDPOINT_RESPONSE_TIME_NS);
        // Both sets of fields describe the same measure, the milliseconds being the nanoseconds rounded to the nearest.
        assertThat(metrics.getGatewayResponseTimeMs()).isEqualTo(Math.round(metrics.getGatewayResponseTimeNs() / 1e6));
        assertThat(metrics.getGatewayLatencyMs()).isEqualTo(Math.round(metrics.getGatewayLatencyNs() / 1e6));
    }

    @Test
    void should_round_a_sub_millisecond_duration_rather_than_floor_it() {
        final Metrics metrics = ctx.metrics();
        metrics.setRequestStartNs(System.nanoTime());
        metrics.setEndpointResponseTimeNs(0);

        new ResponseTimeProcessor().execute(ctx).test().assertResult();

        // A gateway overhead is typically a fraction of a millisecond. Truncating would report a flat 0 ms and bias
        // every dashboard average down; rounding keeps what a difference of currentTimeMillis() used to yield.
        assertThat(metrics.getGatewayLatencyNs()).isPositive();
        assertThat(metrics.getGatewayLatencyMs()).isEqualTo(metrics.getGatewayLatencyNs() >= 500_000 ? 1 : 0);
    }

    @Test
    void should_keep_the_latency_a_difference_when_the_reactor_only_reports_milliseconds() {
        final Metrics metrics = ctx.metrics();
        metrics.setRequestStartNs(System.nanoTime() - ELAPSED_NS);
        // What the v2 emulation reactor does: it writes the millisecond field and never the nanosecond one, while
        // running behind the same platform chain, which does set the monotonic origin.
        metrics.setEndpointResponseTimeMs(MILLISECONDS.convert(ENDPOINT_RESPONSE_TIME_NS, NANOSECONDS));

        new ResponseTimeProcessor().execute(ctx).test().assertResult();

        // Not the whole response time: the endpoint's share still has to be taken out.
        assertThat(metrics.getGatewayLatencyNs()).isLessThan(metrics.getGatewayResponseTimeNs());
        assertThat(metrics.getGatewayLatencyNs()).isEqualTo(metrics.getGatewayResponseTimeNs() - ENDPOINT_RESPONSE_TIME_NS);
    }

    @Test
    void should_charge_the_whole_time_to_the_gateway_when_no_endpoint_was_reached() {
        final Metrics metrics = ctx.metrics();
        metrics.setRequestStartNs(System.nanoTime() - ELAPSED_NS);

        // A request rejected before reaching an endpoint (a policy denying it, say) leaves no endpoint response time.
        new ResponseTimeProcessor().execute(ctx).test().assertResult();

        assertThat(metrics.getGatewayLatencyNs()).isEqualTo(metrics.getGatewayResponseTimeNs());
    }
}
