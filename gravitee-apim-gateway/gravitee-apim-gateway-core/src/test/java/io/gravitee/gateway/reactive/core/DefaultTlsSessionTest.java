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
package io.gravitee.gateway.reactive.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.context.TlsSession;
import java.security.cert.Certificate;
import javax.net.ssl.SSLSession;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultTlsSessionTest {

    @Mock
    private SSLSession sslSession;

    private TlsSession cut;

    @Nested
    class SslSessionDelegate {

        @BeforeEach
        void setUp() {
            cut = new DefaultTlsSession(sslSession);
        }

        @Test
        void should_get_ssl_session_id() {
            cut.getId();
            verify(sslSession).getId();
        }

        @Test
        void should_get_ssl_session_context() {
            cut.getSessionContext();
            verify(sslSession).getSessionContext();
        }

        @Test
        void should_get_ssl_session_creation_time() {
            cut.getCreationTime();
            verify(sslSession).getCreationTime();
        }

        @Test
        void should_get_ssl_session_last_accessed_time() {
            cut.getLastAccessedTime();
            verify(sslSession).getLastAccessedTime();
        }

        @Test
        void should_invalidate_ssl_session() {
            cut.invalidate();
            verify(sslSession).invalidate();
        }

        @Test
        void should_get_ssl_session_validity() {
            cut.isValid();
            verify(sslSession).isValid();
        }

        @Test
        void should_put_value_in_ssl_session() {
            cut.putValue("name", "value");
            verify(sslSession).putValue("name", "value");
        }

        @Test
        void should_get_value_from_ssl_session() {
            cut.getValue("name");
            verify(sslSession).getValue("name");
        }

        @Test
        void should_remove_value_from_ssl_session() {
            cut.removeValue("name");
            verify(sslSession).removeValue("name");
        }

        @Test
        void should_get_value_names_from_ssl_session() {
            cut.getValueNames();
            verify(sslSession).getValueNames();
        }

        @SneakyThrows
        @Test
        void should_get_peer_certificates_from_ssl_session() {
            cut.getPeerCertificates();
            verify(sslSession).getPeerCertificates();
        }

        @Test
        void should_get_local_certificates_from_ssl_session() {
            cut.getLocalCertificates();
            verify(sslSession).getLocalCertificates();
        }

        @SneakyThrows
        @Test
        void should_get_peer_principal_from_ssl_session() {
            cut.getPeerPrincipal();
            verify(sslSession).getPeerPrincipal();
        }

        @Test
        void should_get_local_principal_from_ssl_session() {
            cut.getLocalPrincipal();
            verify(sslSession).getLocalPrincipal();
        }

        @Test
        void should_get_ssl_session_cypher_suite() {
            cut.getCipherSuite();
            verify(sslSession).getCipherSuite();
        }

        @Test
        void should_get_ssl_session_protocol() {
            cut.getProtocol();
            verify(sslSession).getProtocol();
        }

        @Test
        void should_get_ssl_session_peer_host() {
            cut.getPeerHost();
            verify(sslSession).getPeerHost();
        }

        @Test
        void should_get_ssl_session_peer_port() {
            cut.getPeerPort();
            verify(sslSession).getPeerPort();
        }

        @Test
        void should_get_ssl_session_packet_buffer_size() {
            cut.getPacketBufferSize();
            verify(sslSession).getPacketBufferSize();
        }

        @Test
        void should_get_ssl_session_application_buffer_size() {
            cut.getApplicationBufferSize();
            verify(sslSession).getApplicationBufferSize();
        }

        @Test
        @SneakyThrows
        void should_get_certificate_from_ssl_session() {
            when(sslSession.getPeerCertificates()).thenReturn(new Certificate[] { mock(Certificate.class) });
            assertThat(cut.getPeerCertificates()).hasSize(1);
        }
    }
}
