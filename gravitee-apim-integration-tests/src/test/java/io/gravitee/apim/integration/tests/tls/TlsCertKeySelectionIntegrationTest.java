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

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.tcp.proxy.TcpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.entrypoint.tcp.proxy.TcpProxyEntrypointConnectorFactory;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.rxjava3.core.Vertx;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class TlsCertKeySelectionIntegrationTest {

    abstract static class AbstractTlsSelectionTest extends AbstractGatewayTest {

        String brightCert;
        String darkCert;
        String darkAlias;

        @BeforeEach
        void setUpWiremockTestEndpoint() {
            wiremock.stubFor(get(WIREMOCK_ENDPOINT_URI).willReturn(ok(RESPONSE_FROM_BACKEND)));
        }

        @Override
        @SneakyThrows
        protected void configureGateway(GatewayConfigurationBuilder config) {
            config.httpSecured(true).configureTcpGateway(this.tcpPort());
            KeyStoreGenResult genResult = createNewPKCS12KeyStore(BRIGHT_SIDE_FQDN, DARK_SIDE_FQDN);
            brightCert = genResult.certs().get(BRIGHT_SIDE_FQDN).toPem();
            darkCert = genResult.certs().get(DARK_SIDE_FQDN).toPem();
            darkAlias = genResult.aliases().get(DARK_SIDE_FQDN);
            config
                .set("http.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
                .set("http.ssl.keystore.path", genResult.location().toAbsolutePath().toString())
                .set("http.ssl.keystore.password", new String(PASSWORD));
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

        void shouldFallbackOnBrightCertTest() {
            // We call with another domain name, it should use the fallback on first alias of the keystore => bright cert
            var httpClient = getBean(Vertx.class).createHttpClient(
                new HttpClientOptions()
                    .setSsl(true)
                    .setVerifyHost(false) // given the host is not the good one, we bypass this check
                    .setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(brightCert)))
            );
            assertApiCall(httpClient, this.gatewayPort(), "www.foo.com", GATEWAY_HTTP_API_URI);
        }

        void shouldFallbackOnDefaultAliasTest() {
            // We call with another domain name, it should use the fallback on default alias => dark cert
            var httpClient = getBean(Vertx.class).createHttpClient(
                new HttpClientOptions()
                    .setSsl(true)
                    .setVerifyHost(false) // given the host is not the good one, we bypass this check
                    .setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(darkCert)))
            );
            assertApiCall(httpClient, this.gatewayPort(), "www.foo.com", GATEWAY_HTTP_API_URI);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api.json" })
    class SNIFallbackNoAlias extends AbstractTlsSelectionTest {

        @Override
        protected void configureGateway(GatewayConfigurationBuilder config) {
            super.configureGateway(config);
            config.set("http.ssl.sni", true);
        }

        @Test
        void should_fallback_on_bright_cert() {
            shouldFallbackOnBrightCertTest();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api.json" })
    class SNIFallbackWithAlias extends AbstractTlsSelectionTest {

        @Override
        protected void configureGateway(GatewayConfigurationBuilder config) {
            super.configureGateway(config);
            config.set("http.ssl.sni", true);
            config.set("http.ssl.keystore.defaultAlias", darkAlias);
        }

        @Test
        void should_fallback_on_default_alias() {
            shouldFallbackOnDefaultAliasTest();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api.json" })
    class NoSNIFallbackNoAlias extends AbstractTlsSelectionTest {

        @Test
        void should_fallback_on_first_alias() {
            shouldFallbackOnBrightCertTest();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api.json" })
    class NoSNIFallbackWithAlias extends AbstractTlsSelectionTest {

        @Override
        protected void configureGateway(GatewayConfigurationBuilder config) {
            super.configureGateway(config);
            config.set("http.ssl.keystore.defaultAlias", darkAlias);
        }

        @Test
        void should_fallback_on_default_alias() {
            shouldFallbackOnDefaultAliasTest();
        }
    }
}
