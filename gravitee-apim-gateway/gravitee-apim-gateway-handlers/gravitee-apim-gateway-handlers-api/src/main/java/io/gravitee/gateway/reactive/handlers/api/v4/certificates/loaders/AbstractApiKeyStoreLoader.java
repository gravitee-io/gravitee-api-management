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

package io.gravitee.gateway.reactive.handlers.api.v4.certificates.loaders;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.definition.model.v4.listener.tls.Tls;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import java.security.KeyStore;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractApiKeyStoreLoader implements KeyStoreLoader {

    protected final List<Tls> tlsData;
    private final String id;
    private Consumer<KeyStoreEvent> handler;
    protected final String password;

    protected AbstractApiKeyStoreLoader(Api api, List<Tls> tlsData) {
        this.id = "%s-%d" + api.getId() + api.getDeployedAt().toInstant().toEpochMilli();
        this.tlsData = tlsData;
        this.password = UUID.randomUUID().toString();
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public final void stop() {
        handler.accept(KeyStoreEvent.unloadEvent(id()));
    }

    @Override
    public final void setEventHandler(Consumer<KeyStoreEvent> handler) {
        this.handler = handler;
    }

    protected final void fireEvent(List<KeyStore> keyStores) {
        if (!keyStores.isEmpty()) {
            KeyStore keyStore = merge(keyStores, password);
            this.handler.accept(KeyStoreEvent.loadEvent(id(), keyStore, password, null));
        }
    }

    protected KeyStore merge(List<KeyStore> keyStores, String password) {
        return KeyStoreUtils.merge(keyStores, password);
    }
}
