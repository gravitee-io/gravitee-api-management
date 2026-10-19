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
package io.gravitee.apim.integration.tests.secrets.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.graviteesource.secretprovider.hcvault.HCVaultSecretProvider;
import com.graviteesource.secretprovider.hcvault.HCVaultSecretProviderFactory;
import com.graviteesource.secretprovider.hcvault.config.manager.VaultConfig;
import com.graviteesource.service.secrets.SecretsService;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderBuilder;
import io.gravitee.apim.integration.tests.secrets.SecuredVaultContainer;
import io.gravitee.apim.integration.tests.secrets.conf.SSLUtils;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.cluster.ClusterEvent;
import io.gravitee.definition.model.cluster.ClusterSecurityResolutionException;
import io.gravitee.definition.model.cluster.ClusterSecurityResolver;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster.KafkaClusterConnection;
import io.gravitee.definition.model.cluster.ReactableCluster;
import io.gravitee.definition.model.cluster.SaslConfig;
import io.gravitee.definition.model.cluster.SaslMechanism;
import io.gravitee.definition.model.cluster.SecurityConfig;
import io.gravitee.definition.model.cluster.SecurityProtocol;
import io.gravitee.gateway.handlers.cluster.manager.ClusterManager;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Secret references in the credentials of a registered Kafka cluster are discovered when the gateway deploys the cluster,
 * and resolved by the {@link ClusterSecurityResolver} the Kafka connectors use, from a real Vault.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VaultKafkaClusterSecretTest {

    private static final String PASSWORD_REF = "{#secrets.get('/vault/secret/kafka:password')}";

    static SecuredVaultContainer vaultContainer;
    static Vault rootVault;

    @BeforeAll
    static void createVaultContainer() throws IOException, InterruptedException, VaultException {
        vaultContainer = new SecuredVaultContainer();
        vaultContainer.start();
        vaultContainer.initAndUnsealVault();
        vaultContainer.loginAndLoadTestPolicy();
        vaultContainer.setEngineVersions();
        vaultContainer.setupUserPassAuth();
        SSLUtils.SSLPairs clientCertAndKey = SSLUtils.createPairs();
        vaultContainer.setupCertAuth(clientCertAndKey.cert());
        rootVault = vaultContainer.getRootVault();
    }

    @AfterAll
    static void cleanup() {
        vaultContainer.close();
    }

    /** Nested so that Vault is up before the gateway extension starts the gateway. */
    @Nested
    @GatewayTest
    class DeployedCluster extends AbstractGatewayTest {

        final String password = UUID.randomUUID().toString();

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.setYamlProperty("api.secrets.providers[0].plugin", "vault");
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.enabled", true);
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.host", vaultContainer.getHost());
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.port", vaultContainer.getPort());
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.ssl.enabled", true);
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.ssl.format", "pemfile");
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.ssl.file", SecuredVaultContainer.CERT_PEMFILE);
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.auth.method", "userpass");
            configurationBuilder.setYamlProperty(
                "api.secrets.providers[0].configuration.auth.config.username",
                SecuredVaultContainer.USER_ID
            );
            configurationBuilder.setYamlProperty(
                "api.secrets.providers[0].configuration.auth.config.password",
                SecuredVaultContainer.PASSWORD
            );
            configurationBuilder.setYamlProperty("api.secrets.renewal.enabled", true);
            configurationBuilder.setYamlProperty("api.secrets.renewal.check.delay", "1");
            configurationBuilder.setYamlProperty("api.secrets.renewal.check.unit", "SECONDS");
            configurationBuilder.setYamlProperty("api.secrets.renewal.defaultSecretTtl.delay", "2");
            configurationBuilder.setYamlProperty("api.secrets.renewal.defaultSecretTtl.unit", "SECONDS");
        }

        @Override
        public void configureSecretProviders(
            Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
        ) throws Exception {
            secretProviderPlugins.add(
                SecretProviderBuilder.build(HCVaultSecretProvider.PLUGIN_ID, HCVaultSecretProviderFactory.class, VaultConfig.class)
            );
            LogicalResponse write = rootVault.logical().write("secret/kafka", Map.of("password", password));
            assertThat(write.getRestResponse().getStatus()).isLessThan(300);
        }

        @Override
        public void configureServices(Set<Class<? extends AbstractService<?>>> services) {
            super.configureServices(services);
            services.add(SecretsService.class);
        }

        /** The gateway is shared by the tests of this class: each one deploys its own version of the cluster. */
        @AfterEach
        void undeployCluster() {
            getBean(ClusterManager.class).unregister("cluster-id");
        }

        @Test
        void should_resolve_a_secret_in_the_credentials_of_a_deployed_cluster() {
            KafkaClusterReactableCluster cluster = cluster(1, PASSWORD_REF);
            getBean(ClusterManager.class).register(cluster);

            SecurityConfig deployed = deployedSecurity();
            SecurityConfig resolved = resolve(deployed);

            assertThat(resolved.sasl().mechanism()).isEqualTo(new SaslMechanism("PLAIN", "alice", password));
            assertThat(deployed.sasl().mechanism().password()).doesNotContain(password).isNotEqualTo(PASSWORD_REF);
        }

        @Test
        void should_keep_resolving_the_secret_after_the_cluster_is_redeployed() {
            ClusterManager clusterManager = getBean(ClusterManager.class);
            clusterManager.register(cluster(1, PASSWORD_REF));
            clusterManager.register(cluster(2, PASSWORD_REF));

            assertThat(resolve(deployedSecurity()).sasl().mechanism().password()).isEqualTo(password);
        }

        @Test
        void should_deny_the_secrets_of_the_previous_version_once_the_cluster_is_redeployed() {
            ClusterManager clusterManager = getBean(ClusterManager.class);
            clusterManager.register(cluster(1, PASSWORD_REF));
            SecurityConfig previous = deployedSecurity();

            clusterManager.register(cluster(2, PASSWORD_REF));

            assertThat(resolve(deployedSecurity()).sasl().mechanism().password()).isEqualTo(password);
            getBean(ClusterSecurityResolver.class)
                .resolve(previous)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(ClusterSecurityResolutionException.class);
        }

        @Test
        void should_publish_the_cluster_again_with_the_new_value_when_a_renewed_secret_changes() throws VaultException {
            rootVault.logical().write("secret/kafka-rotation", Map.of("password", "before-rotation"));
            List<ReactableCluster> updates = new CopyOnWriteArrayList<>();
            EventListener<ClusterEvent, ReactableCluster> listener = event -> {
                if (event.type() == ClusterEvent.UPDATE) {
                    updates.add(event.content());
                }
            };
            getBean(EventManager.class).subscribeForEvents(listener, ClusterEvent.class);
            getBean(ClusterManager.class).register(
                cluster(1, "{#secrets.get('/vault/secret/kafka-rotation:password?renewable=true&reloadOnChange=true')}")
            );
            assertThat(resolve(deployedSecurity()).sasl().mechanism().password()).isEqualTo("before-rotation");
            updates.clear();

            rootVault.logical().write("secret/kafka-rotation", Map.of("password", "after-rotation"));

            await()
                .atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(updates).anyMatch(cluster -> "cluster-id".equals(cluster.getId())));
            assertThat(resolve(deployedSecurity()).sasl().mechanism().password()).isEqualTo("after-rotation");
        }

        @Test
        void should_keep_literal_credentials_unchanged() {
            getBean(ClusterManager.class).register(cluster(1, "literal-password"));

            assertThat(resolve(deployedSecurity()).sasl().mechanism().password()).isEqualTo("literal-password");
        }

        @Test
        void should_revoke_the_secrets_of_an_undeployed_cluster() {
            ClusterManager clusterManager = getBean(ClusterManager.class);
            clusterManager.register(cluster(1, PASSWORD_REF));
            SecurityConfig deployed = deployedSecurity();

            clusterManager.unregister("cluster-id");

            getBean(ClusterSecurityResolver.class)
                .resolve(deployed)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(ClusterSecurityResolutionException.class);
        }

        private SecurityConfig deployedSecurity() {
            KafkaClusterReactableCluster cluster = (KafkaClusterReactableCluster) getBean(ClusterManager.class).get(
                "my-cluster",
                "DEFAULT"
            );
            return cluster.getConnections().getFirst().getSecurity();
        }

        private SecurityConfig resolve(SecurityConfig security) {
            return getBean(ClusterSecurityResolver.class).resolve(security).timeout(10, TimeUnit.SECONDS).blockingGet();
        }

        private KafkaClusterReactableCluster cluster(int version, String password) {
            return KafkaClusterReactableCluster.builder()
                .id("cluster-id")
                .crossId("my-cluster")
                .environmentId("DEFAULT")
                .version(version)
                .deployedAt(new Date())
                .connections(
                    List.of(
                        KafkaClusterConnection.builder()
                            .crossId("sasl")
                            .bootstrapServers("kafka:9092")
                            .security(
                                new SecurityConfig(
                                    SecurityProtocol.SASL_SSL,
                                    new SaslConfig(new SaslMechanism("PLAIN", "alice", password)),
                                    null
                                )
                            )
                            .build()
                    )
                )
                .build();
        }
    }
}
