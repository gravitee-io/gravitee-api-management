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

import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.definition.model.cluster.ClusterEvent;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster;
import io.gravitee.definition.model.cluster.ReactableCluster;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
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

    @Test
    void should_resolve_cluster_by_cross_id_and_environment() {
        ReactableCluster envA = cluster("c1", "shared-cross-id", "env-a");
        ReactableCluster envB = cluster("c2", "shared-cross-id", "env-b");
        cut.register(envA);
        cut.register(envB);

        assertThat(cut.get("shared-cross-id", "env-a")).isSameAs(envA);
        assertThat(cut.get("shared-cross-id", "env-b")).isSameAs(envB);
        assertThat(cut.get("shared-cross-id", "env-c")).isNull();
        assertThat(cut.get("unknown", "env-a")).isNull();
    }

    @Test
    void should_resolve_the_updated_cluster_by_cross_id() {
        cut.register(KafkaClusterReactableCluster.builder().id("c1").crossId("my-cluster").environmentId("env-a").version(1).build());
        ReactableCluster updated = KafkaClusterReactableCluster.builder()
            .id("c1")
            .crossId("my-cluster")
            .environmentId("env-a")
            .version(2)
            .build();

        cut.register(updated);

        assertThat(cut.get("my-cluster", "env-a")).isSameAs(updated);
    }

    @Test
    void should_not_resolve_an_undeployed_cluster_by_cross_id() {
        cut.register(cluster("c1", "my-cluster", "env-a"));

        cut.unregister("c1");

        assertThat(cut.get("my-cluster", "env-a")).isNull();
    }

    @Test
    void should_not_resolve_null_cross_id_or_environment() {
        cut.register(cluster("c1", "my-cluster", "env-a"));

        assertThat(cut.get(null, "env-a")).isNull();
        assertThat(cut.get("my-cluster", null)).isNull();
    }

    @Test
    void should_discover_the_secrets_of_a_cluster_before_deploying_it() {
        KafkaClusterReactableCluster cluster = cluster("c1", "my-cluster", "env-a");
        cluster.setVersion(3);

        cut.register(cluster);

        InOrder inOrder = Mockito.inOrder(eventManager);
        inOrder
            .verify(eventManager)
            .publishEvent(SecretDiscoveryEventType.DISCOVER, new SecretDiscoveryEvent("env-a", cluster, new DefinitionMetadata("3")));
        inOrder.verify(eventManager).publishEvent(ClusterEvent.DEPLOY, cluster);
    }

    @Test
    void should_discover_the_new_version_and_revoke_the_previous_one_on_update() {
        KafkaClusterReactableCluster previous = cluster("c1", "my-cluster", "env-a");
        previous.setVersion(1);
        KafkaClusterReactableCluster updated = cluster("c1", "my-cluster", "env-a");
        updated.setVersion(2);
        cut.register(previous);

        cut.register(updated);

        InOrder inOrder = Mockito.inOrder(eventManager);
        inOrder
            .verify(eventManager)
            .publishEvent(SecretDiscoveryEventType.DISCOVER, new SecretDiscoveryEvent("env-a", updated, new DefinitionMetadata("2")));
        inOrder.verify(eventManager).publishEvent(ClusterEvent.UPDATE, updated);
        inOrder
            .verify(eventManager)
            .publishEvent(SecretDiscoveryEventType.REVOKE, new SecretDiscoveryEvent("env-a", previous, new DefinitionMetadata("1")));
    }

    @Test
    void should_not_revoke_the_secrets_it_just_discovered_when_neither_version_is_set() {
        KafkaClusterReactableCluster previous = cluster("c1", "my-cluster", "env-a");
        previous.setDeployedAt(new Date(1000));
        KafkaClusterReactableCluster updated = cluster("c1", "my-cluster", "env-a");
        updated.setDeployedAt(new Date(2000));
        cut.register(previous);

        cut.register(updated);

        verify(eventManager).publishEvent(ClusterEvent.UPDATE, updated);
        verify(eventManager, never()).publishEvent(eq(SecretDiscoveryEventType.REVOKE), any());
    }

    @Test
    void should_revoke_the_secrets_of_an_undeployed_cluster() {
        KafkaClusterReactableCluster cluster = cluster("c1", "my-cluster", "env-a");
        cut.register(cluster);

        cut.unregister("c1");

        verify(eventManager).publishEvent(
            SecretDiscoveryEventType.REVOKE,
            new SecretDiscoveryEvent("env-a", cluster, new DefinitionMetadata(null))
        );
    }

    @Test
    void should_publish_the_cluster_again_when_one_of_its_secrets_changes() {
        KafkaClusterReactableCluster cluster = cluster("c1", "my-cluster", "env-a");
        cut.register(cluster);

        valueChangedListener().onEvent(valueChanged(new Definition("cluster", "c1")));

        verify(eventManager).publishEvent(ClusterEvent.UPDATE, cluster);
    }

    @Test
    void should_ignore_a_secret_change_for_another_definition_or_an_unknown_cluster() {
        cut.register(cluster("c1", "my-cluster", "env-a"));
        EventListener<SecretDiscoveryEventType, SecretDiscoveryEvent> listener = valueChangedListener();

        listener.onEvent(valueChanged(new Definition("shared-policy-group", "c1")));
        listener.onEvent(valueChanged(new Definition("cluster", "unknown")));

        verify(eventManager, never()).publishEvent(eq(ClusterEvent.UPDATE), any());
    }

    @SuppressWarnings("unchecked")
    private EventListener<SecretDiscoveryEventType, SecretDiscoveryEvent> valueChangedListener() {
        ArgumentCaptor<EventListener<SecretDiscoveryEventType, SecretDiscoveryEvent>> captor = ArgumentCaptor.forClass(EventListener.class);
        verify(eventManager).subscribeForEvents(captor.capture(), eq(SecretDiscoveryEventType.VALUE_CHANGED));
        return captor.getValue();
    }

    private static SimpleEvent<SecretDiscoveryEventType, SecretDiscoveryEvent> valueChanged(Definition definition) {
        return new SimpleEvent<>(
            SecretDiscoveryEventType.VALUE_CHANGED,
            new SecretDiscoveryEvent("env-a", definition, new DefinitionMetadata("1"))
        );
    }

    private static KafkaClusterReactableCluster cluster(String id, String crossId, String environmentId) {
        return KafkaClusterReactableCluster.builder().id(id).crossId(crossId).environmentId(environmentId).deployedAt(new Date()).build();
    }
}
