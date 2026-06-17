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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.api.event.ApiProductChangedEvent;
import io.gravitee.gateway.handlers.api.event.ApiProductEventType;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.CustomLog;

/**
 * Subscribes to API Product DEPLOY / UPDATE events and runs a single ordered
 * pipeline on the sync deployer thread pool:
 * <ol>
 *   <li>Re-sync member APIs (deploy newly-eligible ones via the normal sync path)</li>
 *   <li>Re-evaluate shard eligibility and undeploy any APIs that are no longer eligible</li>
 * </ol>
 * This ordering guarantees that a newly-eligible API is deployed <em>before</em>
 * an ineligible one is undeployed, eliminating the brief unavailability window
 * that would occur if undeploy were handled independently.
 */
@CustomLog
public class RepositoryApiMemberResyncTrigger {

    private final ApiSynchronizer apiSynchronizer;
    private final ApiManager apiManager;
    private final ThreadPoolExecutor syncDeployerExecutor;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public RepositoryApiMemberResyncTrigger(
        ApiSynchronizer apiSynchronizer,
        ApiManager apiManager,
        ThreadPoolExecutor syncDeployerExecutor,
        EventManager eventManager
    ) {
        this.apiSynchronizer = apiSynchronizer;
        this.apiManager = apiManager;
        this.syncDeployerExecutor = syncDeployerExecutor;

        eventManager.subscribeForEvents(
            event -> {
                if (event.content() instanceof ApiProductChangedEvent productEvent) {
                    handleProductChange(productEvent.getProductId(), productEvent.getEnvironmentId(), productEvent.getApiIds());
                }
            },
            ApiProductEventType.DEPLOY,
            ApiProductEventType.UPDATE
        );
    }

    private void handleProductChange(String productId, String environmentId, Set<String> apiIds) {
        if (environmentId == null || apiIds == null || apiIds.isEmpty()) {
            return;
        }
        final Disposable[] holder = new Disposable[1];
        holder[0] = Completable.defer(() -> {
            log.debug("Scheduling resync of {} member API(s) in environment [{}]", apiIds.size(), environmentId);
            return apiSynchronizer
                .resyncMemberApis(apiIds, Set.of(environmentId))
                .andThen(
                    Completable.fromAction(() -> {
                        log.debug("Resync done — re-evaluating eligibility of {} API(s) for product [{}]", apiIds.size(), productId);
                        apiManager.reEvaluateAfterProductChange(productId, apiIds);
                    })
                );
        })
            .subscribeOn(Schedulers.from(syncDeployerExecutor))
            .doFinally(() -> {
                Disposable disposable = holder[0];
                if (disposable != null) {
                    disposables.remove(disposable);
                }
            })
            .subscribe(
                () -> log.debug("Member API resync + re-evaluation completed for product [{}] in env [{}]", productId, environmentId),
                error ->
                    log.error(
                        "Member API resync pipeline failed for product [{}] in env [{}]: {}",
                        productId,
                        environmentId,
                        error.getMessage(),
                        error
                    )
            );
        disposables.add(holder[0]);
    }

    public void dispose() {
        disposables.dispose();
    }
}
