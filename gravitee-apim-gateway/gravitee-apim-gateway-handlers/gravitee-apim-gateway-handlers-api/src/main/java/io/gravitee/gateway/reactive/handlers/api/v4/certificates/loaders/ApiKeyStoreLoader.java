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
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyStoreLoader extends AbstractApiKeyStoreLoader {

    public ApiKeyStoreLoader(Api api, List<Tls> tlsData) {
        super(api, tlsData);
    }

    @Override
    public void start() {
        AtomicInteger counter = new AtomicInteger(1);
        List<KeyStore> keyStores = tlsData
            .stream()
            .filter(Tls::hasKeyPairs)
            .flatMap(tls -> tls.getPemKeyPairs().stream())
            .map(pair ->
                KeyStoreUtils.initFromPem(
                    pair.getCertificateChain(),
                    pair.getPrivateKey(),
                    password,
                    "key_pair_%d".formatted(counter.getAndIncrement())
                )
            )
            .toList();

        fireEvent(keyStores);
    }
}
