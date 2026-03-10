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
package io.gravitee.gateway.security.core;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.security.core.exception.MalformedCertificateException;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.vertx.server.VertxServer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class SubscriptionTrustStoreLoaderManager {

    private final Map<String, Set<SubscriptionCertificate>> certificates = new HashMap<>();
    private final Map<CacheKey, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final Multimap<CacheKey, SubscriptionTrustStoreLoader> truststoreLoaders = MultimapBuilder.hashKeys().hashSetValues().build();
    private final ServerManager serverManager;

    public SubscriptionTrustStoreLoaderManager(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    /**
     * Register a new subscription. This is called sequentially for each subscription, so no need for concurrent maps here
     * @param subscription the subscription to register
     * @param deployOnServers the servers on which the subscription is deployed
     */
    public void registerSubscription(Subscription subscription, Set<String> deployOnServers) {
        certificates.computeIfPresent(subscription.getId(), (id, currentState) -> {
            Set<SubscriptionCertificate> newState = SubscriptionTrustStoreLoader.readSubscriptionCertificate(subscription);
            // compute diff to minimize disruption: add new certificates before removing old ones to avoid traffic cuts
            Set<SubscriptionCertificate> toAdd = newState
                .stream()
                .filter(newCert -> !currentState.contains(newCert))
                .collect(Collectors.toSet());
            Set<SubscriptionCertificate> toRemove = currentState
                .stream()
                .filter(existing -> !newState.contains(existing))
                .collect(Collectors.toSet());
            Set<SubscriptionCertificate> toUpdate = currentState.stream().filter(newState::contains).collect(Collectors.toSet());

            try {
                // manage truststore loaders
                // register what is new first to avoid traffic cuts
                for (SubscriptionCertificate s : toAdd) {
                    registerSubscriptionTrustStoreLoader(s, deployOnServers);
                }
            } catch (MalformedCertificateException e) {
                log.error("Error while registering certificates", e);
                for (SubscriptionCertificate s : toAdd) {
                    unregisterSubscriptionTrustStoreLoader(s);
                }
                // keep the current state if anything goes wrong
                return currentState;
            }

            toRemove.forEach(this::unregisterSubscriptionTrustStoreLoader);

            toUpdate.forEach(s -> subscriptions.put(new CacheKey(s), subscription));

            // update the entry with what was removed and added
            var state = new HashSet<>(currentState);
            state.addAll(toAdd);
            state.removeAll(toRemove);
            return state;
        });
        certificates.computeIfAbsent(subscription.getId(), id -> {
            try {
                Set<SubscriptionCertificate> newState = SubscriptionTrustStoreLoader.readSubscriptionCertificate(subscription);
                newState.forEach(s -> registerSubscriptionTrustStoreLoader(s, deployOnServers));
                return newState;
            } catch (MalformedCertificateException e) {
                log.error(e.getMessage(), e.getCause());
                return null;
            }
        });
    }

    /**
     * Unregister a subscription. This is called sequentially for each subscription, so no need for concurrent maps here
     * @param subscription the subscription to unregister
     */
    public void unregisterSubscription(Subscription subscription) {
        certificates.computeIfPresent(subscription.getId(), (id, currentState) -> {
            currentState.forEach(this::unregisterSubscriptionTrustStoreLoader);
            return null;
        });
    }

    /**
     * Retrieves a {@link Subscription} based on the provided API, plan, and certificate fingerprint. It is in the critical path of the Gateway, hence the concurrent map is used to ensure fast and secure lookups.
     *
     * @param api the identifier of the API for which the subscription is associated; must not be null.
     * @param plan the identifier of the plan associated with the subscription; must not be null.
     * @param certificateFingerprint the fingerprint of the certificate used to uniquely identify the subscription; must not be null.
     * @return an {@link Optional} containing the matching {@link Subscription} if found, or an empty {@link Optional} if no matching subscription exists.
     */
    public Optional<Subscription> getByCertificate(String api, String plan, String certificateFingerprint) {
        return Optional.ofNullable(subscriptions.get(new CacheKey(api, plan, certificateFingerprint)));
    }

    private void registerSubscriptionTrustStoreLoader(SubscriptionCertificate subscriptionCertificate, Set<String> deployOnServers) {
        serverManager
            .servers()
            .stream()
            .filter(server -> deployOnServers.isEmpty() || deployOnServers.contains(server.id()))
            .map(s -> (VertxServer<?, ?>) s)
            .forEach(server -> {
                SubscriptionTrustStoreLoader loader = new SubscriptionTrustStoreLoader(subscriptionCertificate);
                log.debug(
                    "Registering TrustStoreLoader for subscription {} and certificate {} for server {}",
                    subscriptionCertificate.subscription().getId(),
                    subscriptionCertificate.fingerprint(),
                    server.id()
                );
                server.trustStoreLoaderManager().registerLoader(loader);
                truststoreLoaders.put(new CacheKey(subscriptionCertificate), loader);
            });
        subscriptions.put(new CacheKey(subscriptionCertificate), subscriptionCertificate.subscription());
    }

    private void unregisterSubscriptionTrustStoreLoader(SubscriptionCertificate subscriptionCertificate) {
        log.debug(
            "Stopping TrustStoreLoader for subscription {} and certificate {}",
            subscriptionCertificate.subscription().getId(),
            subscriptionCertificate.fingerprint()
        );
        truststoreLoaders
            .removeAll(new CacheKey(subscriptionCertificate))
            .forEach(loader -> {
                loader.onEvent(new KeyStoreEvent.UnloadEvent(loader.id()));
                loader.stop();
            });
        subscriptions.remove(new CacheKey(subscriptionCertificate));
    }

    record CacheKey(String api, String plan, String fingerPrint) {
        public CacheKey {
            Objects.requireNonNull(api, "API must not be null");
            Objects.requireNonNull(fingerPrint, "Certificate fingerprint must not be null");
        }

        public CacheKey(SubscriptionCertificate subscriptionCertificate) {
            this(
                subscriptionCertificate.subscription().getApi(),
                subscriptionCertificate.subscription().getPlan(),
                subscriptionCertificate.fingerprint()
            );
        }
    }
}
