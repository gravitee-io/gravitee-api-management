/*
 * *
 *  * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.gateway.reactive.handlers.api.v4.certificates;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.tls.Tls;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.certificates.loaders.ApiKeyStoreLoader;
import io.gravitee.gateway.reactive.handlers.api.v4.certificates.loaders.ApiTrustStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.vertx.server.VertxServer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class ApiKeyStoreLoaderManager {

    private final List<KeyStoreLoader> apiKeyStoreLoaders = new ArrayList<>();
    private final List<KeyStoreLoader> apiTrustStoreLoaders = new ArrayList<>();
    private final ServerManager serverManager;
    private final ListenerType listenerType;
    private final Api api;

    public void start() {
        List<Listener> listeners = api.getDefinition().getListeners().stream().filter(l -> l.getType() == listenerType).toList();
        if (listeners.stream().anyMatch(Listener::containsTlsConfig)) {
            registerKeyStoreLoaders(listeners);
        }
    }

    private void registerKeyStoreLoaders(List<Listener> listeners) {
        Predicate<Listener> containsServerId = listener -> listener.getServers() != null && !listener.getServers().isEmpty();

        List<Listener> deployOnAllServers = listeners
            .stream()
            .filter(Predicate.not(containsServerId))
            .filter(Listener::containsTlsConfig)
            .toList();

        if (!deployOnAllServers.isEmpty()) {
            serverManager
                .servers()
                .stream()
                .map(s -> (VertxServer<?, ?>) s)
                .forEach(server -> createKeyStoreLoadersForServer(server, deployOnAllServers.stream().map(Listener::getTls).toList()));
        }

        if (listeners.size() > deployOnAllServers.size()) {
            record ServerListenerPair(Listener listener, String serverId) {}
            listeners
                .stream()
                .filter(containsServerId)
                .filter(Listener::containsTlsConfig)
                .flatMap(listener -> listener.getServers().stream().map(server -> new ServerListenerPair(listener, server)))
                .collect(Collectors.groupingBy(ServerListenerPair::serverId))
                .forEach((serverId, listenerServerPairs) ->
                    createKeyStoreLoadersForServer(
                        (VertxServer<?, ?>) serverManager.server(serverId),
                        listenerServerPairs.stream().map(pair -> pair.listener().getTls()).toList()
                    )
                );
        }
    }

    private void createKeyStoreLoadersForServer(VertxServer<?, ?> server, List<Tls> tlsData) {
        if (tlsData.stream().anyMatch(Tls::hasKeyPairs)) {
            KeyStoreLoader apiKeyStoreLoader = new ApiKeyStoreLoader(api, tlsData);
            server.keyStoreLoaderManager().registerLoader(apiKeyStoreLoader);
            this.apiKeyStoreLoaders.add(apiKeyStoreLoader);
        }

        if (tlsData.stream().anyMatch(Tls::hasClientCertificates)) {
            KeyStoreLoader apiTrustStoreLoader = new ApiTrustStoreLoader(api, tlsData);
            server.trustStoreLoaderManager().registerLoader(apiTrustStoreLoader);
            this.apiTrustStoreLoaders.add(apiTrustStoreLoader);
        }
    }

    public void stop() {
        this.apiKeyStoreLoaders.forEach(loader -> {
                log.info("Stopping ApiKeyStoreLoader with id: {}", loader.id());
                loader.stop();
            });
        this.apiKeyStoreLoaders.clear();
        this.apiTrustStoreLoaders.forEach(loader -> {
                log.info("Stopping ApiTrustStoreLoader with id: {}", loader.id());
                loader.stop();
            });
        this.apiTrustStoreLoaders.clear();
    }

    @VisibleForTesting
    List<KeyStoreLoader> getApiKeyStoreLoaders() {
        return apiKeyStoreLoaders;
    }

    @VisibleForTesting
    List<KeyStoreLoader> getApiTrustStoreLoaders() {
        return apiTrustStoreLoaders;
    }
}
