/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.kubernetes;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.services.sync.SyncManager;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class KubernetesSyncManager extends AbstractService<SyncManager> implements SyncManager {

    private final List<KubernetesSynchronizer> synchronizers;
    private final boolean enabled;

    private Disposable watcherDisposable;

    @Override
    protected void doStart() {
        if (enabled) {
            log.debug("Starting kubernetes synchronization process");
            if (synchronizers != null) {
                watcherDisposable =
                    Flowable
                        .fromIterable(synchronizers)
                        .concatMapCompletable(KubernetesSynchronizer::synchronize)
                        .doOnError(throwable ->
                            log.error("An error occurred during kubernetes synchronization refresh. Restarting.", throwable)
                        )
                        .retry()
                        .subscribe();
            }
        }
    }

    @Override
    protected void doStop() {
        if (watcherDisposable != null && !watcherDisposable.isDisposed()) {
            watcherDisposable.dispose();
        }
    }

    @Override
    public boolean syncDone() {
        return true;
    }
}
