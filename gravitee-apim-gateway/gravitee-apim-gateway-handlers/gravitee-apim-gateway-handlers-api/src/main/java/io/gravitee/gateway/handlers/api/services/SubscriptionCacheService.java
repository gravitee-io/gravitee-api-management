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
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    // Caches only contains active subscriptions
    private final Map<String, Subscription> cacheByApiClientId = new ConcurrentHashMap<>();
    private final Map<String, Subscription> cacheByClientCertificate = new ConcurrentHashMap<>();
    private final Map<String, Subscription> cacheBySubscriptionId = new ConcurrentHashMap<>();
    private final Map<String, Set<Subscription>> cacheBySubscriptionIdAll = new ConcurrentHashMap<>(); // exploded subscriptions
    private final Map<String, Set<String>> cacheKeysByApiId = new ConcurrentHashMap<>();
    private final Map<Integer, Subscription> processed = new ConcurrentHashMap<>();

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
        return Optional.ofNullable(cacheBySubscriptionId.get(subscriptionId));
    }

    /**
     * Returns all subscriptions for the given ID (multiple for exploded API Product subscriptions).
     */
    public Collection<Subscription> getAllById(String subscriptionId) {
        Set<Subscription> subscriptions = cacheBySubscriptionIdAll.get(subscriptionId);
        return subscriptions != null ? Set.copyOf(subscriptions) : Collections.emptySet();
    }

    @Override
    public void register(final Subscription subscription) {
        // only once per synchronization window
        // take all fields (including "updatedAt" in metadata) into account
        if (ACCEPTED.name().equals(subscription.getStatus())) {
            processed.computeIfAbsent(subscription.hashCode(), h -> {
                if (subscription.getClientCertificate() != null) {
                    registerFromClientCertificate(subscription);
                } else if (subscription.getClientId() != null) {
                    registerFromClientId(subscription);
                } else {
                    registerFromId(subscription);
                }
                return subscription;
            });
        } else {
            unregister(subscription);
        }
    }

    @Override
    public void unregister(final Subscription candidate) {
        // only once per synchronization window
        // take all fields (including "updatedAt" in metadata) into account
        processed.computeIfPresent(candidate.hashCode(), (h, existing) -> {
            Subscription removeSubscription = cacheBySubscriptionId.remove(existing.getId());
            // Remove from exploded subscriptions cache
            Set<Subscription> allSubscriptions = cacheBySubscriptionIdAll.get(existing.getId());
            if (allSubscriptions != null) {
                allSubscriptions.removeIf(s -> Objects.equals(existing.getApi(), s.getApi()));
                if (allSubscriptions.isEmpty()) {
                    cacheBySubscriptionIdAll.remove(existing.getId());
                }
            }
            if (removeSubscription != null) {
                log.warn("Found a subscription to remove {} ", existing.getId());
                evictKeyForApi(existing.getApi(), existing.getId());
                unregisterFromClientId(removeSubscription);
                unregisterFromClientCertificate(removeSubscription);
            }

            // In case new ones have different client id than the one in cache
            if (!Objects.equals(candidate.getClientId(), existing.getClientId())) {
                unregisterFromClientId(existing);
            }
            if (!Objects.equals(candidate.getClientCertificate(), existing.getClientCertificate())) {
                unregisterFromClientCertificate(existing);
            }

            // remove from processed
            return null;
        });
    }

    @Override
    public void unregisterByApiId(final String apiId) {
        cacheKeysByApiId.computeIfPresent(apiId, (k, subscriptionsByApi) -> {
            subscriptionsByApi.forEach(cacheKey -> {
                Set<Subscription> all = cacheBySubscriptionIdAll.get(cacheKey);
                if (all != null) {
                    all.removeIf(s -> Objects.equals(apiId, s.getApi()));
                    if (all.isEmpty()) cacheBySubscriptionIdAll.remove(cacheKey);
                }
                cacheBySubscriptionId.remove(cacheKey);
                cacheByApiClientId.remove(cacheKey);
            });
            return null;
        });
    }

    private void registerFromId(final Subscription subscription) {
        String cacheKey = subscription.getId();
        updateSubscriptionIdById(subscription);
        updateCacheKeyByApiId(subscription.getApi(), cacheKey);
    }

    private void registerFromClientCertificate(final Subscription subscription) {
        final Set<String> servers = extractApiServersId(subscription);

        // register new certs and remove old one
        subscriptionTrustStoreLoaderManager.registerSubscription(subscription, servers);

        // keep the old one
        Subscription cached = cacheBySubscriptionId.get(subscription.getId());

        // Update cache
        updateSubscriptionIdById(subscription);
        updateIdentityCache(subscription, cacheByClientCertificate);

        // Evict if cert bundle changed
        if (
            cached != null &&
            Objects.equals(subscription.getApi(), cached.getApi()) &&
            !Objects.equals(subscription.getClientCertificate(), cached.getClientCertificate())
        ) {
            evictKeyForApi(cached.getApi(), identityCacheKey(cached));
            evictKeyForApi(cached.getApi(), identityCacheKeyWithoutPlan(cached));
        }
    }

    private void registerFromClientId(final Subscription subscription) {
        // Snapshot old entries before registering
        Set<Subscription> cachedAll = cacheBySubscriptionIdAll.get(subscription.getId());
        Set<Subscription> oldEntries = cachedAll != null ? Set.copyOf(cachedAll) : Set.of();

        updateSubscriptionIdById(subscription);

        // Register new subscription first
        updateIdentityCache(subscription, cacheByApiClientId);

        // Then clean up old clientId entries if clientId changed
        for (Subscription old : oldEntries) {
            if (old.getClientId() != null && !old.getClientId().equals(subscription.getClientId())) {
                unregisterFromClientId(old);
            }
        }
    }

    private void updateIdentityCache(Subscription subscription, Map<String, Subscription> cache) {
        updateCacheKeyByApiId(subscription.getApi(), subscription.getId());
        updateCacheKeyByApiId(subscription.getApi(), identityCacheKey(subscription));
        cache.put(identityCacheKey(subscription), subscription);
        // Index the subscription without plan id to allow search without plan criteria.
        cache.put(identityCacheKeyWithoutPlan(subscription), subscription);
        updateCacheKeyByApiId(subscription.getApi(), identityCacheKeyWithoutPlan(subscription));
    }

    private void updateCacheKeyByApiId(final String apiId, final String cacheKey) {
        Set<String> subscriptionsByApi = cacheKeysByApiId.get(apiId);
        if (subscriptionsByApi == null) {
            subscriptionsByApi = new HashSet<>();
        }
        subscriptionsByApi.add(cacheKey);
        cacheKeysByApiId.put(apiId, subscriptionsByApi);
    }

    private void updateSubscriptionIdById(Subscription subscription) {
        cacheBySubscriptionId.put(subscription.getId(), subscription);
        cacheBySubscriptionIdAll.computeIfAbsent(subscription.getId(), k -> ConcurrentHashMap.newKeySet()).add(subscription);
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

    private void evictIdentityCache(Subscription subscription, Map<String, Subscription> cacheByApiClientId) {
        final String clientIdKey = identityCacheKey(subscription);
        if (cacheByApiClientId.remove(clientIdKey) != null) {
            evictKeyForApi(subscription.getApi(), clientIdKey);
        }
        final String clientIdKeyWithoutPlan = identityCacheKeyWithoutPlan(subscription);
        if (cacheByApiClientId.remove(clientIdKeyWithoutPlan) != null) {
            evictKeyForApi(subscription.getApi(), clientIdKeyWithoutPlan);
        }
    }

    private void evictKeyForApi(final String apiId, final String cacheKey) {
        Set<String> keysByApi = cacheKeysByApiId.get(apiId);
        if (keysByApi != null && keysByApi.remove(cacheKey)) {
            if (keysByApi.isEmpty()) {
                cacheKeysByApiId.remove(apiId);
            } else {
                cacheKeysByApiId.put(apiId, keysByApi);
            }
        }
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
        Set<Subscription> subscriptions = cacheBySubscriptionIdAll.get(subscriptionId);
        if (subscriptions == null || subscriptions.isEmpty()) {
            // Fallback to old cache for backward compatibility
            return getById(subscriptionId);
        }
        return subscriptions
            .stream()
            .filter(sub -> Objects.equals(api, sub.getApi()))
            .findFirst();
    }

    private String identityCacheKey(Subscription subscription) {
        if (subscription.getClientId() != null) {
            Objects.requireNonNull(subscription.getClientId(), "Client ID must not be null");
            return cacheKey(subscription.getApi(), subscription.getPlan(), subscription.getClientId());
        } else {
            Objects.requireNonNull(subscription.getClientCertificate(), "Client certificate must not be null");
            return cacheKey(subscription.getApi(), subscription.getPlan(), sha256(subscription.getClientCertificate()));
        }
    }

    private String identityCacheKeyWithoutPlan(Subscription subscription) {
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

    private String cacheKey(String api, String plan, String clientIdentity) {
        Objects.requireNonNull(plan, "Plan must not be null");
        return String.format("%s.%s.%s", api, plan, clientIdentity);
    }

    private String cacheKey(String api, String clientIdentity) {
        return String.format("%s.%s", api, clientIdentity);
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
