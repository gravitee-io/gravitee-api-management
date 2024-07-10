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
package io.gravitee.gateway.jupiter.http.vertx.client;

import static io.gravitee.gateway.jupiter.http.vertx.client.VertxHttpClient.HTTP_SSL_OPENSSL_CONFIGURATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.http.HttpClientOptions;
import io.gravitee.definition.model.v4.http.HttpProxyOptions;
import io.gravitee.definition.model.v4.ssl.SslOptions;
import io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore;
import io.gravitee.node.api.configuration.Configuration;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.io.IOException;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class VertxHttpClientTest {

    protected static final String PASSWORD = "gravitee";
    private final Vertx vertx = Vertx.vertx();
    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private Configuration nodeConfiguration;

    @Captor
    private ArgumentCaptor<io.vertx.core.http.HttpClientOptions> vertxOptionsCaptor;

    private VertxHttpClient.VertxHttpClientBuilder builder() throws Exception {
        final HttpClientOptions httpOptions = mapper.readValue(HTTP_CONFIG, HttpClientOptions.class);
        final HttpProxyOptions proxyOptions = mapper.readValue(PROXY_CONFIG, HttpProxyOptions.class);

        when(nodeConfiguration.getProperty(HTTP_SSL_OPENSSL_CONFIGURATION, Boolean.class, false)).thenReturn(false);

        return VertxHttpClient
            .builder()
            .vertx(vertx)
            .nodeConfiguration(nodeConfiguration)
            .name("test")
            .shared(false)
            .defaultTarget("https://api.gravitee.io/echo")
            .httpOptions(httpOptions)
            .proxyOptions(proxyOptions);
    }

    @Test
    void shouldBuildClientWithSystemProxy() throws Exception {
        final HttpProxyOptions proxyOptions = mapper.readValue(PROXY_CONFIG, HttpProxyOptions.class);
        proxyOptions.setUseSystemProxy(true);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().proxyOptions(proxyOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldIllegalArgumentExceptionWithPEMTrustStoreMissingPathOrContent() throws Exception {
        final SslOptions sslOptions = new SslOptions();
        final PEMTrustStore trustStore = new PEMTrustStore();
        sslOptions.setTrustStore(trustStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);

        assertThrows(IllegalArgumentException.class, () -> vertxHttpClientBuilder.build().createHttpClient());
    }

    @Test
    void shouldBuildClientWithPEMTrustStorePath() throws Exception {
        final String location = getLocation("truststore.pem");

        final SslOptions sslOptions = new SslOptions();
        final PEMTrustStore trustStore = new PEMTrustStore();
        trustStore.setPath(location);

        sslOptions.setTrustStore(trustStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldBuildClientWithPEMTrustStoreContent() throws Exception {
        final String content = getContent("truststore.pem");

        final SslOptions sslOptions = new SslOptions();
        final PEMTrustStore trustStore = new PEMTrustStore();
        trustStore.setContent(content);

        sslOptions.setTrustStore(trustStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWithPEMCertMissingPathOrContent() throws Exception {
        final SslOptions sslOptions = new SslOptions();
        final PEMKeyStore keystore = new PEMKeyStore();
        sslOptions.setKeyStore(keystore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);

        assertThrows(IllegalArgumentException.class, () -> vertxHttpClientBuilder.build().createHttpClient());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWithPEMKeyMissingPathOrContent() throws Exception {
        final String certLocation = getLocation("client.cer");

        final SslOptions sslOptions = new SslOptions();
        final PEMKeyStore keyStore = new PEMKeyStore();
        sslOptions.setKeyStore(keyStore);
        keyStore.setCertPath(certLocation);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);

        assertThrows(IllegalArgumentException.class, () -> vertxHttpClientBuilder.build().createHttpClient());
    }

    @Test
    void shouldBuildClientWithPEMKeyStorePath() throws Exception {
        final String keyLocation = getLocation("client.key");
        final String certLocation = getLocation("client.cer");

        final SslOptions sslOptions = new SslOptions();
        final PEMKeyStore keyStore = new PEMKeyStore();
        keyStore.setKeyPath(keyLocation);
        keyStore.setCertPath(certLocation);

        sslOptions.setKeyStore(keyStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldBuildClientWithPEMKeystoreContent() throws Exception {
        final String keyContent = getContent("client.key");
        final String certContent = getContent("client.cer");

        final SslOptions sslOptions = new SslOptions();
        final PEMKeyStore keystore = new PEMKeyStore();
        keystore.setKeyContent(keyContent);
        keystore.setCertContent(certContent);

        sslOptions.setKeyStore(keystore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWithPKCS12TrustStoreMissingPathOrContent() throws Exception {
        final SslOptions sslOptions = new SslOptions();
        final PKCS12TrustStore trustStore = new PKCS12TrustStore();
        sslOptions.setTrustStore(trustStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);

        assertThrows(IllegalArgumentException.class, () -> vertxHttpClientBuilder.build().createHttpClient());
    }

    @Test
    void shouldBuildClientWithPKCS12TrustStorePath() throws Exception {
        final String location = getLocation("truststore.p12");

        final SslOptions sslOptions = new SslOptions();
        final PKCS12TrustStore trustStore = new PKCS12TrustStore();
        trustStore.setPath(location);

        sslOptions.setTrustStore(trustStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldBuildClientWithPKCS12TrustStoreContent() throws Exception {
        final String content = getContentAsBase64("truststore.p12");

        final SslOptions sslOptions = new SslOptions();
        final PKCS12TrustStore trustStore = new PKCS12TrustStore();
        trustStore.setContent(content);

        sslOptions.setTrustStore(trustStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWithPKCS12KeyStoreMissingPathOrContent() throws Exception {
        final SslOptions sslOptions = new SslOptions();
        final PKCS12KeyStore keyStore = new PKCS12KeyStore();
        sslOptions.setKeyStore(keyStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);

        assertThrows(IllegalArgumentException.class, () -> vertxHttpClientBuilder.build().createHttpClient());
    }

    @Test
    void shouldBuildClientWithPKC12KeyStorePath() throws Exception {
        final String location = getLocation("client.p12");

        final SslOptions sslOptions = new SslOptions();
        final PKCS12KeyStore keyStore = new PKCS12KeyStore();
        keyStore.setPath(location);
        keyStore.setPassword(PASSWORD);

        sslOptions.setKeyStore(keyStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldBuildClientWithPKCS12KeystoreContent() throws Exception {
        final String content = getContentAsBase64("client.p12");

        final SslOptions sslOptions = new SslOptions();
        final PKCS12KeyStore keystore = new PKCS12KeyStore();
        keystore.setContent(content);
        keystore.setPassword(PASSWORD);

        sslOptions.setKeyStore(keystore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWithJKSTrustStoreMissingPathOrContent() throws Exception {
        final SslOptions sslOptions = new SslOptions();
        final JKSTrustStore trustStore = new JKSTrustStore();
        sslOptions.setTrustStore(trustStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);

        assertThrows(IllegalArgumentException.class, () -> vertxHttpClientBuilder.build().createHttpClient());
    }

    @Test
    void shouldBuildClientWithJKSTrustStorePath() throws Exception {
        final String location = getLocation("truststore.jks");

        final SslOptions sslOptions = new SslOptions();
        final JKSTrustStore trustStore = new JKSTrustStore();
        trustStore.setPath(location);

        sslOptions.setTrustStore(trustStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldBuildClientWithJKSTrustStoreContent() throws Exception {
        final String content = getContentAsBase64("truststore.jks");

        final SslOptions sslOptions = new SslOptions();
        final JKSTrustStore trustStore = new JKSTrustStore();
        trustStore.setContent(content);

        sslOptions.setTrustStore(trustStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWithJKSKeyStoreMissingPathOrContent() throws Exception {
        final SslOptions sslOptions = new SslOptions();
        final JKSKeyStore keyStore = new JKSKeyStore();
        sslOptions.setKeyStore(keyStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);

        assertThrows(IllegalArgumentException.class, () -> vertxHttpClientBuilder.build().createHttpClient());
    }

    @Test
    void shouldBuildClientWithJKSKeyStorePath() throws Exception {
        final String location = getLocation("client.jks");

        final SslOptions sslOptions = new SslOptions();
        final JKSKeyStore keyStore = new JKSKeyStore();
        keyStore.setPath(location);
        keyStore.setPassword(PASSWORD);

        sslOptions.setKeyStore(keyStore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    @Test
    void shouldBuildClientWithJKSKeystoreContent() throws Exception {
        final String content = getContentAsBase64("client.jks");

        final SslOptions sslOptions = new SslOptions();
        final JKSKeyStore keystore = new JKSKeyStore();
        keystore.setContent(content);
        keystore.setPassword(PASSWORD);

        sslOptions.setKeyStore(keystore);

        final VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder = builder().sslOptions(sslOptions);
        final HttpClient httpClient = vertxHttpClientBuilder.build().createHttpClient();

        assertThat(httpClient).isNotNull();
    }

    private String getLocation(String file) {
        return VertxHttpClientTest.class.getResource("/ssl/" + file).getPath();
    }

    private String getContent(String file) throws IOException {
        return new String(VertxHttpClientTest.class.getResourceAsStream("/ssl/" + file).readAllBytes());
    }

    private String getContentAsBase64(String file) throws IOException {
        return new String(Base64.getEncoder().encode(VertxHttpClientTest.class.getResourceAsStream("/ssl/" + file).readAllBytes()));
    }

    private static final String HTTP_CONFIG =
        "{\n" +
        "                            \"keepAlive\": true,\n" +
        "                            \"followRedirects\": false,\n" +
        "                            \"readTimeout\": 10000,\n" +
        "                            \"idleTimeout\": 60000,\n" +
        "                            \"keepAliveTimeout\": 30000,\n" +
        "                            \"connectTimeout\": 5000,\n" +
        "                            \"propagateClientAcceptEncoding\": true,\n" +
        "                            \"useCompression\": false,\n" +
        "                            \"maxConcurrentConnections\": 100,\n" +
        "                            \"version\": \"HTTP_2\",\n" +
        "                            \"pipelining\": false,\n" +
        "                            \"clearTextUpgrade\": true\n" +
        "                        }";

    private static final String PROXY_CONFIG =
        "{\n" +
        "                            \"enabled\": true,\n" +
        "                            \"useSystemProxy\": false,\n" +
        "                            \"host\": \"localhost\",\n" +
        "                            \"port\": 8080,\n" +
        "                            \"username\": \"user\",\n" +
        "                            \"password\": \"pwd\",\n" +
        "                            \"type\": \"HTTP\"\n" +
        "                        }";
}
