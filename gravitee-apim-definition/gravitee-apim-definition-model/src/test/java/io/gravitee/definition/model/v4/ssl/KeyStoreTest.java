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
package io.gravitee.definition.model.v4.ssl;

import static io.gravitee.definition.model.v4.ssl.KeyStoreType.JKS;
import static io.gravitee.definition.model.v4.ssl.KeyStoreType.PEM;
import static io.gravitee.definition.model.v4.ssl.KeyStoreType.PKCS12;

import io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class KeyStoreTest {

    @Test
    void builder_should_init_pem_keystore() {
        var keyStore = PEMKeyStore.builder()
            .certContent("certContent")
            .certPath("certPath")
            .keyContent("keyContent")
            .keyPath("keyPath")
            .build();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(keyStore.getType()).isEqualTo(PEM);
            softly.assertThat(keyStore.getCertContent()).isEqualTo("certContent");
            softly.assertThat(keyStore.getCertPath()).isEqualTo("certPath");
            softly.assertThat(keyStore.getKeyContent()).isEqualTo("keyContent");
            softly.assertThat(keyStore.getKeyPath()).isEqualTo("keyPath");
        });
    }

    @Test
    void builder_should_init_jks_keystore() {
        var keyStore = JKSKeyStore.builder()
            .alias("alias")
            .content("content")
            .keyPassword("keyPassword")
            .password("password")
            .path("path")
            .build();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(keyStore.getType()).isEqualTo(JKS);
            softly.assertThat(keyStore.getAlias()).isEqualTo("alias");
            softly.assertThat(keyStore.getContent()).isEqualTo("content");
            softly.assertThat(keyStore.getKeyPassword()).isEqualTo("keyPassword");
            softly.assertThat(keyStore.getPassword()).isEqualTo("password");
            softly.assertThat(keyStore.getPath()).isEqualTo("path");
        });
    }

    @Test
    void builder_should_init_pkcs12_keystore() {
        var keyStore = PKCS12KeyStore.builder()
            .alias("alias")
            .content("content")
            .keyPassword("keyPassword")
            .password("password")
            .path("path")
            .build();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(keyStore.getType()).isEqualTo(PKCS12);
            softly.assertThat(keyStore.getAlias()).isEqualTo("alias");
            softly.assertThat(keyStore.getContent()).isEqualTo("content");
            softly.assertThat(keyStore.getKeyPassword()).isEqualTo("keyPassword");
            softly.assertThat(keyStore.getPassword()).isEqualTo("password");
            softly.assertThat(keyStore.getPath()).isEqualTo("path");
        });
    }
}
