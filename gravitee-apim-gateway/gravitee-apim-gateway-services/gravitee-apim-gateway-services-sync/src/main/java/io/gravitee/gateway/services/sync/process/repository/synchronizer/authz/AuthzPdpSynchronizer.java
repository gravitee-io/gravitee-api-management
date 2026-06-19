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

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.CustomLog;

@CustomLog
public class AuthzPdpSynchronizer implements RepositorySynchronizer {

    public static final String PROVISION_ADDRESS = "service:authz-pdp:provision";
    static final String OP_PROVISION = "provision";
    static final String OP_EVICT = "evict";
    static final String DEFAULT_SCOPE = "default";
    static final String SCOPE_TAG_SEPARATOR = "@";
    static final long REPLY_TIMEOUT_MS = 5_000L;
    static final int MAX_PENDING_ATTEMPTS = 10;

    private static final Set<EventType> INIT_EVENT_TYPES = Set.of(EventType.PUBLISH_AUTHZ_PDP);
    private static final Set<EventType> INCREMENTAL_EVENT_TYPES = Set.of(EventType.PUBLISH_AUTHZ_PDP, EventType.UNPUBLISH_AUTHZ_PDP);

    private final LatestEventFetcher eventsFetcher;
    private final AuthzPdpMapper mapper;
    private final AuthzPolicySynchronizer policySynchronizer;
    private final AuthzEntitySynchronizer entitySynchronizer;
    private final Node node;
    private final GatewayConfiguration gatewayConfiguration;
    private final Vertx vertx;
    private final ThreadPoolExecutor syncFetcherExecutor;
    private final ThreadPoolExecutor syncDeployerExecutor;
    private final DeliveryOptions deliveryOptions = new DeliveryOptions().setSendTimeout(REPLY_TIMEOUT_MS);

    /**
     * Node-local set of provisions whose relay did not confirm (startup {@code NO_HANDLERS},
     * reply timeout, explicit failure). Re-driven on every {@link #synchronize} cycle independently
     * of the {@code updatedAt} event window so a transient relay failure is not silently lost.
     * Keyed by runtime key (environmentId + ":" + targetPdpId) so confirmations in one environment
     * cannot drop a pending entry that belongs to another environment.
     */
    private final Map<String, AuthzPdpProvisionDeployable> pendingProvisions = new ConcurrentHashMap<>();

    /**
     * Symmetric to {@link #pendingProvisions} for the evict path: an evict relay that did not confirm
     * is kept here and re-driven on the next cycle, otherwise a disabled PDP keeps serving forever.
     * Keyed by runtime key.
     */
    private final Map<String, AuthzPdpProvisionDeployable> pendingEvicts = new ConcurrentHashMap<>();

    /**
     * Provisioned scopes whose hydration (policy/entity backfill + commit) failed. Re-driven on the
     * next cycle so a transient engine failure does not leave a scope cold-started forever. Keyed by
     * runtime key.
     */
    private final Map<String, AuthzPdpProvisionDeployable> pendingHydrations = new ConcurrentHashMap<>();

    private final Map<String, Integer> pendingProvisionAttempts = new ConcurrentHashMap<>();

    /** Re-drive counter for {@link #pendingHydrations}, capped at {@link #MAX_PENDING_ATTEMPTS} so a
     *  permanently failing hydration stops re-driving every cycle instead of retrying forever. */
    private final Map<String, Integer> pendingHydrationAttempts = new ConcurrentHashMap<>();

    /** Scopes provisioned on this node — so the entity/policy synchronizers only stage into engines that
     *  actually live here, instead of routing blindly to every targetPdpId and hitting NO_HANDLERS. */
    private final AuthzHostedScopes hostedScopes;

    private static String runtimeKey(AuthzPdpProvisionDeployable deployable) {
        return deployable.environmentId() + ":" + deployable.targetPdpId();
    }

    public AuthzPdpSynchronizer(
        LatestEventFetcher eventsFetcher,
        AuthzPdpMapper mapper,
        AuthzPolicySynchronizer policySynchronizer,
        AuthzEntitySynchronizer entitySynchronizer,
        Node node,
        GatewayConfiguration gatewayConfiguration,
        Vertx vertx,
        AuthzHostedScopes hostedScopes,
        ThreadPoolExecutor syncFetcherExecutor,
        ThreadPoolExecutor syncDeployerExecutor
    ) {
        this.eventsFetcher = eventsFetcher;
        this.mapper = mapper;
        this.policySynchronizer = Objects.requireNonNull(policySynchronizer, "policySynchronizer must not be null");
        this.entitySynchronizer = Objects.requireNonNull(entitySynchronizer, "entitySynchronizer must not be null");
        this.node = Objects.requireNonNull(node, "node must not be null");
        this.gatewayConfiguration = Objects.requireNonNull(gatewayConfiguration, "gatewayConfiguration must not be null");
        this.vertx = Objects.requireNonNull(vertx, "vertx must not be null");
        this.hostedScopes = Objects.requireNonNull(hostedScopes, "hostedScopes must not be null");
        this.syncFetcherExecutor = syncFetcherExecutor;
        this.syncDeployerExecutor = syncDeployerExecutor;
    }

    @Override
    public Completable synchronize(Long from, Long to, Set<String> environments) {
        AtomicLong launchTime = new AtomicLong();
        AtomicLong processed = new AtomicLong();
        ConcurrentLinkedQueue<AuthzPdpProvisionDeployable> provisionedScopes = new ConcurrentLinkedQueue<>();
        boolean initialSync = from == null || from.longValue() == -1L;

        return eventsFetcher
            .fetchLatest(
                from,
                to,
                Event.EventProperties.AUTHZ_PDP_ID,
                environments,
                initialSync ? INIT_EVENT_TYPES : INCREMENTAL_EVENT_TYPES
            )
            .subscribeOn(Schedulers.from(syncFetcherExecutor))
            .rebatchRequests(syncFetcherExecutor.getMaximumPoolSize())
            .compose(events -> processEvents(events, provisionedScopes))
            .count()
            .doOnSubscribe(d -> launchTime.set(Instant.now().toEpochMilli()))
            .doOnSuccess(processed::set)
            .ignoreElement()
            .andThen(retryPending(provisionedScopes))
            .andThen(hydrate(provisionedScopes))
            .doOnComplete(() -> {
                String msg = String.format(
                    "%s authz PDP provisioning commands relayed in %sms",
                    processed.get(),
                    System.currentTimeMillis() - launchTime.get()
                );
                if (initialSync) {
                    log.info(msg);
                } else {
                    log.debug(msg);
                }
            });
    }

    @Override
    public int order() {
        return Order.AUTHZ_PDP.index();
    }

    private Flowable<AuthzPdpProvisionDeployable> processEvents(
        Flowable<List<Event>> eventsFlowable,
        ConcurrentLinkedQueue<AuthzPdpProvisionDeployable> provisionedScopes
    ) {
        return eventsFlowable
            .flatMapIterable(events -> events)
            .groupBy(Event::getType)
            .flatMap(eventsByType -> {
                EventType type = eventsByType.getKey();
                if (type == EventType.PUBLISH_AUTHZ_PDP) {
                    return eventsByType.flatMapMaybe(mapper::toDeploy);
                } else if (type == EventType.UNPUBLISH_AUTHZ_PDP) {
                    return eventsByType.flatMapMaybe(mapper::toUndeploy);
                }
                return Flowable.empty();
            })
            .flatMap(this::tagGate)
            .toList()
            .flatMapPublisher(AuthzPdpSynchronizer::reconcileScopeReuse)
            .compose(upstream ->
                upstream
                    .parallel(syncDeployerExecutor.getMaximumPoolSize())
                    .runOn(Schedulers.from(syncDeployerExecutor))
                    .flatMap(deployable -> relayProvisionOrEvict(deployable, provisionedScopes).andThen(Flowable.just(deployable)))
                    .sequential(bulkEvents())
            );
    }

    private static Flowable<AuthzPdpProvisionDeployable> reconcileScopeReuse(List<AuthzPdpProvisionDeployable> deployables) {
        Set<String> livePublishKeys = deployables
            .stream()
            .filter(d -> d.syncAction() == SyncAction.DEPLOY)
            .map(AuthzPdpSynchronizer::runtimeKey)
            .collect(Collectors.toSet());
        return Flowable.fromIterable(
            deployables
                .stream()
                .filter(d -> {
                    if (d.syncAction() == SyncAction.UNDEPLOY && livePublishKeys.contains(runtimeKey(d))) {
                        log.debug(
                            "Suppressing AUTHZ_PDP evict for env [{}] targetPdpId [{}] — a live provision reuses the same scope",
                            d.environmentId(),
                            d.targetPdpId()
                        );
                        return false;
                    }
                    return true;
                })
                .toList()
        );
    }

    private Flowable<AuthzPdpProvisionDeployable> tagGate(AuthzPdpProvisionDeployable deployable) {
        // The default scope is bootstrapped on every node by the PDP service (and addressed on the
        // unscoped bus); it is never provisioned or evicted via events, so drop it here defensively.
        if (DEFAULT_SCOPE.equals(deployable.targetPdpId())) {
            return Flowable.empty();
        }
        // A node provisions AND evicts a named scope only when it carries the PDP's tag — the gate is
        // symmetric. This is required for regional replicas (same targetPdpId, different tags on disjoint
        // nodes): a delete of (X, tagA) emits unpublishPdp(X, tagA), and the engine is keyed by the bare X,
        // so letting that evict through on a (X, tagB) node would tear down the survivor's engine. A node
        // that doesn't carry the tag never hosted the scope, so dropping the evict there is correct.
        // (A re-tag is a delete+recreate: the delete's UNPUBLISH carries the OLD tag and cleans up the node
        // that actually matched it; no cross-node evict broadcast is needed.)
        if (matchesNodeTag(deployable)) {
            return Flowable.just(deployable);
        }
        return Flowable.empty();
    }

    private boolean matchesNodeTag(AuthzPdpProvisionDeployable deployable) {
        // Placement mirrors API sharding (GatewayConfiguration#hasMatchingTags): an untagged gateway is a
        // catch-all that hosts every engine, a tagged gateway hosts a PDP whose tag it carries (and is not
        // excluded via "!tag"). A PDP without a tag behaves like an untagged document — hosted only on an
        // untagged (catch-all) node.
        String tag = deployable.tag();
        Set<String> tags = (tag == null || tag.isBlank()) ? Set.of() : Set.of(tag);
        boolean matches = EnvironmentUtils.hasMatchingTags(gatewayConfiguration.shardingTags(), tags);
        if (!matches) {
            log.debug(
                "Skipping AUTHZ_PDP {} for targetPdpId [{}] — tags {} not matched by this node's sharding tags {}",
                deployable.syncAction(),
                deployable.targetPdpId(),
                tags,
                gatewayConfiguration.shardingTags().orElseGet(List::of)
            );
        }
        return matches;
    }

    private Completable relayProvisionOrEvict(
        AuthzPdpProvisionDeployable deployable,
        ConcurrentLinkedQueue<AuthzPdpProvisionDeployable> provisionedScopes
    ) {
        boolean provision = deployable.syncAction() == SyncAction.DEPLOY;
        String op = provision ? OP_PROVISION : OP_EVICT;
        JsonObject command = new JsonObject()
            .put("op", op)
            .put("environmentId", deployable.environmentId())
            .put("targetPdpId", deployable.targetPdpId());
        return vertx
            .eventBus()
            .rxRequest(PROVISION_ADDRESS, command, deliveryOptions)
            .ignoreElement()
            .doOnComplete(() -> {
                String rk = runtimeKey(deployable);
                if (provision) {
                    // Confirmed relay reply — only now is the scope eligible for hydration and visible
                    // to the entity/policy synchronizers as locally hosted.
                    pendingProvisions.remove(rk);
                    pendingProvisionAttempts.remove(rk);
                    pendingEvicts.remove(rk);
                    hostedScopes.markHosted(deployable.environmentId(), deployable.targetPdpId());
                    provisionedScopes.add(deployable);
                } else {
                    // Evict confirmed: drop any pending provision (evict wins), pending hydration and evict,
                    // and stop treating the scope as locally hosted.
                    pendingProvisions.remove(rk);
                    pendingProvisionAttempts.remove(rk);
                    pendingHydrations.remove(rk);
                    pendingHydrationAttempts.remove(rk);
                    pendingEvicts.remove(rk);
                    hostedScopes.unmarkHosted(deployable.environmentId(), deployable.targetPdpId());
                }
            })
            .onErrorResumeNext(t -> {
                String rk = runtimeKey(deployable);
                log.error("Failed to relay AUTHZ_PDP {} for targetPdpId [{}]", op, deployable.targetPdpId(), t);
                if (provision) {
                    // Re-drive on the next cycle independently of the event window — do NOT report
                    // success-without-retry, the scope is not added to provisionedScopes here.
                    pendingProvisions.put(rk, deployable);
                } else {
                    // Evict relay failed: re-drive it next cycle and clear any pending provision/hydration
                    // so a stale entry cannot resurrect a scope the control plane already removed.
                    pendingProvisions.remove(rk);
                    pendingProvisionAttempts.remove(rk);
                    pendingHydrations.remove(rk);
                    pendingHydrationAttempts.remove(rk);
                    pendingEvicts.put(rk, deployable);
                }
                return Completable.complete();
            });
    }

    private Completable retryPending(ConcurrentLinkedQueue<AuthzPdpProvisionDeployable> provisionedScopes) {
        return Completable.defer(() -> {
            // Evicts re-drive first: an evict and a provision can never both stay pending for the same
            // runtime key (evict clears the pending provision), so ordering only matters for logging.
            List<AuthzPdpProvisionDeployable> evicts = new ArrayList<>(pendingEvicts.values());
            List<AuthzPdpProvisionDeployable> provisions = new ArrayList<>(pendingProvisions.values());
            if (evicts.isEmpty() && provisions.isEmpty()) {
                return Completable.complete();
            }
            log.debug(
                "Re-driving {} pending AUTHZ_PDP evict(s) and {} provision(s) that did not confirm on a previous cycle",
                evicts.size(),
                provisions.size()
            );
            List<AuthzPdpProvisionDeployable> drivableProvisions = provisions
                .stream()
                .filter(deployable -> {
                    String rk = runtimeKey(deployable);
                    if (!matchesNodeTag(deployable)) {
                        // Node was re-sharded away from this tag — stop owning the scope.
                        pendingProvisions.remove(rk);
                        pendingProvisionAttempts.remove(rk);
                        return false;
                    }
                    int attempts = pendingProvisionAttempts.merge(rk, 1, Integer::sum);
                    if (attempts > MAX_PENDING_ATTEMPTS) {
                        log.warn(
                            "Abandoning pending AUTHZ_PDP provision for env [{}] targetPdpId [{}] after {} attempts",
                            deployable.environmentId(),
                            deployable.targetPdpId(),
                            attempts - 1
                        );
                        pendingProvisions.remove(rk);
                        pendingProvisionAttempts.remove(rk);
                        return false;
                    }
                    return true;
                })
                .toList();
            return Flowable.fromIterable(evicts)
                .concatMapCompletable(deployable -> relayProvisionOrEvict(deployable, provisionedScopes))
                .andThen(
                    Flowable.fromIterable(drivableProvisions).concatMapCompletable(deployable ->
                        relayProvisionOrEvict(deployable, provisionedScopes)
                    )
                );
        });
    }

    private Completable hydrate(ConcurrentLinkedQueue<AuthzPdpProvisionDeployable> provisionedScopes) {
        return Flowable.defer(() -> {
            Map<String, AuthzPdpProvisionDeployable> toHydrate = new LinkedHashMap<>();
            // Carried-over hydration failures: count each re-drive and abandon past the cap, mirroring
            // the provision retry bound, so a permanently failing scope stops re-driving every cycle.
            for (Map.Entry<String, AuthzPdpProvisionDeployable> entry : pendingHydrations.entrySet()) {
                String rk = entry.getKey();
                int attempts = pendingHydrationAttempts.merge(rk, 1, Integer::sum);
                if (attempts > MAX_PENDING_ATTEMPTS) {
                    log.warn(
                        "Abandoning pending AUTHZ_PDP hydration for env [{}] targetPdpId [{}] after {} attempts",
                        entry.getValue().environmentId(),
                        entry.getValue().targetPdpId(),
                        attempts - 1
                    );
                    pendingHydrations.remove(rk);
                    pendingHydrationAttempts.remove(rk);
                    continue;
                }
                toHydrate.put(rk, entry.getValue());
            }
            // Freshly provisioned scopes this cycle hydrate as a first attempt — reset any stale counter
            // so a re-provisioned scope gets the full retry budget again.
            provisionedScopes.forEach(d -> {
                String rk = runtimeKey(d);
                pendingHydrationAttempts.remove(rk);
                toHydrate.put(rk, d);
            });
            // Group the scopes to hydrate by environment so each environment's events_latest is scanned
            // and mapped ONCE, then fanned out to every scope — instead of a full scan per scope (startup
            // cost O(docs) instead of O(scopes × docs)).
            Map<String, List<AuthzPdpProvisionDeployable>> byEnv = new LinkedHashMap<>();
            for (AuthzPdpProvisionDeployable d : toHydrate.values()) {
                byEnv.computeIfAbsent(d.environmentId(), k -> new ArrayList<>()).add(d);
            }
            return Flowable.fromIterable(byEnv.values());
        }).concatMapCompletable(this::hydrateEnv);
    }

    private Completable hydrateEnv(List<AuthzPdpProvisionDeployable> provisions) {
        String environmentId = provisions.get(0).environmentId();
        // Scope -> provision, so a per-scope outcome can be mapped back to its runtime key for retry.
        Map<String, AuthzPdpProvisionDeployable> byScope = new LinkedHashMap<>();
        for (AuthzPdpProvisionDeployable p : provisions) {
            byScope.put(routingScope(p.targetPdpId(), p.tag()), p);
        }
        Set<String> scopes = byScope.keySet();
        // One scan+map of policies and one of entities for the whole environment, grouped by scope.
        return Single.zip(
            policySynchronizer.groupByScope(environmentId, scopes),
            entitySynchronizer.groupByScope(environmentId, scopes),
            BackfillGroups::new
        )
            .flatMapCompletable(groups ->
                Flowable.fromIterable(byScope.entrySet()).concatMapCompletable(e ->
                    hydrateScope(environmentId, e.getKey(), e.getValue(), groups)
                )
            )
            .onErrorResumeNext(t -> {
                // The shared fetch/grouping failed: re-drive the whole environment's scopes next cycle.
                provisions.forEach(p -> pendingHydrations.put(runtimeKey(p), p));
                log.error("Failed to fetch documents for hydration of env [{}], will retry next cycle", environmentId, t);
                return Completable.complete();
            });
    }

    private Completable hydrateScope(String environmentId, String scope, AuthzPdpProvisionDeployable provision, BackfillGroups groups) {
        String rk = runtimeKey(provision);
        // Per scope: policies then entities (policy failure short-circuits the scope), so a single scope's
        // engine failure re-pends only that scope, not the whole environment.
        return policySynchronizer
            .stageScope(environmentId, scope, groups.policies().getOrDefault(scope, List.of()))
            .andThen(entitySynchronizer.stageScope(environmentId, scope, groups.entities().getOrDefault(scope, List.of())))
            .doOnComplete(() -> {
                pendingHydrations.remove(rk);
                pendingHydrationAttempts.remove(rk);
                log.debug("Hydrated authz PDP scope [{}] via synchronizer backfill", scope);
            })
            .onErrorResumeNext(t -> {
                pendingHydrations.put(rk, provision);
                log.error("Failed to hydrate authz PDP scope [{}], will retry next cycle", scope, t);
                return Completable.complete();
            });
    }

    private record BackfillGroups(
        Map<String, List<AuthzPolicyReactorDeployable>> policies,
        Map<String, List<AuthzEntityReactorDeployable>> entities
    ) {}

    private static String routingScope(String targetPdpId, String tag) {
        return (tag != null && !tag.isBlank()) ? targetPdpId + SCOPE_TAG_SEPARATOR + tag : targetPdpId;
    }

    protected int bulkEvents() {
        return eventsFetcher.bulkItems();
    }
}
