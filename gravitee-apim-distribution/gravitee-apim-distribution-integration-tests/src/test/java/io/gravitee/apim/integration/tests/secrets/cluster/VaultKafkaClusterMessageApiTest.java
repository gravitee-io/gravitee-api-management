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

import com.graviteesource.endpoint.kafka.KafkaEndpointConnectorFactory;
import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import com.graviteesource.secretprovider.hcvault.HCVaultSecretProvider;
import com.graviteesource.secretprovider.hcvault.HCVaultSecretProviderFactory;
import com.graviteesource.secretprovider.hcvault.config.manager.VaultConfig;
import com.graviteesource.service.secrets.SecretsService;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderBuilder;
import io.gravitee.apim.integration.tests.secrets.SecuredVaultContainer;
import io.gravitee.apim.integration.tests.secrets.conf.SSLUtils;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster.KafkaClusterConnection;
import io.gravitee.definition.model.cluster.SaslConfig;
import io.gravitee.definition.model.cluster.SaslMechanism;
import io.gravitee.definition.model.cluster.SecurityConfig;
import io.gravitee.definition.model.cluster.SecurityProtocol;
import io.gravitee.gateway.handlers.cluster.manager.ClusterManager;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A Message API whose Kafka endpoint uses a registered cluster authenticates to a SASL broker with the password a secret
 * reference of the cluster connection reads from a real Vault.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VaultKafkaClusterMessageApiTest {

    private static final String TOPIC = "cluster-topic";
    private static final String CLIENT_PASSWORD = "client-secret";
    private static final String ADMIN_JAAS_CONFIG =
        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"admin-secret\";";

    static SecuredVaultContainer vaultContainer;
    static Vault rootVault;
    static KafkaContainer kafka;

    @BeforeAll
    static void startContainers() throws IOException, InterruptedException, VaultException {
        vaultContainer = new SecuredVaultContainer();
        vaultContainer.start();
        vaultContainer.initAndUnsealVault();
        vaultContainer.loginAndLoadTestPolicy();
        vaultContainer.setEngineVersions();
        vaultContainer.setupUserPassAuth();
        vaultContainer.setupCertAuth(SSLUtils.createPairs().cert());
        rootVault = vaultContainer.getRootVault();

        // The external listener (PLAINTEXT) requires SASL PLAIN: the broker knows the users admin and client.
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
            .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:SASL_PLAINTEXT")
            .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "PLAIN")
            .withEnv(
                "KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG",
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"admin\" password=\"admin-secret\" " +
                    "user_admin=\"admin-secret\" user_client=\"" +
                    CLIENT_PASSWORD +
                    "\";"
            )
            .withEnv("KAFKA_SASL_JAAS_CONFIG", ADMIN_JAAS_CONFIG)
            .withStartupTimeout(Duration.ofMinutes(2));
        kafka.start();
    }

    @AfterAll
    static void stopContainers() {
        kafka.stop();
        vaultContainer.close();
    }

    /** Nested so that Vault and Kafka are up before the gateway extension starts the gateway. */
    @Nested
    @GatewayTest
    class MessageApiOnACluster extends AbstractGatewayTest {

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
        }

        @Override
        public void configureSecretProviders(
            Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
        ) {
            secretProviderPlugins.add(
                SecretProviderBuilder.build(HCVaultSecretProvider.PLUGIN_ID, HCVaultSecretProviderFactory.class, VaultConfig.class)
            );
        }

        @Override
        public void configureServices(Set<Class<? extends AbstractService<?>>> services) {
            super.configureServices(services);
            services.add(SecretsService.class);
        }

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("kafka", EndpointBuilder.build("kafka", KafkaEndpointConnectorFactory.class));
        }

        @AfterEach
        void undeployCluster() {
            getBean(ClusterManager.class).unregister("cluster-id");
        }

        @Test
        @DeployApi("/apis/v4/messages/cluster/http-post-entrypoint-kafka-cluster-endpoint.json")
        void should_publish_with_the_password_the_cluster_reads_from_vault(HttpClient client) throws VaultException {
            rootVault.logical().write("secret/kafka-client", Map.of("password", CLIENT_PASSWORD));
            getBean(ClusterManager.class).register(cluster("{#secrets.get('/vault/secret/kafka-client:password')}"));
            String body = "message-" + UUID.randomUUID();

            assertThat(post(client, body)).isEqualTo(202);

            assertThat(consumeAll()).contains(body);
        }

        @Test
        @DeployApi("/apis/v4/messages/cluster/http-post-entrypoint-kafka-cluster-endpoint.json")
        void should_not_publish_when_the_password_read_from_vault_is_wrong(HttpClient client) throws VaultException {
            rootVault.logical().write("secret/kafka-wrong", Map.of("password", "wrong-password"));
            getBean(ClusterManager.class).register(cluster("{#secrets.get('/vault/secret/kafka-wrong:password')}"));
            String body = "message-" + UUID.randomUUID();
            long refusedBefore = brokerAuthenticationFailures();

            assertThat(post(client, body)).isEqualTo(500);

            // The broker refused the password the gateway read from Vault: the secret was resolved, to the wrong value.
            await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(brokerAuthenticationFailures()).isGreaterThan(refusedBefore));
            assertThat(consumeAll()).doesNotContain(body);
        }

        /** How many SASL authentications the broker has refused so far. */
        private long brokerAuthenticationFailures() {
            return kafka
                .getLogs()
                .lines()
                .filter(line -> line.contains("Failed authentication with"))
                .count();
        }

        private int post(HttpClient client, String body) {
            return client
                .rxRequest(HttpMethod.POST, "/test")
                .flatMap(request -> request.rxSend(body))
                .map(response -> response.statusCode())
                .timeout(90, TimeUnit.SECONDS)
                .blockingGet();
        }

        private KafkaClusterReactableCluster cluster(String password) {
            return KafkaClusterReactableCluster.builder()
                .id("cluster-id")
                .crossId("my-cluster")
                .environmentId("DEFAULT")
                .version(1)
                .deployedAt(new Date())
                .connections(
                    List.of(
                        KafkaClusterConnection.builder()
                            .crossId("sasl")
                            .bootstrapServers(kafka.getBootstrapServers())
                            .security(
                                new SecurityConfig(
                                    SecurityProtocol.SASL_PLAINTEXT,
                                    new SaslConfig(new SaslMechanism("PLAIN", "client", password)),
                                    null
                                )
                            )
                            .build()
                    )
                )
                .build();
        }
    }

    /** The values of the records on {@link #TOPIC}, read as the broker admin. */
    private static List<String> consumeAll() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        properties.put(SaslConfigs.SASL_JAAS_CONFIG, ADMIN_JAAS_CONFIG);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(TOPIC));
            List<String> values = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    values.add(record.value());
                }
            }
            return values;
        }
    }
}
