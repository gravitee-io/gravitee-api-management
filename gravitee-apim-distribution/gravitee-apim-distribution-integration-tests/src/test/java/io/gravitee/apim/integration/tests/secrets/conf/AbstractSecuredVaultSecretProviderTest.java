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

import com.graviteesource.secretprovider.hcvault.HCVaultSecretProvider;
import com.graviteesource.secretprovider.hcvault.HCVaultSecretProviderFactory;
import com.graviteesource.secretprovider.hcvault.config.manager.VaultConfig;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderBuilder;
import io.gravitee.apim.integration.tests.secrets.SecuredVaultContainer;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractSecuredVaultSecretProviderTest {

    static SecuredVaultContainer vaultContainer;
    static Vault rootVault;
    static SSLUtils.SSLPairs clientCertAndKey;

    @AfterAll
    static void cleanup() {
        vaultContainer.close();
    }

    @BeforeAll
    static void createVaultContainer() throws IOException, InterruptedException, VaultException {
        vaultContainer = new SecuredVaultContainer();
        // this is meant to allow Vault to contact Kubernetes container (for a specific test)
        // through the docker host using "host.docker.internal"
        // allowing to run the test in CI and locally
        vaultContainer.withExtraHost("host.docker.internal", "host-gateway");
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

    abstract static class AbstractGatewayVaultTest extends AbstractGatewayTest {

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
            } catch (Exception e) {
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

        protected Map<String, Object> authConfig(SecuredVaultContainer vaultContainer) {
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
}
