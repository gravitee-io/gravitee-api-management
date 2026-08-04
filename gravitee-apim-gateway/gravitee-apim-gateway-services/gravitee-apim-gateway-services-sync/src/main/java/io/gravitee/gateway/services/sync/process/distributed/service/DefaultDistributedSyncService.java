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
package io.gravitee.gateway.services.sync.process.distributed.service;

import io.gravitee.common.util.Version;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.mapper.AccessPointMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiProductMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.DictionaryMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.LicenseMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.NodeMetadataMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.OrganizationMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SharedPolicyGroupMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.distributed.model.DistributedSyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint.AccessPointDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey.SingleApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.license.LicenseDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.node.NodeMetadataDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup.SharedPolicyGroupReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription.SingleSubscriptionDeployable;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.api.DistributedSyncStateRepository;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.gravitee.repository.distributedsync.model.DistributedSyncState;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class DefaultDistributedSyncService implements DistributedSyncService {

    /**
     * Maximum number of concurrent Redis writes per distributed deployable. A single API deployable
     * can fan out into one event per subscription and API key; an unbounded fan-out overflows the
     * Redis client waiting queue (max-waiting-handlers) during bulk syncs.
     */
    static final int WRITE_MAX_CONCURRENCY = 32;

    /**
     * Upper bound on the number of Redis writes issued concurrently across <b>all</b> distributions.
     * {@link #WRITE_MAX_CONCURRENCY} only caps a single deployable's fan-out, but the deployer runs many
     * deployables in parallel, so the aggregate in-flight writes (≈ parallelism × {@link #WRITE_MAX_CONCURRENCY})
     * must also be bounded. Kept well below the Redis client waiting queue (max-waiting-handlers, 1024 by
     * default) — leaving room for reads/state/scan commands — so the primary never triggers "Redis waiting
     * queue is full", which used to silently drop subscription/api-key events during bulk syncs.
     */
    static final int DISTRIBUTION_WRITE_MAX_CONCURRENCY = 512;

    private final AtomicBoolean distributionFailed = new AtomicBoolean();

    /**
     * Shared across every {@code distributeIfNeeded(...)} call so the bound is global, not per-deployable.
     * Package-private and non-final so tests can shrink it to exercise the bound deterministically.
     */
    WriteGate writeGate = new WriteGate(DISTRIBUTION_WRITE_MAX_CONCURRENCY);

    private final Node node;
    private final ClusterManager clusterManager;
    private final String distributedSyncRepoType;
    private final DistributedEventRepository distributedEventRepository;
    private final DistributedSyncStateRepository distributedSyncStateRepository;
    private final ApiMapper apiMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final OrganizationMapper organizationMapper;
    private final DictionaryMapper dictionaryMapper;
    private final LicenseMapper licenseMapper;
    private final AccessPointMapper accessPointMapper;
    private final SharedPolicyGroupMapper sharedPolicyGroupMapper;
    private final NodeMetadataMapper nodeMetadataMapper;
    private final ApiProductMapper apiProductMapper;

    @Override
    public void validate() {
        if (distributedSyncRepoType == null || distributedSyncRepoType.isEmpty()) {
            throw new SyncException(
                "Distributed sync configuration invalid. No repository configured, check 'distributed-sync.type' value."
            );
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isPrimaryNode() {
        return clusterManager.self().primary();
    }

    @Override
    public Completable ready() {
        return distributedSyncStateRepository
            .ready()
            .onErrorResumeNext(throwable -> Completable.error(new DistributedSyncException("Distributed Sync repository is not ready.")));
    }

    @Override
    public Maybe<DistributedSyncState> state() {
        return Maybe.defer(() -> {
            if (isPrimaryNode()) {
                return distributedSyncStateRepository.findByClusterId(clusterManager.clusterId());
            }
            return Maybe.empty();
        });
    }

    @Override
    public Completable storeState(final long fromTime, final long toTime) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                if (distributionFailed.getAndSet(false)) {
                    return Completable.error(
                        new DistributedSyncException(
                            "Some distributed events could not be written to the distributed sync repository. " +
                                "The sync state is not stored so the time window is replayed on the next sync cycle."
                        )
                    );
                }
                return distributedSyncStateRepository.createOrUpdate(
                    DistributedSyncState.builder()
                        .clusterId(clusterManager.clusterId())
                        .nodeId(node.id())
                        .nodeVersion(Version.RUNTIME_VERSION.toString())
                        .from(fromTime)
                        .to(toTime)
                        .build()
                );
            }
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final ApiReactorDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing API reactor event for {}", deployable.id());
                return trackFailure(
                    distribute(apiMapper.to(deployable)).andThen(
                        Completable.defer(() -> {
                            if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                                return distributedEventRepository.updateAll(
                                    DistributedEventType.API,
                                    deployable.apiId(),
                                    DistributedSyncAction.UNDEPLOY,
                                    new Date()
                                );
                            }
                            return Completable.complete();
                        })
                    ),
                    "API reactor",
                    deployable.id()
                );
            }
            log.debug("Not a primary node, skipping API reactor event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final SingleSubscriptionDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing subscription event for {}", deployable.id());
                return trackFailure(distribute(subscriptionMapper.to(deployable)), "subscription", deployable.id());
            }
            log.debug("Not a primary node, skipping subscription event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final SingleApiKeyDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing API key event for {}", deployable.id());
                return trackFailure(distribute(apiKeyMapper.to(deployable)), "API key", deployable.id());
            }
            log.debug("Not a primary node, skipping API key event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final OrganizationDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing organization event for {}", deployable.id());
                return trackFailure(distribute(organizationMapper.to(deployable)), "organization", deployable.id());
            }
            log.debug("Not a primary node, skipping organization event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final DictionaryDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing dictionary event for {}", deployable.id());
                return trackFailure(distribute(dictionaryMapper.to(deployable)), "dictionary", deployable.id());
            }
            log.debug("Not a primary node, skipping dictionary event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(LicenseDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing license event for organization {}", deployable.id());
                return trackFailure(distribute(licenseMapper.to(deployable)), "license", deployable.id());
            }
            log.debug("Not a primary node, skipping license event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(AccessPointDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing access point event for {}", deployable.id());
                return trackFailure(distribute(accessPointMapper.to(deployable)), "access point", deployable.id());
            }
            log.debug("Not a primary node, skipping access point event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(SharedPolicyGroupReactorDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing shared policy group event for {}", deployable.id());
                return trackFailure(distribute(sharedPolicyGroupMapper.to(deployable)), "shared policy group", deployable.id());
            }
            log.debug("Not a primary node, skipping shared policy group event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(ApiProductReactorDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing API product event for {}", deployable.id());
                return trackFailure(distribute(apiProductMapper.to(deployable)), "API product", deployable.id());
            }
            log.debug("Not a primary node, skipping API product event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final NodeMetadataDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing node metadata event for {}", deployable.id());
                return trackFailure(distribute(nodeMetadataMapper.to(deployable)), "node metadata", deployable.id());
            }
            log.debug("Not a primary node, skipping node metadata event distribution");
            return Completable.complete();
        });
    }

    private Completable distribute(final Flowable<DistributedEvent> events) {
        return events.flatMapCompletable(this::distributeSingle, false, WRITE_MAX_CONCURRENCY);
    }

    /**
     * Writes a single event, gated by the shared {@link #writeGate}: a permit is acquired before the write
     * and released once the write terminates or is disposed. When permits are exhausted the acquire step
     * parks, back-pressuring the (awaited) deployer chain instead of flooding the Redis client.
     */
    private Completable distributeSingle(final DistributedEvent event) {
        return writeGate.runGated(distributedEventRepository.createOrUpdate(event));
    }

    private Completable distribute(final Maybe<DistributedEvent> event) {
        return distribute(event.toFlowable());
    }

    /**
     * A failed distribution must not be silently dropped: the repository synchronizers isolate
     * (un)deployment errors per item, so without this flag the sync window would advance and the
     * missing events would never reach the secondary gateways. {@link #storeState(long, long)}
     * checks the flag and fails the cycle so the same window is replayed.
     */
    private Completable trackFailure(final Completable distribution, final String eventType, final String id) {
        return distribution.onErrorResumeNext(throwable -> {
            distributionFailed.set(true);
            return Completable.error(
                new DistributedSyncException(
                    "Unable to distribute %s event for [%s], the sync time window will be replayed".formatted(eventType, id),
                    throwable
                )
            );
        });
    }

    /**
     * Reactive permit gate bounding the number of concurrent distributed writes. {@link #runGated(Completable)}
     * runs its write once a permit is available and otherwise parks until another write releases one. Because
     * the distribution is awaited inside the deployer chain, parking naturally back-pressures the producer
     * instead of flooding the Redis client waiting queue. It holds no unbounded buffer: only the in-flight
     * fan-out slots (bounded by {@link #WRITE_MAX_CONCURRENCY} per deployable) ever park.
     * <p>
     * The permit is booked to a per-run {@code held} flag the instant it is granted (initial grant or
     * hand-off), under the lock and <em>before</em> any {@code onComplete} is signalled. Release is driven by
     * the run's {@code doFinally} keyed on that flag, so a concurrent dispose that swallows the grant signal
     * (e.g. {@code flatMapCompletable(..., delayErrors=false)} cancelling siblings when one write errors)
     * cannot orphan a counted permit — {@code doFinally} still runs on dispose and releases it.
     * <p>
     * The bound is on the total in-flight writes, independent of how many Redis connections back them: even
     * if every permitted write landed on a single connection, {@code maxConcurrency} stays below that
     * connection's waiting-handler limit, so no connection's queue can overflow.
     */
    static final class WriteGate {

        private final int maxConcurrency;
        private final Deque<Waiter> waiters = new ArrayDeque<>();
        private int inFlight;

        WriteGate(final int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
        }

        Completable runGated(final Completable write) {
            // defer so `held` is per-subscription: correct even if a retry()/repeat() is ever composed above.
            return Completable.defer(() -> {
                AtomicBoolean held = new AtomicBoolean();
                return acquire(held)
                    .andThen(write)
                    .doFinally(() -> {
                        if (held.compareAndSet(true, false)) {
                            release();
                        }
                    });
            });
        }

        // Visible for tests: current number of granted (in-flight) permits.
        int inFlight() {
            synchronized (this) {
                return inFlight;
            }
        }

        private Completable acquire(final AtomicBoolean held) {
            return Completable.create(emitter -> {
                boolean granted = false;
                synchronized (this) {
                    if (inFlight < maxConcurrency) {
                        inFlight++;
                        held.set(true);
                        granted = true;
                    } else {
                        Waiter waiter = new Waiter(emitter, held);
                        waiters.add(waiter);
                        emitter.setCancellable(() -> {
                            synchronized (this) {
                                waiters.remove(waiter);
                            }
                        });
                    }
                }
                // Signalled outside the lock; the permit is already booked to `held`, so a dispose racing this
                // point still releases via the run's doFinally.
                if (granted) {
                    emitter.onComplete();
                }
            });
        }

        private void release() {
            // Hand the freed permit to the next live waiter — booking it under the lock before signalling so a
            // racing dispose cannot orphan it — otherwise return the permit to the pool. Disposed waiters are
            // skipped without decrementing (their run never held the permit).
            while (true) {
                Waiter next;
                synchronized (this) {
                    next = waiters.poll();
                    if (next == null) {
                        inFlight--;
                        return;
                    }
                    if (next.emitter().isDisposed()) {
                        continue;
                    }
                    next.held().set(true);
                }
                next.emitter().onComplete();
                return;
            }
        }

        private record Waiter(CompletableEmitter emitter, AtomicBoolean held) {}
    }
}
