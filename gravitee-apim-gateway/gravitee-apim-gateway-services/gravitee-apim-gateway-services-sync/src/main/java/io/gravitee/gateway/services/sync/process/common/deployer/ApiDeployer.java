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

import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.AuthzRegistry;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEnginePort;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityIdExtractor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@CustomLog
public class ApiDeployer implements Deployer<ApiReactorDeployable> {

    private final ApiManager apiManager;
    private final PlanService planService;
    private final DistributedSyncService distributedSyncService;
    private final AuthzRegistry authzRegistry;
    private final AuthzEntityIdExtractor authzEntityIdExtractor;
    private final AuthzEnginePort authzEnginePort;

    @Override
    public Completable deploy(final ApiReactorDeployable deployable) {
        Set<String> entityIds = extractSafely(deployable.reactableApi());
        return stageAuthzResourcesOnDeploy(deployable, entityIds).andThen(
            Completable.fromRunnable(() -> {
                ReactableApi<?> reactableApi = deployable.reactableApi();
                try {
                    apiManager.register(reactableApi);
                    log.debug("Api [{}] deployed ", deployable.apiId());
                } catch (Exception e) {
                    throw new SyncException(
                        String.format("An error occurred when trying to deploy api %s [%s].", reactableApi.getName(), reactableApi.getId()),
                        e
                    );
                }
            })
        );
    }

    @Override
    public Completable doAfterDeployment(final ApiReactorDeployable deployable) {
        Set<String> entityIds = extractSafely(deployable.reactableApi());
        return Completable.fromRunnable(() -> {
            planService.register(deployable);
            authzRegistry.register(entityIds);
        }).andThen(distributedSyncService.distributeIfNeeded(deployable));
    }

    @Override
    public Completable undeploy(final ApiReactorDeployable deployable) {
        Set<String> entityIds = extractSafely(deployable.reactableApi());
        return Completable.fromRunnable(() -> {
            try {
                authzRegistry.unregister(entityIds);
                planService.unregister(deployable);
                apiManager.unregister(deployable.apiId());
                log.debug("Api [{}] undeployed ", deployable.apiId());
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to undeploy api [%s].", deployable.apiId()), e);
            }
        }).andThen(stageAuthzResourcesOnUndeploy(deployable, entityIds));
    }

    @Override
    public Completable doAfterUndeployment(final ApiReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    private Set<String> extractSafely(final ReactableApi<?> reactableApi) {
        if (reactableApi == null) {
            return Set.of();
        }
        try {
            return authzEntityIdExtractor.extract(reactableApi);
        } catch (RuntimeException e) {
            log.warn("Authz entityId extraction threw for API [{}] — proceeding with empty set", reactableApi.getId(), e);
            return Set.of();
        }
    }

    private Completable stageAuthzResourcesOnDeploy(final ApiReactorDeployable deployable, final Set<String> entityIds) {
        return Completable.defer(() -> {
            if (entityIds.isEmpty()) {
                return Completable.complete();
            }
            return Flowable.fromIterable(entityIds)
                .concatMapCompletable(entityId ->
                    authzEnginePort
                        .addOrUpdateEntity(AuthzEntityIdExtractor.toResourceEngineUid(entityId), Map.of(), List.of())
                        .onErrorComplete(t -> {
                            log.warn("Authz engine addOrUpdateEntity failed for [{}] on API [{}]", entityId, deployable.apiId(), t);
                            return true;
                        })
                )
                .andThen(Completable.defer(authzEnginePort::commit))
                .onErrorComplete(t -> {
                    log.warn(
                        "Authz engine commit failed for API [{}]; entities will be retried on the next API redeploy " +
                            "(events path filters auto-derived ids)",
                        deployable.apiId(),
                        t
                    );
                    return true;
                });
        });
    }

    private Completable stageAuthzResourcesOnUndeploy(final ApiReactorDeployable deployable, final Set<String> entityIds) {
        return Completable.defer(() -> {
            if (entityIds.isEmpty()) {
                return Completable.complete();
            }
            return Flowable.fromIterable(entityIds)
                .concatMapCompletable(entityId ->
                    authzEnginePort
                        .removeEntity(AuthzEntityIdExtractor.toResourceEngineUid(entityId))
                        .onErrorComplete(t -> {
                            log.warn("Authz engine removeEntity failed for [{}] on API [{}]", entityId, deployable.apiId(), t);
                            return true;
                        })
                )
                .andThen(Completable.defer(authzEnginePort::commit))
                .onErrorComplete(t -> {
                    log.warn(
                        "Authz engine commit failed for undeploy of API [{}]; orphan entities will linger until next gateway restart",
                        deployable.apiId(),
                        t
                    );
                    return true;
                });
        });
    }
}
