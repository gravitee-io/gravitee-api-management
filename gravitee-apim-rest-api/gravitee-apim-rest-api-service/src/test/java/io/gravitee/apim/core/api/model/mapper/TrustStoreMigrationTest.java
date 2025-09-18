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
package io.gravitee.apim.core.api.model.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.gravitee.definition.model.ssl.TrustStoreType;
import org.junit.jupiter.api.Test;

class TrustStoreMigrationTest {

    @Test
    void shouldReturnNullWhenInputIsNull() {
        assertThat(TrustStoreMigration.convert(null)).isNull();
    }

    @Test
    void shouldConvertJKSTrustStore() {
        io.gravitee.definition.model.ssl.jks.JKSTrustStore v2Jks = new io.gravitee.definition.model.ssl.jks.JKSTrustStore();
        v2Jks.setPath("jksPath");
        v2Jks.setContent("jksContent");
        v2Jks.setPassword("jksPassword");

        var result = TrustStoreMigration.convert(v2Jks);
        assertThat(result).isInstanceOf(io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore.class);
        io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore jks = (io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore) result;
        assertThat(jks.getPath()).isEqualTo("jksPath");
        assertThat(jks.getContent()).isEqualTo("jksContent");
        assertThat(jks.getPassword()).isEqualTo("jksPassword");
    }

    @Test
    void shouldConvertPEMTrustStore() {
        io.gravitee.definition.model.ssl.pem.PEMTrustStore v2Pem = new io.gravitee.definition.model.ssl.pem.PEMTrustStore();
        v2Pem.setPath("pemPath");
        v2Pem.setContent("pemContent");

        var result = TrustStoreMigration.convert(v2Pem);
        assertThat(result).isInstanceOf(io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore.class);
        io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore pem = (io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore) result;
        assertThat(pem.getPath()).isEqualTo("pemPath");
        assertThat(pem.getContent()).isEqualTo("pemContent");
    }

    @Test
    void shouldConvertPKCS12TrustStore() {
        io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore v2Pkcs12 = new io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore();
        v2Pkcs12.setPath("pkcsPath");
        v2Pkcs12.setContent("pkcsContent");
        v2Pkcs12.setPassword("pkcsPassword");

        var result = TrustStoreMigration.convert(v2Pkcs12);
        assertThat(result).isInstanceOf(io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore.class);
        io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore pkcs12 =
            (io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore) result;
        assertThat(pkcs12.getPath()).isEqualTo("pkcsPath");
        assertThat(pkcs12.getContent()).isEqualTo("pkcsContent");
        assertThat(pkcs12.getPassword()).isEqualTo("pkcsPassword");
    }

    @Test
    void shouldConvertNoneTrustStore() {
        io.gravitee.definition.model.ssl.none.NoneTrustStore v2None = new io.gravitee.definition.model.ssl.none.NoneTrustStore(
            TrustStoreType.None
        );

        var result = TrustStoreMigration.convert(v2None);
        assertThat(result).isInstanceOf(io.gravitee.definition.model.v4.ssl.none.NoneTrustStore.class);
    }
}
