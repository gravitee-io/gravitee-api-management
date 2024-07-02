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

import io.gravitee.definition.model.v4.environmentflow.EnvironmentFlow;
import io.gravitee.gateway.handlers.environmentflow.manager.EnvironmentFlowManager;
import io.gravitee.gateway.reactive.reactor.environmentflow.ReactableEnvironmentFlow;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.environmentflow.EnvironmentFlowReactorDeployable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EnvironmentFlowDeployerTest {

    private EnvironmentFlowDeployer cut;

    @Mock
    private EnvironmentFlowManager environmentFlowManager;

    @BeforeEach
    void setUp() {
        cut = new EnvironmentFlowDeployer(environmentFlowManager, new NoopDistributedSyncService());
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_environment_flow() {
            ReactableEnvironmentFlow reactable = ReactableEnvironmentFlow.builder().id("id").build();
            EnvironmentFlowReactorDeployable environmentFlow = EnvironmentFlowReactorDeployable
                .builder()
                .environmentFlowId("id")
                .reactableEnvironmentFlow(reactable)
                .build();

            cut.deploy(environmentFlow).test().assertComplete();
            verify(environmentFlowManager).register(reactable);
        }

        @Test
        void should_return_error_when_environment_flow_manager_throw_exception() {
            ReactableEnvironmentFlow reactable = ReactableEnvironmentFlow
                .builder()
                .id("id")
                .definition(EnvironmentFlow.builder().build())
                .build();
            EnvironmentFlowReactorDeployable environmentFlow = EnvironmentFlowReactorDeployable
                .builder()
                .environmentFlowId("id")
                .reactableEnvironmentFlow(reactable)
                .build();
            doThrow(new SyncException("error")).when(environmentFlowManager).register(reactable);
            cut.deploy(environmentFlow).test().assertFailure(SyncException.class);
            verify(environmentFlowManager).register(reactable);
        }

        @Test
        void should_ignore_do_post_action() {
            cut.doAfterDeployment(null).test().assertComplete();
            verifyNoInteractions(environmentFlowManager);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_environment_flow() {
            ReactableEnvironmentFlow reactable = ReactableEnvironmentFlow.builder().id("id").build();
            EnvironmentFlowReactorDeployable environmentFlow = EnvironmentFlowReactorDeployable
                .builder()
                .environmentFlowId("id")
                .reactableEnvironmentFlow(reactable)
                .build();
            cut.undeploy(environmentFlow).test().assertComplete();
            verify(environmentFlowManager).unregister("id");
        }

        @Test
        void should_complete_on_error_when_environment_flow_manager_throw_exception() {
            ReactableEnvironmentFlow reactable = ReactableEnvironmentFlow
                .builder()
                .id("id")
                .definition(EnvironmentFlow.builder().build())
                .build();
            EnvironmentFlowReactorDeployable environmentFlow = EnvironmentFlowReactorDeployable
                .builder()
                .environmentFlowId("id")
                .reactableEnvironmentFlow(reactable)
                .build();
            doThrow(new SyncException("error")).when(environmentFlowManager).unregister("id");
            cut.undeploy(environmentFlow).test().assertFailure(SyncException.class);
            verify(environmentFlowManager).unregister("id");
        }
    }
}
