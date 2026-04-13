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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.repository.management.model.Cluster;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ClusterCrossIdUpgraderTest {

    @Mock
    private ClusterRepository clusterRepository;

    @InjectMocks
    private ClusterCrossIdUpgrader upgrader;

    @Test
    public void should_generate_crossId_for_cluster_without_one() throws Exception {
        var cluster = Cluster.builder().id("cluster-id-1").name("My Cluster Name").build();

        when(clusterRepository.findAll()).thenReturn(Set.of(cluster));
        assertThat(upgrader.upgrade()).isTrue();

        var captor = ArgumentCaptor.forClass(Cluster.class);
        verify(clusterRepository).update(captor.capture());

        assertThat(captor.getValue().getCrossId()).isEqualTo("my-cluster-name");
    }

    @Test
    public void should_use_id_when_name_is_null() throws Exception {
        var cluster = Cluster.builder().id("cluster-id-1").build();

        when(clusterRepository.findAll()).thenReturn(Set.of(cluster));
        assertThat(upgrader.upgrade()).isTrue();

        var captor = ArgumentCaptor.forClass(Cluster.class);
        verify(clusterRepository).update(captor.capture());

        assertThat(captor.getValue().getCrossId()).isEqualTo("cluster-id-1");
    }

    @Test
    public void should_skip_cluster_with_existing_crossId() throws Exception {
        var cluster = Cluster.builder().id("cluster-id-1").name("My Cluster").crossId("existing-cross-id").build();

        when(clusterRepository.findAll()).thenReturn(Set.of(cluster));
        assertThat(upgrader.upgrade()).isTrue();

        verify(clusterRepository, never()).update(any());
    }

    @Test
    public void should_return_false_on_exception() throws Exception {
        when(clusterRepository.findAll()).thenThrow(new RuntimeException());
        assertThat(upgrader.upgrade()).isFalse();
    }

    @Test
    public void should_continue_on_update_failure() throws Exception {
        var cluster1 = Cluster.builder().id("cluster-1").name("Cluster 1").build();
        var cluster2 = Cluster.builder().id("cluster-2").name("Cluster 2").build();

        when(clusterRepository.findAll()).thenReturn(Set.of(cluster1, cluster2));
        when(clusterRepository.update(any())).thenThrow(new RuntimeException("update failed")).thenReturn(cluster2);

        assertThat(upgrader.upgrade()).isTrue();
        verify(clusterRepository, times(2)).update(any());
    }

    @Test
    public void should_have_correct_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.CLUSTER_CROSS_ID_UPGRADER);
    }
}
