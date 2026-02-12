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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class SubscriptionCacheService implements SubscriptionService {

    private final ApiKeyService apiKeyService;
    private final SubscriptionTrustStoreLoaderManager subscriptionTrustStoreLoaderManager;
    private final ApiManager apiManager;

    // Caches only contains active subscriptions
    private final Map<String, Subscription> cacheByApiClientId = new ConcurrentHashMap<>();
    private final Map<String, Subscription> cacheByApiClientCertificate = new ConcurrentHashMap<>();
    private final Map<String, Subscription> cacheBySubscriptionId = new ConcurrentHashMap<>();
    private final Map<String, Set<Subscription>> cacheBySubscriptionIdAll = new ConcurrentHashMap<>(); // exploded subscriptions
    private final Map<String, Set<String>> cacheByApiId = new ConcurrentHashMap<>();
    private final Map<String, Object> locksBySubscriptionId = new ConcurrentHashMap<>();

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
            case CERTIFICATE -> subscriptionTrustStoreLoaderManager.getByCertificate(api, securityToken.getTokenValue(), plan);
            default -> Optional.empty();
        };
    }

    @Override
    public Optional<Subscription> getByApiAndClientIdAndPlan(String api, String clientId, String plan) {
        return Optional.ofNullable(cacheByApiClientId.get(buildCacheKeyFromClientInfo(api, clientId, plan)));
    }

    @Override
    public Optional<Subscription> getById(String subscriptionId) {
        return Optional.ofNullable(cacheBySubscriptionId.get(subscriptionId));
    }

    /** Returns all subscriptions for the given ID (multiple for exploded API Product subscriptions). */
    public Collection<Subscription> getAllById(String subscriptionId) {
        Set<Subscription> subscriptions = cacheBySubscriptionIdAll.get(subscriptionId);
        return subscriptions != null ? Set.copyOf(subscriptions) : Collections.emptySet();
    }

    /** Returns subscription for the given API and ID (for exploded subscriptions). */
    public Optional<Subscription> getByApiAndId(String api, String subscriptionId) {
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

    @Override
    public void register(final Subscription subscription) {
        if (ACCEPTED.name().equals(subscription.getStatus())) {
            if (subscription.getClientCertificate() != null) {
                registerFromClientCertificate(subscription);
            } else if (subscription.getClientId() != null) {
                registerFromClientId(subscription);
            } else {
                registerFromId(subscription);
            }
        } else {
            unregister(subscription);
        }
    }

    private void unregisterStaleIfChanged(
        Subscription cached,
        Subscription updated,
        BiPredicate<Subscription, Subscription> hasChanged,
        Consumer<Subscription> unregister
    ) {
        if (cached != null && hasChanged.test(cached, updated)) {
            unregister.accept(cached);
        }
    }

    private Object lockFor(String idKey) {
        return locksBySubscriptionId.computeIfAbsent(idKey, k -> new Object());
    }

    private void registerFromClientCertificate(final Subscription subscription) {
        final String idKey = subscription.getId();
        final String clientCertificateKey = buildClientCertificateCacheKey(subscription);
        // Index the subscription without plan id to allow search without plan criteria.
        final String clientCertificateKeyWithoutPlan = buildCacheKeyFromClientInfo(
            subscription.getApi(),
            subscription.getClientCertificate(),
            null
        );

        synchronized (lockFor(idKey)) {
            Set<Subscription> cachedAll = cacheBySubscriptionIdAll.get(idKey);
            if (cachedAll != null) {
                for (Subscription cached : Set.copyOf(cachedAll)) {
                    unregisterStaleIfChanged(
                        cached,
                        subscription,
                        (c, u) -> c.getClientCertificate() != null && !c.getClientCertificate().equals(u.getClientCertificate()),
                        this::unregister
                    );
                }
            }

            final Set<String> servers = extractApiServersId(subscription);
            subscriptionTrustStoreLoaderManager.registerSubscription(subscription, servers);
            // Update subscription
            cacheBySubscriptionId.put(idKey, subscription);
            cacheBySubscriptionIdAll.computeIfAbsent(idKey, k -> ConcurrentHashMap.newKeySet()).add(subscription);
            addKeyForApi(subscription.getApi(), idKey);
            // Put new client_id
            cacheByApiClientCertificate.put(clientCertificateKey, subscription);
            addKeyForApi(subscription.getApi(), clientCertificateKey);
            cacheByApiClientCertificate.put(clientCertificateKeyWithoutPlan, subscription);
            addKeyForApi(subscription.getApi(), clientCertificateKeyWithoutPlan);
        }
    }

    private void registerFromClientId(final Subscription subscription) {
        final String idKey = subscription.getId();
        final String clientIdKey = buildClientIdCacheKey(subscription);
        // Index the subscription without plan id to allow search without plan criteria.
        final String clientIdKeyWithoutPlan = buildCacheKeyFromClientInfo(subscription.getApi(), subscription.getClientId(), null);

        synchronized (lockFor(idKey)) {
            Set<Subscription> cachedAll = cacheBySubscriptionIdAll.get(idKey);
            if (cachedAll != null) {
                for (Subscription cached : Set.copyOf(cachedAll)) {
                    unregisterStaleIfChanged(
                        cached,
                        subscription,
                        (c, u) -> c.getClientId() != null && !c.getClientId().equals(u.getClientId()),
                        this::unregister
                    );
                }
            }

            // Update subscription
            cacheBySubscriptionId.put(idKey, subscription);
            cacheBySubscriptionIdAll.computeIfAbsent(idKey, k -> ConcurrentHashMap.newKeySet()).add(subscription);
            addKeyForApi(subscription.getApi(), idKey);
            // Put new client_id
            cacheByApiClientId.put(clientIdKey, subscription);
            addKeyForApi(subscription.getApi(), clientIdKey);
            cacheByApiClientId.put(clientIdKeyWithoutPlan, subscription);
            addKeyForApi(subscription.getApi(), clientIdKeyWithoutPlan);
        }
    }

    private void registerFromId(final Subscription subscription) {
        String cacheKey = subscription.getId();
        synchronized (lockFor(cacheKey)) {
            cacheBySubscriptionId.put(cacheKey, subscription);
            cacheBySubscriptionIdAll.computeIfAbsent(cacheKey, k -> ConcurrentHashMap.newKeySet()).add(subscription);
            addKeyForApi(subscription.getApi(), cacheKey);
        }
    }

    private void addKeyForApi(final String apiId, final String cacheKey) {
        Set<String> subscriptionsByApi = cacheByApiId.get(apiId);
        if (subscriptionsByApi == null) {
            subscriptionsByApi = new HashSet<>();
        }
        subscriptionsByApi.add(cacheKey);
        cacheByApiId.put(apiId, subscriptionsByApi);
    }

    private void removeKeyForApi(final String apiId, final String cacheKey) {
        Set<String> keysByApi = cacheByApiId.get(apiId);
        if (keysByApi != null && keysByApi.remove(cacheKey)) {
            if (keysByApi.isEmpty()) {
                cacheByApiId.remove(apiId);
            } else {
                cacheByApiId.put(apiId, keysByApi);
            }
        }
    }

    @Override
    public void unregister(final Subscription subscription) {
        final String idKey = subscription.getId();
        synchronized (lockFor(idKey)) {
            Subscription removeSubscription = cacheBySubscriptionId.remove(idKey);
            // Remove from exploded subscriptions cache
            Set<Subscription> allSubscriptions = cacheBySubscriptionIdAll.get(idKey);
            if (allSubscriptions != null) {
                allSubscriptions.removeIf(s -> Objects.equals(subscription.getApi(), s.getApi()));
                if (allSubscriptions.isEmpty()) {
                    cacheBySubscriptionIdAll.remove(idKey);
                }
            }
            if (removeSubscription != null) {
                removeKeyForApi(subscription.getApi(), idKey);
                unregisterFromClientId(removeSubscription);
                unregisterFromClientCertificate(removeSubscription);
            }
            // In case new one has different client id than the one in cache
            unregisterFromClientId(subscription);
            unregisterFromClientCertificate(subscription);
        }
    }

    private void unregisterFromClientId(final Subscription subscription) {
        if (subscription.getClientId() != null) {
            final String clientIdKey = buildClientIdCacheKey(subscription);
            if (cacheByApiClientId.remove(clientIdKey) != null) {
                removeKeyForApi(subscription.getApi(), clientIdKey);
            }
            final String clientIdKeyWithoutPlan = buildCacheKeyFromClientInfo(subscription.getApi(), subscription.getClientId(), null);
            if (cacheByApiClientId.remove(clientIdKeyWithoutPlan) != null) {
                removeKeyForApi(subscription.getApi(), clientIdKeyWithoutPlan);
            }
        }
    }

    private void unregisterFromClientCertificate(final Subscription subscription) {
        if (subscription.getClientCertificate() != null) {
            final String clientCertificateKey = buildClientCertificateCacheKey(subscription);
            subscriptionTrustStoreLoaderManager.unregisterSubscription(subscription);
            if (cacheByApiClientCertificate.remove(clientCertificateKey) != null) {
                removeKeyForApi(subscription.getApi(), clientCertificateKey);
            }
            final String clientCertificateKeyWithoutPlan = buildCacheKeyFromClientInfo(
                subscription.getApi(),
                subscription.getClientCertificate(),
                null
            );
            if (cacheByApiClientCertificate.remove(clientCertificateKeyWithoutPlan) != null) {
                removeKeyForApi(subscription.getApi(), clientCertificateKeyWithoutPlan);
            }
        }
    }

    @Override
    public void unregisterByApiId(final String apiId) {
        Set<String> subscriptionsByApi = cacheByApiId.remove(apiId);
        if (subscriptionsByApi != null) {
            subscriptionsByApi.forEach(cacheKey -> {
                Set<Subscription> all = cacheBySubscriptionIdAll.get(cacheKey);
                if (all != null) {
                    all.removeIf(s -> Objects.equals(apiId, s.getApi()));
                    if (all.isEmpty()) cacheBySubscriptionIdAll.remove(cacheKey);
                }
                cacheBySubscriptionId.remove(cacheKey);
                cacheByApiClientId.remove(cacheKey);
            });
        }
    }

    String buildClientCertificateCacheKey(Subscription subscription) {
        return buildCacheKeyFromClientInfo(subscription.getApi(), subscription.getClientCertificate(), subscription.getPlan());
    }

    String buildClientIdCacheKey(Subscription subscription) {
        return buildCacheKeyFromClientInfo(subscription.getApi(), subscription.getClientId(), subscription.getPlan());
    }

    String buildCacheKeyFromClientInfo(String api, String clientIdOrCertificate, String plan) {
        return String.format("%s.%s.%s", api, clientIdOrCertificate, plan);
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
