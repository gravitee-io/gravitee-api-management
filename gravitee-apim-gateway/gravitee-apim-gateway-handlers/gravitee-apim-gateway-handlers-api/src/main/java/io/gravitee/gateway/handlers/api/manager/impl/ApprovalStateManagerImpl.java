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
package io.gravitee.gateway.handlers.api.manager.impl;

import io.gravitee.gamma.definition.approval.ApprovalOpenKey;
import io.gravitee.gateway.handlers.api.manager.ApprovalStateManager;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Keeps the approval states the sync receives in this gateway's memory, the way {@code SubscriptionCacheService} and
 * {@code ApiKeyCacheService} keep subscriptions and API keys: one map by id, and one index per other way a state is
 * found (alias, open key, time-based grant), each listing the ids it finds. Several requests can share an open key or a
 * grant (two nodes opening the same call before AIM joins them), so removing one never hides the others.
 *
 * <p>The generic event manager serializes its calls, so states are written one at a time, while tool calls read them on
 * their own threads. A state is still replaced under its id's {@code compute()}, which compares versions so that an
 * older copy never replaces a newer one, and maintains the indexes in the same call, so they cannot drift from the
 * states they find whichever threads write. A newer copy is indexed under its keys before the keys it no longer has are removed, so a lookup
 * made meanwhile still finds it. The calls waiting on a state are woken once it is written.
 *
 * <p>A state leaves memory when the event that carried it is removed or expires, which the sync does at the first
 * commit after the expiry. Lookups read memory on the calling thread and never return a state past its
 * {@code keepUntil}, so a state waiting to be removed is never used.
 */
public class ApprovalStateManagerImpl implements ApprovalStateManager, MeterBinder {

    private static final String TIMED = "TIMED";

    private final Clock clock;

    // Must stay backed by ConcurrentHashMap: the compute() lambdas below update the indexes, and rely on CHM running the
    // remapping function at most once, under the bin lock, as SubscriptionCacheService does.
    private final ConcurrentMap<String, DeployedApprovalState> statesById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> idsByAlias = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> idsByOpenKey = new ConcurrentHashMap<>();
    // Keyed by the open key of the calls a grant covers.
    private final ConcurrentMap<String, Set<String>> idsByGrant = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<CompletableSubject>> waitersById = new ConcurrentHashMap<>();

    public ApprovalStateManagerImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void deploy(DeployedApprovalState state) {
        long now = clock.millis();
        AtomicBoolean written = new AtomicBoolean();
        statesById.compute(state.id(), (id, current) -> {
            if (current != null && !supersedes(state, current)) {
                // An older copy, or the same one read again: nothing changes and nobody is woken.
                return current;
            }
            written.set(true);
            boolean kept = isKept(state, now);
            Set<IndexKey> keys = kept ? indexKeys(state, now) : Set.of();
            keys.forEach(key -> addId(key, id));
            if (current != null) {
                removedKeys(current, keys).forEach(key -> removeId(key, id));
            }
            return kept ? state : null;
        });
        if (written.get()) {
            wakeAll(state);
        }
    }

    @Override
    public void undeploy(String approvalRequestId) {
        AtomicReference<DeployedApprovalState> removed = new AtomicReference<>();
        statesById.computeIfPresent(approvalRequestId, (id, current) -> {
            removedKeys(current, Set.of()).forEach(key -> removeId(key, id));
            removed.set(current);
            return null;
        });
        DeployedApprovalState current = removed.get();
        if (current == null) {
            wake(approvalRequestId);
        } else {
            wakeAll(current);
        }
    }

    @Override
    public Optional<DeployedApprovalState> byId(String approvalRequestId) {
        if (approvalRequestId == null) {
            return Optional.empty();
        }
        long now = clock.millis();
        DeployedApprovalState state = statesById.get(approvalRequestId);
        if (state != null) {
            return isKept(state, now) ? Optional.of(state) : Optional.empty();
        }
        return found(idsByAlias, approvalRequestId)
            .filter(aliased -> isKept(aliased, now) && aliased.aliasIds().contains(approvalRequestId))
            .findFirst();
    }

    @Override
    public Optional<DeployedApprovalState> byOpenKey(String openKey) {
        if (openKey == null) {
            return Optional.empty();
        }
        long now = clock.millis();
        return found(idsByOpenKey, openKey)
            .filter(state -> isKept(state, now) && isFoundByOpenKey(state) && openKey.equals(state.openKey()))
            .reduce((first, second) -> first.updatedAt() >= second.updatedAt() ? first : second);
    }

    @Override
    public Optional<DeployedApprovalState> activeGrant(String openKey, long ruleUpdatedAt) {
        if (openKey == null) {
            return Optional.empty();
        }
        long now = clock.millis();
        return found(idsByGrant, openKey)
            .filter(
                state ->
                    isKept(state, now) &&
                    grantCovers(state, now) &&
                    state.ruleUpdatedAt() == ruleUpdatedAt &&
                    openKey.equals(grantKey(state))
            )
            .reduce((first, second) -> first.grant().coversUntil() >= second.grant().coversUntil() ? first : second);
    }

    /**
     * The wait is registered when asked for, not when subscribed to: a caller that asks for the wait before reading the
     * state cannot miss a change made in between. A wait never subscribed to is forgotten once {@code atMost} has
     * passed.
     */
    @Override
    public Completable awaitChange(String approvalRequestId, Duration atMost, Scheduler scheduler) {
        CompletableSubject changed = CompletableSubject.create();
        waitersById.compute(approvalRequestId, (id, waiters) -> {
            Set<CompletableSubject> registered = waiters != null ? waiters : ConcurrentHashMap.newKeySet();
            registered.add(changed);
            return registered;
        });
        Disposable timeout = scheduler.scheduleDirect(
            () -> {
                forgetWaiter(approvalRequestId, changed);
                changed.onComplete();
            },
            atMost.toMillis(),
            TimeUnit.MILLISECONDS
        );
        return changed
            .doFinally(() -> {
                timeout.dispose();
                forgetWaiter(approvalRequestId, changed);
            })
            .observeOn(scheduler);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("human_approval_states", statesById, ConcurrentMap::size)
            .description("Approval states held by the gateway")
            .register(registry);
        Gauge.builder("human_approval_state_waiters", this, ApprovalStateManagerImpl::waiters)
            .description("Held calls waiting for their approval state to change")
            .register(registry);
    }

    // Visible for testing
    int states() {
        return statesById.size();
    }

    // Visible for testing
    Set<String> indexed(Index index) {
        return ids(index).keySet();
    }

    // Visible for testing
    int waiters() {
        return waitersById.values().stream().mapToInt(Set::size).sum();
    }

    /** The keys a kept state is found by. */
    private static Set<IndexKey> indexKeys(DeployedApprovalState state, long now) {
        Set<IndexKey> keys = new HashSet<>();
        state.aliasIds().forEach(aliasId -> keys.add(new IndexKey(Index.ALIAS, aliasId)));
        if (isFoundByOpenKey(state)) {
            keys.add(new IndexKey(Index.OPEN_KEY, state.openKey()));
        }
        if (grantCovers(state, now)) {
            keys.add(new IndexKey(Index.GRANT, grantKey(state)));
        }
        return keys;
    }

    /** The keys {@code current} may be indexed under that {@code kept} no longer holds. */
    private static Set<IndexKey> removedKeys(DeployedApprovalState current, Set<IndexKey> kept) {
        Set<IndexKey> keys = new HashSet<>();
        current.aliasIds().forEach(aliasId -> keys.add(new IndexKey(Index.ALIAS, aliasId)));
        if (current.openKey() != null) {
            keys.add(new IndexKey(Index.OPEN_KEY, current.openKey()));
        }
        if (current.grant() != null) {
            keys.add(new IndexKey(Index.GRANT, grantKey(current)));
        }
        keys.removeAll(kept);
        return keys;
    }

    private void addId(IndexKey key, String id) {
        // compute() creates, fills and drops a set in one step, so no id is lost whichever threads write, though the sync writes one at a time.
        ids(key.index()).compute(key.key(), (k, ids) -> {
            Set<String> registered = ids != null ? ids : ConcurrentHashMap.newKeySet();
            registered.add(id);
            return registered;
        });
    }

    private void removeId(IndexKey key, String id) {
        ids(key.index()).computeIfPresent(key.key(), (k, ids) -> {
            ids.remove(id);
            return ids.isEmpty() ? null : ids;
        });
    }

    /**
     * AIM raises the version on every change, so when both copies carry one the higher wins, whichever arrives last.
     * Otherwise, or for the same version, the copies are ordered by {@code updatedAt}: AIM keeps one latest event per
     * request and overwrites it, so a copy read later is never an older write than one read before, even when both carry
     * the same millisecond. Such a copy that differs is therefore the later write and replaces the one held; an
     * identical copy, read again, changes nothing and wakes nobody.
     */
    private static boolean supersedes(DeployedApprovalState candidate, DeployedApprovalState current) {
        if (candidate.version() > 0 && current.version() > 0 && candidate.version() != current.version()) {
            return candidate.version() > current.version();
        }
        return (
            candidate.updatedAt() > current.updatedAt() || (candidate.updatedAt() == current.updatedAt() && !candidate.equals(current))
        );
    }

    private static boolean isFoundByOpenKey(DeployedApprovalState state) {
        return state.openKey() != null && state.approvalStatus().isFoundByOpenKey();
    }

    private static boolean isKept(DeployedApprovalState state, long now) {
        return state.keepUntil() > now;
    }

    private static boolean grantCovers(DeployedApprovalState state, long now) {
        DeployedApprovalState.Grant grant = state.grant();
        return (
            grant != null &&
            TIMED.equals(grant.scope()) &&
            grant.coversUntil() != null &&
            grant.coversUntil() > now &&
            state.approvalStatus().isGranting()
        );
    }

    /**
     * The open key of the calls a grant covers. A released request no longer carries its open key, so it is computed
     * from the grant; a digest, so that no end-user id is kept as a key.
     */
    private static String grantKey(DeployedApprovalState state) {
        DeployedApprovalState.Grant grant = state.grant();
        return ApprovalOpenKey.of(
            state.environmentId(),
            state.apiId(),
            state.ruleId(),
            grant.applicationId(),
            grant.subject(),
            grant.argumentsDigest()
        );
    }

    private Stream<DeployedApprovalState> found(ConcurrentMap<String, Set<String>> index, String key) {
        Set<String> ids = index.get(key);
        if (ids == null) {
            return Stream.empty();
        }
        return ids.stream().map(statesById::get).filter(Objects::nonNull);
    }

    private ConcurrentMap<String, Set<String>> ids(Index index) {
        return switch (index) {
            case ALIAS -> idsByAlias;
            case OPEN_KEY -> idsByOpenKey;
            case GRANT -> idsByGrant;
        };
    }

    private void wakeAll(DeployedApprovalState state) {
        wake(state.id());
        state.aliasIds().forEach(this::wake);
    }

    private void wake(String approvalRequestId) {
        Set<CompletableSubject> waiters = waitersById.remove(approvalRequestId);
        if (waiters != null) {
            waiters.forEach(CompletableSubject::onComplete);
        }
    }

    private void forgetWaiter(String approvalRequestId, CompletableSubject waiter) {
        waitersById.computeIfPresent(approvalRequestId, (id, waiters) -> {
            waiters.remove(waiter);
            return waiters.isEmpty() ? null : waiters;
        });
    }

    enum Index {
        ALIAS,
        OPEN_KEY,
        GRANT,
    }

    private record IndexKey(Index index, String key) {}
}
