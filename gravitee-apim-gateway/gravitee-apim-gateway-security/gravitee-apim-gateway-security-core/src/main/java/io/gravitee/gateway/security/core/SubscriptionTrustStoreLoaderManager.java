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

    private final Map<String, Set<SubscriptionCertificate>> certificates = new ConcurrentHashMap<>();
    private final Map<Key, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final Multimap<Key, SubscriptionTrustStoreLoader> truststoreLoaders = MultimapBuilder.hashKeys().hashSetValues().build();
    private final ServerManager serverManager;

    public SubscriptionTrustStoreLoaderManager(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    public void registerSubscription(Subscription subscription, Set<String> deployOnServers) {
        certificates.computeIfPresent(subscription.getId(), (id, currentState) -> {
            try {
                Set<SubscriptionCertificate> newState = SubscriptionTrustStoreLoader.readSubscriptionCertificate(subscription);
                // separate what to add and what to remove
                Set<SubscriptionCertificate> toAdd = newState
                    .stream()
                    .filter(newCert -> !currentState.contains(newCert))
                    .collect(Collectors.toSet());
                Set<SubscriptionCertificate> toRemove = currentState
                    .stream()
                    .filter(existing -> !newState.contains(existing))
                    .collect(Collectors.toSet());
                Set<SubscriptionCertificate> toUpdate = currentState.stream().filter(newState::contains).collect(Collectors.toSet());

                // manage truststore loaders
                // register what is new first to avoid traffic cuts
                for (SubscriptionCertificate s : toAdd) {
                    registerSubscriptionTrustStoreLoader(s, deployOnServers);
                }

                // remove what is no longer needed
                toRemove.forEach(this::unregisterSubscriptionTrustStoreLoader);

                // update the subscription that may have changed
                toUpdate.forEach(s -> subscriptions.put(new Key(s), subscription));

                // update
                var copy = new HashSet<>(currentState);
                copy.addAll(toAdd);
                copy.removeAll(toRemove);
                return copy;
            } catch (MalformedCertificateException e) {
                log.error(e.getMessage(), e.getCause());
                // keep the current state if anything goes wrong
                return currentState;
            }
        });
        certificates.computeIfAbsent(subscription.getId(), id -> {
            try {
                Set<SubscriptionCertificate> newState = SubscriptionTrustStoreLoader.readSubscriptionCertificate(subscription);
                for (SubscriptionCertificate s : newState) {
                    registerSubscriptionTrustStoreLoader(s, deployOnServers);
                }
                return newState;
            } catch (MalformedCertificateException e) {
                log.error(e.getMessage(), e.getCause());
                return null;
            }
        });
    }

    public void unregisterSubscription(Subscription subscription) {
        certificates.computeIfPresent(subscription.getId(), (id, currentState) -> {
            currentState.forEach(this::unregisterSubscriptionTrustStoreLoader);
            return null;
        });
    }

    public Optional<Subscription> getByCertificate(String api, String plan, String certificateFingerprint) {
        return Optional.ofNullable(subscriptions.get(new Key(api, plan, certificateFingerprint)));
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
                truststoreLoaders.put(new Key(subscriptionCertificate), loader);
            });
        subscriptions.put(new Key(subscriptionCertificate), subscriptionCertificate.subscription());
    }

    private void unregisterSubscriptionTrustStoreLoader(SubscriptionCertificate subscriptionCertificate) {
        log.debug(
            "Stopping TrustStoreLoader for subscription {} and certificate {}",
            subscriptionCertificate.subscription().getId(),
            subscriptionCertificate.fingerprint()
        );
        truststoreLoaders
            .removeAll(new Key(subscriptionCertificate))
            .forEach(loader -> {
                loader.onEvent(new KeyStoreEvent.UnloadEvent(loader.id()));
                loader.stop();
            });
        subscriptions.remove(new Key(subscriptionCertificate));
    }

    record Key(String api, String plan, String fingerPrint) {
        public Key {
            Objects.requireNonNull(api, "API must not be null");
            Objects.requireNonNull(fingerPrint, "Certificate fingerprint must not be null");
        }

        public Key(SubscriptionCertificate subscriptionCertificate) {
            this(
                subscriptionCertificate.subscription().getApi(),
                subscriptionCertificate.subscription().getPlan(),
                subscriptionCertificate.fingerprint()
            );
        }
    }
}
