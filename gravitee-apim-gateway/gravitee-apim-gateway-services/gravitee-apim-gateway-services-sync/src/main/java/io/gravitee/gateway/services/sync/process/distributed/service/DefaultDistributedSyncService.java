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
import io.gravitee.gateway.services.sync.process.distributed.mapper.AuthzEntityMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.AuthzPolicyMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.CredentialMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.DictionaryMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.LicenseMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.NodeMetadataMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.OrganizationMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SharedPolicyGroupMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.distributed.model.DistributedSyncException;
import io.gravitee.gateway.services.sync.process.distributed.model.DistributedSyncTransientException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint.AccessPointDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey.SingleApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzPolicyReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.credential.CredentialDeployable;
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
import io.gravitee.repository.distributedsync.api.DistributedSyncUnavailableException;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.gravitee.repository.distributedsync.model.DistributedSyncState;
import io.gravitee.repository.distributedsync.model.DistributedSyncStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class DefaultDistributedSyncService implements DistributedSyncService {

    /**
     * Maximum number of concurrent Redis writes per distributed deployable. A single API deployable can fan
     * out into one event per subscription and API key; capping the fan-out avoids one deployable flooding the
     * write path on its own. The <b>global</b> bound across all deployables lives at the write choke point in
     * the repository ({@code DistributedWriteGate}), so the aggregate cannot overflow the Redis waiting queue.
     */
    static final int WRITE_MAX_CONCURRENCY = 32;

    /** A distribution failed since the last stored state: the next {@link #storeState} must fail. */
    private final AtomicBoolean distributionFailed = new AtomicBoolean();
    /** Already-deployed entities must be re-distributed until a cycle in replay mode stores its state. */
    private final AtomicBoolean replayRequired = new AtomicBoolean();
    /**
     * Role pinned at {@link #beginCycle()} for the whole cycle. Written by the sync scheduler thread, read by the
     * deployer threads through {@link #isPrimaryNode()}; {@code volatile} provides the happens-before, and the
     * field is nulled only in {@link #endCycle()} which runs after all distributions completed.
     */
    private volatile Boolean pinnedPrimary;
    private volatile boolean replayCycle;

    /**
     * Entity types whose full synchronization re-distributes every live entity, so whatever was not rewritten by a
     * catalog rebuild is stale. Other types keep their previous entries.
     */
    static final Set<DistributedEventType> SWEPT_EVENT_TYPES = Set.of(
        DistributedEventType.API,
        DistributedEventType.SUBSCRIPTION,
        DistributedEventType.API_KEY,
        DistributedEventType.DICTIONARY
    );

    private final AtomicBoolean resyncPending = new AtomicBoolean();
    private final AtomicBoolean localFailureDuringResync = new AtomicBoolean();
    private final AtomicLong knownResyncId = new AtomicLong();
    private volatile boolean resyncCycle;
    private volatile long resyncStartedAt;
    private volatile long deadLettersAtResyncStart;
    private volatile boolean stateStoredByThisNode;
    private volatile long lastTombstonePurgeAt;
    private volatile Long lastTombstoneHorizon;
    /** A write failed because the repository is unreachable: the remaining writes of the cycle are not attempted. */
    private volatile boolean repositoryUnavailable;
    private volatile String reportedPrimaryTopology;
    private volatile boolean reportedStalePrimary;

    /** Version of the distributed event payload format written by this gateway. */
    public static final int EVENT_SCHEMA_VERSION = 1;

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
    private final AuthzEntityMapper authzEntityMapper;
    private final AuthzPolicyMapper authzPolicyMapper;
    private final ApiProductMapper apiProductMapper;
    private final CredentialMapper credentialMapper;
    private final DistributedSyncMetrics metrics;
    private final DistributedSyncSettings settings;

    @Override
    public void validate() {
        if (distributedSyncRepoType == null || distributedSyncRepoType.isEmpty()) {
            throw new SyncException(
                "Distributed sync configuration invalid. No repository configured, check 'distributed-sync.type' value."
            );
        }
        String clusterId = clusterManager.clusterId();
        if (clusterId == null || clusterId.isBlank()) {
            throw new SyncException(
                "Distributed sync requires a non-blank cluster id from the cluster manager (configure Hazelcast cluster-name)."
            );
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isPrimaryNode() {
        Boolean pinned = pinnedPrimary;
        return pinned != null ? pinned : clusterManager.self().primary();
    }

    @Override
    public boolean beginCycle() {
        boolean primary = clusterManager.self().primary();
        pinnedPrimary = primary;
        repositoryUnavailable = false;
        replayCycle = primary && replayRequired.get();
        if (replayCycle) {
            metrics.recordReplayedCycle();
        }
        return primary;
    }

    @Override
    public void endCycle() {
        pinnedPrimary = null;
        replayCycle = false;
    }

    @Override
    public boolean republishRequired() {
        return replayCycle;
    }

    @Override
    public void requestReplay() {
        replayRequired.set(true);
        if (Boolean.TRUE.equals(pinnedPrimary) && !replayCycle) {
            replayCycle = true;
            metrics.recordReplayedCycle();
        }
    }

    @Override
    public void onDemotion() {
        replayRequired.set(false);
        distributionFailed.set(false);
        replayCycle = false;
        resyncPending.set(false);
        resyncCycle = false;
        stateStoredByThisNode = false;
    }

    @Override
    public Optional<DistributedSyncMetrics> metrics() {
        return Optional.of(metrics);
    }

    @Override
    public Completable ready() {
        return distributedSyncStateRepository
            .ready()
            .andThen(distributedEventRepository.ensureSearchable())
            .onErrorResumeNext(throwable ->
                Completable.error(new DistributedSyncException("Distributed Sync repository is not ready.", throwable))
            );
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
    public Maybe<DistributedSyncState> sharedState() {
        return Maybe.defer(() -> distributedSyncStateRepository.findByClusterId(clusterManager.clusterId()));
    }

    @Override
    public Single<Boolean> catalogFullyIndexed() {
        return distributedEventRepository.fullyIndexed();
    }

    @Override
    public void requestResync(final String reason) {
        if (!resyncPending.getAndSet(true)) {
            log.info("A rebuild of the distributed sync catalog is required: {}", reason);
        }
    }

    @Override
    public boolean resyncInProgress() {
        return resyncPending.get();
    }

    @Override
    public void recordLocalDeployFailure() {
        if (resyncCycle) {
            localFailureDuringResync.set(true);
        }
    }

    @Override
    public void recordLocalDeploy() {
        metrics.recordLocalDeploy();
    }

    /**
     * Detects that the catalog must be rebuilt (state lost, or left paused by a primary that died while rebuilding
     * it) and starts the pending rebuild: the state is marked {@link DistributedSyncStatus#PAUSED} so that nodes
     * that never synchronized wait instead of starting from a stale or partial catalog, and the cycle runs as a full
     * synchronization re-distributing every entity. Stale entries are removed when the cycle stores its state.
     */
    @Override
    public Single<Boolean> preparePrimaryCycle() {
        return Single.defer(() -> {
            if (!isPrimaryNode()) {
                return Single.just(false);
            }
            return distributedSyncStateRepository
                .findByClusterId(clusterManager.clusterId())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(storedState -> {
                    storedState.map(DistributedSyncState::getResyncId).ifPresent(id -> knownResyncId.accumulateAndGet(id, Math::max));
                    storedState.ifPresent(this::detectForeignWriter);
                    if (!resyncPending.get()) {
                        if (storedState.isEmpty() && stateStoredByThisNode) {
                            requestResync("the distributed sync state disappeared from the distributed repository");
                        } else if (storedState.isPresent() && !storedState.get().active()) {
                            requestResync("the catalog was left paused by node [" + storedState.get().getNodeId() + "]");
                        }
                    }
                    if (!resyncPending.get()) {
                        return Single.just(false);
                    }
                    return startResync().toSingleDefault(true);
                });
        });
    }

    /**
     * Once this primary stored the state, reading another node id back means another node stored it since: two nodes
     * write the same catalog (cluster split, or two clusters sharing the same cluster name and Redis). Their catalogs
     * may differ (sharding tags, versions), so the secondaries may serve a mix of both.
     */
    private void detectForeignWriter(final DistributedSyncState storedState) {
        boolean foreign = stateStoredByThisNode && storedState.getNodeId() != null && !storedState.getNodeId().equals(node.id());
        if (foreign && !metrics.foreignWriterDetected()) {
            log.error(
                "Node [{}] also writes the distributed sync state of cluster [{}]: two primaries share the same distributed repository " +
                    "(cluster split, or several clusters with the same cluster name). Use a distinct cluster name per gateway cluster.",
                storedState.getNodeId(),
                clusterManager.clusterId()
            );
        }
        metrics.foreignWriterDetected(foreign);
    }

    @Override
    public void inspectPrimaryState(final DistributedSyncState state) {
        String primaryTopology = state.getTopology();
        boolean mismatch = primaryTopology != null && settings.topology() != null && !primaryTopology.equals(settings.topology());
        if (mismatch && !primaryTopology.equals(reportedPrimaryTopology)) {
            log.warn(
                "This gateway synchronizes [{}] but the distributed sync primary [{}] publishes [{}]: entities the primary does not " +
                    "publish will be missing here. All gateways of a cluster should share sharding tags, tenant, organizations and environments.",
                settings.topology(),
                state.getNodeId(),
                primaryTopology
            );
            reportedPrimaryTopology = primaryTopology;
        }
        metrics.topologyMismatch(mismatch);

        if (state.getHeartbeatAt() != null) {
            long age = System.currentTimeMillis() - state.getHeartbeatAt();
            metrics.recordPrimaryHeartbeatAge(age);
            boolean stale = settings.primaryStaleAfterMs() > 0 && age > settings.primaryStaleAfterMs();
            if (stale && !reportedStalePrimary) {
                log.warn(
                    "The distributed sync primary [{}] has not published its state for {}s: changes are not reaching this gateway",
                    state.getNodeId(),
                    age / 1000
                );
            } else if (!stale && reportedStalePrimary) {
                log.info("The distributed sync primary [{}] publishes its state again", state.getNodeId());
            }
            reportedStalePrimary = stale;
        }
    }

    private Completable startResync() {
        return Completable.defer(() -> {
            long resyncId = knownResyncId.get() + 1;
            long startedAt = System.currentTimeMillis();
            return distributedSyncStateRepository
                .createOrUpdate(buildState(-1, startedAt, DistributedSyncStatus.PAUSED, resyncId, lastTombstoneHorizon))
                .doOnComplete(() -> {
                    knownResyncId.set(resyncId);
                    resyncStartedAt = startedAt;
                    resyncCycle = true;
                    deadLettersAtResyncStart = metrics.deadLetters();
                    localFailureDuringResync.set(false);
                    stateStoredByThisNode = true;
                    requestReplay();
                    log.info("Rebuilding the distributed sync catalog (resync #{})", resyncId);
                });
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
                boolean storedByReplayCycle = replayCycle;
                boolean storedByResyncCycle = resyncCycle;
                // The window endpoint (toTime) is the Redis server clock when fencing is on, otherwise the local
                // clock: the tombstone horizon must use the same clock the secondaries compare their cursors
                // against, or a skewed primary clock would make every secondary appear to lag behind the horizon.
                long now = toTime >= 0 ? toTime : System.currentTimeMillis();
                Long tombstoneHorizon = tombstonePurgeDue(now) ? now - settings.tombstoneRetentionMs() : null;
                Long horizonToStore = tombstoneHorizon != null ? tombstoneHorizon : lastTombstoneHorizon;
                Completable removeStaleEntries = storedByResyncCycle ? removeStaleEntries() : Completable.complete();
                return removeStaleEntries
                    .andThen(
                        distributedSyncStateRepository.createOrUpdate(
                            buildState(fromTime, toTime, DistributedSyncStatus.ACTIVE, knownResyncId.get(), horizonToStore)
                        )
                    )
                    .doOnComplete(() -> {
                        stateStoredByThisNode = true;
                        if (storedByReplayCycle && !distributionFailed.get()) {
                            replayRequired.set(false);
                        }
                        if (storedByResyncCycle) {
                            resyncPending.set(false);
                            resyncCycle = false;
                            log.info("The distributed sync catalog has been rebuilt (resync #{})", knownResyncId.get());
                        }
                    })
                    .andThen(tombstoneHorizon != null ? purgeTombstones(tombstoneHorizon) : Completable.complete());
            }
            return Completable.complete();
        });
    }

    /**
     * After a full republish every entity still in the catalog has been rewritten, so a DEPLOY event older than the
     * rebuild belongs to an entity that no longer exists (stopped API, revoked API key, closed subscription...). It
     * is turned into an UNDEPLOY event: secondaries already serving remove it through their incremental sync, and
     * nodes starting from scratch never deploy it.
     * <p>
     * Skipped when the rebuild could not re-distribute everything (an entity failed to deploy or to be mapped):
     * its previous event is then the best information the other gateways have.
     */
    private Completable removeStaleEntries() {
        return Completable.defer(() -> {
            if (localFailureDuringResync.get() || metrics.deadLetters() != deadLettersAtResyncStart) {
                log.warn("Some entities could not be re-distributed while rebuilding the distributed sync catalog, stale entries are kept");
                metrics.recordSkippedStaleEntriesRemoval();
                return Completable.complete();
            }
            return distributedEventRepository
                .undeployNotUpdatedSince(clusterManager.clusterId(), SWEPT_EVENT_TYPES, new Date(resyncStartedAt))
                .doOnSuccess(count -> {
                    metrics.recordStaleEntriesRemoved(count);
                    log.info("{} stale distributed sync entries have been marked as undeployed", count);
                })
                .ignoreElement()
                .onErrorResumeNext(throwable -> {
                    if (throwable instanceof UnsupportedOperationException) {
                        log.warn("The distributed sync repository cannot remove stale entries, they are kept: {}", throwable.getMessage());
                        return Completable.complete();
                    }
                    return Completable.error(
                        new DistributedSyncTransientException("Unable to remove stale distributed sync entries", throwable)
                    );
                });
        });
    }

    private boolean tombstonePurgeDue(final long now) {
        return settings.tombstoneRetentionMs() > 0 && now - lastTombstonePurgeAt >= settings.tombstonePurgeIntervalMs();
    }

    /**
     * The horizon is stored before the events are deleted, so a secondary that lagged behind it knows it may have
     * missed removals and performs a full synchronization.
     */
    private Completable purgeTombstones(final long horizon) {
        return distributedEventRepository
            .deleteUndeployedBefore(clusterManager.clusterId(), new Date(horizon))
            .doOnSuccess(count -> {
                lastTombstonePurgeAt = System.currentTimeMillis();
                lastTombstoneHorizon = horizon;
                log.debug("{} distributed sync removal events older than the retention have been purged", count);
            })
            .ignoreElement()
            .onErrorResumeNext(throwable -> {
                log.warn("Unable to purge old distributed sync removal events, it will be retried", throwable);
                return Completable.complete();
            });
    }

    private DistributedSyncState buildState(
        final long fromTime,
        final long toTime,
        final DistributedSyncStatus status,
        final long resyncId,
        final Long tombstoneHorizon
    ) {
        return DistributedSyncState.builder()
            .clusterId(clusterManager.clusterId())
            .nodeId(node.id())
            .nodeVersion(Version.RUNTIME_VERSION.toString())
            .from(fromTime)
            .to(toTime)
            .status(status)
            .resyncId(resyncId > 0 ? resyncId : null)
            .heartbeatAt(System.currentTimeMillis())
            .tombstoneHorizon(tombstoneHorizon)
            .topology(settings.topology())
            .build();
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
                                if (repositoryUnavailable) {
                                    return skippedWrite();
                                }
                                return distributedEventRepository.updateAllAtWriteTime(
                                    clusterManager.clusterId(),
                                    DistributedEventType.API,
                                    deployable.apiId(),
                                    DistributedSyncAction.UNDEPLOY
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

    private Completable createOrUpdateEvent(final DistributedEvent event) {
        return Completable.defer(() -> {
            if (repositoryUnavailable) {
                return skippedWrite();
            }
            event.setClusterId(clusterManager.clusterId());
            event.setSchemaVersion(EVENT_SCHEMA_VERSION);
            return distributedEventRepository.createOrUpdateAtWriteTime(event);
        });
    }

    private Completable skippedWrite() {
        metrics.recordSkippedDistribution();
        return Completable.error(
            new DistributedSyncTransientException("The distributed sync repository is unavailable, the write is not attempted", null)
        );
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
    public Completable distributeIfNeeded(final CredentialDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing credential event for {}", deployable.id());
                return trackFailure(distribute(credentialMapper.to(deployable)), "credential", deployable.id());
            }
            log.debug("Not a primary node, skipping credential event distribution");
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

    @Override
    public Completable distributeIfNeeded(final AuthzEntityReactorDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing authz entity event for {}", deployable.entityId());
                return trackFailure(distribute(authzEntityMapper.to(deployable)), "authz entity", deployable.entityId());
            }
            log.debug("Not a primary node, skipping authz entity event distribution");
            return Completable.complete();
        });
    }

    @Override
    public Completable distributeIfNeeded(final AuthzPolicyReactorDeployable deployable) {
        return Completable.defer(() -> {
            if (isPrimaryNode()) {
                log.debug("Node is primary, distributing authz policy event for {}", deployable.docId());
                return trackFailure(distribute(authzPolicyMapper.to(deployable)), "authz policy", deployable.docId());
            }
            log.debug("Not a primary node, skipping authz policy event distribution");
            return Completable.complete();
        });
    }

    private Completable distribute(final Flowable<DistributedEvent> events) {
        // Per-deployable fan-out cap; the global in-flight bound is enforced by the repository write gate.
        return events.flatMapCompletable(this::createOrUpdateEvent, false, WRITE_MAX_CONCURRENCY);
    }

    private Completable distribute(final Maybe<DistributedEvent> event) {
        return distribute(event.toFlowable());
    }

    /**
     * A failed distribution must not be silently dropped: the repository synchronizers isolate
     * (un)deployment errors per item, so without this flag the sync window would advance and the
     * missing events would never reach the secondary gateways. {@link #storeState(long, long)}
     * checks the flag and fails the cycle so the same window is replayed, and the replay re-distributes
     * entities that are already deployed locally (see {@link #republishRequired()}): without that, the replayed
     * entities are "already up-to-date" for the local managers and never reach the distributed repository.
     * <p>
     * The failure is raised as a {@link DistributedSyncTransientException} so the synchronizers treat it as
     * retryable: a cold primary keeps its initial sync failing (and retrying) instead of isolating the entity.
     */
    private Completable trackFailure(final Completable distribution, final String eventType, final String id) {
        return distribution.onErrorResumeNext(throwable -> {
            distributionFailed.set(true);
            replayRequired.set(true);
            metrics.recordDistributionFailure();
            if (!repositoryUnavailable && ExceptionUtils.throwableOfType(throwable, DistributedSyncUnavailableException.class) != null) {
                repositoryUnavailable = true;
                log.warn(
                    "The distributed sync repository is unavailable: the other distributions of this cycle are skipped and the sync window will be replayed"
                );
            }
            return Completable.error(
                new DistributedSyncTransientException(
                    "Unable to distribute %s event for [%s], the sync time window will be replayed".formatted(eventType, id),
                    throwable
                )
            );
        });
    }
}
