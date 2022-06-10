/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.reactor.processor.transaction;

import static io.gravitee.gateway.jupiter.reactor.processor.transaction.TraceContextProcessor.HEADER_TRACE_PARENT;
import static io.gravitee.gateway.jupiter.reactor.processor.transaction.TraceContextProcessor.HEADER_TRACE_STATE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import io.gravitee.gateway.jupiter.reactor.processor.AbstractProcessorTest;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TraceContextProcessorTest extends AbstractProcessorTest {

    public static final Pattern TRACEPARENT_REGEX = Pattern.compile(
        "(([0-9a-e][0-9a-f])|([0-9a-f][0-9a-e]))-([0-9a-f]{32})-([0-9a-f]{16})-([0-9a-f]{2})"
    );
    private TraceContextProcessor traceContextProcessor;

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("00-00000000000000000000000000000000-00f067aa0ba902b7-01"),
            Arguments.of("ff-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"),
            Arguments.of("00-00000000000000000000000000000000-00f067aa0ba902b7-01"),
            Arguments.of("00-00f067aa0ba902b7-00f067aa0ba902b7-01"),
            Arguments.of("00-4bf92f3577b34da6a3ce929d0e0e4736-0000000000000000-01")
        );
    }

    @BeforeEach
    public void setUp() {
        traceContextProcessor = new TraceContextProcessor();
    }

    @Test
    public void shouldHaveTraceparent() {
        final String tracestate = "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7";
        spyResponseHeaders.set(TraceContextProcessor.HEADER_TRACE_STATE, tracestate);
        traceContextProcessor.execute(ctx).test().assertResult();

        assertThat(TRACEPARENT_REGEX.matcher(spyRequestHeaders.get(HEADER_TRACE_PARENT)).matches()).isTrue();

        // if TRACESTATE provided witout traceparent, tracestate is removed
        assertThat(spyRequestHeaders.get(HEADER_TRACE_STATE)).isNull();
        assertThat(TRACEPARENT_REGEX.matcher(spyResponseHeaders.get(HEADER_TRACE_PARENT)).matches()).isTrue();
    }

    @Test
    public void shouldPropagateSameTraceparent() {
        final String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        final String tracestate = "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7";
        spyRequestHeaders.set(HEADER_TRACE_STATE, tracestate);
        spyRequestHeaders.set(HEADER_TRACE_PARENT, traceparent);
        traceContextProcessor.execute(ctx).test().assertResult();

        assertThat(spyRequestHeaders.get(HEADER_TRACE_PARENT)).isEqualTo(traceparent);
        assertThat(spyRequestHeaders.get(HEADER_TRACE_STATE)).isEqualTo(tracestate);
        assertThat(spyResponseHeaders.get(HEADER_TRACE_PARENT)).isEqualTo(traceparent);
    }

    @ParameterizedTest(name = "traceparent {0}; tracestate {1}")
    @MethodSource("provideParameters")
    public void shouldOverrideRemoteAddressWithForwardedForHeader(final String traceparent) {
        final String tracestate = "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7";
        spyRequestHeaders.set(HEADER_TRACE_STATE, tracestate);
        spyRequestHeaders.set(HEADER_TRACE_PARENT, traceparent);

        traceContextProcessor.execute(ctx).test().assertResult();

        assertThat(spyRequestHeaders.get(HEADER_TRACE_PARENT)).isNotEqualTo(traceparent);
        assertThat(spyRequestHeaders.get(HEADER_TRACE_STATE)).isNull();
        assertThat(spyResponseHeaders.get(HEADER_TRACE_PARENT)).isNotEqualTo(traceparent);
    }
}
