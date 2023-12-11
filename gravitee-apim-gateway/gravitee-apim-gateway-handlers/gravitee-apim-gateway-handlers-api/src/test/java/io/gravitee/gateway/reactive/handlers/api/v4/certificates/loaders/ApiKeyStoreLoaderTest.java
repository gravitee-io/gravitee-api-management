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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.listener.tls.ClientPemCertificate;
import io.gravitee.definition.model.v4.listener.tls.PemKeyPair;
import io.gravitee.definition.model.v4.listener.tls.Tls;
import io.gravitee.gateway.reactive.handlers.api.v4.certificates.TestFixtures;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

class ApiKeyStoreLoaderTest extends ApiKeyStoreLoaderBase<ApiKeyStoreLoader> {

    @BeforeEach
    void before() {
        cut =
            new ApiKeyStoreLoader(
                api,
                List.of(
                    Tls
                        .builder()
                        .pemKeyPairs(
                            List.of(
                                PemKeyPair.builder().certificateChain(TestFixtures.CERT).privateKey(TestFixtures.KEY).build(),
                                PemKeyPair.builder().certificateChain(TestFixtures.CERT).privateKey(TestFixtures.KEY).build()
                            )
                        )
                        .build()
                )
            );
    }

    @Test
    void should_start_keyloader() throws KeyStoreException {
        super.should_start_keyloader();
    }

    @Test
    void should_do_nothing_with_client_certs_only() {
        cut =
            new ApiKeyStoreLoader(
                api,
                List.of(
                    Tls
                        .builder()
                        .clientPemCertificates(
                            List.of(
                                ClientPemCertificate.builder().certificate(TestFixtures.CERT).build(),
                                ClientPemCertificate.builder().certificate(TestFixtures.CERT).build()
                            )
                        )
                        .build()
                )
            );
        ArrayList<KeyStoreEvent> list = new ArrayList<>();
        cut.setEventHandler(list::add);
        cut.start();

        assertThat(list).isEmpty();
    }
}
