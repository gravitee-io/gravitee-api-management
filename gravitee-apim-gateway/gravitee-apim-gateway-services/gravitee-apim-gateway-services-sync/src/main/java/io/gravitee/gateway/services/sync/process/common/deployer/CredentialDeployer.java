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

import io.gravitee.gateway.handlers.api.manager.CredentialManager;
import io.gravitee.gateway.handlers.api.manager.DeployedCredential;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.credential.CredentialDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class CredentialDeployer implements Deployer<CredentialDeployable> {

    private final CredentialManager credentialManager;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(final CredentialDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                credentialManager.deploy(
                    new DeployedCredential(
                        deployable.credentialId(),
                        deployable.environmentId(),
                        deployable.organizationId(),
                        deployable.encryptedSecret(),
                        deployable.updatedAt()
                    )
                );
                log.debug("Credential [{}] deployed", deployable.id());
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to deploy credential [%s].", deployable.id()), e);
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final CredentialDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    @Override
    public Completable undeploy(final CredentialDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                credentialManager.undeploy(deployable.environmentId(), deployable.credentialId());
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to undeploy credential [%s].", deployable.id()), e);
            }
        });
    }

    @Override
    public Completable doAfterUndeployment(final CredentialDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
