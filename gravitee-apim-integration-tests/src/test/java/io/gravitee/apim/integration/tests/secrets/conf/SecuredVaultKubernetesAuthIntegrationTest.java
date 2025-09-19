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

import static io.gravitee.apim.integration.tests.secrets.SecuredVaultContainer.TESTROLE;
import static org.assertj.core.api.Assertions.assertThat;

import com.dajudge.kindcontainer.K3sContainer;
import io.github.jopenlibs.vault.VaultException;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.integration.tests.secrets.KubernetesHelper;
import io.gravitee.apim.integration.tests.secrets.SecuredVaultContainer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.core.env.Environment;
import org.testcontainers.lifecycle.Startables;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecuredVaultKubernetesAuthIntegrationTest extends AbstractSecuredVaultSecretProviderTest {

    private static K3sContainer kubeContainer;

    @BeforeAll
    static void createKubeContainer() throws IOException, InterruptedException {
        kubeContainer = new K3sContainer() {
            @SneakyThrows
            @Override
            public void start() {
                // copied the generic container start to dodge k3s start()
                // that does not let configure --tls-san the way it is required for this test
                if (getContainerId() != null) {
                    return;
                }
                Startables.deepStart(dependencies).get();
                dockerClient.authConfig();
                doStart();
            }
        };
        kubeContainer
            .withCommand(
                "server",
                "--disable=traefik",
                // we need vault to call the host, so adding this to list of SAN so it can happen
                "--tls-san=" + kubeContainer.getHost() + ",host.docker.internal",
                String.format("--service-node-port-range=%d-%d", 30000, 32767)
            )
            .start();

        // create vault service account with expected roles
        // and create gravitee service account
        KubernetesHelper.apply(kubeContainer, "src/test/resources/vault/kube-auth/resources.yaml");

        // write config so the secret provider can pick it up
        Path kubeConfigFile = Files.createTempDirectory(KubernetesSecretProviderIntegrationTest.class.getSimpleName()).resolve(
            "kube_config.yml"
        );
        Files.writeString(kubeConfigFile, kubeContainer.getKubeconfig());

        // get the reviewer token from vault service account
        String vaultToken = KubernetesHelper.createToken(kubeContainer, "vault");

        // enable and configure kubernetes
        // add a test associated with the policy used in other tests and the gravitee service account
        vaultContainer.setupKubernetesRoleAuth(kubeConfigFile, "gravitee", vaultToken, "host.docker.internal");
    }

    @Nested
    @GatewayTest
    class DefaultNamespaceKubernetesAuth extends AbstractGatewayVaultTest {

        String password1 = UUID.randomUUID().toString();
        String password2 = UUID.randomUUID().toString();

        @SneakyThrows
        @Override
        protected Map<String, Object> authConfig(SecuredVaultContainer vaultContainer) {
            String graviteeKubeAuthToken = KubernetesHelper.createToken(kubeContainer, "gravitee");
            Path kubeAuthTokenPath = Files.createTempDirectory(KubernetesSecretProviderIntegrationTest.class.getSimpleName()).resolve(
                "token"
            );
            Files.writeString(kubeAuthTokenPath, graviteeKubeAuthToken);

            return Map.of(
                "secrets.vault.auth.method",
                "kubernetes",
                "secrets.vault.auth.config.role",
                TESTROLE,
                "secrets.vault.auth.config.tokenPath",
                kubeAuthTokenPath.toString()
            );
        }

        @Override
        void createSecrets() throws VaultException {
            writeSecret("secret/foo", Map.of("password", password1));
            writeSecret("secret/bar", Map.of("password", password2));
        }

        @Override
        public void setupAdditionalProperties(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder
                .setYamlProperty("foo", "secret://vault/secret/foo:password")
                .setYamlProperty("bar", "secret://vault/secret/bar:password");
        }

        @Test
        void should_be_able_to_resolve_secret() {
            Environment environment = getBean(Environment.class);
            assertThat(environment.getProperty("foo")).isEqualTo(password1);
            assertThat(environment.getProperty("bar")).isEqualTo(password2);
        }
    }
}
