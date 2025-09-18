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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ClusterCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Cluster;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public class ClusterRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/cluster-tests/";
    }

    @Test
    public void should_find_by_id() throws Exception {
        Cluster cluster = clusterRepository.findById("cluster-id-1").orElseThrow();
        assertAll(
            () -> assertThat(cluster.getId()).isEqualTo("cluster-id-1"),
            () -> assertThat(cluster.getCreatedAt()).isEqualTo(Instant.ofEpochSecond(1753711100)),
            () -> assertThat(cluster.getUpdatedAt()).isNull(),
            () -> assertThat(cluster.getEnvironmentId()).isEqualTo("env-1"),
            () -> assertThat(cluster.getOrganizationId()).isEqualTo("org-1"),
            () -> assertThat(cluster.getName()).isEqualTo("cluster-1"),
            () -> assertThat(cluster.getDescription()).isEqualTo("The cluster no 1"),
            () -> assertThat(cluster.getDefinition()).isEqualTo("Cluster 1 definition"),
            () -> assertThat(cluster.getGroups()).isEqualTo(Set.of("group-1", "group-2"))
        );
    }

    @Test
    public void should_create() throws Exception {
        final Cluster cluster = Cluster
            .builder()
            .id("cluster-id")
            // Because by default, PostgreSQL's timestamp type (without with time zone) supports up to 6 digits of fractional seconds (microsecond precision),
            // while Java's Instant supports up to nanoseconds (9 digits).
            .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
            .environmentId("env-id")
            .organizationId("org-id")
            .name("my-cluster")
            .description("My cluster description")
            .definition("definition")
            .groups(Set.of("group"))
            .build();

        final Cluster createdCluster = clusterRepository.create(cluster);

        assertAll(
            () -> assertThat(createdCluster.getId()).isEqualTo(cluster.getId()),
            () -> assertThat(createdCluster.getCreatedAt()).isEqualTo(cluster.getCreatedAt()),
            () -> assertThat(createdCluster.getUpdatedAt()).isNull(),
            () -> assertThat(createdCluster.getEnvironmentId()).isEqualTo(cluster.getEnvironmentId()),
            () -> assertThat(createdCluster.getOrganizationId()).isEqualTo(cluster.getOrganizationId()),
            () -> assertThat(createdCluster.getName()).isEqualTo(cluster.getName()),
            () -> assertThat(createdCluster.getDescription()).isEqualTo(cluster.getDescription()),
            () -> assertThat(createdCluster.getDefinition()).isEqualTo(cluster.getDefinition()),
            () -> assertThat(createdCluster.getGroups()).isEqualTo(cluster.getGroups())
        );
    }

    @Test
    public void should_update() throws Exception {
        Cluster toUpdate = Cluster
            .builder()
            .id("cluster-id-1")
            // Because by default, PostgreSQL's timestamp type (without with time zone) supports up to 6 digits of fractional seconds (microsecond precision),
            // while Java's Instant supports up to nanoseconds (9 digits).
            .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
            .environmentId("env-1")
            .environmentId("org-1")
            .name("new-cluster-1")
            .description("New description for the cluster no 1")
            .groups(Set.of("group-3"))
            .build();

        Cluster update = clusterRepository.update(toUpdate);

        assertAll(
            () -> assertThat(update.getId()).isEqualTo(toUpdate.getId()),
            () -> assertThat(update.getCreatedAt()).isEqualTo(toUpdate.getCreatedAt()),
            () -> assertThat(update.getUpdatedAt()).isNull(),
            () -> assertThat(update.getEnvironmentId()).isEqualTo(toUpdate.getEnvironmentId()),
            () -> assertThat(update.getOrganizationId()).isEqualTo(toUpdate.getOrganizationId()),
            () -> assertThat(update.getName()).isEqualTo(toUpdate.getName()),
            () -> assertThat(update.getDescription()).isEqualTo(toUpdate.getDescription()),
            () -> assertThat(update.getDefinition()).isNull(),
            () -> assertThat(update.getGroups()).isEqualTo(toUpdate.getGroups())
        );
    }

    @Test
    public void should_delete() throws Exception {
        String id = "cluster-id-1";
        assertThat(clusterRepository.findById(id)).isPresent();
        clusterRepository.delete(id);
        assertThat(clusterRepository.findById(id)).isNotPresent();
    }

    @Test
    public void search_by_env_id_and_ids_no_sortable() {
        Pageable pageable = new PageableBuilder().pageNumber(0).pageSize(10).build();
        ClusterCriteria criteria = ClusterCriteria
            .builder()
            .environmentId("env-1")
            .ids(List.of("cluster-id-1", "cluster-id-2", "cluster-id-3", "cluster-id-4"))
            .build();
        Page<Cluster> clusters = clusterRepository.search(criteria, pageable, Optional.empty());
        assertAll(
            () -> assertThat(clusters.getContent().size()).isEqualTo(2),
            () -> assertThat(clusters.getPageNumber()).isEqualTo(0),
            () -> assertThat(clusters.getPageElements()).isEqualTo(2),
            () -> assertThat(clusters.getTotalElements()).isEqualTo(2),
            () -> assertThat(clusters.getContent().stream().map(Cluster::getName).toList()).isEqualTo(List.of("cluster-1", "cluster-no-2"))
        );
    }

    @Test
    public void search_by_env_id_no_sortable() {
        Pageable pageable = new PageableBuilder().pageNumber(0).pageSize(3).build();
        ClusterCriteria criteria = ClusterCriteria.builder().environmentId("env-1").build();
        Page<Cluster> clusters = clusterRepository.search(criteria, pageable, Optional.empty());
        assertAll(
            () -> assertThat(clusters.getContent().size()).isEqualTo(3),
            () -> assertThat(clusters.getPageNumber()).isEqualTo(0),
            () -> assertThat(clusters.getPageElements()).isEqualTo(3),
            () -> assertThat(clusters.getTotalElements()).isEqualTo(4),
            () ->
                assertThat(clusters.getContent().stream().map(Cluster::getName).toList())
                    .isEqualTo(List.of("8-cluster", "cluster-1", "cluster-10"))
        );
    }

    @Test
    public void search_no_criteria_sort_by_name_desc() {
        ClusterCriteria criteria = ClusterCriteria.builder().environmentId("env-1").build();
        Pageable pageable = new PageableBuilder().pageNumber(0).pageSize(3).build();
        Sortable sortable = new SortableBuilder().field("name").order(Order.DESC).build();
        Page<Cluster> clusters = clusterRepository.search(criteria, pageable, Optional.of(sortable));
        assertAll(
            () -> assertThat(clusters.getContent().size()).isEqualTo(3),
            () -> assertThat(clusters.getPageNumber()).isEqualTo(0),
            () -> assertThat(clusters.getPageElements()).isEqualTo(3),
            () -> assertThat(clusters.getTotalElements()).isEqualTo(4),
            () ->
                assertThat(clusters.getContent().stream().map(Cluster::getName).toList())
                    .isEqualTo(List.of("cluster-no-2", "cluster-10", "cluster-1"))
        );
    }

    @Test
    public void search_by_name_in_query_criteria() {
        ClusterCriteria criteria = ClusterCriteria.builder().environmentId("env-1").query("cluster-1").build();
        Pageable pageable = new PageableBuilder().pageNumber(0).pageSize(3).build();
        Page<Cluster> clusters = clusterRepository.search(criteria, pageable, Optional.empty());
        assertAll(
            () -> assertThat(clusters.getContent().size()).isEqualTo(2),
            () -> assertThat(clusters.getPageNumber()).isEqualTo(0),
            () -> assertThat(clusters.getPageElements()).isEqualTo(2),
            () -> assertThat(clusters.getTotalElements()).isEqualTo(2),
            () -> assertThat(clusters.getContent().stream().map(Cluster::getName).toList()).isEqualTo(List.of("cluster-1", "cluster-10"))
        );
    }

    @Test
    public void search_by_description_in_query_criteria() {
        ClusterCriteria criteria = ClusterCriteria.builder().environmentId("env-2").query("the cluster no 4").build();
        Pageable pageable = new PageableBuilder().pageNumber(0).pageSize(3).build();
        Page<Cluster> clusters = clusterRepository.search(criteria, pageable, Optional.empty());
        assertAll(
            () -> assertThat(clusters.getContent().size()).isEqualTo(1),
            () -> assertThat(clusters.getPageNumber()).isEqualTo(0),
            () -> assertThat(clusters.getPageElements()).isEqualTo(1),
            () -> assertThat(clusters.getTotalElements()).isEqualTo(1),
            () -> assertThat(clusters.getContent().stream().map(Cluster::getName).toList()).isEqualTo(List.of("cluster-4"))
        );
    }

    @Test
    public void should_update_groups() throws Exception {
        String clusterId = "cluster-id-1";

        Cluster cluster = clusterRepository.findById(clusterId).orElseThrow();
        assertThat(cluster.getGroups()).isEqualTo(Set.of("group-1", "group-2"));

        clusterRepository.updateGroups(clusterId, Set.of("group-3", "group-4"));

        Cluster updatedCluster = clusterRepository.findById(clusterId).orElseThrow();
        assertThat(updatedCluster.getGroups()).isEqualTo(Set.of("group-3", "group-4"));
    }
}
