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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@CustomLog
@RequiredArgsConstructor
public class SubscriptionCacheService implements SubscriptionService {

    private final ApiKeyService apiKeyService;
    private final SubscriptionTrustStoreLoaderManager subscriptionTrustStoreLoaderManager;
    private final ApiManager apiManager;

    // Caches only contains active subscriptions.
    // Must stay backed by ConcurrentHashMap: the compute() lambdas below have side effects and
    // rely on CHM running the remapping function at most once, under the bin lock. The
    // ConcurrentMap contract alone allows implementations that retry the lambda (e.g.
    // ConcurrentSkipListMap), which would double those side effects under contention.
    private final ConcurrentMap<IdentityKey, Subscription> cacheByApiClientId = new ConcurrentHashMap<>();
    private final ConcurrentMap<IdentityKey, Subscription> cacheByClientCertificate = new ConcurrentHashMap<>();
    // Single by-id index, including exploded API-Product subscriptions. Each entry maps a leg's
    // identity (api, environmentId) to its subscription, so registering one leg is an O(1) put.
    // The previous immutable-set representation had to copy the whole set per registration, which
    // made warming up an API Product quadratic in its API count: a product spanning P APIs cost
    // ~P² element copies per subscription, which dominates cold start once P reaches the hundreds.
    // Inner maps are ConcurrentHashMap so the outer compute() lambdas stay side-effect-safe.
    private final ConcurrentMap<String, ConcurrentMap<LegKey, Subscription>> cacheBySubscriptionIdAll = new ConcurrentHashMap<>();
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

    /**
     * Identity of a single leg of a subscription. A plain API subscription has exactly one leg; an
     * exploded API-Product subscription has one per API in the product.
     */
    private record LegKey(String api, String environmentId) {}

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
        ConcurrentMap<LegKey, Subscription> legs = cacheBySubscriptionIdAll.get(subscriptionId);
        if (legs == null) {
            return Optional.empty();
        }
        // hasNext() rather than isEmpty()-then-next(): a concurrent unregister may drain the map
        // between the two calls.
        Iterator<Subscription> iterator = legs.values().iterator();
        return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }

    /**
     * Returns all subscriptions for the given ID (multiple for exploded API Product subscriptions).
     * The returned collection is an unmodifiable, weakly-consistent view of the cached legs.
     */
    public Collection<Subscription> getAllById(String subscriptionId) {
        ConcurrentMap<LegKey, Subscription> legs = cacheBySubscriptionIdAll.get(subscriptionId);
        return legs != null ? Collections.unmodifiableCollection(legs.values()) : Collections.emptySet();
    }

    @Override
    public void register(final Subscription subscription) {
        // only once per synchronization window
        // take all fields (including "updatedAt" in metadata) into account
        if (ACCEPTED.name().equals(subscription.getStatus())) {
            if (subscription.getClientCertificate() != null) {
                log.debug("Registering subscription [{}] for API [{}] by client certificate", subscription.getId(), subscription.getApi());
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
            unregister(subscription);
        }
    }

    @Override
    public void unregister(final Subscription candidate) {
        // Use compute() so that the drain-then-remove is atomic with respect to concurrent
        // register() calls on the same subscription ID. Without this, a register() interleaved
        // between the emptiness check and the removal would add a leg and then have the whole
        // entry orphaned by the removal.
        // Legs are only collected under the bin lock; the identity-map and trust-store eviction
        // (which hashes the client certificate) runs after compute returns, matching
        // unregisterByApiId() and keeping the lock hold short.
        List<Subscription> toEvict = new ArrayList<>();
        cacheBySubscriptionIdAll.compute(candidate.getId(), (id, legs) -> {
            if (legs == null) return null;
            legs
                .entrySet()
                .removeIf(entry -> {
                    if (Objects.equals(candidate.getApi(), entry.getKey().api())) {
                        toEvict.add(entry.getValue());
                        return true;
                    }
                    return false;
                });
            return legs.isEmpty() ? null : legs;
        });

        if (toEvict.isEmpty()) {
            return;
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
    }

    @Override
    public void unregisterByApiId(final String apiId) {
        log.debug("Unregistering all subscriptions for API [{}]", apiId);
        // Remove the whole entry first so no cacheKeysByApiId lock is held while updating the
        // other caches (unregister() acquires the same locks in the opposite order).
        Set<String> subscriptionsByApi = cacheKeysByApiId.remove(apiId);
        if (subscriptionsByApi == null) {
            return;
        }
        // Legs are only collected under the cacheBySubscriptionIdAll bin lock; the identity-map
        // and trust-store eviction (which re-derives keys, hashing the client certificate)
        // happens after each compute returns — those structures are not guarded by this lock.
        List<Subscription> evictedLegs = new ArrayList<>();
        subscriptionsByApi.forEach(subscriptionId ->
            cacheBySubscriptionIdAll.computeIfPresent(subscriptionId, (id, legs) -> {
                legs
                    .entrySet()
                    .removeIf(entry -> {
                        if (Objects.equals(apiId, entry.getKey().api())) {
                            evictedLegs.add(entry.getValue());
                            return true;
                        }
                        return false;
                    });
                return legs.isEmpty() ? null : legs;
            })
        );
        // Same per-leg eviction as unregister(): identity maps and certificate trust store
        evictedLegs.forEach(leg -> {
            unregisterFromClientId(leg);
            unregisterFromClientCertificate(leg);
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
        ConcurrentMap<LegKey, Subscription> legs = cacheBySubscriptionIdAll.get(subscription.getId());
        if (legs == null) {
            return null;
        }
        Subscription exact = legs.get(legKey(subscription));
        if (exact != null) {
            return exact;
        }
        // Fall back to an api-only match so a leg cached under a different environmentId (e.g.
        // registered before the field was populated) is still found and evicted.
        for (Subscription existing : legs.values()) {
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
        // Only the same-API leg is ever acted on below, so look it up directly instead of
        // snapshotting every leg: copying the full set here would have kept registration linear in
        // the product's API count, i.e. quadratic over a full warmup.
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
        // O(1) regardless of how many legs the subscription already has: the leg key is the
        // (api, environmentId) pair the previous implementation matched on, so putting under that
        // key replaces the same leg it used to filter out — without copying the set.
        // compute() rather than computeIfAbsent()+put(): it keeps the inner-map creation and the
        // put atomic against a concurrent unregister() removing the outer entry in between, which
        // would otherwise orphan the leg we just registered.
        cacheBySubscriptionIdAll.compute(subscription.getId(), (id, legs) -> {
            ConcurrentMap<LegKey, Subscription> target = legs != null ? legs : new ConcurrentHashMap<>();
            target.put(legKey(subscription), subscription);
            return target;
        });
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
        ConcurrentMap<LegKey, Subscription> legs = cacheBySubscriptionIdAll.get(subscriptionId);
        if (legs == null || legs.isEmpty()) {
            // Fallback to old cache for backward compatibility
            return getById(subscriptionId);
        }
        // Plain loop: this runs on the request hot path for every API-key call and the map is
        // almost always a singleton, so avoid allocating a Stream pipeline per request.
        for (Subscription subscription : legs.values()) {
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
