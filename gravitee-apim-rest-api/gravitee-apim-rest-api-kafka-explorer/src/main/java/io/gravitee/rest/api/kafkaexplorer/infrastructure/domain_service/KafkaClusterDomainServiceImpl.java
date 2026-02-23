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

import io.gravitee.rest.api.kafkaexplorer.domain.domain_service.KafkaClusterDomainService;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.KafkaExplorerException;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.TechnicalCode;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaClusterInfo;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaConnectionConfig;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaNode;
import io.gravitee.rest.api.kafkaexplorer.domain.model.SaslMechanism;
import io.gravitee.rest.api.kafkaexplorer.domain.model.SecurityProtocol;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.errors.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class KafkaClusterDomainServiceImpl implements KafkaClusterDomainService {

    private static final long TIMEOUT_SECONDS = 5;
    private static final long GET_TIMEOUT_SECONDS = TIMEOUT_SECONDS + 2;

    @Override
    public KafkaClusterInfo describeCluster(KafkaConnectionConfig config) {
        Properties properties = buildProperties(config);
        try (AdminClient adminClient = createAdminClient(properties)) {
            DescribeClusterResult result = adminClient.describeCluster();

            String clusterId = result.clusterId().get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Node controller = result.controller().get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var nodes = result.nodes().get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS).stream().map(this::toKafkaNode).toList();

            return new KafkaClusterInfo(clusterId, toKafkaNode(controller), nodes);
        } catch (ExecutionException e) {
            throw handleExecutionException(e);
        } catch (TimeoutException e) {
            throw new KafkaExplorerException("Connection to Kafka cluster timed out", TechnicalCode.TIMEOUT, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaExplorerException("Connection to Kafka cluster was interrupted", TechnicalCode.INTERRUPTED, e);
        } catch (Exception e) {
            throw new KafkaExplorerException("Failed to connect to Kafka cluster: " + e.getMessage(), TechnicalCode.CONNECTION_FAILED, e);
        }
    }

    protected AdminClient createAdminClient(Properties properties) {
        return AdminClient.create(properties);
    }

    private Properties buildProperties(KafkaConnectionConfig config) {
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

        return properties;
    }

    private boolean isSaslProtocol(SecurityProtocol protocol) {
        return protocol == SecurityProtocol.SASL_PLAINTEXT || protocol == SecurityProtocol.SASL_SSL;
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
            return new KafkaExplorerException("Authentication failed: " + cause.getMessage(), TechnicalCode.AUTHENTICATION_FAILED, cause);
        }
        return new KafkaExplorerException(
            "Failed to connect to Kafka cluster: " + cause.getMessage(),
            TechnicalCode.CONNECTION_FAILED,
            cause
        );
    }

    private KafkaNode toKafkaNode(Node node) {
        return new KafkaNode(node.id(), node.host(), node.port());
    }
}
