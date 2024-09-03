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
package io.gravitee.gateway.services.sync.process.kubernetes;

import io.gravitee.common.service.AbstractService;
import io.gravitee.common.utils.RxHelper;
import io.gravitee.gateway.services.sync.SyncManager;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.node.api.Node;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class KubernetesSyncManager extends AbstractService<SyncManager> implements SyncManager {

    private static final int EXPONENTIAL_BACKOFF_RETRY_INITIAL_DELAY_MS = 1_000;
    private static final int EXPONENTIAL_BACKOFF_RETRY_MAX_DELAY_MS = 10_000;
    private static final double EXPONENTIAL_BACKOFF_RETRY_FACTOR = 1.5;

    private final List<KubernetesSynchronizer> synchronizers;
    private final DistributedSyncService distributedSyncService;
    private final Node node;

    private Disposable watcherDisposable;

    private final AtomicBoolean synced = new AtomicBoolean();

    public KubernetesSyncManager(
        final Node node,
        final List<KubernetesSynchronizer> synchronizers,
        final DistributedSyncService distributedSyncService
    ) {
        this.synchronizers = synchronizers;
        this.distributedSyncService = distributedSyncService;
        this.node = node;
    }

    @Override
    protected void doStart() {
        log.debug("Starting kubernetes synchronization process");
        if (!distributedSyncService.isEnabled() || distributedSyncService.isPrimaryNode()) {
            watcherDisposable =
                sync()
                    .doOnComplete(() -> {
                        log.debug("Moving to ready state as all resources have been synchronized");
                        synced.set(true);
                    })
                    .andThen(watch())
                    .doOnError(throwable -> log.error("An error occurred during Kubernetes synchronization. Restarting ...", throwable))
                    .retryWhen(
                        RxHelper.retryExponentialBackoff(
                            EXPONENTIAL_BACKOFF_RETRY_INITIAL_DELAY_MS,
                            EXPONENTIAL_BACKOFF_RETRY_MAX_DELAY_MS,
                            TimeUnit.MILLISECONDS,
                            EXPONENTIAL_BACKOFF_RETRY_FACTOR
                        )
                    )
                    .subscribe();
        } else {
            log.warn("Kubernetes synchronization is disabled as distributed sync is enabled, and current node is secondary.");
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
        return synced.get();
    }

    private Completable sync() {
        return Flowable
            .fromIterable(synchronizers)
            .doOnNext(sync -> log.debug("{} will synchronize all resources ...", sync))
            .concatMapCompletable(sync -> sync.synchronize((Set<String>) node.metadata().get(Node.META_ENVIRONMENTS)));
    }

    private Completable watch() {
        return Flowable
            .fromIterable(synchronizers)
            .doOnNext(sync -> log.debug("{} will start watching on resources ...", sync))
            .concatMapCompletable(sync -> sync.watch((Set<String>) node.metadata().get(Node.META_ENVIRONMENTS)));
    }
}
