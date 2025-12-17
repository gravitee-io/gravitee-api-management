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
package io.gravitee.gateway.reactive.reactor.processor.forward;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.reactor.processor.AbstractProcessorTest;
import io.gravitee.reporter.api.v4.metric.Metrics;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class XForwardProcessorTest extends AbstractProcessorTest {

    private XForwardProcessor xForwardProcessor;

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
        xForwardProcessor = new XForwardProcessor();
        ctx.metrics(Metrics.builder().build());
    }

    @Test
    void shouldNotChangeRemoteAddressWithXForwardedForHeader() {
        xForwardProcessor.execute(ctx).test().assertResult();
        verify(mockRequest, times(4)).headers();
        assertThat(ctx.metrics().getRemoteAddress()).isNull();
    }

    @ParameterizedTest(name = "header {0}; remote address {1}")
    @MethodSource("provideParameters")
    void shouldOverrideRemoteAddressWithForwardedForHeader(String header, String remoteAddress) {
        spyRequestHeaders.set(HttpHeaderNames.X_FORWARDED_FOR, header);
        xForwardProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).remoteAddress(remoteAddress);
        assertThat(ctx.metrics().getRemoteAddress()).isEqualTo(remoteAddress);
    }

    @ParameterizedTest
    @MethodSource("provideForwardedTestsData")
    void test_generateOriginalUrl(ForwardedTestData data, String expected) {
        when(mockRequest.scheme()).thenReturn("http");
        when(mockRequest.originalHost()).thenReturn("client-instance.com");
        when(mockRequest.headers()).thenReturn(data.headers);
        when(mockRequest.uri()).thenReturn(data.uri);

        String originalUrl = XForwardProcessor.generateOriginalUrl(mockRequest);

        assertThat(originalUrl).isEqualTo(expected);
    }

    @AllArgsConstructor
    @Builder
    private static class ForwardedTestData {

        public HttpHeaders headers;
        public String uri;
    }

    private static Stream<Arguments> provideForwardedTestsData() {
        HttpHeaders arg1Headers = HttpHeaders.create();
        ForwardedTestData arg1Data = ForwardedTestData.builder().headers(arg1Headers).uri("/test").build();

        HttpHeaders arg2Headers = HttpHeaders.create();
        arg2Headers.set(HttpHeaderNames.FORWARDED, "for=192.0.2.43;proto=http;host=client-proxy-instance.com");
        ForwardedTestData arg2Data = ForwardedTestData.builder().headers(arg2Headers).uri("/test").build();

        HttpHeaders arg3Headers = HttpHeaders.create();
        arg3Headers.set(HttpHeaderNames.X_FORWARDED_FOR, "192.168.1.15");
        arg3Headers.set(HttpHeaderNames.X_FORWARDED_PROTO, "https");
        arg3Headers.set(HttpHeaderNames.X_FORWARDED_HOST, "client-proxy-instance.com");
        ForwardedTestData arg3Data = ForwardedTestData.builder().headers(arg3Headers).uri("").build();

        HttpHeaders arg4Headers = HttpHeaders.create();
        arg4Headers.set(HttpHeaderNames.X_FORWARDED_FOR, "192.168.1.15");
        arg4Headers.set(HttpHeaderNames.X_FORWARDED_PROTO, "http");
        arg4Headers.set(HttpHeaderNames.X_FORWARDED_HOST, "client-proxy-instance.com");
        arg4Headers.set(HttpHeaderNames.X_FORWARDED_PORT, "80");
        ForwardedTestData arg4Data = ForwardedTestData.builder().headers(arg4Headers).uri("/test").build();

        HttpHeaders arg5Headers = HttpHeaders.create();
        arg5Headers.set(HttpHeaderNames.X_FORWARDED_FOR, "192.168.1.15");
        arg5Headers.set(HttpHeaderNames.X_FORWARDED_HOST, "client-proxy-instance.com");
        arg5Headers.set(HttpHeaderNames.X_FORWARDED_PORT, "8080");
        ForwardedTestData arg5Data = ForwardedTestData.builder().headers(arg5Headers).uri("/test").build();

        HttpHeaders arg6Headers = HttpHeaders.create();
        arg6Headers.set(HttpHeaderNames.X_FORWARDED_FOR, "192.168.1.15");
        arg6Headers.set(HttpHeaderNames.X_FORWARDED_PROTO, "https");
        arg6Headers.set(HttpHeaderNames.X_FORWARDED_HOST, "client-proxy-instance1.com,client-proxy-instance2.com");
        arg6Headers.set(HttpHeaderNames.X_FORWARDED_PORT, "443");
        ForwardedTestData arg6Data = ForwardedTestData.builder().headers(arg6Headers).uri("").build();

        HttpHeaders arg7Headers = HttpHeaders.create();
        arg7Headers.set(HttpHeaderNames.X_FORWARDED_FOR, "192.168.1.15");
        arg7Headers.set(HttpHeaderNames.X_FORWARDED_PROTO, "https");
        arg7Headers.set(HttpHeaderNames.X_FORWARDED_HOST, "client-proxy-instance1.com,client-proxy-instance2.com");
        arg7Headers.set(HttpHeaderNames.X_FORWARDED_PORT, "8080");
        ForwardedTestData arg7Data = ForwardedTestData.builder().headers(arg7Headers).uri("").build();

        HttpHeaders arg8Headers = HttpHeaders.create();
        arg8Headers.set(HttpHeaderNames.X_FORWARDED_FOR, "192.168.1.15");
        arg8Headers.set(HttpHeaderNames.X_FORWARDED_PROTO, "http");
        arg8Headers.set(HttpHeaderNames.X_FORWARDED_HOST, "client-proxy-instance1.com,client-proxy-instance2.com");
        ForwardedTestData arg8Data = ForwardedTestData.builder().headers(arg8Headers).uri("/test").build();

        HttpHeaders arg9Headers = HttpHeaders.create();
        arg9Headers.set(HttpHeaderNames.X_FORWARDED_FOR, "192.168.1.15");
        arg9Headers.set(HttpHeaderNames.X_FORWARDED_HOST, "client-proxy-instance1.com,client-proxy-instance2.com");
        arg9Headers.set(HttpHeaderNames.X_FORWARDED_PORT, "80");
        ForwardedTestData arg9Data = ForwardedTestData.builder().headers(arg9Headers).uri("/test").build();

        HttpHeaders arg10Headers = HttpHeaders.create();
        arg10Headers.set(HttpHeaderNames.X_FORWARDED_FOR, "192.168.1.15");
        arg10Headers.set(HttpHeaderNames.X_FORWARDED_HOST, "client-proxy-instance1.com , client-proxy-instance2.com");
        arg10Headers.set(HttpHeaderNames.X_FORWARDED_PORT, "8080");
        ForwardedTestData arg10Data = ForwardedTestData.builder().headers(arg10Headers).uri("/test").build();

        HttpHeaders arg11Headers = HttpHeaders.create();
        arg11Headers.set(HttpHeaderNames.X_FORWARDED_FOR, "192.168.1.15");
        arg11Headers.set(HttpHeaderNames.X_FORWARDED_HOST, "client-proxy-instance1.com , client-proxy-instance2.com");
        arg11Headers.set(HttpHeaderNames.X_FORWARDED_PORT, "8080");
        arg11Headers.set(HttpHeaderNames.X_FORWARDED_PREFIX, "/app");
        ForwardedTestData arg11Data = ForwardedTestData.builder().headers(arg11Headers).uri("/test").build();

        HttpHeaders arg12Headers = HttpHeaders.create();
        arg12Headers.set(HttpHeaderNames.FORWARDED, "for=192.0.2.43 ; proto=http ; host=client-proxy-instance.com");
        ForwardedTestData arg12Data = ForwardedTestData.builder().headers(arg12Headers).uri("").build();

        HttpHeaders arg13Headers = HttpHeaders.create();
        arg13Headers.set(HttpHeaderNames.FORWARDED, "for=192.0.2.43 ; Proto=http ; HOST=client-proxy-instance.com");
        ForwardedTestData arg13Data = ForwardedTestData.builder().headers(arg13Headers).uri("").build();

        HttpHeaders arg14Headers = HttpHeaders.create();
        arg14Headers.set(HttpHeaderNames.FORWARDED, "for=192.0.2.43 , Proto=http , HOST=client-proxy-instance.com");
        ForwardedTestData arg14Data = ForwardedTestData.builder().headers(arg13Headers).uri("").build();

        HttpHeaders arg15Headers = HttpHeaders.create();
        arg15Headers.set(HttpHeaderNames.X_FORWARDED_FOR, "192.168.1.15");
        arg15Headers.set(HttpHeaderNames.X_FORWARDED_HOST, "client-proxy-instance1.com:8080 , client-proxy-instance2.com");
        arg15Headers.set(HttpHeaderNames.X_FORWARDED_PORT, "8080");
        arg15Headers.set(HttpHeaderNames.X_FORWARDED_PREFIX, "/app");
        ForwardedTestData arg15Data = ForwardedTestData.builder().headers(arg15Headers).uri("/test").build();

        HttpHeaders arg16Headers = HttpHeaders.create();
        arg16Headers.set(HttpHeaderNames.X_FORWARDED_FOR, "192.168.1.15");
        arg16Headers.set(HttpHeaderNames.X_FORWARDED_HOST, "http://client-proxy-instance1.com:8080 , client-proxy-instance2.com");
        arg16Headers.set(HttpHeaderNames.X_FORWARDED_PORT, "8080");
        arg16Headers.set(HttpHeaderNames.X_FORWARDED_PREFIX, "/app");
        ForwardedTestData arg16Data = ForwardedTestData.builder().headers(arg16Headers).uri("/test").build();

        return Stream.of(
            Arguments.of(arg1Data, "http://client-instance.com/test"),
            Arguments.of(arg2Data, "http://client-proxy-instance.com/test"),
            Arguments.of(arg3Data, "https://client-proxy-instance.com"),
            Arguments.of(arg4Data, "http://client-proxy-instance.com/test"),
            Arguments.of(arg5Data, "http://client-proxy-instance.com:8080/test"),
            Arguments.of(arg6Data, "https://client-proxy-instance1.com"),
            Arguments.of(arg7Data, "https://client-proxy-instance1.com:8080"),
            Arguments.of(arg8Data, "http://client-proxy-instance1.com/test"),
            Arguments.of(arg9Data, "http://client-proxy-instance1.com/test"),
            Arguments.of(arg10Data, "http://client-proxy-instance1.com:8080/test"),
            Arguments.of(arg11Data, "http://client-proxy-instance1.com:8080/app/test"),
            Arguments.of(arg12Data, "http://client-proxy-instance.com"),
            Arguments.of(arg13Data, "http://client-proxy-instance.com"),
            Arguments.of(arg14Data, "http://client-proxy-instance.com"),
            Arguments.of(arg15Data, "http://client-proxy-instance1.com:8080/app/test"),
            Arguments.of(arg16Data, "http://client-proxy-instance1.com:8080/app/test")
        );
    }
}
