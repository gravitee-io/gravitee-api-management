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

import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEnginePort;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzSchemaReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class AuthzSchemaDeployer implements Deployer<AuthzSchemaReactorDeployable> {

    private final AuthzEnginePort enginePort;

    @Override
    public Completable deploy(AuthzSchemaReactorDeployable deployable) {
        Set<String> removed = deployable.removedTargetPdpIds();
        Completable eviction = removed == null || removed.isEmpty()
            ? Completable.complete()
            : enginePort.removeSchema(deployable.environmentId(), deployable.docId(), removed);
        return eviction
            .andThen(
                enginePort.addOrUpdateSchema(
                    deployable.environmentId(),
                    deployable.docId(),
                    deployable.name(),
                    deployable.schemaText(),
                    deployable.targetPdpIds(),
                    deployable.updatedAt()
                )
            )
            .doOnComplete(() -> log.debug("Authz schema '{}' staged for next commit", deployable.docId()))
            .doOnError(e -> log.warn("Failed to stage authz schema '{}': {}", deployable.docId(), e.getMessage()));
    }

    // TODO(AUTHZ-32): distributed replay for schema documents, tracked there with the rest of distributed
    // sync. Until it lands, a secondary node with distributed sync enabled receives no schema at all:
    // DefaultSyncManager runs either the distributed synchronizers or the repository ones and never both,
    // so there is no fallback cycle. Opt-in only, the default wiring is NoopDistributedSyncService.
    // No DistributedSyncService is injected until there is something to distribute.
    @Override
    public Completable doAfterDeployment(AuthzSchemaReactorDeployable deployable) {
        return Completable.complete();
    }

    @Override
    public Completable undeploy(AuthzSchemaReactorDeployable deployable) {
        return enginePort
            .removeSchema(deployable.environmentId(), deployable.docId(), deployable.targetPdpIds())
            .doOnComplete(() -> log.debug("Authz schema '{}' staged for removal on next commit", deployable.docId()))
            .doOnError(e -> log.warn("Failed to stage authz schema '{}' removal: {}", deployable.docId(), e.getMessage()));
    }

    @Override
    public Completable doAfterUndeployment(AuthzSchemaReactorDeployable deployable) {
        return Completable.complete();
    }
}
