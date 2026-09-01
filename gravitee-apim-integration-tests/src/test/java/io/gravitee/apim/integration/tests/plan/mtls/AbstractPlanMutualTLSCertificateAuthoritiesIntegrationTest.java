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
package io.gravitee.apim.integration.tests.plan.mtls;

import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configureTrustedHttpClient;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.plan.PlanHelper;
import io.gravitee.common.security.CertificateUtils;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.security.core.SubscriptionTrustStoreLoaderManager;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.mtls.MtlsPolicy;
import io.gravitee.policy.mtls.configuration.MtlsPolicyConfiguration;
import io.vertx.core.http.HttpClientOptions;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ParameterContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Base for the APIM-14863 non-regression tests: what the gateway advertises in the TLS
 * {@code CertificateRequest.certificate_authorities} when an mTLS plan subscription has registered a client
 * certificate at runtime.
 *
 * <p>There is no server-side API to read that field back, but JSSE hands the decoded list to the <em>client</em> key
 * manager, so an instrumented client captures exactly what the gateway sent. The client deliberately presents no
 * certificate: the point of the ticket is what a caller with no client certificate — a consumer of the other,
 * non-mTLS APIs of the same listener — gets to learn.
 *
 * <p>{@code clientAuth} is {@code request} so the handshake completes and the assertion cannot race a connection
 * reset. The advertised list is identical under {@code required}: it is built from the trust store, not from the
 * client auth mode.
 */
@DeployApi(value = { "/apis/plan/v4-proxy-api.json" })
abstract class AbstractPlanMutualTLSCertificateAuthoritiesIntegrationTest extends AbstractGatewayTest {

    /** The leaf a subscription registers at runtime. Not a certificate authority, and the DN the ticket reported. */
    protected static final String SUBSCRIPTION_LEAF = "plans/mtls/client.cer";

    /** Used here as the statically configured trust store, so the two origins can be told apart. */
    protected static final String CONFIGURED_TRUST_STORE = "plans/mtls/client2.cer";

    /**
     * Deliberately shorter than the {@code @Timeout} on the test methods, so a stalled read fails with a
     * {@link java.net.SocketTimeoutException} naming the line rather than with JUnit's generic timeout.
     */
    protected static final Duration HANDSHAKE_TIMEOUT = Duration.ofSeconds(10);

    protected SubscriptionTrustStoreLoaderManager subscriptionTrustStoreLoaderManager;

    /**
     * @return the value of {@code http.ssl.sendClientCertificateAuthorities}, or {@code null} to leave it unset.
     */
    protected abstract Boolean sendClientCertificateAuthorities();

    @Override
    public void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("mtls", PolicyBuilder.build("mtls", MtlsPolicy.class, MtlsPolicyConfiguration.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        configurePlans((Api) api.getDefinition(), Set.of("mtls"));
    }

    @Override
    protected void configureHttpClient(
        HttpClientOptions options,
        GatewayDynamicConfig.Config gatewayConfig,
        ParameterContext parameterContext
    ) {
        configureTrustedHttpClient(options, gatewayConfig.httpPort(), parameterContext.findAnnotation(WithCert.class).isPresent());
    }

    @Override
    protected void configureGateway(GatewayConfigurationBuilder config) {
        config
            .httpSecured(true)
            .set("http.ssl.clientAuth", "request")
            .set("http.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED)
            // a statically configured trust store, so the two origins can be told apart in the advertised list
            .set("http.ssl.truststore.type", KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .set("http.ssl.truststore.path", getUrl(CONFIGURED_TRUST_STORE).getPath());

        Boolean sendAuthorities = sendClientCertificateAuthorities();
        if (sendAuthorities != null) {
            config.set("http.ssl.sendClientCertificateAuthorities", sendAuthorities);
        }
    }

    @BeforeEach
    void setUpSubscriptionTrustStore() {
        subscriptionTrustStoreLoaderManager = getBean(SubscriptionTrustStoreLoaderManager.class);
        // Cheat to use the real SubscriptionTrustStoreLoaderManager instance with SubscriptionService mock
        final SubscriptionCacheService subscriptionService = (SubscriptionCacheService) getBean(SubscriptionService.class);
        when(subscriptionService.getByApiAndSecurityToken(any(), any(), any())).thenCallRealMethod();
        ReflectionTestUtils.setField(subscriptionService, "subscriptionTrustStoreLoaderManager", subscriptionTrustStoreLoaderManager);
    }

    /**
     * Fakes the sync process of an accepted mTLS subscription, which is what puts an application leaf in the
     * gateway trust store at runtime.
     */
    @SneakyThrows
    protected Subscription registerSubscription() {
        final Subscription subscription = new Subscription();
        subscription.setApi("v4-proxy-api");
        subscription.setApplication("application-id");
        subscription.setId("subscription-id");
        subscription.setPlan(PlanHelper.PLAN_MTLS_ID);
        subscription.setClientCertificate(
            Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(getUrl(SUBSCRIPTION_LEAF).getPath())))
        );
        subscriptionTrustStoreLoaderManager.registerSubscription(subscription, Set.of());
        // registerSubscription swallows MalformedCertificateException and returns nothing, so a certificate that
        // failed to load would leave every assertion below vacuously true. Check it really made it in.
        assertThat(
            subscriptionTrustStoreLoaderManager.getByCertificate(
                subscription.getApi(),
                subscription.getPlan(),
                CertificateUtils.generateThumbprint(readCertificate(SUBSCRIPTION_LEAF), "SHA-256")
            )
        )
            .as("the subscription certificate never reached the gateway trust store")
            .isPresent();
        return subscription;
    }

    @SneakyThrows
    private static X509Certificate readCertificate(String resource) {
        try (var stream = Files.newInputStream(Paths.get(getUrl(resource).getPath()))) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(stream);
        }
    }

    /**
     * Subject DN of a test certificate, in the same RFC 2253 form as the principals decoded from the handshake.
     */
    protected static String subjectDn(String resource) {
        return readCertificate(resource).getSubjectX500Principal().getName();
    }

    /**
     * Opens a raw TLS connection to the gateway with a client that holds no certificate, and returns the
     * authorities the gateway asked for.
     */
    protected List<String> advertisedCertificateAuthorities(int gatewayPort, String protocol) throws Exception {
        CapturingKeyManager keyManager = new CapturingKeyManager();
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(new KeyManager[] { keyManager }, new TrustManager[] { new TrustEverything() }, null);

        try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket("localhost", gatewayPort)) {
            // without this a gateway that accepts the connection and never answers hangs the whole CI job instead
            // of failing the test
            socket.setSoTimeout((int) HANDSHAKE_TIMEOUT.toMillis());
            socket.setEnabledProtocols(new String[] { protocol });
            socket.startHandshake();
            // TLS 1.3 sends the CertificateRequest after the client considers the handshake done, so exchange
            // application data to make sure it has been processed before asserting
            socket.getOutputStream().write("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes());
            socket.getOutputStream().flush();
            socket.getInputStream().read();
        }
        // an empty list is only meaningful if the gateway did ask for a certificate: "asked for none" and "never
        // asked" would otherwise be indistinguishable, and the assertions below would hold for both
        assertThat(keyManager.wasAskedForACertificate())
            .as("the gateway did not send a CertificateRequest, so nothing was advertised to capture")
            .isTrue();
        return keyManager.sentAuthorities();
    }

    /**
     * Records the {@code certificate_authorities} the gateway sent, then declines to present a certificate.
     */
    private static class CapturingKeyManager extends X509ExtendedKeyManager {

        private volatile Principal[] sentAuthorities;
        private volatile boolean askedForACertificate;

        boolean wasAskedForACertificate() {
            return askedForACertificate;
        }

        List<String> sentAuthorities() {
            return sentAuthorities == null ? List.of() : Arrays.stream(sentAuthorities).map(Principal::getName).toList();
        }

        @Override
        public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
            this.sentAuthorities = issuers == null ? new Principal[0] : Arrays.copyOf(issuers, issuers.length);
            this.askedForACertificate = true;
            return null;
        }

        @Override
        public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
            return chooseClientAlias(keyTypes, issuers, null);
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return new X509Certificate[0];
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return null;
        }
    }

    /**
     * The gateway certificate is self-signed here; the client side is not what is under test.
     */
    private static class TrustEverything extends X509ExtendedTrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {}

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
