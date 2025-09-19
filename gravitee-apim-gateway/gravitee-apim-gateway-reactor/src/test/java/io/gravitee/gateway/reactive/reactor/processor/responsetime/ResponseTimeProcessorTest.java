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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.reactor.processor.AbstractProcessorTest;
import io.gravitee.reporter.api.v4.metric.Metrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ResponseTimeProcessorTest extends AbstractProcessorTest {

    @Test
    void shouldAddResponseTimeToMetric() {
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
}
