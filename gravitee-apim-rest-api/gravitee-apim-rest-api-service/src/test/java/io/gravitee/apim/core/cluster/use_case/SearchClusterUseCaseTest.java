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
package io.gravitee.apim.core.cluster.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import inmemory.AbstractUseCaseTest;
import inmemory.ClusterQueryServiceInMemory;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.query_service.ClusterQueryService;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchClusterUseCaseTest extends AbstractUseCaseTest {

    private final ClusterQueryService clusterQueryService = new ClusterQueryServiceInMemory();
    private SearchClusterUseCase searchClusterUseCase;

    @BeforeEach
    void setUp() {
        searchClusterUseCase = new SearchClusterUseCase(clusterQueryService);
        initDb();
    }

    @Test
    void should_search_no_pageable_no_sort_by() {
        // When
        var result = searchClusterUseCase.execute(new SearchClusterUseCase.Input(ENV_ID, null, null));
        // Then
        assertAll(
            () -> assertThat(result.pageResult().getPageNumber()).isEqualTo(1),
            () -> assertThat(result.pageResult().getTotalElements()).isEqualTo(15),
            () -> assertThat(result.pageResult().getPageElements()).isEqualTo(10),
            () ->
                assertThat(result.pageResult().getContent().stream().map(Cluster::getName).toList())
                    .isEqualTo(initDbData().stream().map(Cluster::getName).sorted().limit(10).toList())
        );
    }

    @Test
    void should_search_with_pageable_no_sort_by() {
        Pageable pageable = new PageableImpl(2, 5);
        // When
        var result = searchClusterUseCase.execute(new SearchClusterUseCase.Input(ENV_ID, pageable, null));
        // Then
        assertAll(
            () -> assertThat(result.pageResult().getPageNumber()).isEqualTo(2),
            () -> assertThat(result.pageResult().getTotalElements()).isEqualTo(15),
            () -> assertThat(result.pageResult().getPageElements()).isEqualTo(5),
            () ->
                assertThat(result.pageResult().getContent().stream().map(Cluster::getName).toList())
                    .isEqualTo(initDbData().stream().map(Cluster::getName).sorted().toList().subList(5, 10))
        );
    }

    @Test
    void should_search_no_pageable_with_sort_by() {
        String sortBy = "id";
        // When
        var result = searchClusterUseCase.execute(new SearchClusterUseCase.Input(ENV_ID, null, sortBy));
        // Then
        assertAll(
            () -> assertThat(result.pageResult().getPageNumber()).isEqualTo(1),
            () -> assertThat(result.pageResult().getTotalElements()).isEqualTo(15),
            () -> assertThat(result.pageResult().getPageElements()).isEqualTo(10),
            () ->
                assertThat(result.pageResult().getContent().stream().map(Cluster::getName).toList())
                    .isEqualTo(initDbData().stream().sorted(Comparator.comparing(Cluster::getId)).map(Cluster::getName).limit(10).toList())
        );
    }

    @Test
    void should_search_with_pageable_and_sort_by() {
        Pageable pageable = new PageableImpl(2, 5);
        String sortBy = "id";
        // When
        var result = searchClusterUseCase.execute(new SearchClusterUseCase.Input(ENV_ID, pageable, sortBy));
        // Then
        assertAll(
            () -> assertThat(result.pageResult().getPageNumber()).isEqualTo(2),
            () -> assertThat(result.pageResult().getTotalElements()).isEqualTo(15),
            () -> assertThat(result.pageResult().getPageElements()).isEqualTo(5),
            () ->
                assertThat(result.pageResult().getContent().stream().map(Cluster::getName).toList())
                    .isEqualTo(
                        initDbData().stream().sorted(Comparator.comparing(Cluster::getId)).map(Cluster::getName).toList().subList(5, 10)
                    )
        );
    }

    private List<Cluster> initDbData() {
        Cluster cluster1 = Cluster
            .builder()
            .id("cluster-1")
            .name("Cluster 1")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 1")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster2 = Cluster
            .builder()
            .id("cluster-2")
            .name("2 - Cluster")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 2")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster3 = Cluster
            .builder()
            .id("cluster-3")
            .name("Cluster no 3")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 3")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster4 = Cluster
            .builder()
            .id("cluster-4")
            .name("Cluster 4")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 4")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster5 = Cluster
            .builder()
            .id("cluster-5")
            .name("Cluster 5")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 5")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster6 = Cluster
            .builder()
            .id("cluster-6")
            .name("6 Cluster")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 6")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster7 = Cluster
            .builder()
            .id("cluster-7")
            .name("7 - Cluster")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 7")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster8 = Cluster
            .builder()
            .id("cluster-8")
            .name("Cluster 8")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 8")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster9 = Cluster
            .builder()
            .id("cluster-9")
            .name("Cluster - 9")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 9")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster10 = Cluster
            .builder()
            .id("cluster-10")
            .name("10 - Cluster")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 10")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster11 = Cluster
            .builder()
            .id("cluster-11")
            .name("11 Cluster")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 11")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster12 = Cluster
            .builder()
            .id("cluster-12")
            .name("Cluster 12")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 12")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster13 = Cluster
            .builder()
            .id("cluster-13")
            .name("Cluster 13")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 13")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster14 = Cluster
            .builder()
            .id("cluster-14")
            .name("14 Cluster")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 14")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        Cluster cluster15 = Cluster
            .builder()
            .id("cluster-15")
            .name("15 - Cluster")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 15")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        return List.of(
            cluster1,
            cluster2,
            cluster3,
            cluster4,
            cluster5,
            cluster6,
            cluster7,
            cluster8,
            cluster9,
            cluster10,
            cluster11,
            cluster12,
            cluster13,
            cluster14,
            cluster15
        );
    }

    private void initDb() {
        ((ClusterQueryServiceInMemory) clusterQueryService).initWith(initDbData());
    }
}
