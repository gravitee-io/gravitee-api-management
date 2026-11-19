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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.gravitee.apim.common.cluster.DefaultClusterSecurityResolver;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.cluster.service_provider.ClusterSecretResolver;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.cluster.ClusterSecurityResolutionException;
import io.gravitee.definition.model.cluster.ClusterSecurityResolver;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster.KafkaClusterConnection;
import io.gravitee.definition.model.cluster.SecurityConfig;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableProviderFactory;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import java.time.Instant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import lombok.CustomLog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Has the secrets service discover a cluster on its first use, as the gateway has it discover a cluster it deploys, then
 * evaluates the connection security with the resolver it shares with the gateway.
 *
 * <p>The variable providers are looked up on each call: the secrets service registers its {@code #secrets} variable in
 * this context once it has started, after this bean.</p>
 */
@CustomLog
@Service
public class SecretsServiceClusterSecretResolver implements ClusterSecretResolver {

    private static final String SECRETS_VARIABLE = "secrets";
    private static final String EXPRESSION_MARKER = "{#";
    /** What a secret reference still looks like when the secrets service did not discover it. */
    private static final String SECRET_REFERENCE = "{#secrets.get(";

    /** The plain mapper the gateway gives the finder and the resolver: the management API's primary one filters fields. */
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventManager eventManager;
    private final ObjectProvider<TemplateVariableProviderFactory> templateVariableProviderFactories;
    private final ClusterSecurityResolver securityResolver;
    private final Map<String, DiscoveredCluster> discoveredClusters = new ConcurrentHashMap<>();

    @Autowired
    public SecretsServiceClusterSecretResolver(
        EventManager eventManager,
        ObjectProvider<TemplateVariableProviderFactory> templateVariableProviderFactories
    ) {
        this(eventManager, templateVariableProviderFactories, DefaultClusterSecurityResolver::new);
    }

    /** The evaluation is replaceable for tests: initializing the expression language whitelist is global to the JVM. */
    SecretsServiceClusterSecretResolver(
        EventManager eventManager,
        ObjectProvider<TemplateVariableProviderFactory> templateVariableProviderFactories,
        BiFunction<ObjectMapper, Supplier<List<TemplateVariableProvider>>, ClusterSecurityResolver> securityResolverFactory
    ) {
        this.eventManager = eventManager;
        this.templateVariableProviderFactories = templateVariableProviderFactories;
        this.securityResolver = securityResolverFactory.apply(objectMapper, this::templateVariableProviders);
    }

    @Override
    public boolean available() {
        TemplateEngine engine = TemplateEngine.templateEngine();
        templateVariableProviders().forEach(provider -> provider.provide(engine.getTemplateContext()));
        return engine.getTemplateContext().lookupVariable(SECRETS_VARIABLE) != null;
    }

    @Override
    public SecurityConfig resolve(Cluster cluster, String connectionCrossId) {
        KafkaClusterReactableCluster definition = definitionOf(cluster);
        SecurityConfig security = connection(definition, connectionCrossId).getSecurity();
        if (!holdsExpression(objectMapper.valueToTree(security))) {
            // an update can remove the last reference: what the previous revision discovered is no longer used
            revoke(cluster.getId());
            return security;
        }
        SecurityConfig discovered = connection(discover(cluster.getId(), revisionOf(cluster), definition), connectionCrossId).getSecurity();
        refuseUndiscoveredReferences(objectMapper.valueToTree(discovered), null);
        return securityResolver.resolve(discovered).blockingGet();
    }

    @Override
    public void revoke(String clusterId) {
        DiscoveredCluster discovered = discoveredClusters.remove(clusterId);
        if (discovered != null) {
            revokeQuietly(clusterId, discovered);
        }
    }

    /**
     * The discovery runs inside {@code compute}: concurrent first uses of a revision wait for it rather than discovering
     * the cluster twice. It is bounded by the secrets service's discovery timeout.
     */
    private KafkaClusterReactableCluster discover(String clusterId, String revision, KafkaClusterReactableCluster definition) {
        return discoveredClusters
            .compute(clusterId, (id, previous) -> {
                if (previous != null && previous.revision().equals(revision)) {
                    return previous;
                }
                publish(SecretDiscoveryEventType.DISCOVER, definition, revision);
                if (previous != null) {
                    revokeQuietly(id, previous);
                }
                return new DiscoveredCluster(revision, definition);
            })
            .definition();
    }

    /**
     * A cleanup that never fails its caller: neither the cluster deletion it follows, nor the discovery of the newer
     * revision, which must be kept once published.
     */
    private void revokeQuietly(String clusterId, DiscoveredCluster discovered) {
        try {
            publish(SecretDiscoveryEventType.REVOKE, discovered.definition(), discovered.revision());
        } catch (RuntimeException e) {
            log.warn("Unable to revoke the secrets discovered for revision [{}] of cluster [{}]", discovered.revision(), clusterId, e);
        }
    }

    private void publish(SecretDiscoveryEventType type, KafkaClusterReactableCluster definition, String revision) {
        eventManager.publishEvent(
            type,
            new SecretDiscoveryEvent(definition.getEnvironmentId(), definition, new DefinitionMetadata(revision))
        );
    }

    private KafkaClusterReactableCluster definitionOf(Cluster cluster) {
        if (cluster.getType() != ClusterType.KAFKA_CLUSTER) {
            throw new IllegalArgumentException("Cluster [" + cluster.getId() + "] is not a Kafka cluster with connections");
        }
        KafkaClusterConfiguration configuration = cluster.getKafkaClusterConfiguration(objectMapper);
        List<KafkaClusterConnection> connections = configuration == null || configuration.connections() == null
            ? List.of()
            : configuration
                .connections()
                .stream()
                .map(connection ->
                    KafkaClusterConnection.builder()
                        .crossId(connection.crossId())
                        .name(connection.name())
                        .bootstrapServers(connection.bootstrapServers())
                        .security(connection.security())
                        .build()
                )
                .toList();
        KafkaClusterReactableCluster definition = new KafkaClusterReactableCluster(connections);
        definition.setId(cluster.getId());
        definition.setCrossId(cluster.getCrossId());
        definition.setName(cluster.getName());
        definition.setEnvironmentId(cluster.getEnvironmentId());
        definition.setOrganizationId(cluster.getOrganizationId());
        return definition;
    }

    private static KafkaClusterConnection connection(KafkaClusterReactableCluster definition, String connectionCrossId) {
        return definition
            .getConnections()
            .stream()
            .filter(connection -> connection.getCrossId() != null && connection.getCrossId().equals(connectionCrossId))
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException("Connection [" + connectionCrossId + "] not found in cluster [" + definition.getId() + "]")
            );
    }

    /** The saved cluster is what the features connecting to it use, so its last update, not its deployed version, is the revision. */
    private static String revisionOf(Cluster cluster) {
        Instant updatedAt = cluster.getUpdatedAt() == null ? cluster.getCreatedAt() : cluster.getUpdatedAt();
        return updatedAt == null ? "0" : String.valueOf(updatedAt.toEpochMilli());
    }

    private static boolean holdsExpression(JsonNode node) {
        if (node instanceof TextNode text) {
            return text.textValue().contains(EXPRESSION_MARKER);
        }
        for (JsonNode child : node) {
            if (holdsExpression(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Evaluated as is, a reference the secrets service did not discover returns a value that is not the secret: the field
     * is refused, without its value.
     */
    private static void refuseUndiscoveredReferences(JsonNode node, String path) {
        if (node instanceof TextNode text && text.textValue().contains(SECRET_REFERENCE)) {
            throw new ClusterSecurityResolutionException(path, null);
        }
        if (node instanceof ObjectNode object) {
            object
                .properties()
                .forEach(field ->
                    refuseUndiscoveredReferences(field.getValue(), path == null ? field.getKey() : path + "." + field.getKey())
                );
        } else if (node instanceof ArrayNode array) {
            for (int i = 0; i < array.size(); i++) {
                refuseUndiscoveredReferences(array.get(i), (path == null ? "" : path) + "[" + i + "]");
            }
        }
    }

    /** Factories copied into this context can be seen under two names: each one provides its variables once. */
    private List<TemplateVariableProvider> templateVariableProviders() {
        Set<TemplateVariableProviderFactory> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return templateVariableProviderFactories
            .stream()
            .filter(seen::add)
            .filter(factory -> factory.getTemplateVariableScope() == TemplateVariableScope.API)
            .flatMap(factory -> factory.getTemplateVariableProviders().stream())
            .toList();
    }

    private record DiscoveredCluster(String revision, KafkaClusterReactableCluster definition) {}
}
