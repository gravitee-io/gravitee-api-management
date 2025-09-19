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
package io.gravitee.gateway.services.sync.process.local;

import io.gravitee.common.service.AbstractService;
import io.gravitee.common.utils.RxHelper;
import io.gravitee.gateway.services.sync.SyncManager;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
/*
 * INTERNAL USE ONLY: this synchronizer has been introduced to facilitate the tests of the gateway. It's not production ready.
 */
public class LocalSyncManager extends AbstractService<SyncManager> implements SyncManager {

    private static final int EXPONENTIAL_BACKOFF_RETRY_INITIAL_DELAY_MS = 1_000;
    private static final int EXPONENTIAL_BACKOFF_RETRY_MAX_DELAY_MS = 10_000;
    private static final double EXPONENTIAL_BACKOFF_RETRY_FACTOR = 1.5;

    private final List<LocalSynchronizer> synchronizers;
    private final DistributedSyncService distributedSyncService;
    private final String localRegistryPath;

    private Disposable watcherDisposable;

    private final AtomicBoolean synced = new AtomicBoolean();

    public LocalSyncManager(
        final String localRegistryPath,
        final List<LocalSynchronizer> synchronizers,
        final DistributedSyncService distributedSyncService
    ) {
        this.synchronizers = synchronizers;
        this.distributedSyncService = distributedSyncService;
        this.localRegistryPath = localRegistryPath;
    }

    @Override
    protected void doStart() {
        log.debug("Starting local synchronization process");
        if (!distributedSyncService.isEnabled() || distributedSyncService.isPrimaryNode()) {
            if (this.localRegistryPath != null && !this.localRegistryPath.isEmpty()) {
                File localRegistryDir = new File(this.localRegistryPath);
                if (localRegistryDir.isDirectory()) {
                    Path registry = Paths.get(localRegistryPath);
                    try {
                        WatchService watcher = registry.getFileSystem().newWatchService();
                        registry.register(
                            watcher,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY
                        );

                        watcherDisposable = sync(localRegistryDir)
                            .doOnComplete(() -> {
                                log.debug("Moving to ready state as all resources have been synchronized");
                                synced.set(true);
                            })
                            .andThen(watch(registry, watcher))
                            .doOnError(throwable -> log.error("An error occurred during local synchronization. Restarting ...", throwable))
                            .retryWhen(
                                RxHelper.retryExponentialBackoff(
                                    EXPONENTIAL_BACKOFF_RETRY_INITIAL_DELAY_MS,
                                    EXPONENTIAL_BACKOFF_RETRY_MAX_DELAY_MS,
                                    TimeUnit.MILLISECONDS,
                                    EXPONENTIAL_BACKOFF_RETRY_FACTOR
                                )
                            )
                            .subscribe();
                    } catch (IOException ex) {
                        log.error("An error occurred during local synchronization", ex);
                    }
                } else {
                    log.error("Invalid API definitions registry directory, {} is not a directory.", localRegistryDir.getAbsolutePath());
                }
            } else {
                log.error("Local API definitions registry path is not specified.");
            }
        } else {
            log.warn("Local synchronization is disabled as distributed sync is enabled, and current node is secondary.");
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

    private Completable sync(File localRegistryDir) {
        return Flowable.fromIterable(synchronizers)
            .doOnNext(sync -> log.debug("{} will synchronize all resources ...", sync))
            .concatMapCompletable(sync -> sync.synchronize(localRegistryDir));
    }

    private Completable watch(Path localRegistryPath, WatchService watcher) {
        return Flowable.fromIterable(synchronizers)
            .doOnNext(sync -> log.debug("{} will start watching on resources ...", sync))
            .concatMapCompletable(sync -> sync.watch(localRegistryPath, watcher));
    }
}
