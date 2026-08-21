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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.ClusterCrudServiceInMemory;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.rest.api.kafkaexplorer.domain.domain_service.KafkaClusterDomainService;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.KafkaExplorerException;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.TechnicalCode;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeKafkaClusterUseCase;
import io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service.KafkaClusterDomainServiceImpl;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Whatever goes wrong reaching a broker, the resource answers 502 and carries the technical code
 * through. Exercising that against a real broker means waiting out a connection timeout for each case,
 * so the failures are injected here and the one case that needs no broker at all -- an address that
 * cannot answer -- drives the real service.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaExplorerResourceErrorTest {

    private static final String CLUSTER_ID = "test-cluster";
    private static final String ENVIRONMENT_ID = "test-env";

    static Stream<Arguments> brokerFailures() {
        return Stream.of(
            Arguments.of(TechnicalCode.TIMEOUT, "Connection to Kafka cluster timed out"),
            Arguments.of(TechnicalCode.CONNECTION_FAILED, "Failed to connect to Kafka cluster"),
            Arguments.of(TechnicalCode.AUTHENTICATION_FAILED, "Authentication against Kafka cluster failed")
        );
    }

    @ParameterizedTest
    @MethodSource("brokerFailures")
    void should_return_502_carrying_the_technical_code(TechnicalCode technicalCode, String message) throws Exception {
        var resource = resourceFailingWith(new KafkaExplorerException(message, technicalCode));
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        var response = resource.describeCluster(new DescribeClusterRequest().clusterId(CLUSTER_ID));

        assertThat(response.getStatus()).isEqualTo(502);
        assertThat(response.getEntity())
            .asInstanceOf(type(KafkaExplorerError.class))
            .satisfies(error -> {
                assertThat(error.getTechnicalCode()).isEqualTo(technicalCode.name());
                assertThat(error.getMessage()).isEqualTo(message);
            });
    }

    /**
     * The cases above inject the failure, so they say nothing about the service turning a real client
     * error into a {@link TechnicalCode}. This one drives the real service at a bootstrap address that
     * cannot answer, which exercises that translation without waiting on a broker to refuse a handshake.
     *
     * <p>Two codes are accepted because the outcome depends on the resolver: an address in .invalid
     * normally fails while the admin client is built, but a hijacking resolver answers and the call
     * runs out the timeout instead. CONNECTION_FAILED being the catch-all branch, what this pins is
     * that the service maps a real client failure at all -- not which branch it took.
     */
    @Test
    void should_map_an_unreachable_broker_to_a_502() throws Exception {
        var resource = resourceBackedBy(new KafkaClusterDomainServiceImpl(5, 2));
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        var response = resource.describeCluster(new DescribeClusterRequest().clusterId(CLUSTER_ID));

        assertThat(response.getStatus()).isEqualTo(502);
        assertThat(response.getEntity())
            .asInstanceOf(type(KafkaExplorerError.class))
            .satisfies(error -> {
                assertThat(error.getTechnicalCode()).isIn(TechnicalCode.TIMEOUT.name(), TechnicalCode.CONNECTION_FAILED.name());
            });
    }

    private static KafkaExplorerResource resourceFailingWith(KafkaExplorerException failure) throws Exception {
        var clusterService = mock(KafkaClusterDomainService.class);
        when(clusterService.describeCluster(any())).thenThrow(failure);
        return resourceBackedBy(clusterService);
    }

    private static KafkaExplorerResource resourceBackedBy(KafkaClusterDomainService clusterService) throws Exception {
        var clusters = new ClusterCrudServiceInMemory();
        clusters.create(
            Cluster.builder()
                .id(CLUSTER_ID)
                .environmentId(ENVIRONMENT_ID)
                .configuration(Map.of("bootstrapServers", "unreachable.invalid:9092"))
                .build()
        );

        var useCase = new DescribeKafkaClusterUseCase(clusters, clusterService, new ObjectMapper());

        var resource = new KafkaExplorerResource();
        injectField(resource, "describeKafkaClusterUseCase", useCase);
        injectField(resource, "objectMapper", new ObjectMapper());
        return resource;
    }

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
