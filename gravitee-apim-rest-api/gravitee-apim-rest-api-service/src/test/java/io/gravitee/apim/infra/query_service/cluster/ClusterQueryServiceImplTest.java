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
package io.gravitee.apim.infra.query_service.cluster;

import static assertions.CoreAssertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.cluster.model.ClusterSearchCriteria;
import io.gravitee.apim.core.cluster.query_service.ClusterQueryService;
import io.gravitee.apim.infra.adapter.ClusterAdapter;
import io.gravitee.apim.infra.adapter.ClusterAdapterImpl;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.repository.management.model.Cluster;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClusterQueryServiceImplTest {

    ClusterRepository repository;
    ClusterAdapter clusterAdapter;
    ClusterQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(ClusterRepository.class);
        clusterAdapter = new ClusterAdapterImpl();
        service = new ClusterQueryServiceImpl(repository, clusterAdapter);
    }

    @Nested
    class SearchByEnvironmentId {

        @Test
        void should_return_empty_page() {
            // Given
            String environmentId = "environmentId";
            when(
                repository.search(
                    argThat(criteria -> criteria.getEnvironmentId().equals(environmentId)),
                    argThat(pageable -> pageable.pageNumber() == 0 && pageable.pageSize() == 10),
                    argThat(Optional::isEmpty)
                )
            )
                .thenReturn(new Page<>(List.of(), 0, 0, 0));

            // When
            Pageable pageable = new PageableImpl(1, 10);
            var result = service.search(ClusterSearchCriteria.builder().environmentId(environmentId).build(), pageable, Optional.empty());

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void should_return_page_with_clusters() {
            // Given
            String environmentId = "environmentId";
            when(
                repository.search(
                    argThat(criteria -> criteria.getEnvironmentId().equals(environmentId)),
                    argThat(pageable -> pageable.pageNumber() == 0 && pageable.pageSize() == 10),
                    argThat(sortable ->
                        sortable.isPresent() && sortable.get().field().equals("name") && sortable.get().order().name().equals("ASC")
                    )
                )
            )
                .thenReturn(
                    new Page<>(
                        List.of(
                            Cluster.builder().id("cluster1").name("Cluster 1").environmentId(environmentId).build(),
                            Cluster.builder().id("cluster2").name("Cluster 2").environmentId(environmentId).build()
                        ),
                        2,
                        0,
                        2
                    )
                );

            // When
            Pageable pageable = new PageableImpl(1, 10);
            var result = service.search(
                ClusterSearchCriteria.builder().environmentId(environmentId).build(),
                pageable,
                Optional.of(new SortableImpl("name", true))
            );

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getId()).isEqualTo("cluster1");
            assertThat(result.getContent().get(0).getName()).isEqualTo("Cluster 1");
            assertThat(result.getContent().get(1).getId()).isEqualTo("cluster2");
            assertThat(result.getContent().get(1).getName()).isEqualTo("Cluster 2");
            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }
}
