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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.handlers.api.manager.CredentialManager;
import io.gravitee.gateway.handlers.api.manager.DeployedCredential;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.credential.CredentialDeployable;
import io.reactivex.rxjava3.core.Completable;
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
class CredentialDeployerTest {

    @Mock
    private CredentialManager credentialManager;

    @Mock
    private DistributedSyncService distributedSyncService;

    private CredentialDeployer cut;

    @BeforeEach
    void beforeEach() {
        cut = new CredentialDeployer(credentialManager, distributedSyncService);
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_credential() {
            cut.deploy(deployable(SyncAction.DEPLOY)).test().assertComplete();

            verify(credentialManager).deploy(new DeployedCredential("credential-1", "env-1", "org-1", "ciphertext", 1234L));
        }

        @Test
        void should_return_error_when_credential_manager_throws_exception() {
            doThrow(new RuntimeException("error")).when(credentialManager).deploy(any());

            cut.deploy(deployable(SyncAction.DEPLOY)).test().assertFailure(SyncException.class);
        }

        @Test
        void should_distribute_after_deployment() {
            CredentialDeployable deployable = deployable(SyncAction.DEPLOY);
            when(distributedSyncService.distributeIfNeeded(deployable)).thenReturn(Completable.complete());

            cut.doAfterDeployment(deployable).test().assertComplete();

            verify(distributedSyncService).distributeIfNeeded(deployable);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_credential() {
            cut.undeploy(deployable(SyncAction.UNDEPLOY)).test().assertComplete();

            verify(credentialManager).undeploy("env-1", "credential-1");
        }

        @Test
        void should_return_error_when_credential_manager_throws_exception() {
            doThrow(new RuntimeException("error")).when(credentialManager).undeploy(any(), any());

            cut.undeploy(deployable(SyncAction.UNDEPLOY)).test().assertFailure(SyncException.class);
        }

        @Test
        void should_distribute_after_undeployment() {
            CredentialDeployable deployable = deployable(SyncAction.UNDEPLOY);
            when(distributedSyncService.distributeIfNeeded(deployable)).thenReturn(Completable.complete());

            cut.doAfterUndeployment(deployable).test().assertComplete();

            verify(distributedSyncService).distributeIfNeeded(deployable);
        }
    }

    private static CredentialDeployable deployable(SyncAction syncAction) {
        return CredentialDeployable.builder()
            .credentialId("credential-1")
            .environmentId("env-1")
            .organizationId("org-1")
            .encryptedSecret("ciphertext")
            .updatedAt(1234L)
            .syncAction(syncAction)
            .build();
    }
}
