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

import io.gravitee.definition.model.ssl.KeyStoreType;
import org.junit.jupiter.api.Test;

class KeyStoreMapperTest {

    @Test
    void shouldReturnNullWhenInputIsNull() {
        assertThat(KeyStoreMapper.convert(null)).isNull();
    }

    @Test
    void shouldConvertJKSKeyStore() {
        io.gravitee.definition.model.ssl.jks.JKSKeyStore v2Jks = new io.gravitee.definition.model.ssl.jks.JKSKeyStore();
        v2Jks.setPath("path");
        v2Jks.setContent("content");
        v2Jks.setPassword("password");

        var result = KeyStoreMapper.convert(v2Jks);
        assertThat(result).isInstanceOf(io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore.class);
        io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore jks = (io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore) result;
        assertThat(jks.getPath()).isEqualTo("path");
        assertThat(jks.getContent()).isEqualTo("content");
        assertThat(jks.getPassword()).isEqualTo("password");
    }

    @Test
    void shouldConvertPEMKeyStore() {
        io.gravitee.definition.model.ssl.pem.PEMKeyStore v2Pem = new io.gravitee.definition.model.ssl.pem.PEMKeyStore();
        v2Pem.setKeyPath("keyPath");
        v2Pem.setKeyContent("keyContent");
        v2Pem.setCertPath("certPath");
        v2Pem.setCertContent("certContent");

        var result = KeyStoreMapper.convert(v2Pem);
        assertThat(result).isInstanceOf(io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore.class);
        io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore pem = (io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore) result;
        assertThat(pem.getKeyPath()).isEqualTo("keyPath");
        assertThat(pem.getKeyContent()).isEqualTo("keyContent");
        assertThat(pem.getCertPath()).isEqualTo("certPath");
        assertThat(pem.getCertContent()).isEqualTo("certContent");
    }

    @Test
    void shouldConvertPKCS12KeyStore() {
        io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore v2Pkcs12 = new io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore();
        v2Pkcs12.setPath("pkcsPath");
        v2Pkcs12.setPassword("pkcsPassword");
        v2Pkcs12.setContent("pkcsContent");

        var result = KeyStoreMapper.convert(v2Pkcs12);
        assertThat(result).isInstanceOf(io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore.class);
        io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore pkcs12 =
            (io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore) result;
        assertThat(pkcs12.getPath()).isEqualTo("pkcsPath");
        assertThat(pkcs12.getPassword()).isEqualTo("pkcsPassword");
        assertThat(pkcs12.getContent()).isEqualTo("pkcsContent");
    }

    @Test
    void shouldConvertNoneKeyStore() {
        io.gravitee.definition.model.ssl.none.NoneKeyStore v2None = new io.gravitee.definition.model.ssl.none.NoneKeyStore(
            KeyStoreType.None
        );

        var result = KeyStoreMapper.convert(v2None);
        assertThat(result).isInstanceOf(io.gravitee.definition.model.v4.ssl.none.NoneKeyStore.class);
    }
}
