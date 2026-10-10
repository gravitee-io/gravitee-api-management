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
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncMetrics;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.handler.DistributedResyncHandler;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class DefaultSyncManager extends AbstractService<SyncManager> implements SyncManager {

    private static final String PATH = "/sync";
    /**
     * Add 30s delay before and after to avoid problem with "out of sync" clocks.
     */
    public static final int TIMEFRAME_DELAY = 30000;
    public static final int RETRY_DELAY_MS = 3_000;
    public static final int INITIAL_RETRY_DELAY_MS = 3_000;
    public static final int MAX_RETRY_DELAY_MS = 10_000;
    private static final String RESYNC_PATH = PATH + "/distributed/resync";
    private static final long SKIP_CYCLE = Long.MIN_VALUE;

    private final Router router;
    private final Node node;
    private final List<RepositorySynchronizer> synchronizers;
    private final List<DistributedSynchronizer> distributedSynchronizers;
    private final int retryAttempt;
    private final DistributedSyncService distributedSyncService;
    private final int delay;
    private final TimeUnit unit;

    private final AtomicLong syncCounter = new AtomicLong(0);
    /** Set after any successful sync; used only by {@link #syncDone()} for the sync-process health probe. */
    private final AtomicBoolean syncReadyForProbe = new AtomicBoolean(false);
    private final AtomicLong totalSyncOnErrorCounter = new AtomicLong(0);
    private final AtomicLong lastSuccessfulSyncAt = new AtomicLong(-1);
    private final AtomicBoolean lastSyncOnError = new AtomicBoolean(false);
    /** Random 0..initialJitterMs delay before the first secondary sync cycle, so scaled-out pods join gradually. */
    private final long initialJitterMs;
    private volatile boolean firstCycle = true;

    private final AtomicReference<String> lastSyncErrorMessage = new AtomicReference<>();

    /** Role of the last successful cycle, to detect promotions and demotions. */
    private final AtomicBoolean actingPrimary = new AtomicBoolean(true);
    private volatile long nextFromTime = -1;
    private Disposable refreshDisposable;
    private Route routeHandler;
    private Route resyncRouteHandler;

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
        this.initialJitterMs = parseJitterMs(System.getProperty("services.sync.distributed.jitterMs", "0"));
    }

    private static long parseJitterMs(final String value) {
        try {
            return Math.max(0, Long.parseLong(value));
        } catch (NumberFormatException e) {
            return 5_000L;
        }
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
            actingPrimary.set(distributedSyncService.isPrimaryNode());
            resyncRouteHandler = router.post(RESYNC_PATH).produces(MediaType.APPLICATION_JSON).handler(new DistributedResyncHandler(this));
        }

        // force synchronization and then schedule next ones
        synchronize()
            .retryWhen(RxHelper.retryExponentialBackoff(INITIAL_RETRY_DELAY_MS, MAX_RETRY_DELAY_MS, MILLISECONDS, 1.5))
            .andThen(
                Completable.fromRunnable(() -> {
                    log.info("Sync service has been scheduled with delay [{} {}]", delay, unit.name());
                    refreshDisposable = Flowable.<Long, Long>generate(
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
        if (resyncRouteHandler != null) {
            resyncRouteHandler.remove();
        }
        super.doStop();
    }

    private Completable synchronize() {
        AtomicBoolean cyclePrimary = new AtomicBoolean(true);
        return Completable.defer(() -> {
            if (firstCycle && initialJitterMs > 0 && distributedSyncService.isEnabled() && !distributedSyncService.isPrimaryNode()) {
                firstCycle = false;
                // A configured jitter always waits at least 1ms: a deployed fleet never wants instant boots.
                long jitter = 1 + (long) (Math.random() * initialJitterMs);
                log.debug("Distributed sync secondary waiting {}ms jitter before its first cycle", jitter);
                return Completable.timer(jitter, TimeUnit.MILLISECONDS);
            }
            firstCycle = false;
            log.debug("Running synchronization process...");
            return distributedSyncService.ready();
        })
            .andThen(
                Single.defer(() -> {
                    if (!distributedSyncService.isEnabled()) {
                        return Single.just(System.currentTimeMillis());
                    }
                    boolean primary = distributedSyncService.beginCycle();
                    cyclePrimary.set(primary);
                    if (primary) {
                        Single<Long> toTime;
                        if (!actingPrimary.get()) {
                            toTime = onPromotion();
                        } else {
                            if (nextFromTime == -1 && !syncReadyForProbe.get()) {
                                distributedSyncService.requestResync("the primary node starts with nothing deployed");
                            }
                            toTime = distributedSyncService
                                .serverTime()
                                .onErrorResumeNext(throwable -> Single.just(System.currentTimeMillis()));
                        }
                        return toTime.flatMap(to ->
                            distributedSyncService
                                .preparePrimaryCycle()
                                .map(resync -> {
                                    if (resync) {
                                        nextFromTime = -1;
                                    }
                                    return to;
                                })
                        );
                    }
                    if (actingPrimary.get()) {
                        log.info("Node is no longer the distributed sync primary, it now synchronizes from the distributed repository");
                        distributedSyncService.onDemotion();
                    }
                    return distributedSyncService.serverTime().flatMap(this::secondaryCycleStart);
                })
            )
            .flatMapCompletable(nextToTime -> {
                if (nextToTime == SKIP_CYCLE) {
                    return Completable.complete();
                }
                log.debug("Synchronization #{} started at {}", syncCounter.incrementAndGet(), Instant.now());
                log.debug(
                    "Events from {} to {} would be synchronized.",
                    Instant.ofEpochMilli(nextFromTime - TIMEFRAME_DELAY),
                    Instant.ofEpochMilli(nextToTime + TIMEFRAME_DELAY)
                );
                Completable synchronizationCompletable;
                if (distributedSyncService.isEnabled() && !cyclePrimary.get()) {
                    log.debug("Distributed synchronizers will be used as distributed sync is enabled, and current node is secondary.");
                    if (distributedSynchronizers != null) {
                        synchronizationCompletable = Flowable.fromIterable(distributedSynchronizers).concatMapCompletable(synchronizer ->
                            synchronizer
                                .synchronize(nextFromTime, nextToTime)
                                .compose(upstream -> retrySynchronizer(upstream, synchronizer.getClass().getSimpleName()))
                        );
                    } else {
                        synchronizationCompletable = Completable.complete();
                    }
                } else {
                    synchronizationCompletable = Flowable.fromIterable(synchronizers).concatMapCompletable(synchronizer ->
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
                        syncReadyForProbe.set(true);
                        lastSuccessfulSyncAt.set(System.currentTimeMillis());
                        if (distributedSyncService.isEnabled() && !cyclePrimary.get() && consumeHoldRequests()) {
                            log.debug("Distributed sync window is held, the same events will be fetched again");
                            distributedSyncService.metrics().ifPresent(DistributedSyncMetrics::recordHeldWindow);
                        } else {
                            nextFromTime = nextToTime;
                        }
                        if (distributedSyncService.isEnabled()) {
                            actingPrimary.set(cyclePrimary.get());
                        }

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
            })
            .doFinally(distributedSyncService::endCycle);
    }

    /**
     * A secondary promoted to primary must cover what the previous primary may have left half-done: it
     * replays the DB window starting at the last state the previous primary stored, up to now, re-distributing
     * entities it already holds (they came from the distributed repository, so a previous partial
     * distribution would otherwise never be completed). A node that never completed a sync, or that finds no
     * stored state (empty or lost distributed repository), performs a full initial sync and re-distributes
     * everything.
     */
    private Single<Long> onPromotion() {
        log.info("Node has been promoted to distributed sync primary");
        if (nextFromTime == -1) {
            distributedSyncService.requestReplay();
            distributedSyncService.requestResync("the node was promoted before completing any synchronization");
            return Single.just(System.currentTimeMillis());
        }
        return distributedSyncService
            .state()
            .map(distributedSyncState -> {
                if (!distributedSyncState.active()) {
                    nextFromTime = -1;
                    distributedSyncService.requestResync(
                        "the previous primary [" + distributedSyncState.getNodeId() + "] left the catalog paused"
                    );
                } else {
                    Long storedFrom = distributedSyncState.getFrom();
                    nextFromTime = storedFrom == null ? -1 : storedFrom;
                    log.info("Resuming from the distributed sync state stored by node [{}]", distributedSyncState.getNodeId());
                }
                return System.currentTimeMillis();
            })
            .switchIfEmpty(
                Single.fromCallable(() -> {
                    log.warn("No distributed sync state found on promotion, performing a full synchronization");
                    nextFromTime = -1;
                    distributedSyncService.requestResync("no distributed sync state was found on promotion");
                    return System.currentTimeMillis();
                })
            )
            .doOnSuccess(ignored -> distributedSyncService.requestReplay());
    }

    /**
     * A secondary only trusts the distributed catalog once the primary published it as complete: a node that never
     * synchronized waits (and stays not ready) while there is no state or while the primary rebuilds the catalog,
     * instead of starting from an empty, stale or partial catalog. A node already serving keeps applying changes.
     */
    private Single<Long> secondaryCycleStart(final long toTime) {
        return distributedSyncService
            .sharedState()
            .map(Optional::of)
            .defaultIfEmpty(Optional.empty())
            .flatMap(state -> {
                state.ifPresent(distributedSyncService::inspectPrimaryState);
                if (state.isEmpty() || !state.get().active()) {
                    if (!syncReadyForProbe.get()) {
                        log.info(
                            state.isEmpty()
                                ? "Waiting for the distributed sync primary to publish its catalog"
                                : "Waiting for the distributed sync primary to finish rebuilding its catalog"
                        );
                        return Single.just(SKIP_CYCLE);
                    }
                    return Single.just(toTime);
                }
                Long tombstoneHorizon = state.get().getTombstoneHorizon();
                if (tombstoneHorizon != null && nextFromTime != -1 && nextFromTime - TIMEFRAME_DELAY < tombstoneHorizon) {
                    log.warn(
                        "This node lagged behind the retention of removal events, it performs a full synchronization (entities removed meanwhile may remain until a restart)"
                    );
                    nextFromTime = -1;
                }
                if (nextFromTime == -1) {
                    return distributedSyncService
                        .catalogFullyIndexed()
                        .map(indexed -> {
                            if (!indexed) {
                                log.info("Waiting for the distributed sync repository to finish indexing before a full synchronization");
                                return SKIP_CYCLE;
                            }
                            return toTime;
                        });
                }
                return Single.just(toTime);
            });
    }

    private boolean consumeHoldRequests() {
        if (distributedSynchronizers == null) {
            return false;
        }
        boolean hold = false;
        for (DistributedSynchronizer synchronizer : distributedSynchronizers) {
            hold |= synchronizer.consumeHoldRequest();
        }
        return hold;
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

    public long lastSuccessfulSyncAt() {
        return lastSuccessfulSyncAt.get();
    }

    public boolean distributedSyncEnabled() {
        return distributedSyncService.isEnabled();
    }

    public boolean actingPrimary() {
        return actingPrimary.get();
    }

    public Optional<DistributedSyncMetrics> distributedSyncMetrics() {
        return distributedSyncService.metrics();
    }

    public boolean distributedResyncInProgress() {
        return distributedSyncService.resyncInProgress();
    }

    /**
     * @return whether the request was accepted: only the primary rebuilds the catalog
     */
    public boolean requestDistributedResync() {
        if (!distributedSyncService.isEnabled() || !distributedSyncService.isPrimaryNode()) {
            return false;
        }
        distributedSyncService.requestResync("requested through the node management API");
        return true;
    }

    public long syncCounter() {
        return syncCounter.longValue();
    }

    public boolean syncDone() {
        return syncReadyForProbe.get();
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
