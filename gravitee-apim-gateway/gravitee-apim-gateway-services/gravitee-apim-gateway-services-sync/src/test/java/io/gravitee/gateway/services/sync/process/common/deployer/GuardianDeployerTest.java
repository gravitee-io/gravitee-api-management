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

import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.manager.GuardianManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.guardian.GuardianReactorDeployable;
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
class GuardianDeployerTest {

    @Mock
    private GuardianManager guardianManager;

    private GuardianDeployer cut;

    @BeforeEach
    void setUp() {
        cut = new GuardianDeployer(guardianManager);
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_guardian() {
            ReactableGuardian reactable = ReactableGuardian.builder().id("id").build();
            GuardianReactorDeployable deployable = GuardianReactorDeployable.builder()
                .guardianId("id")
                .reactableGuardian(reactable)
                .build();

            cut.deploy(deployable).test().assertComplete();

            verify(guardianManager).register(reactable);
        }

        @Test
        void should_fail_with_sync_exception_when_guardian_manager_throws() {
            ReactableGuardian reactable = ReactableGuardian.builder().id("id").definition(Guardian.builder().build()).build();
            GuardianReactorDeployable deployable = GuardianReactorDeployable.builder()
                .guardianId("id")
                .reactableGuardian(reactable)
                .build();
            doThrow(new RuntimeException("error")).when(guardianManager).register(reactable);

            cut.deploy(deployable).test().assertFailure(SyncException.class);
        }

        @Test
        void should_do_nothing_after_deployment() {
            cut.doAfterDeployment(null).test().assertComplete();
            verifyNoInteractions(guardianManager);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_guardian_of_the_given_environment() {
            GuardianReactorDeployable deployable = GuardianReactorDeployable.builder().guardianId("id").environmentId("env").build();

            cut.undeploy(deployable).test().assertComplete();

            verify(guardianManager).unregister("id", "env");
        }

        @Test
        void should_undeploy_every_environment_when_the_event_lists_none() {
            GuardianReactorDeployable deployable = GuardianReactorDeployable.builder().guardianId("id").allEnvironments(true).build();

            cut.undeploy(deployable).test().assertComplete();

            verify(guardianManager).unregisterAll("id");
        }

        @Test
        void should_fail_with_sync_exception_when_guardian_manager_throws() {
            GuardianReactorDeployable deployable = GuardianReactorDeployable.builder().guardianId("id").environmentId("env").build();
            doThrow(new RuntimeException("error")).when(guardianManager).unregister("id", "env");

            cut.undeploy(deployable).test().assertFailure(SyncException.class);
        }
    }
}
