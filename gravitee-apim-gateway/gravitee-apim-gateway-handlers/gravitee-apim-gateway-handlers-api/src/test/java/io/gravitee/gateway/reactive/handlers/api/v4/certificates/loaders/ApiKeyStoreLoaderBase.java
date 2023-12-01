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
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
abstract class ApiKeyStoreLoaderBase<K extends AbstractApiKeyStoreLoader> {

    @Mock
    Api api;

    K cut;

    @BeforeEach
    final void begin() {
        when(api.getId()).thenReturn(UUID.randomUUID().toString());
        when(api.getDeployedAt()).thenReturn(new Date());
    }

    void should_start_keyloader() throws KeyStoreException {
        ArrayList<KeyStoreEvent> list = new ArrayList<>();
        cut.setEventHandler(list::add);
        cut.start();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).loaderId()).isEqualTo(cut.id());
        assertThat(list.get(0).type()).isEqualTo(KeyStoreEvent.EventType.LOAD);
        assertThat(list.get(0).password()).isNotEmpty();
        assertThat(list.get(0).keyStore().size()).isEqualTo(2);

        cut.stop();
        assertThat(list).hasSize(2);
        assertThat(list.get(1).loaderId()).isEqualTo(cut.id());
        assertThat(list.get(1).type()).isEqualTo(KeyStoreEvent.EventType.UNLOAD);
    }
}
