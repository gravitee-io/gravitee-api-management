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

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.TlsSession;
import java.security.cert.Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
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
class HttpTlsSessionTest {

    @Mock
    private SSLSession sslSession;

    private TlsSession cut;

    @Nested
    class SslSessionDelegate {

        @BeforeEach
        void setUp() {
            cut = new HttpTlsSession(sslSession, HttpHeaders.create(), null);
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
    }

    @Nested
    class ClientAuthCertificateExtraction {

        public static final String SSL_CERT_HEADER = "ssl-cert-header";

        @BeforeEach
        void setUp() {
            cut = new HttpTlsSession(sslSession, HttpHeaders.create().add(SSL_CERT_HEADER, VALID_PEM), SSL_CERT_HEADER);
        }

        @Test
        @SneakyThrows
        void should_get_certificate_from_ssl_session() {
            when(sslSession.getPeerCertificates()).thenReturn(new Certificate[] { mock(Certificate.class) });
            assertThat(cut.getPeerCertificates()).hasSize(1);
        }

        @Test
        @SneakyThrows
        void should_get_certificate_from_header_because_empty_in_ssl_session() {
            when(sslSession.getPeerCertificates()).thenReturn(new Certificate[] {});
            assertThat(cut.getPeerCertificates()).hasSize(1);
        }

        @Test
        @SneakyThrows
        void should_get_certificate_from_header_because_null_in_ssl_session() {
            when(sslSession.getPeerCertificates()).thenReturn(null);
            assertThat(cut.getPeerCertificates()).hasSize(1);
        }

        @Test
        @SneakyThrows
        void should_get_certificate_from_header_because_exception_in_ssl_session() {
            when(sslSession.getPeerCertificates()).thenThrow(SSLPeerUnverifiedException.class);
            assertThat(cut.getPeerCertificates()).hasSize(1);
        }

        @Test
        @SneakyThrows
        void should_not_get_certificate_from_header_because_no_header_matching_name() {
            cut = new HttpTlsSession(sslSession, HttpHeaders.create(), SSL_CERT_HEADER);
            when(sslSession.getPeerCertificates()).thenThrow(SSLPeerUnverifiedException.class);
            assertThat(cut.getPeerCertificates()).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_not_get_certificate_from_header_because_invalid_pem() {
            cut = new HttpTlsSession(sslSession, HttpHeaders.create().add(SSL_CERT_HEADER, "invalid"), SSL_CERT_HEADER);
            when(sslSession.getPeerCertificates()).thenThrow(SSLPeerUnverifiedException.class);
            assertThat(cut.getPeerCertificates()).isEmpty();
        }

        private final String VALID_PEM =
            """
                  -----BEGIN CERTIFICATE-----
                  MIIFxjCCA64CCQD9kAnHVVL02TANBgkqhkiG9w0BAQsFADCBozEsMCoGCSqGSIb3
                  DQEJARYddW5pdC50ZXN0c0BncmF2aXRlZXNvdXJjZS5jb20xEzARBgNVBAMMCnVu
                  aXQtdGVzdHMxFzAVBgNVBAsMDkdyYXZpdGVlU291cmNlMRcwFQYDVQQKDA5HcmF2
                  aXRlZVNvdXJjZTEOMAwGA1UEBwwFTGlsbGUxDzANBgNVBAgMBkZyYW5jZTELMAkG
                  A1UEBhMCRlIwIBcNMjExMDE5MTUyMDQxWhgPMjEyMTA5MjUxNTIwNDFaMIGjMSww
                  KgYJKoZIhvcNAQkBFh11bml0LnRlc3RzQGdyYXZpdGVlc291cmNlLmNvbTETMBEG
                  A1UEAwwKdW5pdC10ZXN0czEXMBUGA1UECwwOR3Jhdml0ZWVTb3VyY2UxFzAVBgNV
                  BAoMDkdyYXZpdGVlU291cmNlMQ4wDAYDVQQHDAVMaWxsZTEPMA0GA1UECAwGRnJh
                  bmNlMQswCQYDVQQGEwJGUjCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIB
                  AOKxBeF33XOd5sVaHbavIGFU+DMTX+cqTbRiJQJqAlrrDeuPQ3YEfga7hpHHB3ev
                  OjunNCBJp4p/6VsBhylqcqd8KU+xqQ/wvNsqzp/50ssMkud+0sbPFjjjxM1rDI9X
                  JVCqGqa15jlKfylcOOggH6KAOugM4BquBjeTRH0mGv2MBgZvtKHAieW0gzPslXxp
                  UZZZ+gvvSSLo7NkAv7awWKSoV+yMlXma0yX0ygAj14EK1AxhFLZFgWDm8Ex919ry
                  rbcPV6tqUHjw7Us8cy8p/pqftOUnwyRQ4LmaSdqwESZmdU+GXNXq22sAB6rX0G7u
                  tXmoXVwQVlD8kEb79JbbIEOfPvLATyr8VStCK5dSXyc/JuzDo7QCquQUdrGpWrSy
                  wdKKbCbOWDStakmBTEkgB0Bqg6yWFrHjgj+rzNeWFvIoZA+sLV2UCrlhDQ8BUV9O
                  PMdgGBMKu4TrdEezt1NqDHjvThC3c6quxixxmaO/K7YPncVzguypijw7U7yl8CkG
                  DlUJ+rPddEgsQCf+1E6z/xIeh8sCEdLm6TN80Dsw1yTdwzhRO9KvVY/gjE/ZaUYL
                  g8Z0Htjq6vvnMwvr4C/8ykRk9oMYlv3o52pXQEcsbiZYm7LCTwgCs6k7KEiaHUze
                  ySEqlkqFC8PG2GzCC6dM50xYktbcmwC+mep7c6bTAsexAgMBAAEwDQYJKoZIhvcN
                  AQELBQADggIBAIHpb9solYTIPszzgvw0S6BVBAGzARDNDSi/jj+4KXKlKxYvVvq+
                  bTX7YE6rC/wFGpyCjwfoWzzIrfLiIcmVTfu1o13Y/B8IEP4WyiAYrGszLqbjy1wM
                  cyfwaxYpP/XfIQgcP5idI6kAA7hbGrFrLijIcdfYhh4tr6dsjD81uNrsVhp+JcAV
                  CPv2o5YeRSMFUJrImAU5s73yX/x6fb2nCUR6PIMiPm9gveIAuY2+L12NzIJUugwN
                  EZjqCeOr52f/yDuA+pAvVCGnZSSdkVWUh02ZsPxM4TiRzmxSkM5ODb59XWHeoFT1
                  yvKA2F7+WFAL2R8BhBoVlBp1hug33Mrsix7L6yG4G9Ljss9Y0pzEd4B+IFGbpMZN
                  R4dqZGpKS0aiStnvnurXBVWwIcJ3kCaAl2OgXZO5ivi+iNIx8e5qtXqDCnnlpeGz
                  1KVhzZaqND1I+X1JS6I/V/HiTsnuVdg5aBZPYbQI0QLSgB+0SOjmTlWzjyJEt0PS
                  kyOEs4bB9CPf3JaWgB9aORczsgn/cz8S7kEc8JlXDflePiSl4QPWYbX05wY9l2lJ
                  yzuug/vKMCWUq0cU2i8WSA02N0+tEm4hCNol04KLKa3MRAa/yOSmDIJ4z+2D/BSD
                  FZHaYejhPQFZzv73SxOAu2QCaXH5vIBEDx4Mb+lvc4BukgeIT2Gyi2gg
                  -----END CERTIFICATE-----
                  """;
    }
}
