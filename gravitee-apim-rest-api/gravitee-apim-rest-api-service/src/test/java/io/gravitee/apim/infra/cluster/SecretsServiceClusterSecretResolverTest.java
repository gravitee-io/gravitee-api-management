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
package io.gravitee.apim.infra.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.gravitee.apim.common.cluster.KafkaClusterDefinitionSecretRefsFinder;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.cluster.model.KafkaClusterConnection;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.definition.model.cluster.ClusterSecurityResolutionException;
import io.gravitee.definition.model.cluster.ClusterSecurityResolver;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster;
import io.gravitee.definition.model.cluster.SaslConfig;
import io.gravitee.definition.model.cluster.SaslMechanism;
import io.gravitee.definition.model.cluster.SecurityConfig;
import io.gravitee.definition.model.cluster.SecurityProtocol;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableProviderFactory;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.secrets.api.el.DelegatingEvaluatedSecretsMethods;
import io.gravitee.secrets.api.el.EvaluatedSecretsMethods;
import io.gravitee.secrets.api.el.SecretFieldAccessControl;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Plays the secrets service the way it runs in the management API: it registers its {@code #secrets} variable once
 * started, and on DISCOVER rewrites the secret references the cluster finder hands it into grant expressions.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretsServiceClusterSecretResolverTest {

    private static final String REFERENCE = "{#secrets.get('/vault/secret/kafka:password')}";
    private static final String GRANT = "{#secrets.fromGrant('password', #secret_field_access_control_var)}";
    private static final Instant UPDATED_AT = Instant.parse("2026-11-19T10:00:00Z");

    private final EventManager eventManager = new EventManagerImpl();
    private final KafkaClusterDefinitionSecretRefsFinder finder = new KafkaClusterDefinitionSecretRefsFinder(new ObjectMapper());
    private final List<SecretDiscoveryEvent> discovered = new ArrayList<>();
    private final List<SecretDiscoveryEvent> revoked = new ArrayList<>();
    private boolean rewriteReferences = true;
    private boolean failRevocations = false;
    private GenericApplicationContext apim;
    private SecretsServiceClusterSecretResolver cut;

    @BeforeEach
    void setUp() {
        eventManager.subscribeForEvents(
            event -> {
                SecretDiscoveryEvent discovery = (SecretDiscoveryEvent) event.content();
                discovered.add(discovery);
                if (rewriteReferences) {
                    finder.findSecretRefs((KafkaClusterReactableCluster) discovery.definition(), (value, location, update) ->
                        update.accept(value.replace(REFERENCE, GRANT))
                    );
                }
            },
            SecretDiscoveryEventType.DISCOVER
        );
        eventManager.subscribeForEvents(
            event -> {
                revoked.add((SecretDiscoveryEvent) event.content());
                if (failRevocations) {
                    throw new IllegalStateException("revocation failed");
                }
            },
            SecretDiscoveryEventType.REVOKE
        );

        apim = new GenericApplicationContext();
        apim.registerBean(SecretsServiceClusterSecretResolver.class, () ->
            new SecretsServiceClusterSecretResolver(
                eventManager,
                apim.getBeanProvider(TemplateVariableProviderFactory.class),
                (mapper, providers) -> grantEvaluation(mapper)
            )
        );
        apim.refresh();
        cut = apim.getBean(SecretsServiceClusterSecretResolver.class);
    }

    @AfterEach
    void tearDown() {
        apim.close();
    }

    @Test
    void should_not_be_available_before_the_secrets_service_registers_its_variable() {
        assertThat(cut.available()).isFalse();

        startSecretsService();

        assertThat(cut.available()).isTrue();
    }

    @Test
    void should_resolve_a_secret_reference_of_a_connection_after_discovering_the_cluster() {
        startSecretsService();

        SecurityConfig resolved = cut.resolve(cluster(UPDATED_AT, REFERENCE), "main");

        assertThat(resolved.sasl().mechanism().password()).isEqualTo("s3cr3t");
        assertThat(resolved.sasl().mechanism().username()).isEqualTo("alice");
        assertThat(discovered)
            .singleElement()
            .satisfies(event -> {
                assertThat(event.envId()).isEqualTo("DEFAULT");
                assertThat(event.metadata().revision()).isEqualTo(String.valueOf(UPDATED_AT.toEpochMilli()));
                assertThat(((KafkaClusterReactableCluster) event.definition()).getId()).isEqualTo("cluster-id");
            });
    }

    @Test
    void should_discover_a_revision_of_a_cluster_once() {
        startSecretsService();

        cut.resolve(cluster(UPDATED_AT, REFERENCE), "main");
        cut.resolve(cluster(UPDATED_AT, REFERENCE), "main");

        assertThat(discovered).hasSize(1);
        assertThat(revoked).isEmpty();
    }

    @Test
    void should_discover_a_newer_revision_and_revoke_the_previous_one() {
        startSecretsService();
        Instant later = UPDATED_AT.plusSeconds(60);

        cut.resolve(cluster(UPDATED_AT, REFERENCE), "main");
        SecurityConfig resolved = cut.resolve(cluster(later, REFERENCE), "main");

        assertThat(resolved.sasl().mechanism().password()).isEqualTo("s3cr3t");
        assertThat(discovered)
            .extracting(event -> event.metadata().revision())
            .containsExactly(String.valueOf(UPDATED_AT.toEpochMilli()), String.valueOf(later.toEpochMilli()));
        assertThat(revoked)
            .singleElement()
            .satisfies(event -> assertThat(event.metadata().revision()).isEqualTo(String.valueOf(UPDATED_AT.toEpochMilli())));
    }

    @Test
    void should_return_a_security_without_expression_without_discovering_the_cluster() {
        startSecretsService();

        SecurityConfig resolved = cut.resolve(cluster(UPDATED_AT, "literal"), "main");

        assertThat(resolved.sasl().mechanism().password()).isEqualTo("literal");
        assertThat(discovered).isEmpty();
    }

    @Test
    void should_refuse_a_secret_reference_the_discovery_left_as_is_naming_the_field() {
        startSecretsService();
        rewriteReferences = false;

        assertThatThrownBy(() -> cut.resolve(cluster(UPDATED_AT, REFERENCE), "main"))
            .isInstanceOf(ClusterSecurityResolutionException.class)
            .hasMessageContaining("sasl.mechanism.password")
            .hasMessageNotContaining("/vault/secret/kafka");
    }

    @Test
    void should_refuse_a_connection_the_cluster_does_not_have() {
        startSecretsService();

        assertThatThrownBy(() -> cut.resolve(cluster(UPDATED_AT, REFERENCE), "unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unknown");
    }

    @Test
    void should_revoke_what_was_discovered_for_a_deleted_cluster_once() {
        startSecretsService();
        cut.resolve(cluster(UPDATED_AT, REFERENCE), "main");

        cut.revoke("cluster-id");
        cut.revoke("cluster-id");

        assertThat(revoked)
            .singleElement()
            .satisfies(event -> {
                assertThat(((KafkaClusterReactableCluster) event.definition()).getId()).isEqualTo("cluster-id");
                assertThat(event.metadata().revision()).isEqualTo(String.valueOf(UPDATED_AT.toEpochMilli()));
            });
    }

    @Test
    void should_revoke_the_previous_revision_once_the_cluster_no_longer_references_a_secret() {
        startSecretsService();
        cut.resolve(cluster(UPDATED_AT, REFERENCE), "main");

        SecurityConfig resolved = cut.resolve(cluster(UPDATED_AT.plusSeconds(60), "literal"), "main");

        assertThat(resolved.sasl().mechanism().password()).isEqualTo("literal");
        assertThat(revoked)
            .singleElement()
            .satisfies(event -> assertThat(event.metadata().revision()).isEqualTo(String.valueOf(UPDATED_AT.toEpochMilli())));
    }

    @Test
    void should_not_fail_when_the_secrets_service_fails_to_revoke() {
        startSecretsService();
        cut.resolve(cluster(UPDATED_AT, REFERENCE), "main");
        failRevocations = true;

        cut.revoke("cluster-id");

        assertThat(revoked).hasSize(1);
    }

    @Test
    void should_keep_a_newer_revision_discovered_when_revoking_the_previous_one_fails() {
        startSecretsService();
        Instant later = UPDATED_AT.plusSeconds(60);
        cut.resolve(cluster(UPDATED_AT, REFERENCE), "main");
        failRevocations = true;

        SecurityConfig resolved = cut.resolve(cluster(later, REFERENCE), "main");
        cut.resolve(cluster(later, REFERENCE), "main");

        assertThat(resolved.sasl().mechanism().password()).isEqualTo("s3cr3t");
        assertThat(discovered)
            .extracting(event -> event.metadata().revision())
            .containsExactly(String.valueOf(UPDATED_AT.toEpochMilli()), String.valueOf(later.toEpochMilli()));
    }

    private void startSecretsService() {
        FakeSecrets secrets = new FakeSecrets(Map.of("password", "s3cr3t"));
        apim
            .getBeanFactory()
            .registerSingleton(
                "secretTemplateVariableProviderFactory",
                new TemplateVariableProviderFactory() {
                    @Override
                    public List<TemplateVariableProvider> getTemplateVariableProviders() {
                        return List.of(context ->
                            context.setDeferredFunctionHolderVariable("secrets", new DelegatingEvaluatedSecretsMethods(secrets))
                        );
                    }

                    @Override
                    public TemplateVariableScope getTemplateVariableScope() {
                        return TemplateVariableScope.API;
                    }
                }
            );
    }

    /**
     * Evaluates the grants the discovery wrote as the secrets service would, without the expression language: its
     * whitelist is global to the JVM, and the shared resolver has its own tests in gravitee-apim-common.
     */
    private static ClusterSecurityResolver grantEvaluation(ObjectMapper mapper) {
        return security -> {
            JsonNode tree = mapper.valueToTree(security);
            if (tree.toString().contains("{#secrets.get(")) {
                return Single.error(new IllegalStateException("undiscovered reference evaluated"));
            }
            return Single.just(mapper.convertValue(replaceGrants(tree), SecurityConfig.class));
        };
    }

    private static JsonNode replaceGrants(JsonNode node) {
        if (node.isTextual() && node.textValue().equals(GRANT)) {
            return TextNode.valueOf("s3cr3t");
        }
        if (node instanceof ObjectNode object) {
            object.properties().forEach(field -> field.setValue(replaceGrants(field.getValue())));
        }
        return node;
    }

    private static Cluster cluster(Instant updatedAt, String password) {
        return Cluster.builder()
            .id("cluster-id")
            .crossId("cluster-cross-id")
            .name("Cluster")
            .environmentId("DEFAULT")
            .organizationId("DEFAULT")
            .type(ClusterType.KAFKA_CLUSTER)
            .updatedAt(updatedAt)
            .configuration(
                new KafkaClusterConfiguration(
                    List.of(
                        new KafkaClusterConnection(
                            "main",
                            "Main",
                            "kafka:9092",
                            new SecurityConfig(
                                SecurityProtocol.SASL_PLAINTEXT,
                                new SaslConfig(new SaslMechanism("PLAIN", "alice", password)),
                                null
                            )
                        )
                    )
                )
            )
            .build();
    }

    private record FakeSecrets(Map<String, String> values) implements EvaluatedSecretsMethods {
        @Override
        public Single<String> fromGrant(String contextId, SecretFieldAccessControl accessControl) {
            String value = values.get(contextId);
            return value == null ? Single.error(new IllegalStateException("secret [" + contextId + "] not found")) : Single.just(value);
        }

        @Override
        public Single<String> fromGrant(String contextId, String secretKey, SecretFieldAccessControl accessControl) {
            return fromGrant(contextId, accessControl);
        }

        @Override
        public Single<String> fromEL(String contextId, String uriOrName, SecretFieldAccessControl accessControl) {
            return Single.error(new IllegalStateException("not discovered"));
        }
    }
}
