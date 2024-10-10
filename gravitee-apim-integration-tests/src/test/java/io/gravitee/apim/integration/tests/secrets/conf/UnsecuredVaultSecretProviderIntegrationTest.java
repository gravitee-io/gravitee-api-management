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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.node.api.secrets.SecretManagerConfiguration;
import io.gravitee.node.api.secrets.SecretProviderFactory;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.Container;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 *
 * Using the test containers vaul that is unsecured by default to test our plugin is able to connect to an unsecured instance too.
 */

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UnsecuredVaultSecretProviderIntegrationTest {

    private static final String VAULT_TOKEN = UUID.randomUUID().toString();
    private static org.testcontainers.vault.VaultContainer<?> vaultContainer;
    private static String token;

    @BeforeAll
    static void startAndConfigure() throws IOException, InterruptedException {
        vaultContainer =
            new org.testcontainers.vault.VaultContainer<>(SecuredVaultContainer.HASHICORP_VAULT_IMAGE)
                .withVaultToken(VAULT_TOKEN)
                .withInitCommand("kv put secret/test top_secret=thatWillRemainOurDirtyLittleSecret");
        vaultContainer.start();
        // create a renewable token so the plugin does not start panicking
        Container.ExecResult execResult = vaultContainer.execInContainer(
            "vault",
            "token",
            "create",
            "-pollInterval=10m",
            "-field",
            "token"
        );
        token = execResult.getStdout();
    }

    @AfterAll
    static void cleanup() {
        vaultContainer.close();
    }

    @GatewayTest
    @Nested
    class Simple extends AbstractGatewayTest {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.setYamlProperty("secrets.vault.enabled", true);
            configurationBuilder.setYamlProperty("secrets.vault.host", vaultContainer.getHost());
            configurationBuilder.setYamlProperty("secrets.vault.port", vaultContainer.getMappedPort(8200));
            configurationBuilder.setYamlProperty("secrets.vault.ssl.enabled", false);
            configurationBuilder.setYamlProperty("secrets.vault.auth.method", "token");
            configurationBuilder.setYamlProperty("secrets.vault.auth.config.token", token);
            configurationBuilder.setYamlProperty("test", "secret://vault/secret/test:top_secret");
        }

        @Override
        public void configureSecretProviders(
            Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
        ) {
            SecuredVaultSecretProviderIntegrationTest.addPlugin(secretProviderPlugins);
        }

        @Test
        void should_be_able_to_resolve_secret() {
            Environment environment = getBean(Environment.class);
            assertThat(environment.getProperty("test")).isEqualTo("thatWillRemainOurDirtyLittleSecret");
        }
    }
}
