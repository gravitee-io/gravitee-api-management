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
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.CustomLog;

/**
 * Subscribes to API Product change events and triggers a re-sync of member APIs
 * through the sync process. Lives in the sync plugin context which has visibility
 * to both the parent gateway context (EventManager) and the sync infrastructure
 * (ApiSynchronizer).
 */
@CustomLog
public class RepositoryApiMemberResyncTrigger {

    private final ApiSynchronizer apiSynchronizer;
    private final ThreadPoolExecutor syncDeployerExecutor;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public RepositoryApiMemberResyncTrigger(
        ApiSynchronizer apiSynchronizer,
        ThreadPoolExecutor syncDeployerExecutor,
        EventManager eventManager
    ) {
        this.apiSynchronizer = apiSynchronizer;
        this.syncDeployerExecutor = syncDeployerExecutor;

        eventManager.subscribeForEvents(
            event -> {
                if (event.content() instanceof ApiProductChangedEvent productEvent) {
                    resyncMemberApis(productEvent.getEnvironmentId(), productEvent.getApiIds());
                }
            },
            ApiProductEventType.DEPLOY,
            ApiProductEventType.UPDATE
        );
    }

    private void resyncMemberApis(String environmentId, Set<String> apiIds) {
        if (environmentId == null || apiIds == null || apiIds.isEmpty()) {
            return;
        }
        Disposable d = Completable.fromAction(() ->
            log.debug("Scheduling resync of {} member API(s) in environment [{}]", apiIds.size(), environmentId)
        )
            .andThen(apiSynchronizer.resyncMemberApis(apiIds, Set.of(environmentId)))
            .subscribeOn(Schedulers.from(syncDeployerExecutor))
            .subscribe(
                () -> log.debug("Member API resync completed for environment [{}]", environmentId),
                error -> log.error("Member API resync failed for environment [{}]: {}", environmentId, error.getMessage(), error)
            );
        disposables.add(d);
    }

    public void dispose() {
        disposables.dispose();
    }
}
