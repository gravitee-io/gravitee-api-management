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
package io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service;

import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.cluster.model.SaslMechanism;
import io.gravitee.apim.core.cluster.model.SecurityProtocol;
import io.gravitee.plugin.configurations.ssl.KeyStore;
import io.gravitee.plugin.configurations.ssl.SslOptions;
import io.gravitee.plugin.configurations.ssl.TrustStore;
import io.gravitee.plugin.configurations.ssl.jks.JKSKeyStore;
import io.gravitee.plugin.configurations.ssl.jks.JKSTrustStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMKeyStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMTrustStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12TrustStore;
import io.gravitee.rest.api.kafkaexplorer.domain.domain_service.KafkaClusterDomainService;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.KafkaExplorerException;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.TechnicalCode;
import io.gravitee.rest.api.kafkaexplorer.domain.model.BrokerDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaClusterInfo;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaNode;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaTopic;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicConfigEntry;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicPartitionDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicsPage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.errors.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class KafkaClusterDomainServiceImpl implements KafkaClusterDomainService {

    private static final long TIMEOUT_SECONDS = 5;
    private static final long GET_TIMEOUT_SECONDS = TIMEOUT_SECONDS + 2;

    @Override
    public KafkaClusterInfo describeCluster(KafkaClusterConfiguration config) {
        return withAdminClient(config, adminClient -> {
            DescribeClusterResult result = adminClient.describeCluster();

            String clusterId = result.clusterId().get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Node controller = result.controller().get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var nodes = result.nodes().get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var topicStats = fetchTopicStats(adminClient);

            List<Integer> brokerIds = nodes.stream().map(Node::id).toList();
            var logDirSizes = fetchLogDirSizes(adminClient, brokerIds);

            List<BrokerDetail> brokerDetails = nodes
                .stream()
                .map(node -> toBrokerDetail(node, topicStats, logDirSizes))
                .toList();

            return new KafkaClusterInfo(
                clusterId,
                toKafkaNode(controller),
                brokerDetails,
                topicStats.totalTopics(),
                topicStats.totalPartitions()
            );
        });
    }

    @Override
    public TopicsPage listTopics(KafkaClusterConfiguration config, String nameFilter, int page, int perPage) {
        return withAdminClient(config, adminClient -> {
            // Step 1: List all topics (cheap call)
            Collection<TopicListing> listings = adminClient
                .listTopics(new ListTopicsOptions().listInternal(true))
                .listings()
                .get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            Map<String, Boolean> internalFlags = new HashMap<>();
            for (TopicListing listing : listings) {
                internalFlags.put(listing.name(), listing.isInternal());
            }

            if (internalFlags.isEmpty()) {
                return new TopicsPage(List.of(), 0, page, perPage);
            }

            // Step 2: Filter by nameFilter (case-insensitive contains)
            List<String> filteredNames = internalFlags
                .keySet()
                .stream()
                .filter(name -> nameFilter == null || nameFilter.isBlank() || name.toLowerCase().contains(nameFilter.toLowerCase()))
                .sorted()
                .toList();

            int totalCount = filteredNames.size();

            // Step 3: Paginate
            int fromIndex = Math.min(page * perPage, totalCount);
            int toIndex = Math.min(fromIndex + perPage, totalCount);
            List<String> pageNames = filteredNames.subList(fromIndex, toIndex);

            if (pageNames.isEmpty()) {
                return new TopicsPage(List.of(), totalCount, page, perPage);
            }

            // Step 4: Describe topics for the current page only (expensive calls)
            Map<String, TopicDescription> descriptions = adminClient
                .describeTopics(pageNames)
                .allTopicNames()
                .get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Step 5: Fetch topic sizes via describeLogDirs
            Map<String, Long> topicSizes = fetchTopicSizes(adminClient, Set.copyOf(pageNames));

            // Step 6: Fetch message counts via listOffsets
            Map<String, Long> messageCounts = fetchMessageCounts(adminClient, descriptions);

            // Step 7: Map to KafkaTopic records
            List<KafkaTopic> topics = pageNames
                .stream()
                .map(name -> {
                    TopicDescription topic = descriptions.get(name);
                    int partitionCount = topic.partitions().size();
                    int replicationFactor = topic.partitions().isEmpty() ? 0 : topic.partitions().get(0).replicas().size();
                    int underReplicatedCount = (int) topic
                        .partitions()
                        .stream()
                        .filter(p -> p.isr().size() < p.replicas().size())
                        .count();
                    boolean internal = internalFlags.getOrDefault(name, false);
                    return new KafkaTopic(
                        name,
                        partitionCount,
                        replicationFactor,
                        underReplicatedCount,
                        internal,
                        topicSizes.get(name),
                        messageCounts.get(name)
                    );
                })
                .toList();

            return new TopicsPage(topics, totalCount, page, perPage);
        });
    }

    @Override
    public TopicDetail describeTopic(KafkaClusterConfiguration config, String topicName) {
        return withAdminClient(config, adminClient -> {
            // Describe topic to get partition info
            TopicDescription description = adminClient
                .describeTopics(List.of(topicName))
                .allTopicNames()
                .get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .get(topicName);

            boolean internal = adminClient
                .listTopics(new ListTopicsOptions().listInternal(true))
                .listings()
                .get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .stream()
                .filter(l -> l.name().equals(topicName))
                .findFirst()
                .map(TopicListing::isInternal)
                .orElse(false);

            List<TopicPartitionDetail> partitions = description
                .partitions()
                .stream()
                .map(p ->
                    new TopicPartitionDetail(
                        p.partition(),
                        p.leader() != null ? toKafkaNode(p.leader()) : null,
                        p.replicas().stream().map(this::toKafkaNode).toList(),
                        p.isr().stream().map(this::toKafkaNode).toList(),
                        getOfflineNodes(p).stream().map(this::toKafkaNode).toList()
                    )
                )
                .toList();

            // Describe topic configs
            ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
            var configResult = adminClient
                .describeConfigs(Collections.singleton(configResource))
                .all()
                .get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<TopicConfigEntry> configs = configResult
                .get(configResource)
                .entries()
                .stream()
                .map(entry ->
                    new TopicConfigEntry(entry.name(), entry.value(), entry.source().name(), entry.isReadOnly(), entry.isSensitive())
                )
                .toList();

            return new TopicDetail(topicName, internal, partitions, configs);
        });
    }

    private List<Node> getOfflineNodes(TopicPartitionInfo partitionInfo) {
        Set<Integer> isrIds = partitionInfo.isr().stream().map(Node::id).collect(Collectors.toSet());
        return partitionInfo
            .replicas()
            .stream()
            .filter(r -> !isrIds.contains(r.id()))
            .toList();
    }

    private Map<String, Long> fetchTopicSizes(AdminClient adminClient, Set<String> topicNames) {
        try {
            List<Integer> brokerIds = adminClient
                .describeCluster()
                .nodes()
                .get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .stream()
                .map(Node::id)
                .toList();

            var logDirsResult = adminClient.describeLogDirs(brokerIds).allDescriptions().get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            Map<String, Long> sizes = new HashMap<>();
            for (var brokerEntry : logDirsResult.values()) {
                for (var logDir : brokerEntry.values()) {
                    for (var replicaEntry : logDir.replicaInfos().entrySet()) {
                        String topic = replicaEntry.getKey().topic();
                        long replicaSize = replicaEntry.getValue().size();
                        if (topicNames.contains(topic) && replicaSize >= 0) {
                            sizes.merge(topic, replicaSize, Long::sum);
                        }
                    }
                }
            }
            return sizes;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Long> fetchMessageCounts(AdminClient adminClient, Map<String, TopicDescription> descriptions) {
        try {
            Map<TopicPartition, OffsetSpec> offsetRequests = new HashMap<>();
            for (TopicDescription topic : descriptions.values()) {
                for (var partition : topic.partitions()) {
                    offsetRequests.put(new TopicPartition(topic.name(), partition.partition()), OffsetSpec.latest());
                }
            }

            if (offsetRequests.isEmpty()) {
                return Map.of();
            }

            var offsets = adminClient.listOffsets(offsetRequests).all().get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            return offsets
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(e -> e.getKey().topic(), Collectors.summingLong(e -> e.getValue().offset())));
        } catch (Exception e) {
            return Map.of();
        }
    }

    @FunctionalInterface
    private interface AdminClientAction<T> {
        T execute(AdminClient adminClient) throws Exception;
    }

    private <T> T withAdminClient(KafkaClusterConfiguration config, AdminClientAction<T> action) {
        List<Path> tempFiles = new ArrayList<>();
        Properties properties = buildProperties(config, tempFiles);
        try (AdminClient adminClient = createAdminClient(properties)) {
            return action.execute(adminClient);
        } catch (ExecutionException e) {
            throw handleExecutionException(e);
        } catch (TimeoutException e) {
            throw new KafkaExplorerException("Connection to Kafka cluster timed out", TechnicalCode.TIMEOUT, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaExplorerException("Connection to Kafka cluster was interrupted", TechnicalCode.INTERRUPTED, e);
        } catch (Exception e) {
            throw new KafkaExplorerException("Failed to connect to Kafka cluster", TechnicalCode.CONNECTION_FAILED, e);
        } finally {
            deleteTempFiles(tempFiles);
        }
    }

    protected AdminClient createAdminClient(Properties properties) {
        return AdminClient.create(properties);
    }

    private record TopicStats(
        int totalTopics,
        int totalPartitions,
        Map<Integer, Integer> leaderCounts,
        Map<Integer, Integer> replicaCounts
    ) {}

    private TopicStats fetchTopicStats(AdminClient adminClient) throws ExecutionException, InterruptedException, TimeoutException {
        Set<String> topicNames = adminClient
            .listTopics(new ListTopicsOptions().listInternal(true))
            .names()
            .get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (topicNames.isEmpty()) {
            return new TopicStats(0, 0, Map.of(), Map.of());
        }

        Map<String, TopicDescription> descriptions = adminClient
            .describeTopics(topicNames)
            .allTopicNames()
            .get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Map<Integer, Integer> leaderCounts = new HashMap<>();
        Map<Integer, Integer> replicaCounts = new HashMap<>();
        int totalPartitions = 0;

        for (TopicDescription topic : descriptions.values()) {
            for (var partition : topic.partitions()) {
                totalPartitions++;
                if (partition.leader() != null && partition.leader().id() >= 0) {
                    leaderCounts.merge(partition.leader().id(), 1, Integer::sum);
                }
                for (Node replica : partition.replicas()) {
                    replicaCounts.merge(replica.id(), 1, Integer::sum);
                }
            }
        }

        return new TopicStats(topicNames.size(), totalPartitions, leaderCounts, replicaCounts);
    }

    private Map<Integer, Long> fetchLogDirSizes(AdminClient adminClient, List<Integer> brokerIds) {
        try {
            var logDirsResult = adminClient.describeLogDirs(brokerIds).allDescriptions().get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            Map<Integer, Long> sizes = new HashMap<>();
            for (var entry : logDirsResult.entrySet()) {
                long totalSize = entry
                    .getValue()
                    .values()
                    .stream()
                    .flatMap(logDir -> logDir.replicaInfos().values().stream())
                    .mapToLong(info -> info.size())
                    .sum();
                sizes.put(entry.getKey(), totalSize);
            }
            return sizes;
        } catch (Exception e) {
            // Log dir info is best-effort, don't fail the whole request
            return Map.of();
        }
    }

    private BrokerDetail toBrokerDetail(Node node, TopicStats topicStats, Map<Integer, Long> logDirSizes) {
        return new BrokerDetail(
            node.id(),
            node.host(),
            node.port(),
            node.rack(),
            topicStats.leaderCounts().getOrDefault(node.id(), 0),
            topicStats.replicaCounts().getOrDefault(node.id(), 0),
            logDirSizes.getOrDefault(node.id(), null)
        );
    }

    private Properties buildProperties(KafkaClusterConfiguration config, List<Path> tempFiles) {
        Properties properties = new Properties();
        // TODO: Add allowed bootstrap servers validation via gravitee.yml config to prevent SSRF
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) Duration.ofSeconds(TIMEOUT_SECONDS).toMillis());
        properties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) Duration.ofSeconds(TIMEOUT_SECONDS).toMillis());

        SecurityProtocol protocol = SecurityProtocol.PLAINTEXT;
        if (config.security() != null && config.security().protocol() != null) {
            protocol = config.security().protocol();
        }
        properties.put("security.protocol", protocol.name());

        if (
            isSaslProtocol(protocol) &&
            config.security() != null &&
            config.security().sasl() != null &&
            config.security().sasl().mechanism() != null
        ) {
            configureSasl(properties, config.security().sasl().mechanism());
        }

        if (isSslProtocol(protocol) && config.security() != null && config.security().ssl() != null) {
            configureSsl(properties, config.security().ssl(), tempFiles);
        }

        return properties;
    }

    private boolean isSaslProtocol(SecurityProtocol protocol) {
        return protocol == SecurityProtocol.SASL_PLAINTEXT || protocol == SecurityProtocol.SASL_SSL;
    }

    private boolean isSslProtocol(SecurityProtocol protocol) {
        return protocol == SecurityProtocol.SSL || protocol == SecurityProtocol.SASL_SSL;
    }

    private void configureSsl(Properties properties, SslOptions sslOptions, List<Path> tempFiles) {
        if (sslOptions.isTrustAll()) {
            properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        }

        if (!sslOptions.isHostnameVerifier()) {
            properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        }

        configureTrustStore(properties, sslOptions.getTrustStore(), tempFiles);
        configureKeyStore(properties, sslOptions.getKeyStore(), tempFiles);
    }

    private void configureTrustStore(Properties properties, TrustStore trustStore, List<Path> tempFiles) {
        if (trustStore == null) {
            return;
        }
        switch (trustStore) {
            case JKSTrustStore jks -> {
                properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");
                setStoreLocation(properties, SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, jks.getPath(), jks.getContent(), tempFiles);
                if (jks.getPassword() != null) {
                    properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, jks.getPassword());
                }
            }
            case PKCS12TrustStore pkcs12 -> {
                properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
                setStoreLocation(properties, SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, pkcs12.getPath(), pkcs12.getContent(), tempFiles);
                if (pkcs12.getPassword() != null) {
                    properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, pkcs12.getPassword());
                }
            }
            case PEMTrustStore pem -> {
                properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
                if (pem.getContent() != null) {
                    properties.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, pem.getContent());
                } else if (pem.getPath() != null) {
                    properties.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, readFileContent(pem.getPath()));
                }
            }
            default -> {}
        }
    }

    private void configureKeyStore(Properties properties, KeyStore keyStore, List<Path> tempFiles) {
        if (keyStore == null) {
            return;
        }
        switch (keyStore) {
            case JKSKeyStore jks -> {
                properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "JKS");
                setStoreLocation(properties, SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, jks.getPath(), jks.getContent(), tempFiles);
                if (jks.getPassword() != null) {
                    properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, jks.getPassword());
                }
                if (jks.getKeyPassword() != null) {
                    properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, jks.getKeyPassword());
                }
            }
            case PKCS12KeyStore pkcs12 -> {
                properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
                setStoreLocation(properties, SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, pkcs12.getPath(), pkcs12.getContent(), tempFiles);
                if (pkcs12.getPassword() != null) {
                    properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, pkcs12.getPassword());
                }
                if (pkcs12.getKeyPassword() != null) {
                    properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, pkcs12.getKeyPassword());
                }
            }
            case PEMKeyStore pem -> {
                properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PEM");
                if (pem.getCertContent() != null) {
                    properties.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, pem.getCertContent());
                } else if (pem.getCertPath() != null) {
                    properties.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, readFileContent(pem.getCertPath()));
                }
                if (pem.getKeyContent() != null) {
                    properties.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, pem.getKeyContent());
                } else if (pem.getKeyPath() != null) {
                    properties.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, readFileContent(pem.getKeyPath()));
                }
            }
            default -> {}
        }
    }

    private void setStoreLocation(Properties properties, String configKey, String path, String base64Content, List<Path> tempFiles) {
        if (path != null) {
            properties.put(configKey, path);
        } else if (base64Content != null) {
            try {
                Path tempFile = Files.createTempFile("kafka-ssl-", ".store");
                Files.write(tempFile, Base64.getDecoder().decode(base64Content));
                tempFiles.add(tempFile);
                properties.put(configKey, tempFile.toString());
            } catch (IOException e) {
                throw new KafkaExplorerException("Failed to create temporary SSL store file", TechnicalCode.CONNECTION_FAILED, e);
            }
        }
    }

    private String readFileContent(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new KafkaExplorerException("Failed to read file: " + path, TechnicalCode.CONNECTION_FAILED, e);
        }
    }

    private void deleteTempFiles(List<Path> tempFiles) {
        for (Path tempFile : tempFiles) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // Best effort cleanup
            }
        }
    }

    private void configureSasl(Properties properties, SaslMechanism mechanism) {
        properties.put("sasl.mechanism", mechanism.type());
        String jaasConfig = buildJaasConfig(mechanism);
        properties.put("sasl.jaas.config", jaasConfig);
    }

    private String buildJaasConfig(SaslMechanism mechanism) {
        String loginModule = switch (mechanism.type()) {
            case "PLAIN" -> "org.apache.kafka.common.security.plain.PlainLoginModule";
            case "SCRAM-SHA-256", "SCRAM-SHA-512" -> "org.apache.kafka.common.security.scram.ScramLoginModule";
            default -> throw new IllegalArgumentException("Unsupported SASL mechanism: " + mechanism.type());
        };
        String escapedUsername = escapeJaasValue(mechanism.username());
        String escapedPassword = escapeJaasValue(mechanism.password());
        return String.format("%s required username=\"%s\" password=\"%s\";", loginModule, escapedUsername, escapedPassword);
    }

    private static String escapeJaasValue(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private KafkaExplorerException handleExecutionException(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof AuthenticationException) {
            return new KafkaExplorerException("Authentication failed", TechnicalCode.AUTHENTICATION_FAILED, cause);
        }
        return new KafkaExplorerException("Failed to connect to Kafka cluster", TechnicalCode.CONNECTION_FAILED, cause);
    }

    private KafkaNode toKafkaNode(Node node) {
        return new KafkaNode(node.id(), node.host(), node.port());
    }
}
