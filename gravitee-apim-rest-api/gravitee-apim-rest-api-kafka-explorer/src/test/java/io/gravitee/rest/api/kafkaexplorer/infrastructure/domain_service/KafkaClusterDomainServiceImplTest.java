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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.kafkaexplorer.domain.exception.KafkaExplorerException;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.TechnicalCode;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaConnectionConfig;
import io.gravitee.rest.api.kafkaexplorer.domain.model.SecurityConfig;
import io.gravitee.rest.api.kafkaexplorer.domain.model.SecurityProtocol;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.AuthenticationException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaClusterDomainServiceImplTest {

    private static final KafkaConnectionConfig CONFIG = new KafkaConnectionConfig(
        "localhost:9092",
        new SecurityConfig(SecurityProtocol.PLAINTEXT, null, null)
    );

    @Test
    void should_throw_connection_failed_when_admin_client_creation_fails() {
        var service = serviceWithAdminClient(() -> {
            throw new RuntimeException("Cannot create admin client");
        });

        assertThatThrownBy(() -> service.describeCluster(CONFIG))
            .isInstanceOf(KafkaExplorerException.class)
            .hasMessageContaining("Cannot create admin client")
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.CONNECTION_FAILED));
    }

    @Test
    void should_throw_timeout_when_cluster_response_times_out() throws Exception {
        var service = serviceWithFailingFuture(new TimeoutException("Request timed out"));

        assertThatThrownBy(() -> service.describeCluster(CONFIG))
            .isInstanceOf(KafkaExplorerException.class)
            .hasMessageContaining("timed out")
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.TIMEOUT));
    }

    @Test
    void should_throw_authentication_failed_when_auth_error() throws Exception {
        var service = serviceWithFailingFuture(new ExecutionException(new AuthenticationException("Bad credentials")));

        assertThatThrownBy(() -> service.describeCluster(CONFIG))
            .isInstanceOf(KafkaExplorerException.class)
            .hasMessageContaining("Authentication failed")
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.AUTHENTICATION_FAILED));
    }

    @Test
    void should_throw_interrupted_when_thread_is_interrupted() throws Exception {
        var service = serviceWithFailingFuture(new InterruptedException("Thread interrupted"));

        assertThatThrownBy(() -> service.describeCluster(CONFIG))
            .isInstanceOf(KafkaExplorerException.class)
            .hasMessageContaining("interrupted")
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.INTERRUPTED));

        // Reset interrupted flag
        Thread.interrupted();
    }

    @Test
    void should_throw_connection_failed_when_execution_fails_with_unknown_cause() throws Exception {
        var service = serviceWithFailingFuture(new ExecutionException(new RuntimeException("Something went wrong")));

        assertThatThrownBy(() -> service.describeCluster(CONFIG))
            .isInstanceOf(KafkaExplorerException.class)
            .hasMessageContaining("Something went wrong")
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.CONNECTION_FAILED));
    }

    @SuppressWarnings("unchecked")
    private KafkaClusterDomainServiceImpl serviceWithFailingFuture(Exception exception) throws Exception {
        KafkaFuture<String> failingFuture = mock(KafkaFuture.class);
        when(failingFuture.get(anyLong(), any())).thenThrow(exception);

        DescribeClusterResult describeResult = mock(DescribeClusterResult.class);
        when(describeResult.clusterId()).thenReturn(failingFuture);

        AdminClient adminClient = mock(AdminClient.class);
        when(adminClient.describeCluster()).thenReturn(describeResult);

        return serviceWithAdminClient(() -> adminClient);
    }

    private KafkaClusterDomainServiceImpl serviceWithAdminClient(AdminClientFactory factory) {
        return new KafkaClusterDomainServiceImpl() {
            @Override
            protected AdminClient createAdminClient(Properties properties) {
                return factory.create();
            }
        };
    }

    @FunctionalInterface
    private interface AdminClientFactory {
        AdminClient create();
    }
}
