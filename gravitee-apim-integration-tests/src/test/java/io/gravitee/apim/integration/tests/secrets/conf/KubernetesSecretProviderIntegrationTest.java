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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

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
import io.gravitee.secretprovider.kubernetes.KubernetesSecretProvider;
import io.gravitee.secretprovider.kubernetes.KubernetesSecretProviderFactory;
import io.gravitee.secretprovider.kubernetes.config.K8sConfig;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.testcontainers.k3s.K3sContainer;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KubernetesSecretProviderIntegrationTest {

    static final String CERT = """
        -----BEGIN CERTIFICATE-----
        MIIDazCCAlOgAwIBAgIUJjfny3beplZzojjkJ1fhbV1RHD4wDQYJKoZIhvcNAQEL
        BQAwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoM
        GEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yMzA5MDgxMDM3MzVaFw0yNDA4
        MjkxMDM3MzVaMEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEw
        HwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggEiMA0GCSqGSIb3DQEB
        AQUAA4IBDwAwggEKAoIBAQCt3A4qP+9Rl7iv/wx3fi33sVECYJBTpUMouDl9Amu2
        Gi/W5nsbRQY26KenWPr05wrnDlDvsnLxRXbb3ezdwcbFbT8m7Qvec0jId0XhU40m
        b0DUjCs4vQCyAKde/VpJC0soNsc0Wfx9NWAEdRvwfdJJdQ+v75tO2SzuiK460dFo
        rOtwVwLKL3KOD0syifUHEKeDJS6eN3h/N1nM6wI8jnpXoHgN8RJ/2G7SZPyn1rmY
        lEjoX57daAVEtR011nHO97zdncBjfR/iswsfmkhCisbKi5P+Lng9OS3RF5dl30wG
        8tiHIOAn2z0eAQNoyr70oLtCaHjC+SPPuzwAps1gfUf1AgMBAAGjUzBRMB0GA1Ud
        DgQWBBQ3syOvxPbQq4GaYFTjP7EantnBzzAfBgNVHSMEGDAWgBQ3syOvxPbQq4Ga
        YFTjP7EantnBzzAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQB1
        ws1gimBdXMJ00IgrzyZd6nS9roGAbIueWEnnfVsJAuz1kc1WtGzZPDPW7qUHoZNy
        Lcb/xksIsw8MnFhmC++aiB4c+VmNNeqdY+pHVFhgEuCsH/Mm/Obkvw1zImfOmurp
        QZXEdTZ6uQVYPYZ8kyfABJg5bkCWKc++XbtsFQy2H4Xk8tYvABLKrxh3mkkgTypx
        dxDgjT806ZVjxgXdcryMskFX8amsofowzDwU6u8Wo+SW8jloItWv+j5hCR8eiIIz
        29AxHtIJmaiTidz2eHsjfuhSqKgS74ndeJnsdz5ZHRsWoEtu0t/nIrwSclZKrjBq
        VXwOSZSQT3z99f/MsavL
        -----END CERTIFICATE-----
        """;

    static final String KEY = """
        -----BEGIN PRIVATE KEY-----
        MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCt3A4qP+9Rl7iv
        /wx3fi33sVECYJBTpUMouDl9Amu2Gi/W5nsbRQY26KenWPr05wrnDlDvsnLxRXbb
        3ezdwcbFbT8m7Qvec0jId0XhU40mb0DUjCs4vQCyAKde/VpJC0soNsc0Wfx9NWAE
        dRvwfdJJdQ+v75tO2SzuiK460dForOtwVwLKL3KOD0syifUHEKeDJS6eN3h/N1nM
        6wI8jnpXoHgN8RJ/2G7SZPyn1rmYlEjoX57daAVEtR011nHO97zdncBjfR/iswsf
        mkhCisbKi5P+Lng9OS3RF5dl30wG8tiHIOAn2z0eAQNoyr70oLtCaHjC+SPPuzwA
        ps1gfUf1AgMBAAECggEADhqWaZYDL47L1DcwBzeMuhW/2R4FR0vWTWTYgQwjucOZ
        Eulinj00ulqYUyqUPS7LAyB1r2Q+D9WPRVnU/85a9iQdJea/+j1G78BBQny5LB+F
        VljCntkyR75m1X1fCCLq52m+MkCEi5G7ZtErQZCrcPsWmTKqWjSjAPzEiZAA2Wlf
        Z3hemgge3pmASz964TR4Nd1yC6rceEJvAr5d/Ez6MU8mgez9o/ZuaIoi0q4n12NZ
        /rexM9B8rnP93nedNjyy1lCc9+T8x0s7haN/ZjKR3nGj+cp6PCxAgNX18G5shmqR
        6bJrjn0Mu04w2n3bfoG0NNNpf3j06vIP1HNyAuhKlwKBgQDhtzfet3/h68eDJO3m
        oD3oI45vDvesHgIeXPR+BZGsujW6ab1DSUEeZhAgbxooD/NioWZVer/jehgcvJdg
        TUALq63so4Q24DFJp6WdQPU0uLvlajqhykF0SccdFo8iN3xGGbCK8Kb2tHexULaN
        rvPCLZTEjlpPzULUemc70yAVowKBgQDFL7TwMakwiTk4ed26uoru1cth+IOQz1YP
        DoiGvBTU0uvegGCclWxFwkfXfMzqQGpTK2v9EG2afL5CZUnGCSAO2Zq6nTuXpLr4
        GmtosQcJmzA7BDiY86eLDsSCxAQb/5xOqjDIvJR/BZnH7+8duqCWcMqiwYoUdz1n
        qxJCZb6VhwKBgBwI8buL9ypMar9zOslGZeoLYImSxlhucbzrtsJgVrOpfTrmH0fY
        NWpdKuucYRdQw94gReGgGW1boNsQ4Yxoi+fnLvcRaD6YogaP+BYMF2iw+UWJaDbo
        NDEJaN3IC4codRsP3cmkEljaGXPAnqwCauxXVP8E31rCF+bkPSZFFtsZAoGAV1CU
        sneLD67z44ozIOhRdQi+kpdUyt7EoM4yrlbCcqsjPtdh8HRKCWnKHiVpJ6F2c3Wa
        z+hiYDI0nXn0fPi1dV3uIgxVwwRytkIcpbMeBqbtaHSqCzB5VB4p7i2WFD/PmxXJ
        nFnE96onOl2IaIWnbnZrhD5nQkC6tBkQcM5U4ikCgYAUMBYsZJpTnPYojMp6EM9B
        icwZQsuhNFgn+WM2/itFlPH7N/s1cScs4stkS1OzrlzZHLAzOfbqLeTbpNfQM5lE
        utWjVUNvzathT7PMDCxR1VtuNvpAZon5/ResDgimGyr/YvZ5XuriHdudTeAN75TZ
        0LCyEgd6Noz/STJZdPuW+A==
        -----END PRIVATE KEY-----
        """;

    abstract static class AbstractKubernetesTest extends AbstractGatewayTest {

        Path kubeConfigFile;
        K3sContainer k3sServer;

        @AfterEach
        void cleanup() throws IOException {
            k3sServer.close();
            Files.delete(kubeConfigFile);
        }

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            try {
                kubeConfigFile = Files.createTempDirectory(KubernetesSecretProviderIntegrationTest.class.getSimpleName()).resolve(
                    "kube_config.yml"
                );
                // this allows to test if the plugin can be configured with or the other method -D... or gravitee.yml
                if (useSystemProperties()) {
                    configurationBuilder.setSystemProperty("secrets.kubernetes.enabled", true);
                    configurationBuilder.setSystemProperty("secrets.kubernetes.kubeConfigFile", kubeConfigFile.toString());
                } else {
                    configurationBuilder.setYamlProperty("secrets.kubernetes.enabled", true);
                    configurationBuilder.setYamlProperty("secrets.kubernetes.kubeConfigFile", kubeConfigFile.toString());
                }
                setupAdditionalProperties(configurationBuilder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void configureSecretProviders(
            Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
        ) throws Exception {
            addPlugin(secretProviderPlugins);
            startK3s();
            createSecrets();
        }

        abstract void createSecrets() throws IOException, InterruptedException;

        void addPlugin(
            Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
        ) {
            secretProviderPlugins.add(
                SecretProviderBuilder.build(KubernetesSecretProvider.PLUGIN_ID, KubernetesSecretProviderFactory.class, K8sConfig.class)
            );
        }

        final void startK3s() throws IOException {
            k3sServer = KubernetesHelper.getK3sServer();
            k3sServer.start();
            // write config so the secret provider can pick it up
            Files.writeString(kubeConfigFile, k3sServer.getKubeConfigYaml());
        }

        abstract void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder);

        boolean useSystemProperties() {
            return false;
        }
    }

    @Nested
    @GatewayTest
    class DefaultNamespace extends AbstractKubernetesTest {

        String password1 = UUID.randomUUID().toString();
        String password2 = UUID.randomUUID().toString();

        @Override
        public void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder
                .setYamlProperty("test", "secret://kubernetes/test:password")
                .setYamlProperty("foo", "secret://kubernetes/foo:password");
        }

        @Override
        void createSecrets() throws IOException, InterruptedException {
            KubernetesHelper.createSecret(k3sServer, "default", "test", Map.of("password", password1));
            KubernetesHelper.createSecret(k3sServer, "default", "foo", Map.of("password", password2));
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
    class DefaultNamespaceUsingSystemProps extends AbstractKubernetesTest {

        String password1 = UUID.randomUUID().toString();
        String password2 = UUID.randomUUID().toString();

        @Override
        boolean useSystemProperties() {
            return true;
        }

        @Override
        void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            // setting secret refs to yaml as it is not supported in system props
            configurationBuilder
                .setYamlProperty("test", "secret://kubernetes/test:password")
                .setYamlProperty("foo", "secret://kubernetes/foo:password");
        }

        @Override
        public void createSecrets() throws IOException, InterruptedException {
            KubernetesHelper.createSecret(k3sServer, "default", "test", Map.of("password", password1));
            KubernetesHelper.createSecret(k3sServer, "default", "foo", Map.of("password", password2));
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
    class NonDefaultNamespace extends AbstractKubernetesTest {

        String password1 = UUID.randomUUID().toString();
        String password2 = UUID.randomUUID().toString();

        @Override
        void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder
                .setYamlProperty("test", "secret://kubernetes/test:password?namespace=test")
                .setYamlProperty("foo", "secret://kubernetes/foo:password");
        }

        @Override
        public void createSecrets() throws IOException, InterruptedException {
            KubernetesHelper.createNamespace(k3sServer, "test");
            KubernetesHelper.createSecret(k3sServer, "test", "test", Map.of("password", password1));
            // create one more to make sure we don't get the right one by chance
            KubernetesHelper.createSecret(k3sServer, "default", "foo", Map.of("password", password2));
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
    class TLSWithDefaultKeyMap extends AbstractKubernetesTest {

        @Override
        boolean useSystemProperties() {
            return true;
        }

        @Override
        void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.httpSecured(true).httpSslKeystoreType("pem").httpSslSecret("secret://kubernetes/tls-test");
        }

        @Override
        protected void configureHttpClient(HttpClientOptions options) {
            options.setSsl(true).setVerifyHost(false).setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(CERT)));
        }

        @Override
        public void createSecrets() throws IOException, InterruptedException {
            KubernetesHelper.createSecret(k3sServer, "default", "tls-test", Map.of("tls.crt", CERT, "tls.key", KEY), true);
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
    class TLSWithCustomKeyMapAndNamespace extends AbstractKubernetesTest {

        @Override
        void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder
                .httpSecured(true)
                .httpSslSecret("secret://kubernetes/tls-test?namespace=test&keymap=certificate:cert&keymap=private_key:key")
                .httpSslKeystoreType("pem");
        }

        @Override
        protected void configureHttpClient(HttpClientOptions options) {
            options.setSsl(true).setVerifyHost(false).setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(CERT)));
        }

        @Override
        public void createSecrets() throws IOException, InterruptedException {
            KubernetesHelper.createNamespace(k3sServer, "test");
            KubernetesHelper.createSecret(k3sServer, "test", "tls-test", Map.of("cert", CERT, "key", KEY));
        }

        @Test
        void should_be_able_to_call_on_https(HttpClient httpClient) {
            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
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
    class WatchProperty extends AbstractKubernetesTest {

        @Override
        void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.setYamlProperty("test", "secret://kubernetes/test:password?watch");
        }

        @Override
        void createSecrets() throws IOException, InterruptedException {
            KubernetesHelper.createSecret(k3sServer, "default", "test", Map.of("password", "changeme"));
        }

        @Test
        void should_be_able_to_watch_secret() throws IOException, InterruptedException {
            Environment environment = getBean(Environment.class);
            assertThat(environment.getProperty("test")).isEqualTo("changeme");
            KubernetesHelper.updateSecret(k3sServer, "default", "test", Map.of("password", "okiamchanged"), false);
            await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(environment.getProperty("test")).isEqualTo("okiamchanged"));
        }
    }

    @Nested
    @GatewayTest
    class WatchCert extends AbstractKubernetesTest {

        public static final String NEW_CERT = """
            -----BEGIN CERTIFICATE-----
            MIIDazCCAlOgAwIBAgIUFt51mLlSyPnZQNJM9hk8BmJpHsQwDQYJKoZIhvcNAQEL
            BQAwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoM
            GEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yMzA5MjUwNzUzNTNaFw0yNDA5
            MTUwNzUzNTNaMEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEw
            HwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggEiMA0GCSqGSIb3DQEB
            AQUAA4IBDwAwggEKAoIBAQDIARUP2xAVoeW5P5L41m6sgEOOeN/Qs5XIFHUXXBfd
            Mgld/WzJNUW9IaEOqsZYY9QZyveRn5cLfT0Srg2k/tYqsSsIRbsjj/r8IXFMgpbz
            65Fs/WImTmMDeZM6eY2ty5H8DI9hhhNSDsKHS7qnagadLdoc/QovYTsaih3z5DWh
            nade3zeD6xCYe7+JdVaZT0z+I8vl74PQlEjwbR926reqMnC6x0zeWS1WJa8Zzm5H
            dFZQ7X8mjPFld/YDcOkPjkmS3OmO7pOJe8WYUaR3JUj5arI0xMsQZQdVkzcY6K/h
            xxpGADMOnJilcQKn0i8QB0jvg1veogw/Evm/Pk3cYAlFAgMBAAGjUzBRMB0GA1Ud
            DgQWBBTtj8/LQ8ZJFFyskk2ihdN9tYUfwDAfBgNVHSMEGDAWgBTtj8/LQ8ZJFFys
            kk2ihdN9tYUfwDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQAq
            TPgKNgQ1849I4nDcGVI6PUbz68yUm+YkdbWYISWHmEHQl3Z/6RW6QXHVod2QT3rl
            kjRRtJuIgJ9FFPPG5SLqc2XjrLWyMNpdOhJ1MNSaoFOGgz98NY9hUJcA8eSoFM3/
            vkCwQ7LcvybKlyO+RnXyUhyKyFzIoOuRwBg6pxpQ4ehJ8+PQx98O3kzOvKNi5O+n
            4mhcYsZczXHo4keUv9I+F2CvmxIsiDQ1Mnud7loIxRfq+3ahns5Y366ROB3Uk5+C
            /iQvkxYDa72DIXIhl1333z1noKBTn08UOUq2RmK6J/8Cw5bCSVhA4c8aMNgHeOO+
            fcZHGP5FL+Wfgeqew/MP
            -----END CERTIFICATE-----
            """;

        public static final String NEW_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDIARUP2xAVoeW5
            P5L41m6sgEOOeN/Qs5XIFHUXXBfdMgld/WzJNUW9IaEOqsZYY9QZyveRn5cLfT0S
            rg2k/tYqsSsIRbsjj/r8IXFMgpbz65Fs/WImTmMDeZM6eY2ty5H8DI9hhhNSDsKH
            S7qnagadLdoc/QovYTsaih3z5DWhnade3zeD6xCYe7+JdVaZT0z+I8vl74PQlEjw
            bR926reqMnC6x0zeWS1WJa8Zzm5HdFZQ7X8mjPFld/YDcOkPjkmS3OmO7pOJe8WY
            UaR3JUj5arI0xMsQZQdVkzcY6K/hxxpGADMOnJilcQKn0i8QB0jvg1veogw/Evm/
            Pk3cYAlFAgMBAAECggEABsaUnFYnUBhNRE6dT81R8AmjYEUDjho7aY0Z32n5C/8Z
            NqQyhomFvI/SWUEbWZy+L4aDBx2xPAwo4MRhRXT1s/oPE2drXNvQnLCUWkt1Sjux
            kE/wPox6ycAZZwp9rITgQ/n39I2ag3XpDLTZ1Ligkzwdrsw6x1qjqjVCKQRS9c/m
            Zb7mcf5sQK8dE6sGxgQajKJVR8XkcdNZsvqRJJUJJ0uZZg+SzswbcJFr302iya3I
            n5evHsj4gxOfpYDUPhEji/ES9KhnHs6fEZ3oaL5XJhTKyb5VAsuPAELfCDS2xiHt
            Xsa61Wa6FHuayUPypXu4mZeQSX8hcSVcut/lm6NXYQKBgQDtEM5u4eWoKB5Bu44i
            oY5/VXFEYMQWuBOLKgS1boFENC51h5ssdgc/i++hxbBsMAsA0ki4uiBRxWqmbzem
            61OZTfCMn7w6+8K2WyJN+AMOrX3UgRMF3/+a31yqKeCjwBmW8NKWI6UUuomyk79X
            YM9zEv738dwaMuOfNvfqf6ZfZQKBgQDX+n6lEwhZPEr0j+MslWK1iIqlBn13rauL
            YPgCW97F1c16gQiSKwvxHQsFuLeyF86I2OszRcI65zTrmOVvHxVJ5noNdt29VRxD
            PeEhji5Cqj/XU86CRSvh6wAQr8J+WzDBbasbXE1Uyttn2H4y+6Zmll2a42oV3+5z
            5n28OjoUYQKBgQDKjoqkSZYOKUE5DwVyZ7I28I2YTEof02a3iM5/K4199kwgFh8r
            TSoCTRISmrSUrDQqnoKOfFJzLAhlbzARCo/itKqtrSqLB2SmpZXZIumR2AFk2mij
            o3JmJSWrK58Kq2/x/ZEhwhEidgSDtiROh6SZgYij2F8lb4f4GWKZVjqU1QKBgCrU
            HbEWaR1o+2Qr9Fyu8vgUr4myE8dbxRzKiePN3AtXLnwQgmaZ6rBRlhH4Y1UJq4cv
            nNR6DN4pYzElDLpQa6RP8/wfijE1Y3liF/bTfxDxOd+1WsoydVVDiKvGbscnxi9V
            VA0E7MDXyVJ6d+wcQw8s2jsQxcS4t0x8dIqS8VmBAoGAOmjui7hN8mRFEpuc//BD
            qqpVXwar/u3VJjvNzqVppejDEhdMTy4cM7/GyYQl5wwMMvX8cd5OV9+n7PkT90Qm
            xFPo1KxXhriTCglXsvAzGKLjfNJklOpaFfXbszLU0b7JaVXq1MByCv2zU/llWIVJ
            rW5wPvM1jnDq1ArTCNwORtc=
            -----END PRIVATE KEY-----
            """;

        @Override
        void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder
                .httpSecured(true)
                .httpSslKeystoreType("pem")
                .httpSslSecret("secret://kubernetes/tls-test")
                .setSystemProperty("http.ssl.keystore.watch", true);
        }

        @Override
        void createSecrets() throws IOException, InterruptedException {
            KubernetesHelper.createSecret(k3sServer, "default", "tls-test", Map.of("tls.crt", CERT, "tls.key", KEY), true);
        }

        @Override
        protected void configureHttpClient(HttpClientOptions options) {
            options.setSsl(true).setVerifyHost(false).setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(CERT)));
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
        void should_be_able_to_call_on_https_then_not(HttpClient httpClient) throws IOException, InterruptedException {
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

            KubernetesHelper.updateSecret(k3sServer, "default", "tls-test", Map.of("tls.crt", NEW_CERT, "tls.key", NEW_KEY), true);

            // create a new client to avoid sharing the connection
            var newHttpClient = getBean(Vertx.class).createHttpClient(
                new HttpClientOptions()
                    .setDefaultPort(this.gatewayPort())
                    .setDefaultHost("localhost")
                    .setSsl(true)
                    .setVerifyHost(false)
                    .setPemTrustOptions(new PemTrustOptions().addCertValue(Buffer.buffer(CERT)))
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
    class Errors extends AbstractKubernetesTest {

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
            final GraviteeYamlPropertySource graviteeProperties = getGraviteeYamlProperties();
            if (graviteeProperties != null) {
                graviteeProperties.getSource().put("missing", "secret://kubernetes/test:pass");
                graviteeProperties.getSource().put("missing2", "secret://kubernetes/test:pass?namespace=test");
                graviteeProperties.getSource().put("no_plugin", "secret://foo/test:pass");
            }
        }

        @Test
        void should_fail_resolve_secret() {
            final Environment environment = getBean(Environment.class);
            assertThatCode(() -> environment.getProperty("missing")).isInstanceOf(Exception.class);
            assertThatCode(() -> environment.getProperty("missing2")).isInstanceOf(Exception.class);
            assertThat(environment.getProperty("no_plugin")).isEqualTo("secret://foo/test:pass"); // not recognized as a secret does not return value
        }
    }
}
