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
package io.gravitee.gateway.services.sync.process;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import io.gravitee.common.http.MediaType;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.utils.RxHelper;
import io.gravitee.gateway.services.sync.SyncManager;
import io.gravitee.gateway.services.sync.process.handler.DefaultSyncHandler;
import io.gravitee.gateway.services.sync.process.synchronizer.Synchronizer;
import io.gravitee.node.api.Node;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import java.time.Instant;
import java.util.ArrayList;
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

    private final Router router;
    private final List<Synchronizer> synchronizers;
    private final int retryAttempt;
    private final int delay;
    private final TimeUnit unit;
    private final List<String> environments;

    private final AtomicLong syncCounter = new AtomicLong(0);
    private final AtomicBoolean initialSync = new AtomicBoolean(false);
    private final AtomicLong totalSyncOnErrorCounter = new AtomicLong(0);
    private final AtomicBoolean lastSyncOnError = new AtomicBoolean(false);

    private final AtomicReference<String> lastSyncErrorMessage = new AtomicReference<>();

    private long nextFromTime = -1;
    private Disposable refreshDisposable;
    private Route routeHandler;

    public DefaultSyncManager(
        final Router router,
        final Node node,
        final List<Synchronizer> synchronizers,
        final int delay,
        final TimeUnit unit,
        final int retryAttempt
    ) {
        this.router = router;
        this.synchronizers = synchronizers;
        this.delay = delay;
        this.unit = unit;
        this.retryAttempt = retryAttempt;
        this.environments = new ArrayList<>((Set<String>) node.metadata().get(Node.META_ENVIRONMENTS));
    }

    @Override
    protected void doStart() {
        log.info("Associate a new HTTP handler on {}", PATH);
        DefaultSyncHandler syncHandler = new DefaultSyncHandler(this);
        routeHandler = router.get(PATH).produces(MediaType.APPLICATION_JSON).handler(syncHandler);

        // Sort synchronizer by order
        synchronizers.sort(Comparator.comparingInt(Synchronizer::order));

        // force synchronization and then schedule next ones
        synchronize()
            .andThen(
                Completable.fromRunnable(() -> {
                    log.info("Sync service has been scheduled with delay [{}{}]", delay, unit.name());
                    refreshDisposable = Flowable.interval(delay, delay, unit).flatMapCompletable(interval -> synchronize()).subscribe();
                })
            )
            .blockingSubscribe();
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
        return Completable.defer(() -> {
            final long nextToTime = System.currentTimeMillis();
            if (synchronizers != null) {
                log.debug("Starting synchronization process...");
                log.debug("Synchronization #{} started at {}", syncCounter.incrementAndGet(), Instant.now());
                log.debug(
                    "Events from {} to {} would be synchronized.",
                    Instant.ofEpochMilli(nextFromTime - TIMEFRAME_DELAY),
                    Instant.ofEpochMilli(nextToTime + TIMEFRAME_DELAY)
                );
                return Flowable
                    .fromIterable(synchronizers)
                    .concatMapCompletable(synchronizer ->
                        synchronizer
                            .synchronize(nextFromTime, nextToTime, environments)
                            .doOnError(throwable ->
                                log.warn(
                                    "An error occurs while executing synchronizer {}, retrying...",
                                    synchronizer.getClass().getSimpleName()
                                )
                            )
                            .compose(RxHelper.retry(retryAttempt, RETRY_DELAY_MS, MILLISECONDS))
                            .doOnError(throwable ->
                                log.error(
                                    "Latest attempt of synchronizer {} has failed",
                                    synchronizer.getClass().getSimpleName(),
                                    throwable
                                )
                            )
                    )
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
                    })
                    .onErrorResumeNext(throwable ->
                        Completable.fromRunnable(() -> {
                            log.error("Synchronization process has failed", throwable);
                            lastSyncOnError.set(true);
                            lastSyncErrorMessage.set(throwable.getMessage());
                        })
                    );
            }
            return Completable.complete();
        });
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
