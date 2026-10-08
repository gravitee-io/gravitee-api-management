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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer;

import io.gravitee.gateway.services.sync.process.common.SyncErrors;
import io.gravitee.gateway.services.sync.process.common.deployer.Deployer;
import io.gravitee.gateway.services.sync.process.common.model.Deployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.distributed.DistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public abstract class AbstractDistributedSynchronizer<T extends Deployable, Y extends Deployer<T>> implements DistributedSynchronizer {

    protected static final Set<DistributedSyncAction> INIT_SYNC_ACTIONS = Set.of(DistributedSyncAction.DEPLOY);
    protected static final Set<DistributedSyncAction> INCREMENTAL_SYNC_ACTIONS = Set.of(
        DistributedSyncAction.DEPLOY,
        DistributedSyncAction.UNDEPLOY
    );
    /**
     * How many consecutive incremental synchronizations may hold the sync window because some entities hit a
     * transient failure. Holding keeps the window from moving, so the same events are fetched (and those entities
     * retried) on the next cycle; past this bound the window moves on and the entities stay skipped.
     */
    public static final int MAX_HELD_ATTEMPTS = 12;

    protected final DistributedEventFetcher distributedEventFetcher;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;
    private final AtomicInteger consecutiveHeldAttempts = new AtomicInteger();
    private final AtomicBoolean holdRequested = new AtomicBoolean();
/** Schema version this node can decode; events with a higher version fail the cycle instead of being misread. */
    private final int supportedSchemaVersion =
        io.gravitee.gateway.services.sync.process.distributed.service.DefaultDistributedSyncService.EVENT_SCHEMA_VERSION;
    /** id -> last applied updatedAt, committed only when a cycle succeeds. An event whose timestamp is not newer
     * than the committed watermark has already been applied on an overlapping window and is skipped. */
    private final java.util.concurrent.ConcurrentHashMap<String, Long> appliedWatermark = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> pendingWatermark = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public Completable synchronize(final Long from, final Long to) {
        boolean initialSync = from == null || from == -1;
        if (initialSync) {
            // A full synchronization re-applies the authoritative set idempotently; the watermark must not suppress it.
            appliedWatermark.clear();
        }
        pendingWatermark.clear();
        AtomicLong launchTime = new AtomicLong();
        AtomicReference<Throwable> transientItemFailure = new AtomicReference<>();
        return distributedEventFetcher
            .fetchLatest(from, to, distributedEventType(), syncActions(initialSync))
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .flatMapMaybe(event -> {
                if (event.getSchemaVersion() != null && event.getSchemaVersion() > supportedSchemaVersion) {
                    return Maybe.error(
                        new io.gravitee.gateway.services.sync.process.distributed.model.DistributedSyncException(
                            "A " +
                                distributedEventType() +
                                " event uses payload schema v" +
                                event.getSchemaVersion() +
                                ", this gateway only supports v" +
                                supportedSchemaVersion +
                                ". Upgrade this gateway so events are not read incorrectly."
                        )
                    );
                }
                return Maybe.just(event);
            })
            .doOnNext(event -> {
                if (event.getUpdatedAt() != null) {
                    pendingWatermark.merge(event.getId(), event.getUpdatedAt().getTime(), Math::max);
                }
            })
            .filter(distributedEvent -> shouldApply(distributedEvent, initialSync))
            .flatMapMaybe(this::mapTo)
            // per deployable
            .compose(upstream -> {
                Y deployer = createDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deploy(deployer, deployable).onErrorResumeNext(throwable ->
                                resumeOnError(initialSync, throwable, transientItemFailure)
                            );
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeploy(deployer, deployable).onErrorResumeNext(throwable ->
                                resumeOnError(initialSync, throwable, transientItemFailure)
                            );
                        } else {
                            return Flowable.empty();
                        }
                    })
                    .sequential(distributedEventFetcher.bulkItems());
            })
            .count()
            .doOnSubscribe(disposable -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(count -> {
                String logMsg = String.format(
                    "%s %s(s) synchronized in %sms",
                    count,
                    distributedEventType().name().toLowerCase(),
                    (System.currentTimeMillis() - launchTime.get())
                );
                if (initialSync) {
                    log.info(logMsg);
                } else {
                    log.debug(logMsg);
                }
            })
            .ignoreElement()
            .doOnComplete(() -> {
                if (transientItemFailure.get() == null) {
                    appliedWatermark.putAll(pendingWatermark);
                }
                requestHoldIfNeeded(transientItemFailure.get());
            });
    }

    /**
     * An event is skipped when it is not newer than the watermark already committed for its id: it was applied by
     * an earlier overlapping window. A missing timestamp is always applied; a full synchronization applies every
     * event regardless.
     */
    private boolean shouldApply(final DistributedEvent event, final boolean initialSync) {
        if (initialSync || event.getUpdatedAt() == null) {
            return true;
        }
        Long applied = appliedWatermark.get(event.getId());
        return applied == null || applied < event.getUpdatedAt().getTime();
    }

    @Override
    public boolean consumeHoldRequest() {
        return holdRequested.getAndSet(false);
    }

    private void requestHoldIfNeeded(final Throwable transientItemFailure) {
        if (transientItemFailure == null) {
            consecutiveHeldAttempts.set(0);
            return;
        }
        int attempts = consecutiveHeldAttempts.incrementAndGet();
        if (attempts <= MAX_HELD_ATTEMPTS) {
            log.warn(
                "Some {}(s) hit a transient failure, the sync window is held so they are retried (attempt {}/{})",
                distributedEventType().name().toLowerCase(),
                attempts,
                MAX_HELD_ATTEMPTS
            );
            holdRequested.set(true);
            return;
        }
        consecutiveHeldAttempts.set(0);
        log.error(
            "Some {}(s) kept failing after {} attempts, the sync window moves on and they stay skipped until they change again",
            distributedEventType().name().toLowerCase(),
            MAX_HELD_ATTEMPTS,
            transientItemFailure
        );
    }

    protected Set<DistributedSyncAction> syncActions(final boolean initialSync) {
        return initialSync ? INIT_SYNC_ACTIONS : INCREMENTAL_SYNC_ACTIONS;
    }

    protected abstract DistributedEventType distributedEventType();

    protected abstract Maybe<T> mapTo(final DistributedEvent distributedEvent);

    protected abstract Y createDeployer();

    private Flowable<T> deploy(final Y apiDeployer, final T deployable) {
        return apiDeployer.deploy(deployable).andThen(apiDeployer.doAfterDeployment(deployable)).andThen(Flowable.just(deployable));
    }

    private Flowable<T> undeploy(final Y apiDeployer, final T deployable) {
        return apiDeployer.undeploy(deployable).andThen(apiDeployer.doAfterUndeployment(deployable)).andThen(Flowable.just(deployable));
    }

    /**
     * Mirrors the repository sync error handling (see {@code AbstractApiSynchronizer}) for the distributed
     * path: a transient failure (a repository {@code TechnicalException} in the cause chain) on the initial
     * sync fails the sync so the secondary node stays not-ready and retries, while a permanent failure isolates
     * the offending entity so a single poison event cannot block the whole distributed synchronization. On an
     * incremental sync a transient failure isolates the entity too, but is recorded so the window is held and
     * the entity retried (see {@link #requestHoldIfNeeded}).
     */
    private Flowable<T> resumeOnError(
        final boolean initialSync,
        final Throwable throwable,
        final AtomicReference<Throwable> transientFailure
    ) {
        if (SyncErrors.isTransient(throwable)) {
            if (initialSync) {
                return Flowable.error(throwable);
            }
            transientFailure.compareAndSet(null, throwable);
            log.warn("A transient error occurred while (un)deploying a distributed event, it will be retried", throwable);
            return Flowable.empty();
        }
        log.error("An error occurred while (un)deploying a distributed event during synchronization", throwable);
        return Flowable.empty();
    }
}
