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
import io.gravitee.gateway.core.subscription.SubscriptionScope;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.security.core.SubscriptionTrustStoreLoaderManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

/**
 * Runtime cache of active subscriptions.
 *
 * <h2>Scope</h2>
 *
 * A subscription is cached once, under the entity it was taken on — its <em>scope</em>: the API for
 * a regular subscription, the API Product for an API Product subscription. It is never duplicated
 * per member API.
 *
 * <p>The {@link SubscriptionService} contract is API-scoped, so lookups arrive with an API and this
 * class resolves the scopes to probe: the API itself, then the products that API belongs to
 * ({@link ApiProductRegistry#getApiProductIdsForApi(String)}). That reverse index is bounded by
 * (APIs x products) — a few thousand entries — where caching one subscription per member API grows
 * with (subscriptions x member APIs) and reaches millions of entries on large products.</p>
 *
 * <p>The request path already knows about products: {@code HttpSecurityChain} builds a product
 * plan's security plan from the same registry. Flattening product subscriptions onto member APIs
 * therefore bought nothing at read time; it only existed to fit index keys designed before API
 * Products existed.</p>
 */
@CustomLog
@RequiredArgsConstructor
public class SubscriptionCacheService implements SubscriptionService {

    private final ApiKeyService apiKeyService;
    private final SubscriptionTrustStoreLoaderManager subscriptionTrustStoreLoaderManager;
    private final ApiManager apiManager;
    private final ApiProductRegistry apiProductRegistry;

    // Caches only contain active subscriptions.
    // Must stay backed by ConcurrentHashMap: the compute() lambdas below have side effects and
    // rely on CHM running the remapping function at most once, under the bin lock. The
    // ConcurrentMap contract alone allows implementations that retry the lambda (e.g.
    // ConcurrentSkipListMap), which would double those side effects under contention.
    private final ConcurrentMap<IdentityKey, Subscription> cacheByClientId = new ConcurrentHashMap<>();
    private final ConcurrentMap<IdentityKey, Subscription> cacheByClientCertificate = new ConcurrentHashMap<>();
    // One entry per subscription: the by-id index is the single point where a registration is
    // published, so every other index is maintained under its bin lock and cannot drift from it.
    private final ConcurrentMap<String, Subscription> cacheById = new ConcurrentHashMap<>();
    // Subscription ids by scope, for bulk eviction when an API or an API Product is undeployed.
    // Identity keys are not tracked here: they are re-derived from the cached subscriptions.
    private final ConcurrentMap<String, Set<String>> cacheIdsByScope = new ConcurrentHashMap<>();

    /**
     * Identity-cache key referencing the subscription's own field strings instead of a formatted
     * composite String: no per-key byte[] allocation and no String.format on the request hot path.
     * A null plan is the plan-less variant.
     */
    private record IdentityKey(String scope, String plan, String clientIdentity) {}

    private static String scopeOf(final Subscription subscription) {
        return SubscriptionScope.of(subscription);
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
            case CERTIFICATE -> getByApiAndCertificate(api, plan, securityToken.getTokenValue());
            default -> Optional.empty();
        };
    }

    @Override
    public Optional<Subscription> getByApiAndClientIdAndPlan(String api, String clientId, String plan) {
        Subscription direct = cacheByClientId.get(cacheKey(api, plan, clientId));
        if (direct != null) {
            return Optional.of(direct);
        }
        // Miss on the API's own scope: the subscription may have been taken on a product this API
        // belongs to. Most APIs belong to no product, in which case this loop does not run.
        for (String productId : apiProductRegistry.getApiProductIdsForApi(api)) {
            Subscription fromProduct = cacheByClientId.get(cacheKey(productId, plan, clientId));
            if (fromProduct != null) {
                return Optional.of(fromProduct);
            }
        }
        return Optional.empty();
    }

    private Optional<Subscription> getByApiAndCertificate(String api, String plan, String certificateFingerprint) {
        Optional<Subscription> direct = subscriptionTrustStoreLoaderManager.getByCertificate(api, plan, certificateFingerprint);
        if (direct.isPresent()) {
            return direct;
        }
        for (String productId : apiProductRegistry.getApiProductIdsForApi(api)) {
            Optional<Subscription> fromProduct = subscriptionTrustStoreLoaderManager.getByCertificate(
                productId,
                plan,
                certificateFingerprint
            );
            if (fromProduct.isPresent()) {
                return fromProduct;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Subscription> getById(String subscriptionId) {
        return Optional.ofNullable(cacheById.get(subscriptionId));
    }

    @Override
    public void register(final Subscription subscription) {
        // only once per synchronization window
        // take all fields (including "updatedAt" in metadata) into account
        if (ACCEPTED.name().equals(subscription.getStatus())) {
            if (subscription.getClientCertificate() != null) {
                log.debug(
                    "Registering subscription [{}] for scope [{}] by client certificate",
                    subscription.getId(),
                    scopeOf(subscription)
                );
                registerFromClientCertificate(subscription);
            } else if (subscription.getClientId() != null) {
                log.debug(
                    "Registering subscription [{}] for scope [{}] by clientId [{}]",
                    subscription.getId(),
                    scopeOf(subscription),
                    subscription.getClientId()
                );
                publish(subscription, cacheByClientId);
            } else {
                log.debug("Registering subscription [{}] for scope [{}] by ID", subscription.getId(), scopeOf(subscription));
                publish(subscription, null);
            }
        } else {
            log.debug(
                "Unregistering subscription [{}] for scope [{}] with status [{}]",
                subscription.getId(),
                scopeOf(subscription),
                subscription.getStatus()
            );
            unregister(subscription);
        }
    }

    private void registerFromClientCertificate(final Subscription subscription) {
        // Ensure trust store is updated before cache to maintain certificate validation consistency.
        // The trust store is the one place that still needs the member APIs: a certificate has to be
        // trusted on the listeners of every API the product exposes.
        subscriptionTrustStoreLoaderManager.registerSubscription(subscription, extractScopeServerIds(subscription));
        publish(subscription, cacheByClientCertificate);
    }

    /**
     * Publishes a registration and reconciles every index under the by-id bin lock.
     *
     * <p>Because a subscription now has exactly one cache entry, replacing it is a plain swap: the
     * previous entry's identity keys are evicted when they differ from the ones just written, which
     * covers a changed clientId or plan (subscription transfer), a credential-type switch, and a
     * scope change. The old per-member-API bookkeeping — find the sibling leg for this API, keep the
     * others — is gone with the legs it existed for.</p>
     *
     * @param identityCache the identity index this credential type belongs to, null when the
     *                      subscription carries no credential and is only reachable by id
     */
    private void publish(final Subscription subscription, final ConcurrentMap<IdentityKey, Subscription> identityCache) {
        cacheById.compute(subscription.getId(), (id, previous) -> {
            if (identityCache != null) {
                identityCache.put(identityCacheKey(subscription), subscription);
                // Index the subscription without plan id to allow search without plan criteria.
                identityCache.put(identityCacheKeyWithoutPlan(subscription), subscription);
            }
            updateCacheIdsByScope(scopeOf(subscription), id);

            if (previous != null) {
                evictStaleIdentityKeys(previous, subscription);
                if (!Objects.equals(scopeOf(previous), scopeOf(subscription))) {
                    evictIdFromScope(scopeOf(previous), id);
                }
            }
            return subscription;
        });
    }

    /**
     * Evicts the identity keys of the replaced subscription that the new one does not overwrite.
     * A key identical to one just written is left alone — removing it would undo the registration.
     */
    private void evictStaleIdentityKeys(final Subscription previous, final Subscription current) {
        if (previous.getClientId() != null) {
            removeIfStale(cacheByClientId, previous, current);
        }
        if (previous.getClientCertificate() != null) {
            if (current.getClientCertificate() == null) {
                // Credential-type switch: registerSubscription() was not called for the new state,
                // so the trust store still holds the replaced certificate.
                subscriptionTrustStoreLoaderManager.unregisterSubscription(previous);
            }
            removeIfStale(cacheByClientCertificate, previous, current);
        }
    }

    private void removeIfStale(
        final ConcurrentMap<IdentityKey, Subscription> cache,
        final Subscription previous,
        final Subscription current
    ) {
        boolean sameCredentialType =
            (previous.getClientId() != null) == (current.getClientId() != null) &&
            (previous.getClientCertificate() != null) == (current.getClientCertificate() != null);

        IdentityKey previousKey = identityCacheKey(previous);
        IdentityKey previousKeyWithoutPlan = identityCacheKeyWithoutPlan(previous);
        if (!sameCredentialType || !previousKey.equals(identityCacheKey(current))) {
            cache.remove(previousKey);
        }
        if (!sameCredentialType || !previousKeyWithoutPlan.equals(identityCacheKeyWithoutPlan(current))) {
            cache.remove(previousKeyWithoutPlan);
        }
    }

    @Override
    public void unregister(final Subscription candidate) {
        // compute() so the check-then-remove is atomic with respect to a concurrent register() on
        // the same subscription id.
        cacheById.compute(candidate.getId(), (id, cached) -> {
            if (cached == null) {
                return null;
            }
            evictEverywhere(cached);
            // The candidate may carry different credentials than what was cached (e.g. a status
            // change arriving after a credential update): evict those keys too.
            if (
                !Objects.equals(cached.getClientId(), candidate.getClientId()) ||
                !Objects.equals(cached.getClientCertificate(), candidate.getClientCertificate())
            ) {
                evictEverywhere(candidate);
            }
            evictIdFromScope(scopeOf(cached), id);
            return null;
        });
    }

    @Override
    public void unregisterByApiId(final String apiId) {
        log.debug("Unregistering all subscriptions for API [{}]", apiId);
        unregisterByScopeId(apiId);
    }

    /**
     * Unregisters every subscription taken on the given API Product.
     *
     * <p>Undeploying a single member API must not drop a product subscription — the product's other
     * APIs still serve it — so {@link #unregisterByApiId(String)} no longer reaches it. This is the
     * matching entry point, driven by the API Product undeployment.</p>
     */
    public void unregisterByApiProductId(final String apiProductId) {
        log.debug("Unregistering all subscriptions for API Product [{}]", apiProductId);
        unregisterByScopeId(apiProductId);
    }

    private void unregisterByScopeId(final String scopeId) {
        Set<String> subscriptionIds = cacheIdsByScope.remove(scopeId);
        if (subscriptionIds == null) {
            return;
        }
        subscriptionIds.forEach(subscriptionId ->
            cacheById.computeIfPresent(subscriptionId, (id, cached) -> {
                if (!Objects.equals(scopeId, scopeOf(cached))) {
                    // Re-registered under another scope in the meantime: leave it alone.
                    return cached;
                }
                evictEverywhere(cached);
                return null;
            })
        );
    }

    private void evictEverywhere(final Subscription subscription) {
        if (subscription.getClientId() != null) {
            evictIdentityCache(subscription, cacheByClientId);
        }
        if (subscription.getClientCertificate() != null) {
            subscriptionTrustStoreLoaderManager.unregisterSubscription(subscription);
            evictIdentityCache(subscription, cacheByClientCertificate);
        }
    }

    private void updateCacheIdsByScope(final String scopeId, final String subscriptionId) {
        // compute() so concurrent register/unregister calls for the same scope (sync appenders run
        // in parallel) cannot lose updates or mutate a plain HashSet across threads.
        cacheIdsByScope.compute(scopeId, (id, subscriptionIds) -> {
            Set<String> ids = subscriptionIds != null ? subscriptionIds : ConcurrentHashMap.newKeySet();
            ids.add(subscriptionId);
            return ids;
        });
    }

    private void evictIdFromScope(final String scopeId, final String subscriptionId) {
        cacheIdsByScope.computeIfPresent(scopeId, (id, subscriptionIds) -> {
            subscriptionIds.remove(subscriptionId);
            return subscriptionIds.isEmpty() ? null : subscriptionIds;
        });
    }

    private void evictIdentityCache(Subscription subscription, ConcurrentMap<IdentityKey, Subscription> cache) {
        cache.remove(identityCacheKey(subscription));
        cache.remove(identityCacheKeyWithoutPlan(subscription));
    }

    // visible for testing
    Optional<Subscription> getByClientCertificate(final Subscription subscription) {
        if (subscription.getPlan() != null) {
            return Optional.ofNullable(cacheByClientCertificate.get(identityCacheKey(subscription)));
        }
        return Optional.ofNullable(cacheByClientCertificate.get(identityCacheKeyWithoutPlan(subscription)));
    }

    // Visible for testing
    Optional<Subscription> getByApiAndClientId(String api, String clientId) {
        return getByApiAndClientIdAndPlan(api, clientId, null);
    }

    // Visible for testing
    Set<String> getByScopeId(String scopeId) {
        return cacheIdsByScope.getOrDefault(scopeId, Collections.emptySet());
    }

    private Optional<Subscription> getByApiAndId(String api, String subscriptionId) {
        Subscription subscription = cacheById.get(subscriptionId);
        if (subscription == null) {
            return Optional.empty();
        }
        return servesApi(subscription, api) ? Optional.of(subscription) : Optional.empty();
    }

    /** Whether the subscription is reachable through the given API — directly, or via its product. */
    private boolean servesApi(final Subscription subscription, final String api) {
        String scope = scopeOf(subscription);
        return Objects.equals(scope, api) || apiProductRegistry.getApiProductIdsForApi(api).contains(scope);
    }

    private IdentityKey identityCacheKey(Subscription subscription) {
        if (subscription.getClientId() != null) {
            return cacheKey(scopeOf(subscription), subscription.getPlan(), subscription.getClientId());
        }
        Objects.requireNonNull(subscription.getClientCertificate(), "Client certificate must not be null");
        return cacheKey(scopeOf(subscription), subscription.getPlan(), sha256(subscription.getClientCertificate()));
    }

    private IdentityKey identityCacheKeyWithoutPlan(Subscription subscription) {
        if (subscription.getClientId() != null) {
            return cacheKey(scopeOf(subscription), null, subscription.getClientId());
        }
        Objects.requireNonNull(subscription.getClientCertificate(), "Client certificate must not be null");
        return cacheKey(scopeOf(subscription), null, sha256(subscription.getClientCertificate()));
    }

    @SneakyThrows
    private String sha256(String toProcess) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(toProcess.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private IdentityKey cacheKey(String scope, String plan, String clientIdentity) {
        return new IdentityKey(scope, plan, clientIdentity);
    }

    /**
     * Server ids the subscription's certificate must be trusted on.
     *
     * <p>A product subscription spans every member API, and TLS trust is held by the listeners, so
     * this is the one place a product still fans out over its APIs. The fan-out is bounded by the
     * product size, not by the number of subscriptions.</p>
     */
    private Set<String> extractScopeServerIds(Subscription subscription) {
        if (subscription.getApiProductId() == null) {
            return serversOf(subscription.getApi());
        }
        ReactableApiProduct product = apiProductRegistry.get(subscription.getApiProductId(), subscription.getEnvironmentId());
        if (product == null || product.getApiIds() == null) {
            return Set.of();
        }
        Set<String> servers = new HashSet<>();
        product.getApiIds().forEach(apiId -> servers.addAll(serversOf(apiId)));
        return servers;
    }

    private Set<String> serversOf(String apiId) {
        final ReactableApi<?> reactableApi = apiManager.get(apiId);
        if (reactableApi instanceof Api api) {
            return api
                .getDefinition()
                .getListeners()
                .stream()
                .flatMap(l -> l.getServers() != null ? l.getServers().stream() : Stream.empty())
                .collect(Collectors.toSet());
        }
        return Set.of();
    }
}
