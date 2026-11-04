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
package io.gravitee.definition.model.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class KafkaReactableClusterTest {

    @Test
    void should_keep_the_constructor_without_backend_metadata_max_age() {
        var connection = KafkaClusterReactableCluster.KafkaClusterConnection.builder().crossId("main").build();
        var backend = KafkaVirtualClusterReactableCluster.KafkaVirtualClusterBackend.builder().clusterCrossId("c").build();

        assertThat(new KafkaClusterReactableCluster(List.of(connection)).getBackendMetadataMaxAge()).isNull();
        assertThat(new KafkaVirtualClusterReactableCluster(List.of(backend)).getBackendMetadataMaxAge()).isNull();
    }

    @Test
    void should_carry_the_backend_metadata_max_age() {
        assertThat(KafkaClusterReactableCluster.builder().backendMetadataMaxAge(30).build().getBackendMetadataMaxAge()).isEqualTo(30);
        assertThat(KafkaVirtualClusterReactableCluster.builder().backendMetadataMaxAge(0).build().getBackendMetadataMaxAge()).isZero();
    }
}
