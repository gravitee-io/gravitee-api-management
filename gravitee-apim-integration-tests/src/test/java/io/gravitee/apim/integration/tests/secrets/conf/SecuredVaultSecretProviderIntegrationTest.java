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
import static io.gravitee.apim.integration.tests.secrets.conf.SecuredVaultContainer.TEST_POLICY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.graviteesource.secretprovider.hcvault.HCVaultSecretProvider;
import com.graviteesource.secretprovider.hcvault.HCVaultSecretProviderFactory;
import com.graviteesource.secretprovider.hcvault.config.manager.VaultConfig;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Auth;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderBuilder;
import io.gravitee.node.api.secrets.SecretManagerConfiguration;
import io.gravitee.node.api.secrets.SecretProviderFactory;
import io.gravitee.node.container.spring.env.GraviteeYamlPropertySource;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecuredVaultSecretProviderIntegrationTest {

    static SecuredVaultContainer vaultContainer;
    static Vault rootVault;
    private static SSLUtils.SSLPairs clientCertAndKey;

    @AfterAll
    static void cleanup() {
        vaultContainer.close();
    }

    @BeforeAll
    static void createVaultContainer() throws IOException, InterruptedException, VaultException {
        vaultContainer = new SecuredVaultContainer();
        vaultContainer.start();
        vaultContainer.initAndUnsealVault();
        vaultContainer.loginAndLoadTestPolicy();
        vaultContainer.setEngineVersions();
        vaultContainer.setupUserPassAuth();
        vaultContainer.setupAppRoleAuth();
        clientCertAndKey = SSLUtils.createPairs();
        vaultContainer.setupCertAuth(clientCertAndKey.cert());
        rootVault = vaultContainer.getRootVault();
    }

    static void addPlugin(
        Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
    ) {
        secretProviderPlugins.add(
            SecretProviderBuilder.build(HCVaultSecretProvider.PLUGIN_ID, HCVaultSecretProviderFactory.class, VaultConfig.class)
        );
    }

    abstract static class AbstractVaultTest extends AbstractGatewayTest {

        final Set<String> createSecrets = new HashSet<>();

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            try {
                if (useSystemProperties()) {
                    configurationBuilder.setSystemProperty("secrets.vault.enabled", true);
                    vaultInstanceConfig(vaultContainer).forEach(configurationBuilder::setSystemProperty);
                    authConfig(vaultContainer).forEach(configurationBuilder::setSystemProperty);
                } else {
                    configurationBuilder.setYamlProperty("secrets.vault.enabled", true);
                    vaultInstanceConfig(vaultContainer).forEach(configurationBuilder::setYamlProperty);
                    authConfig(vaultContainer).forEach(configurationBuilder::setYamlProperty);
                }
                setupAdditionalProperties(configurationBuilder);
            } catch (IOException | InterruptedException | VaultException e) {
                throw new RuntimeException(e);
            }
        }

        boolean useSystemProperties() {
            return false;
        }

        Map<String, Object> vaultInstanceConfig(SecuredVaultContainer vaultContainer) {
            return Map.of(
                "secrets.vault.host",
                vaultContainer.getHost(),
                "secrets.vault.port",
                vaultContainer.getPort(),
                "secrets.vault.ssl.enabled",
                true,
                "secrets.vault.ssl.format",
                "pemfile",
                "secrets.vault.ssl.file",
                SecuredVaultContainer.CERT_PEMFILE
            );
        }

        protected Map<String, Object> authConfig(SecuredVaultContainer vaultContainer)
            throws IOException, InterruptedException, VaultException {
            return Map.of(
                "secrets.vault.auth.method",
                "userpass",
                "secrets.vault.auth.config.username",
                SecuredVaultContainer.USER_ID,
                "secrets.vault.auth.config.password",
                SecuredVaultContainer.PASSWORD
            );
        }

        abstract void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder);

        @Override
        public void configureSecretProviders(
            Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
        ) throws Exception {
            addPlugin(secretProviderPlugins);
            createSecrets();
        }

        abstract void createSecrets() throws IOException, InterruptedException, VaultException;

        final void writeSecret(String path, Map<String, Object> data) throws VaultException {
            LogicalResponse write = rootVault.logical().write(path, data);
            assertThat(write.getRestResponse().getStatus()).isLessThan(300);
            createSecrets.add(path);
        }

        @AfterEach
        final void cleanSecrets() throws VaultException {
            for (String path : createSecrets) {
                rootVault.logical().delete(path);
            }
        }
    }

    @Nested
    @GatewayTest
    class DefaultNamespace extends AbstractVaultTest {

        String password1 = UUID.randomUUID().toString();
        String password2 = UUID.randomUUID().toString();

        @Override
        public void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder
                .setYamlProperty("test", "secret://vault/secret/test:password")
                .setYamlProperty("foo", "secret://vault/secret/foo:password");
        }

        @Override
        void createSecrets() throws VaultException {
            writeSecret("secret/test", Map.of("password", password1));
            writeSecret("secret/foo", Map.of("password", password2));
        }

        @Test
        void should_be_able_to_resolve_secret() {
            Environment environment = getBean(Environment.class);
            assertThat(environment.getProperty("test")).isEqualTo(password1);
            assertThat(environment.getProperty("foo")).isEqualTo(password2);
        }
    }

    @Nested
    @GatewayTest
    class DefaultNamespaceAppRoleAuth extends DefaultNamespace {

        @Override
        protected Map<String, Object> authConfig(SecuredVaultContainer vaultContainer)
            throws IOException, InterruptedException, VaultException {
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

        @Override
        protected Map<String, Object> authConfig(SecuredVaultContainer vaultContainer) throws VaultException {
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
    class TLSWithDefaultKeyMap extends AbstractVaultTest {

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
            options
                .setSsl(true)
                .setVerifyHost(false)
                .setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(sslPairs.cert())));
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
    class WatchProperty extends AbstractVaultTest {

        @Override
        public void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder
                .setYamlProperty("test", "secret://vault/secret/test:password?watch")
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
            assertThat(environment.getProperty("test")).isEqualTo("changeme");
            writeSecret("secret/test", Map.of("password", "okiamchanged"));
            await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(environment.getProperty("test")).isEqualTo("okiamchanged"));
        }
    }

    @Nested
    @GatewayTest
    class WatchCert extends AbstractVaultTest {

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
            options
                .setSsl(true)
                .setVerifyHost(false)
                .setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(sslPairs.cert())));
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
                    .setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(sslPairs.cert())))
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
    class Errors extends AbstractVaultTest {

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
