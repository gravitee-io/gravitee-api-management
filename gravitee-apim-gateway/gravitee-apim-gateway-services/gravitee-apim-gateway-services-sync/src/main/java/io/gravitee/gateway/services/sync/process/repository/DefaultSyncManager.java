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
package io.gravitee.gateway.services.sync.process.repository;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import io.gravitee.common.http.MediaType;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.utils.RxHelper;
import io.gravitee.gateway.services.sync.SyncManager;
import io.gravitee.gateway.services.sync.process.distributed.DistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.handler.SyncHandler;
import io.gravitee.node.api.Node;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DefaultSyncManager extends AbstractService<SyncManager> implements SyncManager {

    private static final String PATH = "/sync";
    /**
     * Add 30s delay before and after to avoid problem with "out of sync" clocks.
     */
    public static final int TIMEFRAME_DELAY = 30000;
    public static final int RETRY_DELAY_MS = 3_000;
    public static final int INITIAL_RETRY_DELAY_MS = 3_000;
    public static final int MAX_RETRY_DELAY_MS = 10_000;

    private final Router router;
    private final Node node;
    private final List<RepositorySynchronizer> synchronizers;
    private final List<DistributedSynchronizer> distributedSynchronizers;
    private final int retryAttempt;
    private final DistributedSyncService distributedSyncService;
    private final int delay;
    private final TimeUnit unit;

    private final AtomicLong syncCounter = new AtomicLong(0);
    private final AtomicBoolean initialSync = new AtomicBoolean(false);
    private final AtomicLong totalSyncOnErrorCounter = new AtomicLong(0);
    private final AtomicBoolean lastSyncOnError = new AtomicBoolean(false);

    private final AtomicReference<String> lastSyncErrorMessage = new AtomicReference<>();

    private final AtomicBoolean isClusterPrimaryNode = new AtomicBoolean(true);
    private long nextFromTime = -1;
    private Disposable refreshDisposable;
    private Route routeHandler;

    public DefaultSyncManager(
        final Router router,
        final Node node,
        final List<RepositorySynchronizer> synchronizers,
        final List<DistributedSynchronizer> distributedSynchronizers,
        final DistributedSyncService distributedSyncService,
        final int delay,
        final TimeUnit unit,
        final int retryAttempt
    ) {
        this.router = router;
        this.node = node;
        this.synchronizers = synchronizers;
        this.distributedSynchronizers = distributedSynchronizers;
        this.distributedSyncService = distributedSyncService;
        this.delay = delay;
        this.unit = unit;
        this.retryAttempt = retryAttempt;
    }

    @Override
    protected void doStart() {
        log.info("Starting sync manager");
        log.debug("Associate a new HTTP handler on {}", PATH);
        SyncHandler syncHandler = new SyncHandler(this);
        routeHandler = router.get(PATH).produces(MediaType.APPLICATION_JSON).handler(syncHandler);

        // Sort synchronizer by order
        synchronizers.sort(Comparator.comparingInt(RepositorySynchronizer::order));

        if (distributedSyncService.isEnabled()) {
            distributedSyncService.validate();
            if (distributedSynchronizers != null) {
                distributedSynchronizers.sort(Comparator.comparingInt(DistributedSynchronizer::order));
            }
            isClusterPrimaryNode.set(distributedSyncService.isPrimaryNode());
        }

        // force synchronization and then schedule next ones
        synchronize()
            .retryWhen(RxHelper.retryExponentialBackoff(INITIAL_RETRY_DELAY_MS, MAX_RETRY_DELAY_MS, MILLISECONDS, 1.5))
            .andThen(
                Completable.fromRunnable(() -> {
                    log.info("Sync service has been scheduled with delay [{} {}]", delay, unit.name());
                    refreshDisposable =
                        Flowable
                            .<Long, Long>generate(
                                () -> 0L,
                                (state, emitter) -> {
                                    emitter.onNext(state);
                                    return state + 1;
                                }
                            )
                            .delay(delay, unit)
                            .rebatchRequests(1)
                            .concatMapCompletable(interval -> synchronize())
                            .retry()
                            .subscribe();
                })
            )
            .subscribe();
    }

    @Override
    protected void doStop() throws Exception {
        // Stop disposable
        if (refreshDisposable != null) {
            refreshDisposable.dispose();
        }
        if (routeHandler != null) {
            routeHandler.remove();
        }
        super.doStop();
    }

    private Completable synchronize() {
        return Completable
            .defer(() -> {
                log.debug("Running synchronization process...");
                return distributedSyncService.ready();
            })
            .andThen(
                Single.defer(() -> {
                    if (
                        distributedSyncService.isEnabled() &&
                        distributedSyncService.isPrimaryNode() &&
                        (nextFromTime == -1 || isClusterPrimaryNode.compareAndSet(false, true))
                    ) {
                        return distributedSyncService
                            .state()
                            .map(distributedSyncState -> {
                                log.debug("Retrieving distributed sync state");
                                nextFromTime = distributedSyncState.getFrom();
                                return distributedSyncState.getTo();
                            })
                            .switchIfEmpty(Single.just(System.currentTimeMillis()));
                    }
                    return Single.just(System.currentTimeMillis());
                })
            )
            .flatMapCompletable(nextToTime -> {
                log.debug("Synchronization #{} started at {}", syncCounter.incrementAndGet(), Instant.now());
                log.debug(
                    "Events from {} to {} would be synchronized.",
                    Instant.ofEpochMilli(nextFromTime - TIMEFRAME_DELAY),
                    Instant.ofEpochMilli(nextToTime + TIMEFRAME_DELAY)
                );
                Completable synchronizationCompletable;
                if (distributedSyncService.isEnabled() && !distributedSyncService.isPrimaryNode()) {
                    log.debug("Distributed synchronizers will be used as distributed sync is enabled, and current node is secondary.");
                    if (distributedSynchronizers != null) {
                        synchronizationCompletable =
                            Flowable
                                .fromIterable(distributedSynchronizers)
                                .concatMapCompletable(synchronizer ->
                                    synchronizer
                                        .synchronize(nextFromTime, nextToTime)
                                        .compose(upstream -> retrySynchronizer(upstream, synchronizer.getClass().getSimpleName()))
                                );
                    } else {
                        synchronizationCompletable = Completable.complete();
                    }
                } else {
                    synchronizationCompletable =
                        Flowable
                            .fromIterable(synchronizers)
                            .concatMapCompletable(synchronizer ->
                                synchronizer
                                    .synchronize(nextFromTime, nextToTime, (Set<String>) node.metadata().get(Node.META_ENVIRONMENTS))
                                    .compose(upstream -> retrySynchronizer(upstream, synchronizer.getClass().getSimpleName()))
                            );
                }
                return synchronizationCompletable
                    .andThen(distributedSyncService.storeState(nextFromTime, nextToTime))
                    .doOnComplete(() -> {
                        lastSyncOnError.set(false);
                        lastSyncErrorMessage.set(null);
                        if (nextFromTime == -1) {
                            initialSync.set(true);
                        }
                        nextFromTime = nextToTime;

                        log.debug(
                            "Synchronization #{} ended at {} (took {}ms}",
                            syncCounter.get(),
                            Instant.now().toString(),
                            System.currentTimeMillis() - nextToTime
                        );
                    });
            })
            .doOnError(throwable -> {
                // This condition is only to avoid printing the stacktrace on every loop
                if (!lastSyncOnError.get()) {
                    log.error("Synchronization process has failed", throwable);
                } else {
                    log.error("Synchronization process is still failing.");
                }
                lastSyncOnError.set(true);
                lastSyncErrorMessage.set(throwable.getMessage());
            });
    }

    private Completable retrySynchronizer(final Completable upstream, final String synchronizerClazz) {
        return upstream
            .doOnError(throwable -> log.warn("An error occurs while executing synchronizer {}, retrying...", synchronizerClazz, throwable))
            .compose(RxHelper.retry(retryAttempt, RETRY_DELAY_MS, MILLISECONDS))
            .doOnError(throwable -> log.error("Latest attempt of synchronizer {} has failed", synchronizerClazz, throwable));
    }

    public long nextSyncTime() {
        return nextFromTime;
    }

    public long syncCounter() {
        return syncCounter.longValue();
    }

    public boolean syncDone() {
        return initialSync.get();
    }

    public long totalSyncOnError() {
        return totalSyncOnErrorCounter.get();
    }

    public boolean lastSyncOnError() {
        return lastSyncOnError.get();
    }

    public String lastSyncErrorMessage() {
        return lastSyncErrorMessage.get();
    }
}
