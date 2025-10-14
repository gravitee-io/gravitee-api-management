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
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.utils.TLSUtils;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.tcp.proxy.TcpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.entrypoint.tcp.proxy.TcpProxyEntrypointConnectorFactory;
import io.vertx.rxjava3.core.Vertx;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume SALA (guillaume.sala at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
public class CrlIntegrationTest extends AbstractGatewayTest {

    String httpCert;
    Path httpClientKeyStorePath;
    Path httpServerTrustStorePath;
    Path crlPath;
    TLSUtils.X509Pair caKeyPair;
    TLSUtils.X509Pair clientX509Pair;

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
        TestHelper.KeyStoreGenResult serverKeystore = createNewPKCS12KeyStore(BRIGHT_SIDE_FQDN);
        this.httpCert = serverKeystore.certs().get(BRIGHT_SIDE_FQDN).toPem();
        var httpKeyStoreLocation = serverKeystore.location().toAbsolutePath();

        this.caKeyPair = TLSUtils.createCA("TestCA");
        this.clientX509Pair = TLSUtils.createCASignedCertificate(caKeyPair, CLIENT_CN);

        this.httpClientKeyStorePath = Files.createTempFile("client_keystore", ".p12");
        KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
        clientKeyStore.load(null, PASSWORD);
        X509Certificate[] chain = new X509Certificate[] { clientX509Pair.certificate().data(), caKeyPair.certificate().data() };
        clientKeyStore.setKeyEntry("client", clientX509Pair.privateKey().data(), PASSWORD, chain);
        try (var out = new FileOutputStream(httpClientKeyStorePath.toFile())) {
            clientKeyStore.store(out, PASSWORD);
        }

        this.httpServerTrustStorePath = Files.createTempFile("ca_truststore", ".p12");
        KeyStore caTrustStore = TLSUtils.createKeyStore("ca", caKeyPair.certificate(), PASSWORD);
        try (var out = new FileOutputStream(httpServerTrustStorePath.toFile())) {
            caTrustStore.store(out, PASSWORD);
        }

        crlPath = Files.createTempFile("crl", ".pem");
        X509CRL emptyCrl = TLSUtils.generateCRL(caKeyPair);
        TLSUtils.writeCrlToPemFile(emptyCrl, crlPath);

        config
            .httpSecured(true)
            .set("http.ssl.sni", true)
            .set("http.ssl.clientAuth", "required")
            .set("http.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .set("http.ssl.keystore.path", httpKeyStoreLocation.toString())
            .set("http.ssl.keystore.password", new String(PASSWORD))
            .set("http.ssl.truststore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .set("http.ssl.truststore.path", httpServerTrustStorePath.toString())
            .set("http.ssl.truststore.password", new String(PASSWORD))
            .set("http.ssl.crl.path", crlPath.toString())
            .enableTcpGateway()
            .set("tcp.ssl.clientAuth", "required")
            .set("tcp.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .set("tcp.ssl.keystore.path", httpKeyStoreLocation.toString())
            .set("tcp.ssl.keystore.password", new String(PASSWORD))
            .set("tcp.ssl.truststore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .set("tcp.ssl.truststore.path", httpServerTrustStorePath.toString())
            .set("tcp.ssl.truststore.password", new String(PASSWORD))
            .set("tcp.ssl.crl.path", crlPath.toString());
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

    @Test
    @DeployApi({ "/apis/v4/http/api.json" })
    void should_accept_request_with_empty_crl_using_http(GatewayDynamicConfig.HttpConfig httpConfig) throws Exception {
        TLSUtils.writeCrlToPemFile(TLSUtils.generateCRL(caKeyPair), crlPath);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                var httpClient = createTrustedHttpClient(vertx(), httpCert, httpClientKeyStorePath);
                assertApiCall(httpClient, httpConfig.httpPort(), BRIGHT_SIDE_FQDN, GATEWAY_HTTP_API_URI);
            });
    }

    @Test
    @DeployApi({ "/apis/v4/http/api.json" })
    void should_reject_request_with_revoked_certificate_using_http(GatewayDynamicConfig.HttpConfig httpConfig) throws Exception {
        TLSUtils.writeCrlToPemFile(TLSUtils.generateCRL(caKeyPair, clientX509Pair.certificate().data()), crlPath);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertHandshakeError(vertx(), httpCert, GATEWAY_HTTP_API_URI, httpConfig.httpPort(), httpClientKeyStorePath);
            });
    }

    @Test
    @DeployApi({ "/apis/v4/tcp/api-multi-domain.json" })
    void should_accept_request_with_empty_crl_using_tcp(GatewayDynamicConfig.TcpConfig tcpConfig) throws Exception {
        TLSUtils.writeCrlToPemFile(TLSUtils.generateCRL(caKeyPair), crlPath);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                var httpClient = createTrustedHttpClient(vertx(), httpCert, httpClientKeyStorePath);
                assertApiCall(httpClient, tcpConfig.tcpPort(), BRIGHT_SIDE_FQDN, GATEWAY_TCP_API_URI);
            });
    }

    @Test
    @DeployApi({ "/apis/v4/tcp/api-multi-domain.json" })
    void should_reject_request_with_revoked_certificate_using_tcp(GatewayDynamicConfig.TcpConfig tcpConfig) throws Exception {
        TLSUtils.writeCrlToPemFile(TLSUtils.generateCRL(caKeyPair, clientX509Pair.certificate().data()), crlPath);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertHandshakeError(vertx(), httpCert, GATEWAY_TCP_API_URI, tcpConfig.tcpPort(), httpClientKeyStorePath);
            });
    }
}
