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
package io.gravitee.apim.integration.tests.secrets.conf;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.integration.tests.secrets.SecuredVaultContainer.TEST_POLICY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Auth;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.secrets.SecuredVaultContainer;
import io.gravitee.node.container.spring.env.GraviteeYamlPropertySource;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecuredVaultSecretProviderIntegrationTest extends AbstractSecuredVaultSecretProviderTest {

    @Nested
    @GatewayTest
    class DefaultNamespace extends AbstractGatewayVaultTest {

        String password1 = UUID.randomUUID().toString();
        String password2 = UUID.randomUUID().toString();

        @Override
        public void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder
                .setYamlProperty("foo", "secret://vault/secret/foo:password")
                .setYamlProperty("bar", "secret://vault/secret/bar:password");
        }

        @Override
        void createSecrets() throws VaultException {
            writeSecret("secret/foo", Map.of("password", password1));
            writeSecret("secret/bar", Map.of("password", password2));
        }

        @Test
        void should_be_able_to_resolve_secret() {
            Environment environment = getBean(Environment.class);
            assertThat(environment.getProperty("foo")).isEqualTo(password1);
            assertThat(environment.getProperty("bar")).isEqualTo(password2);
        }
    }

    @Nested
    @GatewayTest
    class DefaultNamespaceAppRoleAuth extends DefaultNamespace {

        @SneakyThrows
        @Override
        protected Map<String, Object> authConfig(SecuredVaultContainer vaultContainer) {
            var appRoleIDs = vaultContainer.newAppRoleSecretId();
            return Map.of(
                "secrets.vault.auth.method",
                "approle",
                "secrets.vault.auth.config.roleId",
                appRoleIDs.roleId(),
                "secrets.vault.auth.config.secretId",
                appRoleIDs.secretId()
            );
        }
    }

    @Nested
    @GatewayTest
    class DefaultNamespaceNonRootTokenAuth extends DefaultNamespace {

        @SneakyThrows
        @Override
        protected Map<String, Object> authConfig(SecuredVaultContainer vaultContainer) {
            String token = rootVault.auth().createToken(new Auth.TokenRequest().polices(List.of(TEST_POLICY_NAME))).getAuthClientToken();
            return Map.of("secrets.vault.auth.method", "token", "secrets.vault.auth.config.token", token);
        }
    }

    @Nested
    @GatewayTest
    class DefaultNamespaceCertAuth extends DefaultNamespace {

        @Override
        protected Map<String, Object> authConfig(SecuredVaultContainer vaultContainer) {
            return Map.of(
                "secrets.vault.auth.method",
                "cert",
                "secrets.vault.auth.config.format",
                "pem",
                "secrets.vault.auth.config.cert",
                clientCertAndKey.cert(),
                "secrets.vault.auth.config.key",
                clientCertAndKey.privateKey()
            );
        }
    }

    @Nested
    @GatewayTest
    class TLSWithDefaultKeyMap extends AbstractGatewayVaultTest {

        private SSLUtils.SSLPairs sslPairs;

        @Override
        boolean useSystemProperties() {
            return super.useSystemProperties();
        }

        @Override
        void createSecrets() throws VaultException, IOException {
            sslPairs = SSLUtils.createPairs();
            writeSecret("secret/tls-test", Map.of("certificate", sslPairs.cert(), "private_key", sslPairs.privateKey()));
        }

        @Override
        void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.httpSecured(true).httpSslKeystoreType("pem").httpSslSecret("secret://vault/secret/tls-test");
        }

        @Override
        protected void configureHttpClient(HttpClientOptions options) {
            options.setSsl(true).setVerifyHost(false).setTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(sslPairs.cert())));
        }

        @Test
        void should_be_able_to_call_on_https(HttpClient httpClient) {
            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .doOnError(Throwable::printStackTrace)
                .flatMap(response -> {
                    // just asserting we get a response (hence no SSL errors), no need for an API.
                    assertThat(response.statusCode()).isEqualTo(404);
                    return response.body();
                })
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete();
        }
    }

    @Nested
    @GatewayTest
    class WatchProperty extends AbstractGatewayVaultTest {

        @Override
        public void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder
                .setYamlProperty("watched", "secret://vault/secret/test:password?watch")
                .setYamlProperty("secrets.vault.watch.enabled", true)
                .setYamlProperty("secrets.vault.watch.pollIntervalSec", "1");
        }

        @Override
        void createSecrets() throws VaultException {
            writeSecret("secret/test", Map.of("password", "changeme"));
        }

        @Test
        void should_be_able_to_watch_secret() throws VaultException {
            Environment environment = getBean(Environment.class);
            assertThat(environment.getProperty("watched")).isEqualTo("changeme");
            writeSecret("secret/test", Map.of("password", "okiamchanged"));
            await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(environment.getProperty("watched")).isEqualTo("okiamchanged"));
        }
    }

    @Nested
    @GatewayTest
    class WatchCert extends AbstractGatewayVaultTest {

        private SSLUtils.SSLPairs sslPairs;

        @Override
        boolean useSystemProperties() {
            return true;
        }

        @Override
        void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder
                .httpSecured(true)
                .httpSslKeystoreType("pem")
                .httpSslSecret("secret://vault/secret/tls-test?keymap=certificate:crt&keymap=private_key:key")
                .setSystemProperty("http.ssl.keystore.watch", true)
                .setSystemProperty("secrets.vault.watch.enabled", true)
                .setSystemProperty("secrets.vault.watch.pollIntervalSec", "1");
        }

        @Override
        void createSecrets() throws VaultException, IOException {
            sslPairs = SSLUtils.createPairs();
            writeSecret("secret/tls-test", Map.of("crt", sslPairs.cert(), "key", sslPairs.privateKey()));
        }

        @Override
        protected void configureHttpClient(HttpClientOptions options) {
            options.setSsl(true).setVerifyHost(false).setTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(sslPairs.cert())));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        @Test
        @DeployApi({ "/apis/v4/http/api.json" })
        void should_be_able_to_call_on_https_then_not(HttpClient httpClient) throws VaultException, IOException {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // just asserting we get a response (hence no SSL errors)
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .flatMapCompletable(body -> {
                    assertThat(body).hasToString("response from backend");
                    return Completable.complete();
                })
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertComplete();

            var newSSLPairs = SSLUtils.createPairs();
            writeSecret("secret/tls-test", Map.of("crt", newSSLPairs.cert(), "key", newSSLPairs.privateKey()));

            // create a new client to avoid sharing the connection
            var newHttpClient = getBean(Vertx.class).createHttpClient(
                new HttpClientOptions()
                    .setDefaultPort(this.gatewayPort())
                    .setDefaultHost("localhost")
                    .setSsl(true)
                    .setVerifyHost(false)
                    .setTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(sslPairs.cert())))
            );

            await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                    newHttpClient
                        .rxRequest(HttpMethod.GET, "/test")
                        .flatMap(HttpClientRequest::rxSend)
                        .test()
                        .awaitDone(2, TimeUnit.SECONDS)
                        .assertError(SSLHandshakeException.class)
                );
        }
    }

    @Nested
    @GatewayTest
    class Errors extends AbstractGatewayVaultTest {

        @Override
        void createSecrets() {
            // no op
        }

        @Override
        public void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            // We can't add invalid secret here unless the gateway will fail to start.
        }

        @BeforeAll
        void setupAdditionalSecrets() {
            ConfigurableEnvironment environment = (ConfigurableEnvironment) getBean(Environment.class);
            GraviteeYamlPropertySource graviteeProperties = (GraviteeYamlPropertySource) environment
                .getPropertySources()
                .get("graviteeYamlConfiguration");
            if (graviteeProperties != null) {
                graviteeProperties.getSource().put("missing", "secret://vault/test:pass");
                graviteeProperties.getSource().put("missing2", "secret://vault/test:pass?namespace=test");
                graviteeProperties.getSource().put("no_plugin", "secret://foo/test:pass");
            }
        }

        @Test
        void should_fail_resolve_secret() {
            Environment environment = getBean(Environment.class);
            assertThatCode(() -> environment.getProperty("missing")).isInstanceOf(Exception.class);
            assertThatCode(() -> environment.getProperty("missing2")).isInstanceOf(Exception.class);
            assertThat(environment.getProperty("no_plugin")).isEqualTo("secret://foo/test:pass"); // not recognized as a secret does not return value
        }
    }
}
