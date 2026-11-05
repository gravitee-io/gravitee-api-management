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

import static io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateChecks.grantCovers;
import static io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateChecks.isFoundByOpenKey;
import static io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateChecks.isKept;
import static io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateChecks.supersedes;

import io.gravitee.gateway.handlers.api.manager.ApprovalStateManager;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
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
 * <p>The sync threads write in parallel. A state is replaced under its id's {@code compute()}, which compares versions so
 * that an older copy never replaces a newer one, and maintains the indexes in the same call, so they cannot drift from
 * the states they find. A newer copy is indexed under its keys before the keys it no longer has are removed, so a lookup
 * made meanwhile still finds it. The calls waiting on a state are woken once it is written.
 *
 * <p>A state leaves memory when AIM unpublishes it, the way a closed subscription or a revoked API key does. Lookups
 * read memory on the calling thread and never return a state past its {@code keepUntil}, so a state waiting for its
 * unpublish is never used.
 */
public class ApprovalStateManagerImpl implements ApprovalStateManager {

    private final ApprovalFeature approvalFeature;
    private final Clock clock;

    // Must stay backed by ConcurrentHashMap: the compute() lambdas below update the indexes, and rely on CHM running the
    // remapping function at most once, under the bin lock, as SubscriptionCacheService does.
    private final ConcurrentMap<String, DeployedApprovalState> statesById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> idsByAlias = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> idsByOpenKey = new ConcurrentHashMap<>();
    // Keyed by the grant's digest, so that no end-user id is kept as a key.
    private final ConcurrentMap<String, Set<String>> idsByGrant = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<CompletableSubject>> waitersById = new ConcurrentHashMap<>();

    public ApprovalStateManagerImpl(ApprovalFeature approvalFeature, Clock clock) {
        this.approvalFeature = approvalFeature;
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
    public Maybe<DeployedApprovalState> byId(String approvalRequestId) {
        if (approvalRequestId == null) {
            return Maybe.empty();
        }
        return Maybe.fromCallable(() -> {
            long now = clock.millis();
            DeployedApprovalState state = statesById.get(approvalRequestId);
            if (state != null) {
                return isKept(state, now) ? state : null;
            }
            return found(idsByAlias, approvalRequestId)
                .filter(aliased -> isKept(aliased, now) && aliased.aliasIds().contains(approvalRequestId))
                .findFirst()
                .orElse(null);
        });
    }

    @Override
    public Maybe<DeployedApprovalState> byOpenKey(String openKey) {
        if (openKey == null) {
            return Maybe.empty();
        }
        return Maybe.fromCallable(() -> {
            long now = clock.millis();
            return found(idsByOpenKey, openKey)
                .filter(state -> isKept(state, now) && isFoundByOpenKey(state) && openKey.equals(state.openKey()))
                .reduce((first, second) -> first.updatedAt() >= second.updatedAt() ? first : second)
                .orElse(null);
        });
    }

    @Override
    public Maybe<DeployedApprovalState> activeGrant(
        String environmentId,
        String apiId,
        String ruleId,
        String applicationId,
        String subject,
        String argumentsDigest
    ) {
        GrantKey key = new GrantKey(environmentId, apiId, ruleId, applicationId, subject, argumentsDigest);
        return Maybe.fromCallable(() -> {
            long now = clock.millis();
            return found(idsByGrant, key.digest())
                .filter(state -> isKept(state, now) && grantCovers(state, now) && key.equals(GrantKey.of(state)))
                .reduce((first, second) -> first.grant().coversUntil() >= second.grant().coversUntil() ? first : second)
                .orElse(null);
        });
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
    public boolean receivesStates(String organizationId) {
        return approvalFeature.isEnabledFor(organizationId);
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
            keys.add(new IndexKey(Index.GRANT, GrantKey.of(state).digest()));
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
            keys.add(new IndexKey(Index.GRANT, GrantKey.of(current).digest()));
        }
        keys.removeAll(kept);
        return keys;
    }

    private void addId(IndexKey key, String id) {
        // compute() so that the sync threads writing at once cannot lose an id or share a plain set across threads.
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
