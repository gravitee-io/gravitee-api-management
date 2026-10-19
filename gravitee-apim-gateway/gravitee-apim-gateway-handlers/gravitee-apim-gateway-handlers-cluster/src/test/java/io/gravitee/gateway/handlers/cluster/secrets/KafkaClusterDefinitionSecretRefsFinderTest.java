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
package io.gravitee.gateway.handlers.cluster.secrets;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster.KafkaClusterConnection;
import io.gravitee.definition.model.cluster.SaslConfig;
import io.gravitee.definition.model.cluster.SaslMechanism;
import io.gravitee.definition.model.cluster.SecurityConfig;
import io.gravitee.definition.model.cluster.SecurityProtocol;
import io.gravitee.plugin.configurations.ssl.SslOptions;
import io.gravitee.plugin.configurations.ssl.jks.JKSKeyStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMTrustStore;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsListener;
import io.gravitee.secrets.api.discovery.SecretRefsLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaClusterDefinitionSecretRefsFinderTest {

    private static final String SECRET_REF = "{#secrets.get('/vault/secret/kafka:password')}";
    private static final String REWRITTEN = "{#secrets.fromGrant('ctx-1', #secret_field_access_control_var)}";

    private final KafkaClusterDefinitionSecretRefsFinder cut = new KafkaClusterDefinitionSecretRefsFinder(new ObjectMapper());

    @Test
    void should_handle_registered_kafka_clusters_only() {
        assertThat(cut.canHandle(KafkaClusterReactableCluster.builder().build())).isTrue();
        assertThat(cut.canHandle(new Object())).isFalse();
        assertThat(cut.canHandle(null)).isFalse();
    }

    @Test
    void should_describe_the_cluster_by_id_and_version() {
        KafkaClusterReactableCluster cluster = KafkaClusterReactableCluster.builder().id("c1").build();

        assertThat(cut.toDefinitionDescriptor(cluster, new DefinitionMetadata("4"))).isEqualTo(
            new DefinitionDescriptor(new Definition("cluster", "c1"), Optional.of("4"))
        );
    }

    @Test
    void should_rewrite_the_secret_references_of_every_connection_security() {
        KafkaClusterConnection sasl = connection("sasl", saslSecurity(SECRET_REF));
        KafkaClusterConnection ssl = connection(
            "ssl",
            new SecurityConfig(
                SecurityProtocol.SSL,
                null,
                SslOptions.builder()
                    .trustStore(PEMTrustStore.builder().content("-----BEGIN CERTIFICATE-----").build())
                    .keyStore(JKSKeyStore.builder().path("/etc/kafka/client.jks").password(SECRET_REF).build())
                    .build()
            )
        );
        KafkaClusterConnection plaintext = connection("plaintext", null);
        RewritingListener listener = new RewritingListener();

        cut.findSecretRefs(cluster(sasl, ssl, plaintext), listener);

        assertThat(listener.locations).containsOnly(new SecretRefsLocation("cluster", "KAFKA_CLUSTER")).hasSize(2);
        assertThat(sasl.getSecurity().sasl().mechanism()).isEqualTo(new SaslMechanism("PLAIN", "user", REWRITTEN));
        assertThat(sasl.getSecurity().protocol()).isEqualTo(SecurityProtocol.SASL_SSL);
        JKSKeyStore keyStore = (JKSKeyStore) ssl.getSecurity().ssl().getKeyStore();
        assertThat(keyStore.getPassword()).isEqualTo(REWRITTEN);
        assertThat(keyStore.getPath()).isEqualTo("/etc/kafka/client.jks");
        assertThat(((PEMTrustStore) ssl.getSecurity().ssl().getTrustStore()).getContent()).isEqualTo("-----BEGIN CERTIFICATE-----");
        assertThat(plaintext.getSecurity()).isNull();
    }

    @Test
    void should_find_nothing_in_a_cluster_without_connections() {
        RewritingListener listener = new RewritingListener();

        cut.findSecretRefs(KafkaClusterReactableCluster.builder().id("c1").build(), listener);

        assertThat(listener.locations).isEmpty();
    }

    private static SecurityConfig saslSecurity(String password) {
        return new SecurityConfig(SecurityProtocol.SASL_SSL, new SaslConfig(new SaslMechanism("PLAIN", "user", password)), null);
    }

    private static KafkaClusterConnection connection(String crossId, SecurityConfig security) {
        return KafkaClusterConnection.builder().crossId(crossId).bootstrapServers("kafka:9092").security(security).build();
    }

    private static KafkaClusterReactableCluster cluster(KafkaClusterConnection... connections) {
        return KafkaClusterReactableCluster.builder().id("c1").connections(List.of(connections)).build();
    }

    /** Stands for the secrets service: replaces every secret reference with the expression it generates. */
    private static class RewritingListener implements DefinitionSecretRefsListener {

        private final List<SecretRefsLocation> locations = new ArrayList<>();

        @Override
        public void onCandidate(String candidate, SecretRefsLocation location, Consumer<String> updated) {
            locations.add(location);
            updated.accept(candidate.replace(SECRET_REF, REWRITTEN));
        }
    }
}
