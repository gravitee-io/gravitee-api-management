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
package io.gravitee.gateway.handlers.api.services;

import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;

import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.security.core.SubscriptionTrustStoreLoaderManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@CustomLog
@RequiredArgsConstructor
public class SubscriptionCacheService implements SubscriptionService {

    private static final int SUBSCRIPTION_LOCK_STRIPES = 1024;
    private static final Object[] SUBSCRIPTION_LOCKS = IntStream.range(0, SUBSCRIPTION_LOCK_STRIPES)
        .mapToObj(ignored -> new Object())
        .toArray();

    private final ApiKeyService apiKeyService;
    private final SubscriptionTrustStoreLoaderManager subscriptionTrustStoreLoaderManager;
    private final ApiManager apiManager;

    // Caches only contain active subscriptions. Mutations for one subscription id are serialized
    // by subscriptionLock(id). The maps remain concurrent because request-path readers and updates
    // for different subscription ids still run concurrently; compute() also publishes each by-id
    // representation change atomically to those lock-free readers.
    private final ConcurrentMap<IdentityKey, Subscription> cacheByApiClientId = new ConcurrentHashMap<>();
    private final ConcurrentMap<IdentityKey, Subscription> cacheByClientCertificate = new ConcurrentHashMap<>();
    // Single by-id index, including exploded API-Product subscriptions. Plain API subscriptions
    // keep a compact singleton value. Only an id with several API/environment legs is promoted to
    // a ConcurrentHashMap, making API-Product registration O(1) without allocating one map per
    // regular subscription at multi-million-subscription scale.
    private final ConcurrentMap<String, SubscriptionLegs> cacheBySubscriptionIdAll = new ConcurrentHashMap<>();
    // Holds subscription ids only, for per-API eviction. Identity keys are not tracked here:
    // unregisterByApiId() re-derives them from the cached subscriptions, which keeps this index
    // at one entry per subscription instead of three (id + 2 identity keys) at multi-million scale.
    private final ConcurrentMap<String, Set<String>> cacheKeysByApiId = new ConcurrentHashMap<>();

    /**
     * Identity-cache key referencing the subscription's own field strings instead of a formatted
     * composite String: no per-key byte[] allocation (hundreds of MB at multi-million-subscription
     * scale) and no String.format on the request hot path. A null plan is the plan-less variant.
     */
    private record IdentityKey(String api, String plan, String clientIdentity) {}

    private record LegKey(String api, String environmentId) {}

    private sealed interface SubscriptionLegs permits SingleSubscriptionLeg, MultipleSubscriptionLegs {
        Collection<Subscription> values();
    }

    private record SingleSubscriptionLeg(Subscription subscription) implements SubscriptionLegs {
        @Override
        public Collection<Subscription> values() {
            return List.of(subscription);
        }
    }

    private static final class MultipleSubscriptionLegs implements SubscriptionLegs {

        private final ConcurrentMap<LegKey, Subscription> subscriptions = new ConcurrentHashMap<>();

        private MultipleSubscriptionLegs(Subscription first, Subscription second) {
            subscriptions.put(legKey(first), first);
            subscriptions.put(legKey(second), second);
        }

        @Override
        public Collection<Subscription> values() {
            return subscriptions.values();
        }
    }

    private static LegKey legKey(Subscription subscription) {
        return new LegKey(subscription.getApi(), subscription.getEnvironmentId());
    }

    @Override
    public Optional<Subscription> getByApiAndSecurityToken(String api, SecurityToken securityToken, String plan) {
        return switch (SecurityToken.TokenType.valueOfOrNone(securityToken.getTokenType())) {
            case API_KEY -> apiKeyService
                .getByApiAndKey(api, securityToken.getTokenValue())
                .flatMap(apiKey -> getByApiAndId(api, apiKey.getSubscription()));
            case MD5_API_KEY -> apiKeyService
                .getByApiAndMd5Key(api, securityToken.getTokenValue())
                .flatMap(apiKey -> getByApiAndId(api, apiKey.getSubscription()));
            case CLIENT_ID -> getByApiAndClientIdAndPlan(api, securityToken.getTokenValue(), plan);
            case CERTIFICATE -> subscriptionTrustStoreLoaderManager.getByCertificate(api, plan, securityToken.getTokenValue());
            default -> Optional.empty();
        };
    }

    @Override
    public Optional<Subscription> getByApiAndClientIdAndPlan(String api, String clientId, String plan) {
        return Optional.ofNullable(cacheByApiClientId.get(cacheKey(api, plan, clientId)));
    }

    @Override
    public Optional<Subscription> getById(String subscriptionId) {
        // For exploded API-Product subscriptions this returns an arbitrary leg, which matches the
        // historical behavior of the dedicated by-id map (last-registered/rehydrated leg).
        SubscriptionLegs subscriptions = cacheBySubscriptionIdAll.get(subscriptionId);
        if (subscriptions == null) {
            return Optional.empty();
        }
        var iterator = subscriptions.values().iterator();
        return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }

    /**
     * Returns all subscriptions for the given ID (multiple for exploded API Product subscriptions).
     * The returned collection cannot be modified through this handle. Multi-leg subscriptions
     * expose a weakly-consistent concurrent view so callers can iterate without an O(P) snapshot.
     */
    public Collection<Subscription> getAllById(String subscriptionId) {
        SubscriptionLegs subscriptions = cacheBySubscriptionIdAll.get(subscriptionId);
        if (subscriptions == null) {
            return Collections.emptySet();
        }
        if (subscriptions instanceof SingleSubscriptionLeg single) {
            return single.values();
        }
        return Collections.unmodifiableCollection(subscriptions.values());
    }

    @Override
    public void register(final Subscription subscription) {
        synchronized (subscriptionLock(subscription.getId())) {
            // only once per synchronization window
            // take all fields (including "updatedAt" in metadata) into account
            if (ACCEPTED.name().equals(subscription.getStatus())) {
                if (subscription.getClientCertificate() != null) {
                    log.debug(
                        "Registering subscription [{}] for API [{}] by client certificate",
                        subscription.getId(),
                        subscription.getApi()
                    );
                    registerFromClientCertificate(subscription);
                } else if (subscription.getClientId() != null) {
                    log.debug(
                        "Registering subscription [{}] for API [{}] by clientId [{}]",
                        subscription.getId(),
                        subscription.getApi(),
                        subscription.getClientId()
                    );
                    registerFromClientId(subscription);
                } else {
                    log.debug("Registering subscription [{}] for API [{}] by ID", subscription.getId(), subscription.getApi());
                    registerFromId(subscription);
                }
            } else {
                log.debug(
                    "Unregistering subscription [{}] for API [{}] with status [{}]",
                    subscription.getId(),
                    subscription.getApi(),
                    subscription.getStatus()
                );
                unregisterInternal(subscription);
            }
        }
    }

    /**
     * Registers a subscription while hydrating an empty cache during gateway cold start.
     *
     * <p>The regular registration path must look for and clean up a previous leg because it is
     * also used by incremental updates. For a new API Product leg that lookup scans sibling APIs
     * when there is no exact match. Repeating it for every API turns a product subscription into
     * O(number of APIs squared) work. Initial repository sync contains unique, active legs and
     * starts from an empty cache, so it can publish them directly through the concurrent indexes.
     * Certificate subscriptions keep per-subscription serialization for trust-store ordering but
     * still skip the incremental replacement lookup.</p>
     */
    public void registerInitial(final Subscription subscription) {
        if (!ACCEPTED.name().equals(subscription.getStatus())) {
            register(subscription);
            return;
        }

        if (subscription.getClientCertificate() != null) {
            synchronized (subscriptionLock(subscription.getId())) {
                subscriptionTrustStoreLoaderManager.registerSubscription(subscription, extractApiServersId(subscription));
                updateSubscriptionIdById(subscription);
                updateIdentityCache(subscription, cacheByClientCertificate);
            }
        } else if (subscription.getClientId() != null) {
            updateSubscriptionIdById(subscription);
            updateIdentityCache(subscription, cacheByApiClientId);
        } else {
            updateSubscriptionIdById(subscription);
            updateCacheKeyByApiId(subscription.getApi(), subscription.getId());
        }
    }

    @Override
    public void unregister(final Subscription candidate) {
        synchronized (subscriptionLock(candidate.getId())) {
            unregisterInternal(candidate);
        }
    }

    private void unregisterInternal(final Subscription candidate) {
        // The caller holds subscriptionLock(id), which serializes mutations for this subscription.
        // compute() atomically publishes the resulting representation to lock-free readers.
        cacheBySubscriptionIdAll.compute(candidate.getId(), (id, allSubscriptions) -> {
            if (allSubscriptions == null) return null;

            var toEvict = allSubscriptions
                .values()
                .stream()
                .filter(s -> Objects.equals(candidate.getApi(), s.getApi()))
                .toList();

            if (toEvict.isEmpty()) {
                return allSubscriptions;
            }

            toEvict.forEach(existing -> {
                unregisterFromClientId(existing);
                unregisterFromClientCertificate(existing);
            });

            evictKeyForApi(candidate.getApi(), candidate.getId());
            // Handle the case where the candidate carries a different clientId/cert than
            // what was cached (e.g. a status-change event arriving after a credential update).
            if (toEvict.stream().noneMatch(s -> Objects.equals(s.getClientId(), candidate.getClientId()))) {
                unregisterFromClientId(candidate);
            }
            if (toEvict.stream().noneMatch(s -> Objects.equals(s.getClientCertificate(), candidate.getClientCertificate()))) {
                unregisterFromClientCertificate(candidate);
            }

            return removeApiLegs(allSubscriptions, candidate.getApi());
        });
    }

    @Override
    public void unregisterByApiId(final String apiId) {
        log.debug("Unregistering all subscriptions for API [{}]", apiId);
        Set<String> subscriptionsByApi = cacheKeysByApiId.get(apiId);
        if (subscriptionsByApi == null) {
            return;
        }
        // Snapshot the ids, but leave the live API index in place. Each evicted leg removes its
        // own index entry atomically below. Removing the whole bucket first would race with a new
        // registration: it could create a replacement bucket before its by-id leg was evicted,
        // leaving a stale API index entry behind.
        List.copyOf(subscriptionsByApi).forEach(subscriptionId -> {
            synchronized (subscriptionLock(subscriptionId)) {
                List<Subscription> evictedLegs = new ArrayList<>();
                cacheBySubscriptionIdAll.computeIfPresent(subscriptionId, (id, all) -> {
                    all
                        .values()
                        .stream()
                        .filter(subscription -> Objects.equals(apiId, subscription.getApi()))
                        .forEach(evictedLegs::add);
                    return removeApiLegs(all, apiId);
                });
                // Keep secondary indexes consistent with the by-id index before allowing another
                // registration for the same subscription id.
                evictedLegs.forEach(leg -> {
                    unregisterFromClientId(leg);
                    unregisterFromClientCertificate(leg);
                    evictKeyForApi(leg.getApi(), leg.getId());
                });
            }
        });
    }

    private void registerFromId(final Subscription subscription) {
        Subscription cached = cachedSameApiLeg(subscription);
        updateSubscriptionIdById(subscription);
        updateCacheKeyByApiId(subscription.getApi(), subscription.getId());
        // The new registration carries no credentials: any identity keys derived from the
        // replaced leg's clientId/certificate are stale and would otherwise never be evicted.
        if (cached != null) {
            unregisterFromClientId(cached);
            unregisterFromClientCertificate(cached);
        }
    }

    /**
     * Returns the cached leg of the same subscription for the same API, or null. Exploded
     * API-Product subscriptions may carry sibling legs for other APIs, which must not be
     * evicted when one API's leg is replaced.
     */
    private Subscription cachedSameApiLeg(final Subscription subscription) {
        SubscriptionLegs existingLegs = cacheBySubscriptionIdAll.get(subscription.getId());
        if (existingLegs == null) {
            return null;
        }
        if (existingLegs instanceof SingleSubscriptionLeg single) {
            return Objects.equals(subscription.getApi(), single.subscription().getApi()) ? single.subscription() : null;
        }

        MultipleSubscriptionLegs multiple = (MultipleSubscriptionLegs) existingLegs;
        Subscription exact = multiple.subscriptions.get(legKey(subscription));
        if (exact != null) {
            return exact;
        }
        for (Subscription existing : multiple.values()) {
            if (Objects.equals(subscription.getApi(), existing.getApi())) {
                return existing;
            }
        }
        return null;
    }

    private void registerFromClientCertificate(final Subscription subscription) {
        final Set<String> servers = extractApiServersId(subscription);

        // Ensure trust store is updated before cache to maintain certificate validation consistency
        subscriptionTrustStoreLoaderManager.registerSubscription(subscription, servers);

        // keep the old same-API leg before the cache is updated
        Subscription cached = cachedSameApiLeg(subscription);

        // Update cache
        updateSubscriptionIdById(subscription);
        updateIdentityCache(subscription, cacheByClientCertificate);

        // Evict if cert bundle, clientId or plan changed (e.g. subscription transfer): the keys
        // derived from the old leg differ from the ones just registered
        if (
            cached != null &&
            (!Objects.equals(subscription.getClientCertificate(), cached.getClientCertificate()) ||
                !Objects.equals(subscription.getClientId(), cached.getClientId()) ||
                !Objects.equals(subscription.getPlan(), cached.getPlan()))
        ) {
            cacheByClientCertificate.remove(identityCacheKey(cached));
            cacheByClientCertificate.remove(identityCacheKeyWithoutPlan(cached));
        }
        // Credential-type switch: if the old leg was clientId-registered, its entries live in
        // the clientId map, which this certificate registration never overwrites.
        if (cached != null) {
            unregisterFromClientId(cached);
        }
    }

    private void registerFromClientId(final Subscription subscription) {
        // Only the same-API leg can be replaced. Looking it up directly keeps registration O(1)
        // for API-Product subscriptions instead of snapshotting every sibling leg.
        Subscription cached = cachedSameApiLeg(subscription);

        updateSubscriptionIdById(subscription);

        // Register new subscription first
        updateIdentityCache(subscription, cacheByApiClientId);

        // Then clean up the old leg if clientId or plan changed (e.g. subscription transfer)
        if (cached != null) {
            if (
                (cached.getClientId() != null && !cached.getClientId().equals(subscription.getClientId())) ||
                (cached.getPlan() != null && !cached.getPlan().equals(subscription.getPlan()))
            ) {
                unregisterFromClientId(cached);
            }
            // Credential-type switch: if the old leg was certificate-registered, its entries
            // live in the certificate map and trust store, never overwritten by this
            // clientId registration.
            unregisterFromClientCertificate(cached);
        }
    }

    private void updateIdentityCache(Subscription subscription, ConcurrentMap<IdentityKey, Subscription> cache) {
        updateCacheKeyByApiId(subscription.getApi(), subscription.getId());
        cache.put(identityCacheKey(subscription), subscription);
        // Index the subscription without plan id to allow search without plan criteria.
        cache.put(identityCacheKeyWithoutPlan(subscription), subscription);
    }

    private void updateCacheKeyByApiId(final String apiId, final String subscriptionId) {
        // compute() so concurrent register/unregister calls for the same API (sync appenders run
        // in parallel) cannot lose updates or mutate a plain HashSet across threads.
        cacheKeysByApiId.compute(apiId, (id, subscriptionsByApi) -> {
            Set<String> keys = subscriptionsByApi != null ? subscriptionsByApi : ConcurrentHashMap.newKeySet();
            keys.add(subscriptionId);
            return keys;
        });
    }

    private void updateSubscriptionIdById(Subscription subscription) {
        cacheBySubscriptionIdAll.compute(subscription.getId(), (id, existing) -> {
            if (existing == null) {
                return new SingleSubscriptionLeg(subscription);
            }
            if (existing instanceof SingleSubscriptionLeg single) {
                if (legKey(single.subscription()).equals(legKey(subscription))) {
                    return new SingleSubscriptionLeg(subscription);
                }
                return new MultipleSubscriptionLegs(single.subscription(), subscription);
            }
            MultipleSubscriptionLegs multiple = (MultipleSubscriptionLegs) existing;
            multiple.subscriptions.put(legKey(subscription), subscription);
            return multiple;
        });
    }

    private static SubscriptionLegs removeApiLegs(SubscriptionLegs existing, String apiId) {
        if (existing instanceof SingleSubscriptionLeg single) {
            return Objects.equals(apiId, single.subscription().getApi()) ? null : single;
        }

        MultipleSubscriptionLegs multiple = (MultipleSubscriptionLegs) existing;
        multiple.subscriptions.entrySet().removeIf(entry -> Objects.equals(apiId, entry.getKey().api()));
        if (multiple.subscriptions.isEmpty()) {
            return null;
        }
        if (multiple.subscriptions.size() == 1) {
            return new SingleSubscriptionLeg(multiple.subscriptions.values().iterator().next());
        }
        return multiple;
    }

    private static Object subscriptionLock(String subscriptionId) {
        int hash = subscriptionId.hashCode();
        hash ^= hash >>> 16;
        return SUBSCRIPTION_LOCKS[hash & (SUBSCRIPTION_LOCK_STRIPES - 1)];
    }

    private void unregisterFromClientId(final Subscription subscription) {
        if (subscription.getClientId() != null) {
            evictIdentityCache(subscription, cacheByApiClientId);
        }
    }

    private void unregisterFromClientCertificate(final Subscription subscription) {
        if (subscription.getClientCertificate() != null) {
            subscriptionTrustStoreLoaderManager.unregisterSubscription(subscription);
            evictIdentityCache(subscription, cacheByClientCertificate);
        }
    }

    private void evictIdentityCache(Subscription subscription, ConcurrentMap<IdentityKey, Subscription> cache) {
        cache.remove(identityCacheKey(subscription));
        cache.remove(identityCacheKeyWithoutPlan(subscription));
    }

    private void evictKeyForApi(final String apiId, final String subscriptionId) {
        cacheKeysByApiId.computeIfPresent(apiId, (id, keysByApi) -> {
            keysByApi.remove(subscriptionId);
            return keysByApi.isEmpty() ? null : keysByApi;
        });
    }

    // visible for testing
    Optional<Subscription> getByClientCertificate(final Subscription subscription) {
        if (subscription.getPlan() != null) {
            return Optional.ofNullable(cacheByClientCertificate.get(identityCacheKey(subscription)));
        } else {
            return Optional.ofNullable(cacheByClientCertificate.get(identityCacheKeyWithoutPlan(subscription)));
        }
    }

    // Visible for testing
    Optional<Subscription> getByApiAndClientId(String api, String clientId) {
        return Optional.ofNullable(cacheByApiClientId.get(cacheKey(api, clientId)));
    }

    // Visible for testing
    Set<String> getByApiId(String apiId) {
        return cacheKeysByApiId.getOrDefault(apiId, Collections.emptySet());
    }

    private Optional<Subscription> getByApiAndId(String api, String subscriptionId) {
        SubscriptionLegs subscriptions = cacheBySubscriptionIdAll.get(subscriptionId);
        if (subscriptions == null) {
            // Fallback to old cache for backward compatibility
            return getById(subscriptionId);
        }
        // Plain loop: this runs on the request hot path for every API-key call and the index is
        // almost always a singleton, so avoid allocating a Stream pipeline per request.
        for (Subscription subscription : subscriptions.values()) {
            if (Objects.equals(api, subscription.getApi())) {
                return Optional.of(subscription);
            }
        }
        return Optional.empty();
    }

    private IdentityKey identityCacheKey(Subscription subscription) {
        if (subscription.getClientId() != null) {
            Objects.requireNonNull(subscription.getClientId(), "Client ID must not be null");
            return cacheKey(subscription.getApi(), subscription.getPlan(), subscription.getClientId());
        } else {
            Objects.requireNonNull(subscription.getClientCertificate(), "Client certificate must not be null");
            return cacheKey(subscription.getApi(), subscription.getPlan(), sha256(subscription.getClientCertificate()));
        }
    }

    private IdentityKey identityCacheKeyWithoutPlan(Subscription subscription) {
        if (subscription.getClientId() != null) {
            Objects.requireNonNull(subscription.getClientId(), "Client ID must not be null");
            return cacheKey(subscription.getApi(), subscription.getClientId());
        } else {
            Objects.requireNonNull(subscription.getClientCertificate(), "Client certificate must not be null");
            return cacheKey(subscription.getApi(), sha256(subscription.getClientCertificate()));
        }
    }

    @SneakyThrows
    private String sha256(String toProcess) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(toProcess.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private IdentityKey cacheKey(String api, String plan, String clientIdentity) {
        Objects.requireNonNull(plan, "Plan must not be null");
        return new IdentityKey(api, plan, clientIdentity);
    }

    private IdentityKey cacheKey(String api, String clientIdentity) {
        return new IdentityKey(api, null, clientIdentity);
    }

    private Set<String> extractApiServersId(Subscription subscription) {
        final ReactableApi<?> reactableApi = apiManager.get(subscription.getApi());
        final Set<String> servers;
        if (reactableApi instanceof Api api) {
            servers = api
                .getDefinition()
                .getListeners()
                .stream()
                .flatMap(l -> l.getServers() != null ? l.getServers().stream() : Stream.empty())
                .collect(Collectors.toSet());
        } else {
            servers = Set.of();
        }
        return servers;
    }
}
