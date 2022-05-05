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
package io.gravitee.gateway.reactive.reactor.processor.forward;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.reactor.processor.AbstractProcessorTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class XForwardForProcessorTest extends AbstractProcessorTest {

    private XForwardForProcessor xForwardForProcessor;

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("197.225.30.74", "197.225.30.74"),
            Arguments.of("197.225.30.74:5000", "197.225.30.74"),
            Arguments.of("197.225.30.74, 10.0.0.1, 10.0.0.2", "197.225.30.74"),
            Arguments.of("197.225.30.74:5000, 10.0.0.1:5000, 10.0.0.2:5000", "197.225.30.74"),
            Arguments.of("2001:0db8:85a3:0000:0000:8a2e:0370:7334", "2001:0db8:85a3:0000:0000:8a2e:0370:7334"),
            Arguments.of("2001:db8:85a3:0:0:8a2e:370:7334", "2001:db8:85a3:0:0:8a2e:370:7334"),
            Arguments.of("2001:db8:85a3::8a2e:370:7334", "2001:db8:85a3::8a2e:370:7334"),
            Arguments.of("197.225.30.74:5000, 2001:0db8:85a3:0000:0000:8a2e:0370:7334", "197.225.30.74"),
            Arguments.of("2001:0db8:85a3:0000:0000:8a2e:0370:7334, 197.225.30.74:5000", "2001:0db8:85a3:0000:0000:8a2e:0370:7334")
        );
    }

    @BeforeEach
    public void setUp() {
        xForwardForProcessor = new XForwardForProcessor();
    }

    @Test
    public void shouldNotChangeRemoteAddressWithXForwardedForHeader() {
        xForwardForProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).headers();
        verifyNoMoreInteractions(mockRequest);
        assertThat(metrics.getRemoteAddress()).isNull();
    }

    @ParameterizedTest(name = "header {0}; remote address {1}")
    @MethodSource("provideParameters")
    public void shouldOverrideRemoteAddressWithForwardedForHeader(String header, String remoteAddress) {
        spyRequestHeaders.set(HttpHeaderNames.X_FORWARDED_FOR, header);
        xForwardForProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).remoteAddress(eq(remoteAddress));
        assertThat(metrics.getRemoteAddress()).isEqualTo(remoteAddress);
    }
}
