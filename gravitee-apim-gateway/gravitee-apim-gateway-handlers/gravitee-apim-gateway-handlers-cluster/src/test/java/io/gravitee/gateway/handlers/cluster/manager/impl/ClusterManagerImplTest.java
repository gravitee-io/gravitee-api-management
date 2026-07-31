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
package io.gravitee.gateway.handlers.cluster.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.cluster.ClusterEvent;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster;
import io.gravitee.definition.model.cluster.ReactableCluster;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClusterManagerImplTest {

    @Mock
    private EventManager eventManager;

    private ClusterManagerImpl cut;

    @BeforeEach
    void setUp() {
        cut = new ClusterManagerImpl(eventManager);
    }

    @Test
    void should_deploy_new_cluster() {
        ReactableCluster cluster = KafkaClusterReactableCluster.builder()
            .id("cluster-1")
            .crossId("my-cluster")
            .deployedAt(new Date())
            .build();

        boolean result = cut.register(cluster);

        assertThat(result).isTrue();
        verify(eventManager).publishEvent(ClusterEvent.DEPLOY, cluster);
        assertThat(cut.get("cluster-1")).isEqualTo(cluster);
    }

    @Test
    void should_update_cluster_when_newer() {
        ReactableCluster oldCluster = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(new Date(1000)).build();
        cut.register(oldCluster);

        ReactableCluster newCluster = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(new Date(2000)).build();

        boolean result = cut.register(newCluster);

        assertThat(result).isTrue();
        verify(eventManager).publishEvent(ClusterEvent.UPDATE, newCluster);
        assertThat(cut.get("cluster-1")).isEqualTo(newCluster);
    }

    @Test
    void should_not_update_cluster_when_older() {
        ReactableCluster oldCluster = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(new Date(2000)).build();
        cut.register(oldCluster);

        ReactableCluster olderCluster = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(new Date(1000)).build();

        boolean result = cut.register(olderCluster);

        assertThat(result).isFalse();
        verify(eventManager, never()).publishEvent(eq(ClusterEvent.UPDATE), any());
    }

    @Test
    void should_update_cluster_when_version_advances_within_same_deployed_at_second() {
        // Two redeploys landing in the same wall-clock second carry an identical (second-resolution)
        // deployedAt, but the monotonic version still advances. The update must not be dropped.
        Date sameSecond = new Date(1_698_000_000L);
        ReactableCluster oldCluster = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(sameSecond).version(1).build();
        cut.register(oldCluster);

        ReactableCluster newCluster = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(sameSecond).version(2).build();

        boolean result = cut.register(newCluster);

        assertThat(result).isTrue();
        verify(eventManager).publishEvent(ClusterEvent.UPDATE, newCluster);
        assertThat(cut.get("cluster-1")).isEqualTo(newCluster);
    }

    @Test
    void should_not_update_cluster_when_version_unchanged() {
        // Idempotent re-delivery of the same event (same version) must not republish an update.
        Date sameSecond = new Date(1_698_000_000L);
        ReactableCluster cluster = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(sameSecond).version(1).build();
        cut.register(cluster);

        ReactableCluster redelivered = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(sameSecond).version(1).build();

        boolean result = cut.register(redelivered);

        assertThat(result).isFalse();
        verify(eventManager, never()).publishEvent(eq(ClusterEvent.UPDATE), any());
    }

    @Test
    void should_update_cluster_when_candidate_has_version_and_current_does_not() {
        // Transition case: the deployed cluster comes from an old event without version, the new
        // event carries one. Even with an equal (second-resolution) deployedAt, the versioned
        // candidate must win — falling back to deployedAt would silently drop the update.
        Date sameSecond = new Date(1_698_000_000L);
        ReactableCluster oldCluster = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(sameSecond).build();
        cut.register(oldCluster);

        ReactableCluster newCluster = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(sameSecond).version(1).build();

        boolean result = cut.register(newCluster);

        assertThat(result).isTrue();
        verify(eventManager).publishEvent(ClusterEvent.UPDATE, newCluster);
        assertThat(cut.get("cluster-1")).isEqualTo(newCluster);
    }

    @Test
    void should_not_update_cluster_when_candidate_has_no_version_and_current_does() {
        // A versioned deployment must never be superseded by an unversioned (older-producer) event.
        ReactableCluster versioned = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(new Date(1000)).version(1).build();
        cut.register(versioned);

        ReactableCluster unversioned = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(new Date(5000)).build();

        boolean result = cut.register(unversioned);

        assertThat(result).isFalse();
        verify(eventManager, never()).publishEvent(eq(ClusterEvent.UPDATE), any());
    }

    @Test
    void should_undeploy_cluster() {
        ReactableCluster cluster = KafkaClusterReactableCluster.builder().id("cluster-1").deployedAt(new Date()).build();
        cut.register(cluster);

        cut.unregister("cluster-1");

        verify(eventManager).publishEvent(ClusterEvent.UNDEPLOY, cluster);
        assertThat(cut.get("cluster-1")).isNull();
    }

    @Test
    void should_not_publish_event_when_unregistering_unknown_cluster() {
        cut.unregister("unknown");

        verify(eventManager, never()).publishEvent(eq(ClusterEvent.UNDEPLOY), any());
    }

    @Test
    void should_list_all_clusters() {
        cut.register(KafkaClusterReactableCluster.builder().id("c1").deployedAt(new Date()).build());
        cut.register(KafkaClusterReactableCluster.builder().id("c2").deployedAt(new Date()).build());

        assertThat(cut.clusters()).hasSize(2);
    }
}
