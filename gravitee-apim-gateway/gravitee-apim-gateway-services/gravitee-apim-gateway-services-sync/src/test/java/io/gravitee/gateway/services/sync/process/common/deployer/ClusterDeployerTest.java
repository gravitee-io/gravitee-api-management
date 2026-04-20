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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster;
import io.gravitee.gateway.handlers.cluster.manager.ClusterManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.cluster.ClusterReactorDeployable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClusterDeployerTest {

    private ClusterDeployer cut;

    @Mock
    private ClusterManager clusterManager;

    @BeforeEach
    void setUp() {
        cut = new ClusterDeployer(clusterManager);
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_cluster() {
            var reactable = KafkaClusterReactableCluster.builder().id("id").build();
            var deployable = ClusterReactorDeployable.builder().clusterId("id").reactableCluster(reactable).build();

            cut.deploy(deployable).test().assertComplete();
            verify(clusterManager).register(reactable);
        }

        @Test
        void should_return_error_when_cluster_manager_throws() {
            var reactable = KafkaClusterReactableCluster.builder().id("id").build();
            var deployable = ClusterReactorDeployable.builder().clusterId("id").reactableCluster(reactable).build();

            doThrow(new SyncException("error")).when(clusterManager).register(reactable);
            cut.deploy(deployable).test().assertFailure(SyncException.class);
        }

        @Test
        void should_ignore_do_post_action() {
            cut.doAfterDeployment(null).test().assertComplete();
            verifyNoInteractions(clusterManager);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_cluster() {
            var deployable = ClusterReactorDeployable.builder().clusterId("id").build();

            cut.undeploy(deployable).test().assertComplete();
            verify(clusterManager).unregister("id");
        }

        @Test
        void should_return_error_when_cluster_manager_throws() {
            var deployable = ClusterReactorDeployable.builder().clusterId("id").build();

            doThrow(new SyncException("error")).when(clusterManager).unregister("id");
            cut.undeploy(deployable).test().assertFailure(SyncException.class);
        }
    }
}
