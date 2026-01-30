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
package io.gravitee.gateway.reactive.policy.adapter.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.TlsSession;
import io.gravitee.gateway.reactive.core.HttpTlsSession;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class RequestAdapterTest {

    @Mock
    private VertxHttpServerRequest request;

    @Mock
    private Runnable onResumeHandler;

    @Mock
    private Handler<Buffer> bodyHandler;

    @Mock
    private Handler<Void> endHandler;

    private RequestAdapter cut;

    @BeforeEach
    void init() {
        cut = new RequestAdapter(request, null);
        cut.onResume(onResumeHandler);
        cut.bodyHandler(bodyHandler);
        cut.endHandler(endHandler);
    }

    @Test
    void shouldCallOnResumeHandlerWhenResumeForTheFirstTime() {
        cut.resume();
        verify(onResumeHandler).run();
    }

    @Test
    void shouldResumeRequest() {
        // First resume calls onResume handler, any other resumes the request.
        for (int i = 0; i < 10; i++) {
            cut.resume();
        }

        verify(request, times(9)).resume();
    }

    @Test
    void shouldPauseRequest() {
        for (int i = 0; i < 10; i++) {
            cut.pause();
        }

        verify(request, times(10)).pause();
    }

    @Nested
    @DisplayName("SSL Session tests")
    class SslSessionTests {

        @Mock
        private SSLSession sslSession;

        @Test
        @DisplayName("Should return null sslSession for HTTP request (non-SSL connection)")
        void shouldReturnNullSSLSessionForHttpRequest() {
            // HttpTlsSession with null delegate represents HTTP request
            TlsSession httpTlsSession = new HttpTlsSession(null, HttpHeaders.create(), null);
            when(request.tlsSession()).thenReturn(httpTlsSession);

            SSLSession result = cut.sslSession();

            assertThat(result).isNull();
            verify(request).tlsSession();
        }

        @Test
        @DisplayName("Should return TlsSession for HTTPS request (SSL connection)")
        void shouldReturnTlsSessionForHttpsRequest() {
            // Given: HTTPS request with TlsSession that is an SSL connection
            TlsSession httpsTlsSession = new HttpTlsSession(sslSession, HttpHeaders.create(), null);
            when(request.tlsSession()).thenReturn(httpsTlsSession);

            SSLSession result = cut.sslSession();

            assertThat(result).isNotNull();
            assertThat(result).isSameAs(httpsTlsSession);
            // Verify it's actually a TlsSession and is an SSL connection
            assertThat(result).isInstanceOf(TlsSession.class);
            assertThat(((TlsSession) result).isSSLConnection()).isTrue();
            verify(request).tlsSession();
        }

        @Test
        @DisplayName("Should return null when tlsSession() returns null")
        void shouldReturnNullWhenTlsSessionIsNull() {
            // Given: Request with no TlsSession
            when(request.tlsSession()).thenReturn(null);

            SSLSession result = cut.sslSession();

            assertThat(result).isNull();
            verify(request).tlsSession();
        }

        @Test
        @DisplayName("Should return null when TlsSession exists but isSSLConnection() returns false")
        void shouldReturnNullWhenTlsSessionIsNotSslConnection() {
            // Given: TlsSession that exists but isSSLConnection() returns false
            // This simulates an edge case where TlsSession is not null but not an SSL connection
            TlsSession nonSslTlsSession = new HttpTlsSession(null, HttpHeaders.create(), null);
            when(request.tlsSession()).thenReturn(nonSslTlsSession);
            assertThat(nonSslTlsSession.isSSLConnection()).isFalse();

            SSLSession result = cut.sslSession();
            assertThat(result).isNull();
            verify(request).tlsSession();
        }
    }
}
