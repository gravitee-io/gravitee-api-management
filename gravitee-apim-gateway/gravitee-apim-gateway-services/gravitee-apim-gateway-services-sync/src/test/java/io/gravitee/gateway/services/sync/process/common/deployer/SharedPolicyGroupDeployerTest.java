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

import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.manager.SharedPolicyGroupManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup.SharedPolicyGroupReactorDeployable;
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
class SharedPolicyGroupDeployerTest {

    private SharedPolicyGroupDeployer cut;

    @Mock
    private SharedPolicyGroupManager sharedPolicyGroupManager;

    @BeforeEach
    void setUp() {
        cut = new SharedPolicyGroupDeployer(sharedPolicyGroupManager, new NoopDistributedSyncService());
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_shared_policy_group() {
            ReactableSharedPolicyGroup reactable = ReactableSharedPolicyGroup.builder().id("id").build();
            SharedPolicyGroupReactorDeployable sharedPolicyGroup = SharedPolicyGroupReactorDeployable
                .builder()
                .sharedPolicyGroupId("id")
                .reactableSharedPolicyGroup(reactable)
                .build();

            cut.deploy(sharedPolicyGroup).test().assertComplete();
            verify(sharedPolicyGroupManager).register(reactable);
        }

        @Test
        void should_return_error_when_shared_policy_group_manager_throw_exception() {
            ReactableSharedPolicyGroup reactable = ReactableSharedPolicyGroup
                .builder()
                .id("id")
                .definition(SharedPolicyGroup.builder().build())
                .build();
            SharedPolicyGroupReactorDeployable sharedPolicyGroup = SharedPolicyGroupReactorDeployable
                .builder()
                .sharedPolicyGroupId("id")
                .reactableSharedPolicyGroup(reactable)
                .build();
            doThrow(new SyncException("error")).when(sharedPolicyGroupManager).register(reactable);
            cut.deploy(sharedPolicyGroup).test().assertFailure(SyncException.class);
            verify(sharedPolicyGroupManager).register(reactable);
        }

        @Test
        void should_ignore_do_post_action() {
            cut.doAfterDeployment(null).test().assertComplete();
            verifyNoInteractions(sharedPolicyGroupManager);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_shared_policy_group() {
            ReactableSharedPolicyGroup reactable = ReactableSharedPolicyGroup.builder().id("id").build();
            SharedPolicyGroupReactorDeployable sharedPolicyGroup = SharedPolicyGroupReactorDeployable
                .builder()
                .sharedPolicyGroupId("id")
                .reactableSharedPolicyGroup(reactable)
                .build();
            cut.undeploy(sharedPolicyGroup).test().assertComplete();
            verify(sharedPolicyGroupManager).unregister("id");
        }

        @Test
        void should_complete_on_error_when_shared_policy_group_manager_throw_exception() {
            ReactableSharedPolicyGroup reactable = ReactableSharedPolicyGroup
                .builder()
                .id("id")
                .definition(SharedPolicyGroup.builder().build())
                .build();
            SharedPolicyGroupReactorDeployable sharedPolicyGroup = SharedPolicyGroupReactorDeployable
                .builder()
                .sharedPolicyGroupId("id")
                .reactableSharedPolicyGroup(reactable)
                .build();
            doThrow(new SyncException("error")).when(sharedPolicyGroupManager).unregister("id");
            cut.undeploy(sharedPolicyGroup).test().assertFailure(SyncException.class);
            verify(sharedPolicyGroupManager).unregister("id");
        }
    }
}
