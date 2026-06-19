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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import io.gravitee.gateway.services.sync.process.common.deployer.Deployer;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;

/**
 * Shared reactive synchronization engine for the two scoped authz document types (entities and
 * policies). It owns the fetch → map → (un)deploy → retry → commit pipeline, the per-node retry
 * bookkeeping and the scope-placement reconciliation; subclasses only supply the type-specific
 * bindings (mapper, deployer, event types, labels). Keeping a single implementation means a fix to
 * the shared logic (e.g. placement eviction or commit retry) lands once instead of diverging across
 * near-identical copies.
 *
 * @param <D> the scoped deployable type handled by this synchronizer
 */
@CustomLog
public abstract class AbstractAuthzReactorSynchronizer<D extends AuthzScopedDeployable> implements RepositorySynchronizer {

    static final int MAX_PENDING_ATTEMPTS = 10;
    static final String WILDCARD = "*";

    protected final LatestEventFetcher eventsFetcher;
    protected final AuthzEnginePort enginePort;
    private final AuthzScopePlacement placement;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;

    private final Map<String, D> pendingDeploys = new ConcurrentHashMap<>();
    private final Map<String, D> pendingUndeploys = new ConcurrentHashMap<>();
    private final Map<String, Integer> pendingAttempts = new ConcurrentHashMap<>();

    protected AbstractAuthzReactorSynchronizer(
        LatestEventFetcher eventsFetcher,
        AuthzEnginePort enginePort,
        AuthzScopePlacement placement,
        ThreadPoolExecutor syncFetcherExecutor,
        ThreadPoolExecutor syncDeployerExecutor
    ) {
        this.eventsFetcher = eventsFetcher;
        this.enginePort = enginePort;
        this.placement = placement;
        this.syncFetcherExecutor = syncFetcherExecutor;
        this.syncDeployerExecutor = syncDeployerExecutor;
    }

    protected abstract Maybe<D> toDeploy(Event event);

    protected abstract Maybe<D> toUndeploy(Event event);

    protected abstract Deployer<D> createDeployer();

    protected abstract Event.EventProperties eventProperty();

    protected abstract EventType publishType();

    protected abstract EventType unpublishType();

    /** Singular log label, e.g. {@code "authz entity"}. */
    protected abstract String singularLabel();

    /** Plural log label, e.g. {@code "authz entities"}. */
    protected abstract String pluralLabel();

    private String runtimeKey(D deployable) {
        return deployable.environmentId() + ":" + deployable.id();
    }

    @Override
    public Completable synchronize(Long from, Long to, Set<String> environments) {
        AtomicLong launchTime = new AtomicLong();
        AtomicLong processed = new AtomicLong();
        Set<String> attemptedThisCycle = ConcurrentHashMap.newKeySet();
        boolean initialSync = from == null || from.longValue() == -1L;

        return eventsFetcher
            .fetchLatest(
                from,
                to,
                eventProperty(),
                environments,
                initialSync ? Set.of(publishType()) : Set.of(publishType(), unpublishType())
            )
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            .compose(events -> processEvents(events, attemptedThisCycle))
            .count()
            .doOnSubscribe(d -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(processed::set)
            .ignoreElement()
            .andThen(retryPending(attemptedThisCycle))
            .andThen(commitIfAny())
            .doOnComplete(() -> {
                String msg = String.format(
                    "%s %s synchronized in %sms",
                    processed.get(),
                    pluralLabel(),
                    System.currentTimeMillis() - launchTime.get()
                );
                if (initialSync) {
                    log.info(msg);
                } else {
                    log.debug(msg);
                }
            });
    }

    private Completable commitIfAny() {
        // Always commit: enginePort.commit() short-circuits when nothing is armed, and calling it every
        // cycle also retries a previous cycle's commit that failed and re-armed its address.
        return Completable.defer(enginePort::commit);
    }

    private Flowable<D> processEvents(Flowable<List<Event>> eventsFlowable, Set<String> attemptedThisCycle) {
        return eventsFlowable
            .flatMap(events ->
                Flowable.just(events)
                    .doOnNext(e -> log.debug("New {} events fetched", singularLabel()))
                    .flatMapIterable(e -> e)
                    .groupBy(Event::getType)
                    .flatMap(eventsByType -> {
                        EventType type = eventsByType.getKey();
                        if (type == publishType()) {
                            return eventsByType
                                .flatMapMaybe(this::toDeploy)
                                .buffer(bulkEvents())
                                .flatMapIterable(d -> d);
                        } else if (type == unpublishType()) {
                            return eventsByType.flatMapMaybe(this::toUndeploy);
                        }
                        return Flowable.empty();
                    })
            )
            .compose(upstream -> {
                Deployer<D> deployer = createDeployer();
                return upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> {
                        if (deployable.syncAction() == SyncAction.DEPLOY) {
                            return deploy(deployer, deployable, attemptedThisCycle);
                        } else if (deployable.syncAction() == SyncAction.UNDEPLOY) {
                            return undeploy(deployer, deployable, attemptedThisCycle);
                        }
                        return Flowable.just(deployable);
                    })
                    .sequential(bulkEvents());
            });
    }

    private Flowable<D> deploy(Deployer<D> deployer, D deployable, Set<String> attemptedThisCycle) {
        String rk = runtimeKey(deployable);
        attemptedThisCycle.add(rk);
        // Latest control-plane intent for this document is PUBLISH: cancel any pending removal.
        pendingUndeploys.remove(rk);
        // Placement tracks the full control-plane target (the intent). The engine port skips scopes not
        // hosted on this node, so an unhosted target is simply not routed; eviction is the placement delta.
        Set<String> target = deployable.targetPdpIds();
        Set<String> dropped = new LinkedHashSet<>(placement.applied(rk));
        dropped.removeAll(target);
        deployable.removedTargetPdpIds(dropped);
        return deployer
            .deploy(deployable)
            .andThen(deployer.doAfterDeployment(deployable))
            .doOnComplete(() -> {
                pendingDeploys.remove(rk);
                pendingAttempts.remove(rk);
                placement.replace(rk, target);
            })
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(t -> {
                log.error("Failed to stage {} '{}', will retry next cycle", singularLabel(), deployable.id(), t);
                pendingDeploys.put(rk, deployable);
                return Flowable.empty();
            });
    }

    private Flowable<D> undeploy(Deployer<D> deployer, D deployable, Set<String> attemptedThisCycle) {
        String rk = runtimeKey(deployable);
        attemptedThisCycle.add(rk);
        // Latest control-plane intent for this document is UNPUBLISH: cancel any pending staging.
        pendingDeploys.remove(rk);
        // UNPUBLISH can arrive with no targetPdpIds (e.g. the PDP was already deleted, so the control
        // plane resolves an empty set). Fall back to the last known placement so the document is evicted
        // from the scopes it was actually deployed to, instead of being left orphaned in the engines.
        if (deployable.targetPdpIds().isEmpty()) {
            deployable.targetPdpIds(placement.applied(rk));
        }
        return deployer
            .undeploy(deployable)
            .andThen(deployer.doAfterUndeployment(deployable))
            .doOnComplete(() -> {
                pendingUndeploys.remove(rk);
                pendingAttempts.remove(rk);
                placement.forget(rk);
            })
            .andThen(Flowable.just(deployable))
            .onErrorResumeNext(t -> {
                log.error("Failed to stage {} '{}' removal, will retry next cycle", singularLabel(), deployable.id(), t);
                pendingUndeploys.put(rk, deployable);
                return Flowable.empty();
            });
    }

    private Completable retryPending(Set<String> attemptedThisCycle) {
        return Completable.defer(() -> {
            List<D> undeploys = drivable(pendingUndeploys, attemptedThisCycle);
            List<D> deploys = drivable(pendingDeploys, attemptedThisCycle);
            if (undeploys.isEmpty() && deploys.isEmpty()) {
                return Completable.complete();
            }
            log.debug("Re-driving {} pending {} removal(s) and {} staging(s)", undeploys.size(), singularLabel(), deploys.size());
            Deployer<D> deployer = createDeployer();
            return Flowable.fromIterable(undeploys)
                .concatMapCompletable(deployable -> undeploy(deployer, deployable, attemptedThisCycle).ignoreElements())
                .andThen(
                    Flowable.fromIterable(deploys).concatMapCompletable(deployable ->
                        deploy(deployer, deployable, attemptedThisCycle).ignoreElements()
                    )
                );
        });
    }

    private List<D> drivable(Map<String, D> pending, Set<String> attemptedThisCycle) {
        List<D> result = new ArrayList<>();
        for (D deployable : new ArrayList<>(pending.values())) {
            String rk = runtimeKey(deployable);
            // An item already attempted live this cycle is retried on the NEXT cycle, not immediately.
            if (attemptedThisCycle.contains(rk)) {
                continue;
            }
            int attempts = pendingAttempts.merge(rk, 1, Integer::sum);
            if (attempts > MAX_PENDING_ATTEMPTS) {
                log.warn("Abandoning pending {} '{}' after {} attempts", singularLabel(), deployable.id(), attempts - 1);
                pending.remove(rk);
                pendingAttempts.remove(rk);
                continue;
            }
            result.add(deployable);
        }
        return result;
    }

    /**
     * Catch-up phase for freshly-provisioned scopes, reusing the same fetch → map → deploy → commit
     * pipeline as {@link #synchronize}. The expensive part — one full ({@code from = -1}) events_latest
     * scan plus mapping of every document — is done ONCE here and the result grouped by scope, so a node
     * hydrating N scopes pays a single scan instead of N (startup cost O(docs) instead of O(scopes×docs)).
     * Membership is captured from each document's original target set BEFORE any staging mutates it; a
     * {@code *} document is fanned out to every scope. {@link AuthzPdpSynchronizer} then drives the
     * per-scope staging via {@link #stageScope}, keeping per-scope commit and retry isolation.
     */
    public Single<Map<String, List<D>>> groupByScope(String environmentId, Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return Single.just(Map.of());
        }
        return eventsFetcher
            .fetchLatest(-1L, null, eventProperty(), Set.of(environmentId), Set.of(publishType()))
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .flatMapIterable(events -> events)
            .flatMapMaybe(this::toDeploy)
            .filter(deployable -> environmentId.equals(deployable.environmentId()))
            .toList()
            .map(docs -> groupDocs(docs, scopes));
    }

    private Map<String, List<D>> groupDocs(List<D> docs, Set<String> scopes) {
        Map<String, List<D>> byScope = new LinkedHashMap<>();
        for (String scope : scopes) {
            byScope.put(scope, new ArrayList<>());
        }
        for (D doc : docs) {
            Set<String> targets = doc.targetPdpIds();
            if (targets == null) {
                continue;
            }
            boolean wildcard = targets.contains(WILDCARD);
            for (String scope : scopes) {
                if (wildcard || targets.contains(scope)) {
                    byScope.get(scope).add(doc);
                }
            }
        }
        return byScope;
    }

    /** Stage the pre-grouped documents for a single scope (route to {@code scope} only, never evict) and
     *  seal that scope's engine. Records each document in the shared {@link AuthzScopePlacement} so a later
     *  narrowing PUBLISH can still evict it. */
    public Completable stageScope(String environmentId, String scope, List<D> docs) {
        Deployer<D> deployer = createDeployer();
        return Flowable.fromIterable(docs)
            .concatMapCompletable(doc -> backfillOne(deployer, doc, scope))
            .andThen(Completable.defer(() -> enginePort.commitScope(environmentId, scope)));
    }

    private Completable backfillOne(Deployer<D> deployer, D deployable, String scope) {
        String rk = runtimeKey(deployable);
        // Route only to the freshly-provisioned scope (not the document's full target set) and ADD — never
        // evict — so scopes already applied on this node are untouched. Record the scope so a later
        // narrowing PUBLISH can still evict the document from it.
        deployable.targetPdpIds(Set.of(scope));
        deployable.removedTargetPdpIds(Set.of());
        return deployer.deploy(deployable).doOnComplete(() -> placement.addScope(rk, scope));
    }

    protected int bulkEvents() {
        return eventsFetcher.bulkItems();
    }
}
