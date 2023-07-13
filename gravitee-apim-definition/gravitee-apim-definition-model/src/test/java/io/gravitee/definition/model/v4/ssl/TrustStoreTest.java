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

import static io.gravitee.definition.model.v4.ssl.TrustStoreType.JKS;
import static io.gravitee.definition.model.v4.ssl.TrustStoreType.PEM;
import static io.gravitee.definition.model.v4.ssl.TrustStoreType.PKCS12;

import io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class TrustStoreTest {

    @Test
    void builder_should_init_pem_truststore() {
        var trustStore = PEMTrustStore.builder().content("content").path("path").build();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(trustStore.getType()).isEqualTo(PEM);
            softly.assertThat(trustStore.getContent()).isEqualTo("content");
            softly.assertThat(trustStore.getPath()).isEqualTo("path");
        });
    }

    @Test
    void builder_should_init_jks_truststore() {
        var trustStore = JKSTrustStore.builder().alias("alias").content("content").password("password").path("path").build();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(trustStore.getType()).isEqualTo(JKS);
            softly.assertThat(trustStore.getAlias()).isEqualTo("alias");
            softly.assertThat(trustStore.getContent()).isEqualTo("content");
            softly.assertThat(trustStore.getPassword()).isEqualTo("password");
            softly.assertThat(trustStore.getPath()).isEqualTo("path");
        });
    }

    @Test
    void builder_should_init_pkcs12_truststore() {
        var trustStore = PKCS12TrustStore.builder().alias("alias").content("content").password("password").path("path").build();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(trustStore.getType()).isEqualTo(PKCS12);
            softly.assertThat(trustStore.getAlias()).isEqualTo("alias");
            softly.assertThat(trustStore.getContent()).isEqualTo("content");
            softly.assertThat(trustStore.getPassword()).isEqualTo("password");
            softly.assertThat(trustStore.getPath()).isEqualTo("path");
        });
    }
}
