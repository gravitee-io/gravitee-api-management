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
package io.gravitee.apim.integration.tests.tls;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.integration.tests.tls.TestHelper.*;
import static io.gravitee.apim.integration.tests.tls.TestHelper.PASSWORD;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.tcp.proxy.TcpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.entrypoint.tcp.proxy.TcpProxyEntrypointConnectorFactory;
import io.vertx.rxjava3.core.Vertx;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MutualTLSIntegrationTest {

    abstract static class AbstractMTlsTest extends AbstractGatewayTest {

        String httpCert;
        String tcpCert;

        @BeforeEach
        void setUpWiremockTestEndpoint() {
            wiremock.stubFor(get(WIREMOCK_ENDPOINT_URI).willReturn(ok(RESPONSE_FROM_BACKEND)));
        }

        @Override
        public void configurePlaceHolderVariables(Map<String, String> variables) {
            variables.put("WIREMOCK_PORT", "" + wiremock.port());
        }

        @Override
        @SneakyThrows
        protected void configureGateway(GatewayConfigurationBuilder config) {
            /*
             * standard TLS config, we want to be able to call API without trustall options so we keep the server certs
             */
            TestHelper.KeyStoreGenResult http = createNewPKCS12KeyStore(BRIGHT_SIDE_FQDN);
            this.httpCert = http.certs().get(BRIGHT_SIDE_FQDN).toPem();
            var httpKeyStoreLocation = http.location().toAbsolutePath();

            KeyStoreGenResult tcp = createNewPKCS12KeyStore(BRIGHT_SIDE_FQDN);
            this.tcpCert = tcp.certs().get(BRIGHT_SIDE_FQDN).toPem();
            var tcpKeyStoreLocation = tcp.location().toAbsolutePath();

            config
                .httpSecured(true)
                .set("http.ssl.sni", true)
                .set("http.ssl.clientAuth", "required") // force mTLS always
                .set("http.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                .set("http.ssl.keystore.path", httpKeyStoreLocation.toString())
                .set("http.ssl.keystore.password", new String(PASSWORD))
                .enableTcpGateway()
                .set("tcp.ssl.clientAuth", "required") // force mTLS always
                .set("tcp.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                .set("tcp.ssl.keystore.path", tcpKeyStoreLocation.toString())
                .set("tcp.ssl.keystore.password", new String(PASSWORD));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
            entrypoints.putIfAbsent("tcp-proxy", EntrypointBuilder.build("tcp-proxy", TcpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
            endpoints.putIfAbsent("tcp-proxy", EndpointBuilder.build("tcp-proxy", TcpProxyEndpointConnectorFactory.class));
        }

        Vertx vertx() {
            return getBean(Vertx.class);
        }
    }

    @GatewayTest
    @Nested
    class Pkcs12TruststoreTest extends AbstractMTlsTest {

        Path httpClientKeyStorePath;
        Path httpServerTrustStorePath;
        Path tcpClientKeyStorePath;
        Path tcpServerTrustStorePath;

        @Override
        @SneakyThrows
        protected void configureGateway(GatewayConfigurationBuilder config) {
            super.configureGateway(config);

            TestHelper.KeyStoreGenResult tcpClientKeyPair = createNewPKCS12KeyStore(CLIENT_CN);
            this.tcpServerTrustStorePath = tcpClientKeyPair.toTrustStore();
            this.tcpClientKeyStorePath = tcpClientKeyPair.location();
            config.set("tcp.ssl.truststore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12);
            config.set("tcp.ssl.truststore.path", tcpServerTrustStorePath);
            config.set("tcp.ssl.truststore.password", new String(PASSWORD));

            TestHelper.KeyStoreGenResult httpClientKeyPair = createNewPKCS12KeyStore(CLIENT_CN);
            this.httpServerTrustStorePath = httpClientKeyPair.toTrustStore();
            this.httpClientKeyStorePath = httpClientKeyPair.location();
            config.set("http.ssl.truststore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12);
            config.set("http.ssl.truststore.path", httpServerTrustStorePath);
            config.set("http.ssl.truststore.password", new String(PASSWORD));
        }

        @Test
        @DeployApi({ "/apis/v4/http/api.json" })
        void should_be_able_to_call_with_trust_store_and_renew_certs_using_http(GatewayDynamicConfig.HttpConfig httpConfig)
            throws Exception {
            var httpClient = createTrustedHttpClient(vertx(), httpCert, httpClientKeyStorePath);
            assertApiCall(httpClient, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);

            KeyStoreGenResult newClientKeyPair = createNewPKCS12KeyStore(CLIENT_CN);

            Files.copy(newClientKeyPair.toTrustStore(), this.httpServerTrustStorePath, REPLACE_EXISTING);
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var httpClient2 = createTrustedHttpClient(vertx(), httpCert, newClientKeyPair.location());
                    assertApiCall(httpClient2, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);
                });
        }

        @Test
        @DeployApi({ "/apis/v4/tcp/api-multi-domain.json" })
        void should_be_able_to_call_with_trust_store_and_renew_certs_using_tcp(GatewayDynamicConfig.TcpConfig tcpConfig) throws Exception {
            var httpClient = createTrustedHttpClient(vertx(), tcpCert, tcpClientKeyStorePath);
            assertApiCall(httpClient, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);

            KeyStoreGenResult newClientKeyPair = createNewPKCS12KeyStore(CLIENT_CN);

            Files.copy(newClientKeyPair.toTrustStore(), this.tcpServerTrustStorePath, REPLACE_EXISTING);
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var httpClient2 = createTrustedHttpClient(vertx(), tcpCert, newClientKeyPair.location());
                    assertApiCall(httpClient2, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);
                });

            assertHandshakeError(vertx(), tcpCert, GATEWAY_TCP_API_URI, tcpConfig.tcpPort(), tcpClientKeyStorePath);
        }
    }

    @GatewayTest
    @Nested
    class PemTrustStoreTest extends AbstractMTlsTest {

        private PemGenResult.KeyPairLocation tcpClientKeyPair;
        private Path tcpServerTrustPemPath;
        private PemGenResult.KeyPairLocation httpClientKeyPair;
        private Path httpServerTrustPemPath;

        @Override
        @SneakyThrows
        protected void configureGateway(GatewayConfigurationBuilder config) {
            super.configureGateway(config);

            PemGenResult httpClientCertGen = createNewPEMs(CLIENT_CN);
            this.tcpClientKeyPair = httpClientCertGen.locations().get(CLIENT_CN);
            this.tcpServerTrustPemPath = httpClientCertGen.certificates().get(CLIENT_CN).certPath();
            config.set("tcp.ssl.truststore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PEM);
            config.set("tcp.ssl.truststore.path", this.tcpServerTrustPemPath.toAbsolutePath().toString());

            PemGenResult tcpClientCertGen = createNewPEMs(CLIENT_CN);
            this.httpClientKeyPair = tcpClientCertGen.locations().get(CLIENT_CN);
            this.httpServerTrustPemPath = tcpClientCertGen.certificates().get(CLIENT_CN).certPath();
            config.set("http.ssl.truststore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PEM);
            config.set("http.ssl.truststore.path", this.httpServerTrustPemPath.toAbsolutePath().toString());
        }

        @Test
        @DeployApi({ "/apis/v4/tcp/api-multi-domain.json" })
        void should_be_able_to_call_with_trust_store_and_renew_certs_using_tcp(GatewayDynamicConfig.TcpConfig tcpConfig) throws Exception {
            var httpClient = createTrustedHttpClient(vertx(), tcpCert, tcpClientKeyPair);
            assertApiCall(httpClient, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);

            PemGenResult newClientKeyPair = createNewPEMs(CLIENT_CN);

            Files.copy(newClientKeyPair.certificates().get(CLIENT_CN).certPath(), this.tcpServerTrustPemPath, REPLACE_EXISTING);
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var httpClient2 = createTrustedHttpClient(vertx(), tcpCert, newClientKeyPair.locations().get(CLIENT_CN));
                    assertApiCall(httpClient2, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);
                });

            assertHandshakeError(vertx(), tcpCert, GATEWAY_TCP_API_URI, tcpConfig.tcpPort(), tcpClientKeyPair);
        }

        @Test
        @DeployApi({ "/apis/v4/http/api.json" })
        void should_be_able_to_call_with_trust_store_and_renew_certs_using_http(GatewayDynamicConfig.HttpConfig httpConfig)
            throws Exception {
            var httpClient = createTrustedHttpClient(vertx(), httpCert, httpClientKeyPair);
            assertApiCall(httpClient, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);

            PemGenResult newClientKeyPair = createNewPEMs(CLIENT_CN);

            Files.copy(newClientKeyPair.certificates().get(CLIENT_CN).certPath(), this.httpServerTrustPemPath, REPLACE_EXISTING);
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var httpClient2 = createTrustedHttpClient(vertx(), httpCert, newClientKeyPair.locations().get(CLIENT_CN));
                    assertApiCall(httpClient2, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);
                });

            assertHandshakeError(vertx(), httpCert, GATEWAY_HTTP_API_URI, httpConfig.httpPort(), httpClientKeyPair);
        }
    }

    @GatewayTest
    @Nested
    class PemFolderTrustStoreTest extends AbstractMTlsTest {

        private PemGenResult.KeyPairLocation tcpClientKeyPair;
        private Path tcpServerTrustPemFolder;
        private Path tcpServerInitialTrustPemFile;
        private PemGenResult.KeyPairLocation httpClientKeyPair;
        private Path httpServerTrustPemFolder;
        private Path httpServerInitialTrustPemFile;

        @Override
        @SneakyThrows
        protected void configureGateway(GatewayConfigurationBuilder config) {
            super.configureGateway(config);

            PemGenResult tcpClientCertGen = createNewPEMs(CLIENT_CN);
            this.tcpClientKeyPair = tcpClientCertGen.locations().get(CLIENT_CN);
            this.tcpServerTrustPemFolder = tcpClientCertGen.certDirectory();
            this.tcpServerInitialTrustPemFile = tcpClientCertGen.certificates().get(CLIENT_CN).certPath();
            config.set("tcp.ssl.truststore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PEM_FOLDER);
            config.set("tcp.ssl.truststore.path", this.tcpServerTrustPemFolder.toAbsolutePath().toString());

            PemGenResult httpClientCertGen = createNewPEMs(CLIENT_CN);
            this.httpClientKeyPair = httpClientCertGen.locations().get(CLIENT_CN);
            this.httpServerTrustPemFolder = httpClientCertGen.certDirectory();
            this.httpServerInitialTrustPemFile = httpClientCertGen.certificates().get(CLIENT_CN).certPath();
            config.set("http.ssl.truststore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PEM_FOLDER);
            config.set("http.ssl.truststore.path", this.httpServerTrustPemFolder.toAbsolutePath().toString());
        }

        @Test
        @DeployApi({ "/apis/v4/tcp/api-multi-domain.json" })
        void should_be_able_to_call_with_trust_store_and_renew_certs_using_tcp(GatewayDynamicConfig.TcpConfig tcpConfig) throws Exception {
            Path certNumber2Path = this.tcpServerTrustPemFolder.resolve(certFileName(2));

            // test first cert
            var httpClient = createTrustedHttpClient(vertx(), tcpCert, tcpClientKeyPair);
            assertApiCall(httpClient, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);

            // add a new cert to the folder
            PemGenResult clientKeyPair2 = createNewPEMs(CLIENT_CN);
            Files.copy(clientKeyPair2.certificates().get(CLIENT_CN).certPath(), certNumber2Path);

            //#1 still work
            assertApiCall(httpClient, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);

            // #2 works too
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var client = createTrustedHttpClient(vertx(), tcpCert, clientKeyPair2.locations().get(CLIENT_CN));
                    assertApiCall(client, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);
                });

            // update #2
            PemGenResult clientKeyPair2Prime = createNewPEMs(CLIENT_CN);
            Files.copy(clientKeyPair2Prime.certificates().get(CLIENT_CN).certPath(), certNumber2Path, REPLACE_EXISTING);

            // #2 is no longer working, #2' works
            var httpClient2Prime = createTrustedHttpClient(vertx(), tcpCert, clientKeyPair2Prime.locations().get(CLIENT_CN));
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertHandshakeError(vertx(), tcpCert, GATEWAY_TCP_API_URI, tcpConfig.tcpPort(), clientKeyPair2);
                    assertApiCall(httpClient2Prime, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);
                });

            // #1 remove
            Files.delete(tcpServerInitialTrustPemFile);

            // #2' should continue to work
            assertApiCall(httpClient2Prime, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);

            // while #1 not so much anymore
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertHandshakeError(vertx(), tcpCert, GATEWAY_TCP_API_URI, tcpConfig.tcpPort(), tcpClientKeyPair);
                });
        }

        @Test
        @DeployApi({ "/apis/v4/http/api.json" })
        void should_be_able_to_call_with_trust_store_and_renew_certs_using_http(GatewayDynamicConfig.HttpConfig httpConfig)
            throws Exception {
            Path certNumber2Path = this.httpServerTrustPemFolder.resolve(certFileName(2));

            // test first cert
            var httpClient = createTrustedHttpClient(vertx(), httpCert, httpClientKeyPair);
            assertApiCall(httpClient, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);

            // add a new cert to the folder
            PemGenResult clientKeyPair2 = createNewPEMs(CLIENT_CN);
            Files.copy(clientKeyPair2.certificates().get(CLIENT_CN).certPath(), certNumber2Path);

            //#1 still work
            assertApiCall(httpClient, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);

            // #2 works too
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var client = createTrustedHttpClient(vertx(), httpCert, clientKeyPair2.locations().get(CLIENT_CN));
                    assertApiCall(client, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);
                });

            // update #2
            PemGenResult clientKeyPair2Prime = createNewPEMs(CLIENT_CN);
            Files.copy(clientKeyPair2Prime.certificates().get(CLIENT_CN).certPath(), certNumber2Path, REPLACE_EXISTING);

            // #2 is no longer working, #2' works
            var httpClient2Prime = createTrustedHttpClient(vertx(), httpCert, clientKeyPair2Prime.locations().get(CLIENT_CN));
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertHandshakeError(vertx(), httpCert, GATEWAY_HTTP_API_URI, httpConfig.httpPort(), clientKeyPair2);
                    assertApiCall(httpClient2Prime, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);
                });

            // #1 remove
            Files.delete(httpServerInitialTrustPemFile);

            // #2' should continue to work
            assertApiCall(httpClient2Prime, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);

            // while #1 not so much anymore
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertHandshakeError(vertx(), httpCert, GATEWAY_HTTP_API_URI, httpConfig.httpPort(), httpClientKeyPair);
                });
        }
    }
}
