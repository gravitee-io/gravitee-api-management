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
package io.gravitee.apim.infra.crud_service.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.exception.DbEntityNotFoundException;
import io.gravitee.apim.infra.adapter.ClusterAdapter;
import io.gravitee.apim.infra.adapter.ClusterAdapterImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClusterCrudServiceImplTest {

    private final ClusterRepository clusterRepository = mock(ClusterRepository.class);

    private final ClusterAdapter clusterAdapter = new ClusterAdapterImpl();

    private final ClusterCrudServiceImpl clusterCrudService = new ClusterCrudServiceImpl(clusterRepository, clusterAdapter);

    @Nested
    class FindById {

        @Test
        public void findById_throws_not_found_exception() throws TechnicalException {
            String clusterId = "cluster-id";
            when(clusterRepository.findById(clusterId)).thenReturn(Optional.empty());
            var exception = assertThrows(
                DbEntityNotFoundException.class,
                () -> clusterCrudService.findByIdAndEnvironmentId(clusterId, "env-1")
            );
            assertThat(exception.getMessage()).isEqualTo(String.format("Cluster with id %s cannot be found", clusterId));
        }

        @Test
        public void findById_throws_technical_exception() throws TechnicalException {
            String clusterId = "cluster-id";
            when(clusterRepository.findById(clusterId)).thenThrow(new TechnicalException());
            var exception = assertThrows(
                TechnicalManagementException.class,
                () -> clusterCrudService.findByIdAndEnvironmentId(clusterId, "env-1")
            );
            assertThat(exception.getMessage())
                .isEqualTo(String.format("An error occurred while trying to find a Cluster with id %s", clusterId));
        }
    }

    @Nested
    class Create {

        @Test
        public void create_throws_technical_exception() throws TechnicalException {
            var cluster = Cluster.builder().name("cluster-name").environmentId("env-1").build();
            cluster.setId("cluster-id");
            when(clusterRepository.create(any())).thenThrow(new TechnicalException());
            var exception = assertThrows(TechnicalManagementException.class, () -> clusterCrudService.create(cluster));
            assertThat(exception.getMessage())
                .isEqualTo(String.format("An error occurred while trying to create a Cluster with id %s", cluster.getId()));
        }

        @Test
        public void create_succeeds() throws TechnicalException {
            var cluster = Cluster.builder().id("cluster-id").name("cluster-name").environmentId("env-1").configuration("{}").build();
            when(clusterRepository.create(any())).thenReturn(clusterAdapter.toRepository(cluster));

            var createdCluster = clusterCrudService.create(cluster);
            assertThat(createdCluster).isNotNull();
            assertThat(createdCluster.getId()).isEqualTo(cluster.getId());
            assertThat(createdCluster.getName()).isEqualTo(cluster.getName());
            assertThat(createdCluster.getEnvironmentId()).isEqualTo(cluster.getEnvironmentId());
            assertThat(createdCluster.getConfiguration()).isEqualTo(cluster.getConfiguration());
        }
    }
}
