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
import io.vertx.rxjava3.core.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class TlsIntegrationTest {

    abstract static class AbstractTlsTest extends AbstractGatewayTest {

        String httpBrightCert;
        String httpDarkCert;
        String tcpBrightCert;
        String tcpDarkCert;

        @BeforeEach
        void setUpWiremockTestEndpoint() {
            wiremock.stubFor(get(WIREMOCK_ENDPOINT_URI).willReturn(ok(RESPONSE_FROM_BACKEND)));
        }

        @Override
        public void configurePlaceHolderVariables(Map<String, String> variables) {
            variables.put("WIREMOCK_PORT", "" + wiremock.port());
        }

        @Override
        protected void configureGateway(GatewayConfigurationBuilder config) {
            config.httpSecured(true).set("http.ssl.sni", true).enableTcpGateway();
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

    abstract static class AbstractKeyStoreTLSTest extends AbstractTlsTest {

        Path httpKeyStoreLocation;
        Path tcpKeyStoreLocation;

        @Override
        @SneakyThrows
        protected void configureGateway(GatewayConfigurationBuilder config) {
            super.configureGateway(config);

            KeyStoreGenResult http = createNewPKCS12KeyStore(BRIGHT_SIDE_FQDN, DARK_SIDE_FQDN);
            httpBrightCert = http.certs().get(BRIGHT_SIDE_FQDN).toPem();
            httpDarkCert = http.certs().get(DARK_SIDE_FQDN).toPem();
            httpKeyStoreLocation = http.location().toAbsolutePath();

            KeyStoreGenResult tcp = createNewPKCS12KeyStore(BRIGHT_SIDE_FQDN, DARK_SIDE_FQDN);
            tcpBrightCert = tcp.certs().get(BRIGHT_SIDE_FQDN).toPem();
            tcpDarkCert = tcp.certs().get(DARK_SIDE_FQDN).toPem();
            tcpKeyStoreLocation = tcp.location().toAbsolutePath();
            config
                .set("http.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                .set("http.ssl.keystore.path", httpKeyStoreLocation.toString())
                .set("http.ssl.keystore.password", new String(PASSWORD))
                .set("tcp.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                .set("tcp.ssl.keystore.path", tcpKeyStoreLocation.toString())
                .set("tcp.ssl.keystore.password", new String(PASSWORD));
        }
    }

    @Nested
    @GatewayTest
    class StaticKeyStore extends AbstractKeyStoreTLSTest {

        @Test
        @DeployApi({ "/apis/v4/http/api.json" })
        void should_connect_to_gateway_with_tls_via_http(GatewayDynamicConfig.HttpConfig httpConfig) {
            var brightHttpClient = createTrustedHttpClient(vertx(), httpBrightCert);

            assertApiCall(brightHttpClient, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);

            var darkHttpClient = createTrustedHttpClient(vertx(), httpDarkCert);

            assertApiCall(darkHttpClient, httpConfig.httpPort(), DARK_SIDE_FQDN, GATEWAY_HTTP_API_URI);
        }

        @Test
        @DeployApi({ "/apis/v4/tcp/api-multi-domain.json" })
        void should_connect_to_gateway_with_tls_via_tcp(GatewayDynamicConfig.TcpConfig tcpConfig) {
            var brightHttpClient = createTrustedHttpClient(vertx(), tcpBrightCert);

            // there no api path so we call directly the backend

            assertApiCall(brightHttpClient, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);

            var darkHttpClient = createTrustedHttpClient(vertx(), tcpDarkCert);

            assertApiCall(darkHttpClient, tcpConfig.tcpPort(), DARK_SIDE_FQDN, GATEWAY_TCP_API_URI);
        }
    }

    @Nested
    @GatewayTest
    class ReloadKeyStore extends AbstractKeyStoreTLSTest {

        KeyStoreGenResult replaceKeyStore(String domainName, Path fileToReplace) throws Exception {
            KeyStoreGenResult result = createNewPKCS12KeyStore(domainName);
            Path location = result.location();
            Files.copy(location, fileToReplace, REPLACE_EXISTING);
            return result;
        }

        @Test
        @DeployApi({ "/apis/v4/http/api.json" })
        void should_not_be_able_to_call_on_removed_domains_using_http(GatewayDynamicConfig.HttpConfig httpConfig) throws Exception {
            var genResult = replaceKeyStore(DARK_SIDE_FQDN, httpKeyStoreLocation);

            // now bright side should not available anymore
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertHandshakeError(vertx(), httpBrightCert, BRIGHT_SIDE_FQDN, httpConfig.httpPort());
                });

            // call with new dark certPath
            HttpClient darkClient = createTrustedHttpClient(vertx(), genResult.certs().get(DARK_SIDE_FQDN).toPem());
            assertApiCall(darkClient, httpConfig.httpPort(), DARK_SIDE_FQDN, GATEWAY_HTTP_API_URI);
        }

        @Test
        @DeployApi({ "/apis/v4/tcp/api-multi-domain.json" })
        void should_not_be_able_to_call_on_removed_domains_using_tcp(GatewayDynamicConfig.TcpConfig tcpConfig) throws Exception {
            var genResult = replaceKeyStore(DARK_SIDE_FQDN, tcpKeyStoreLocation);

            // now bright side should not available anymore
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertHandshakeError(vertx(), tcpBrightCert, BRIGHT_SIDE_FQDN, tcpConfig.tcpPort());
                });

            // call with new dark certPath
            HttpClient darkClient = createTrustedHttpClient(vertx(), genResult.certs().get(DARK_SIDE_FQDN).toPem());
            assertApiCall(darkClient, tcpConfig.tcpPort(), DARK_SIDE_FQDN, GATEWAY_TCP_API_URI);
        }
    }

    abstract static class AbstractPemTLSTest extends AbstractTlsTest {

        PemGenResult httpPems;
        PemGenResult tcpPems;

        @Override
        @SneakyThrows
        protected void configureGateway(GatewayConfigurationBuilder config) {
            super.configureGateway(config);
            config.httpSslKeystoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PEM);
            config.set("tcp.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PEM);
            String[] names = { BRIGHT_SIDE_FQDN, DARK_SIDE_FQDN };
            this.httpPems = createNewPEMs(names);
            this.tcpPems = createNewPEMs(names);
            httpBrightCert = httpPems.certificates().get(BRIGHT_SIDE_FQDN).cert();
            httpDarkCert = httpPems.certificates().get(DARK_SIDE_FQDN).cert();
            tcpBrightCert = tcpPems.certificates().get(BRIGHT_SIDE_FQDN).cert();
            tcpDarkCert = tcpPems.certificates().get(DARK_SIDE_FQDN).cert();
            for (int i = 0; i < names.length; i++) {
                config.set("http.ssl.keystore.certificates[%d].cert".formatted(i), httpPems.locations().get(names[i]).certPath());
                config.set("http.ssl.keystore.certificates[%d].key".formatted(i), httpPems.locations().get(names[i]).keyPath());
                config.set("tcp.ssl.keystore.certificates[%d].cert".formatted(i), tcpPems.locations().get(names[i]).certPath());
                config.set("tcp.ssl.keystore.certificates[%d].key".formatted(i), tcpPems.locations().get(names[i]).keyPath());
            }
        }
    }

    @Nested
    @GatewayTest
    class StaticPem extends AbstractPemTLSTest {

        @Test
        @DeployApi({ "/apis/v4/http/api.json" })
        void should_connect_to_gateway_with_tls_using_http(GatewayDynamicConfig.HttpConfig httpConfig) {
            var brightHttpClient = createTrustedHttpClient(vertx(), httpBrightCert);

            assertApiCall(brightHttpClient, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);

            var darkHttpClient = createTrustedHttpClient(vertx(), httpDarkCert);

            assertApiCall(darkHttpClient, httpConfig.httpPort(), DARK_SIDE_FQDN, GATEWAY_HTTP_API_URI);
        }

        @Test
        @DeployApi({ "/apis/v4/tcp/api-multi-domain.json" })
        void should_connect_to_gateway_with_tls_using_tcp(GatewayDynamicConfig.TcpConfig tcpConfig) {
            var brightHttpClient = createTrustedHttpClient(vertx(), tcpBrightCert);

            assertApiCall(brightHttpClient, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);

            var darkHttpClient = createTrustedHttpClient(vertx(), tcpDarkCert);

            assertApiCall(darkHttpClient, tcpConfig.tcpPort(), DARK_SIDE_FQDN, GATEWAY_TCP_API_URI);
        }
    }

    @Nested
    @GatewayTest
    class ReloadPem extends AbstractPemTLSTest {

        PemGenResult replacePem(String domainName, Path certToReplace, Path privateKeyToReplace) throws Exception {
            var result = createNewPEMs(domainName);
            Files.copy(result.locations().get(domainName).certPath(), certToReplace, REPLACE_EXISTING);
            Files.copy(result.locations().get(domainName).keyPath(), privateKeyToReplace, REPLACE_EXISTING);
            return result;
        }

        @Test
        @DeployApi({ "/apis/v4/http/api.json" })
        void should_not_be_able_to_call_on_removed_domains_using_http(GatewayDynamicConfig.HttpConfig httpConfig) throws Exception {
            // generate new pair for dark
            Path originalCertPath = httpPems.locations().get(DARK_SIDE_FQDN).certPath();
            Path originalKeyPath = httpPems.locations().get(DARK_SIDE_FQDN).keyPath();
            var newDark = replacePem(DARK_SIDE_FQDN, originalCertPath, originalKeyPath);

            // it should now fail with old one, then work with new one.
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertHandshakeError(vertx(), httpDarkCert, DARK_SIDE_FQDN, httpConfig.httpPort());
                });

            // call with new dark certPath
            HttpClient darkClient = createTrustedHttpClient(vertx(), newDark.certificates().get(DARK_SIDE_FQDN).cert());
            assertApiCall(darkClient, httpConfig.httpPort(), DARK_SIDE_FQDN, GATEWAY_HTTP_API_URI);
        }

        @Test
        @DeployApi({ "/apis/v4/tcp/api-multi-domain.json" })
        void should_not_be_able_to_call_on_removed_domains_using_tcp(GatewayDynamicConfig.TcpConfig tcpConfig) throws Exception {
            // generate new pair for dark
            Path originalCertPath = tcpPems.locations().get(DARK_SIDE_FQDN).certPath();
            Path originalKeyPath = tcpPems.locations().get(DARK_SIDE_FQDN).keyPath();
            var newDark = replacePem(DARK_SIDE_FQDN, originalCertPath, originalKeyPath);

            // it should now fail with old one, then work with new one.
            await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertHandshakeError(vertx(), tcpDarkCert, DARK_SIDE_FQDN, tcpConfig.tcpPort());
                });

            // call with new dark certPath
            HttpClient darkClient = createTrustedHttpClient(vertx(), newDark.certificates().get(DARK_SIDE_FQDN).cert());
            assertApiCall(darkClient, tcpConfig.tcpPort(), DARK_SIDE_FQDN, GATEWAY_TCP_API_URI);
        }
    }
}
