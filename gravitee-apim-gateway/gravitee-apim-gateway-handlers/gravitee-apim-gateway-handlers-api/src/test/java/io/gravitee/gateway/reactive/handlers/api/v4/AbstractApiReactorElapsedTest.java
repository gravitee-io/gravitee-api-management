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
package io.gravitee.gateway.reactive.handlers.api.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The request-timeout budget is what this measures, and the only way to tell the two clocks apart is to make them
 * disagree: a request that arrived long ago on the monotonic clock while its wall-clock timestamp says "now".
 * End-to-end tests stay green either way, because in them both clocks agree.
 */
@ExtendWith(MockitoExtension.class)
class AbstractApiReactorElapsedTest {

    @Mock
    MutableExecutionContext ctx;

    @Mock
    MutableRequest request;

    @Test
    void should_measure_elapsed_time_from_the_monotonic_origin() {
        when(ctx.request()).thenReturn(request);
        when(request.timestampNs()).thenReturn(System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(180));
        // Reading this one instead would report roughly zero, and hand the request its full budget a second time.
        lenient().when(request.timestamp()).thenReturn(System.currentTimeMillis());

        assertThat(AbstractApiReactor.elapsedMillis(ctx)).isGreaterThanOrEqualTo(180).isLessThan(1_000);
    }

    @Test
    void should_fall_back_to_the_wall_clock_without_a_monotonic_origin() {
        when(ctx.request()).thenReturn(request);
        // A request that never went through the HTTP layer carries no monotonic reading.
        when(request.timestampNs()).thenReturn(-1L);
        when(request.timestamp()).thenReturn(System.currentTimeMillis() - 180);

        assertThat(AbstractApiReactor.elapsedMillis(ctx)).isGreaterThanOrEqualTo(180).isLessThan(1_000);
    }
}
