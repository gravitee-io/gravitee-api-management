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
package io.gravitee.rest.api.kafkaexplorer.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.ClusterCrudServiceInMemory;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeBrokerUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeConsumerGroupUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeKafkaClusterUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeTopicUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.ListConsumerGroupsUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.ListTopicsUseCase;
import io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service.KafkaClusterDomainServiceImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.File;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
abstract class AbstractKafkaExplorerResourceIntegrationTest {

    protected static final String BROKER_SERVICE = "broker";
    protected static final int PLAINTEXT_PORT = 9092;
    protected static final int SSL_PORT = 9094;
    protected static final int SASL_PLAINTEXT_PORT = 9095;
    protected static final String CLUSTER_ID = "test-cluster";
    protected static final String ENVIRONMENT_ID = "test-env";

    protected static final DockerComposeContainer<?> kafka;
    protected static KafkaExplorerResource resource;
    protected static ClusterCrudServiceInMemory clusterCrudService;

    static {
        kafka = new DockerComposeContainer<>(new File("src/test/resources/docker/docker-compose.yml"))
            .withExposedService(BROKER_SERVICE, PLAINTEXT_PORT, Wait.forHealthcheck().withStartupTimeout(Duration.ofSeconds(60)))
            .withExposedService(BROKER_SERVICE, SSL_PORT)
            .withExposedService(BROKER_SERVICE, SASL_PLAINTEXT_PORT);
        kafka.start();

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        try {
            resource = new KafkaExplorerResource();
            clusterCrudService = new ClusterCrudServiceInMemory();
            var clusterService = new KafkaClusterDomainServiceImpl();
            var objectMapper = new ObjectMapper();
            injectField(
                resource,
                "describeKafkaClusterUseCase",
                new DescribeKafkaClusterUseCase(clusterCrudService, clusterService, objectMapper)
            );
            injectField(resource, "listTopicsUseCase", new ListTopicsUseCase(clusterCrudService, clusterService, objectMapper));
            injectField(resource, "describeTopicUseCase", new DescribeTopicUseCase(clusterCrudService, clusterService, objectMapper));
            injectField(resource, "describeBrokerUseCase", new DescribeBrokerUseCase(clusterCrudService, clusterService, objectMapper));
            injectField(
                resource,
                "listConsumerGroupsUseCase",
                new ListConsumerGroupsUseCase(clusterCrudService, clusterService, objectMapper)
            );
            injectField(
                resource,
                "describeConsumerGroupUseCase",
                new DescribeConsumerGroupUseCase(clusterCrudService, clusterService, objectMapper)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up integration test", e);
        }
    }

    @BeforeEach
    void resetClusterCrudService() {
        clusterCrudService.reset();
    }

    protected static String plaintextBootstrapServers() {
        return kafka.getServiceHost(BROKER_SERVICE, PLAINTEXT_PORT) + ":" + kafka.getServicePort(BROKER_SERVICE, PLAINTEXT_PORT);
    }

    protected static String sslBootstrapServers() {
        return kafka.getServiceHost(BROKER_SERVICE, SSL_PORT) + ":" + kafka.getServicePort(BROKER_SERVICE, SSL_PORT);
    }

    protected static String saslBootstrapServers() {
        return kafka.getServiceHost(BROKER_SERVICE, SASL_PLAINTEXT_PORT) + ":" + kafka.getServicePort(BROKER_SERVICE, SASL_PLAINTEXT_PORT);
    }

    protected static void givenClusterWithConfig(Map<String, Object> config) {
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(config).build());
    }

    protected static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
