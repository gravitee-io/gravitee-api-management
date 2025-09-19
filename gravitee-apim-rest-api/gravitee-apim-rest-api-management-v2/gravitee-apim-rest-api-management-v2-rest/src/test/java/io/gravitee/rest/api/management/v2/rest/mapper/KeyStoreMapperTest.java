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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.gravitee.rest.api.management.v2.rest.model.JKSKeyStore;
import io.gravitee.rest.api.management.v2.rest.model.KeyStore;
import io.gravitee.rest.api.management.v2.rest.model.KeyStoreType;
import io.gravitee.rest.api.management.v2.rest.model.NoneKeyStore;
import io.gravitee.rest.api.management.v2.rest.model.PEMKeyStore;
import io.gravitee.rest.api.management.v2.rest.model.PKCS12KeyStore;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

public class KeyStoreMapperTest {

    private final KeyStoreMapper keyStoreMapper = Mappers.getMapper(KeyStoreMapper.class);

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of(KeyStoreType.JKS, io.gravitee.definition.model.ssl.KeyStoreType.JKS),
            Arguments.of(KeyStoreType.PEM, io.gravitee.definition.model.ssl.KeyStoreType.PEM),
            Arguments.of(KeyStoreType.PKCS12, io.gravitee.definition.model.ssl.KeyStoreType.PKCS12),
            Arguments.of(KeyStoreType.NONE, io.gravitee.definition.model.ssl.KeyStoreType.None)
        );
    }

    @ParameterizedTest(name = "should map trust store type {0} to model {1}")
    @MethodSource("provideParameters")
    void shouldMapToKeyStoreTypeModel(KeyStoreType trustStoreType, io.gravitee.definition.model.ssl.KeyStoreType trustStoreTypeModel) {
        io.gravitee.definition.model.ssl.KeyStoreType converted = keyStoreMapper.mapToKeyStoreType(trustStoreType);

        assertThat(converted).isEqualTo(trustStoreTypeModel);
    }

    @ParameterizedTest(name = "should map trust store model {1} to type {0}")
    @MethodSource("provideParameters")
    void shouldMapFromKeyStoreTypeModel(KeyStoreType trustStoreType, io.gravitee.definition.model.ssl.KeyStoreType trustStoreTypeModel) {
        KeyStoreType converted = keyStoreMapper.mapKeyStoreType(trustStoreTypeModel);

        assertThat(converted).isEqualTo(trustStoreType);
    }

    @Test
    void shouldMapToJKSKeyStoreV2() {
        var jksKeyStore = JKSKeyStore.builder().type(KeyStoreType.JKS).path("path").content("content").password("password").build();

        var jksKeyStoreEntityV2 = keyStoreMapper.mapToV2(new KeyStore(jksKeyStore));
        assertThat(jksKeyStoreEntityV2).isNotNull();
        assertThat(jksKeyStoreEntityV2).isInstanceOf(io.gravitee.definition.model.ssl.jks.JKSKeyStore.class);
        assertThat(jksKeyStoreEntityV2.getType()).isEqualTo(
            io.gravitee.definition.model.ssl.KeyStoreType.valueOf(jksKeyStore.getType().name())
        );
        assertThat(((io.gravitee.definition.model.ssl.jks.JKSKeyStore) jksKeyStoreEntityV2).getPath()).isEqualTo(jksKeyStore.getPath());
        assertThat(((io.gravitee.definition.model.ssl.jks.JKSKeyStore) jksKeyStoreEntityV2).getContent()).isEqualTo(
            jksKeyStore.getContent()
        );
        assertThat(((io.gravitee.definition.model.ssl.jks.JKSKeyStore) jksKeyStoreEntityV2).getPassword()).isEqualTo(
            jksKeyStore.getPassword()
        );
    }

    @Test
    void shouldMapToPKCS12KeyStoreV2() {
        var pkcs12KeyStore = PKCS12KeyStore.builder()
            .type(KeyStoreType.PKCS12)
            .path("path")
            .content("content")
            .password("password")
            .build();

        var pkcs12KeyStoreEntityV2 = keyStoreMapper.mapToV2(new KeyStore(pkcs12KeyStore));
        assertThat(pkcs12KeyStoreEntityV2).isNotNull();
        assertThat(pkcs12KeyStoreEntityV2).isInstanceOf(io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore.class);
        assertThat(pkcs12KeyStoreEntityV2.getType()).isEqualTo(
            io.gravitee.definition.model.ssl.KeyStoreType.valueOf(pkcs12KeyStore.getType().name())
        );
        assertThat(((io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore) pkcs12KeyStoreEntityV2).getPath()).isEqualTo(
            pkcs12KeyStore.getPath()
        );
        assertThat(((io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore) pkcs12KeyStoreEntityV2).getContent()).isEqualTo(
            pkcs12KeyStore.getContent()
        );
        assertThat(((io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore) pkcs12KeyStoreEntityV2).getPassword()).isEqualTo(
            pkcs12KeyStore.getPassword()
        );
    }

    @Test
    void shouldMapToPEMKeyStoreV2() {
        var pemKeyStore = PEMKeyStore.builder()
            .type(KeyStoreType.PEM)
            .keyPath("path")
            .keyContent("content")
            .certPath("cert-path")
            .certContent("cert-content")
            .build();

        var pemKeyStoreEntityV2 = keyStoreMapper.mapToV2(new KeyStore(pemKeyStore));
        assertThat(pemKeyStoreEntityV2).isNotNull();
        assertThat(pemKeyStoreEntityV2).isInstanceOf(io.gravitee.definition.model.ssl.pem.PEMKeyStore.class);
        assertThat(pemKeyStoreEntityV2.getType()).isEqualTo(
            io.gravitee.definition.model.ssl.KeyStoreType.valueOf(pemKeyStore.getType().name())
        );
        assertThat(((io.gravitee.definition.model.ssl.pem.PEMKeyStore) pemKeyStoreEntityV2).getKeyPath()).isEqualTo(
            pemKeyStore.getKeyPath()
        );
        assertThat(((io.gravitee.definition.model.ssl.pem.PEMKeyStore) pemKeyStoreEntityV2).getKeyContent()).isEqualTo(
            pemKeyStore.getKeyContent()
        );
        assertThat(((io.gravitee.definition.model.ssl.pem.PEMKeyStore) pemKeyStoreEntityV2).getCertPath()).isEqualTo(
            pemKeyStore.getCertPath()
        );
        assertThat(((io.gravitee.definition.model.ssl.pem.PEMKeyStore) pemKeyStoreEntityV2).getCertContent()).isEqualTo(
            pemKeyStore.getCertContent()
        );
    }

    @Test
    void shouldMapToNoneKeyStoreV2() {
        var noneKeyStore = NoneKeyStore.builder().type(KeyStoreType.NONE).build();

        var noneKeyStoreEntityV2 = keyStoreMapper.mapToV2(new KeyStore(noneKeyStore));
        assertThat(noneKeyStoreEntityV2).isNotNull();
        assertThat(noneKeyStoreEntityV2).isInstanceOf(io.gravitee.definition.model.ssl.none.NoneKeyStore.class);
        assertThat(noneKeyStoreEntityV2.getType()).isEqualTo(io.gravitee.definition.model.ssl.KeyStoreType.None);
    }

    @Test
    void shouldMapFromJKSKeyStoreV2() {
        var jksKeyStoreEntityV2 = io.gravitee.definition.model.ssl.jks.JKSKeyStore.builder()
            .type(io.gravitee.definition.model.ssl.KeyStoreType.JKS)
            .path("path")
            .content("content")
            .password("password")
            .build();

        var jksKeyStore = keyStoreMapper.map(jksKeyStoreEntityV2);
        assertThat(jksKeyStore).isNotNull();
        assertThat(jksKeyStore.getType()).isEqualTo(KeyStoreType.valueOf(jksKeyStoreEntityV2.getType().name()));
        assertThat(jksKeyStore.getPath()).isEqualTo(jksKeyStoreEntityV2.getPath());
        assertThat(jksKeyStore.getContent()).isEqualTo(jksKeyStoreEntityV2.getContent());
        assertThat(jksKeyStore.getPassword()).isEqualTo(jksKeyStoreEntityV2.getPassword());
    }

    @Test
    void shouldMapFromPKCS12KeyStoreV2() {
        var pkcs12KeyStoreEntityV2 = io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore.builder()
            .type(io.gravitee.definition.model.ssl.KeyStoreType.PKCS12)
            .path("path")
            .content("content")
            .password("password")
            .build();

        var pkcs12KeyStore = keyStoreMapper.map(pkcs12KeyStoreEntityV2);
        assertThat(pkcs12KeyStore).isNotNull();
        assertThat(pkcs12KeyStore.getType()).isEqualTo(KeyStoreType.valueOf(pkcs12KeyStoreEntityV2.getType().name()));
        assertThat(pkcs12KeyStore.getPath()).isEqualTo(pkcs12KeyStoreEntityV2.getPath());
        assertThat(pkcs12KeyStore.getContent()).isEqualTo(pkcs12KeyStoreEntityV2.getContent());
        assertThat(pkcs12KeyStore.getPassword()).isEqualTo(pkcs12KeyStoreEntityV2.getPassword());
    }

    @Test
    void shouldMapFromPEMKeyStoreV2() {
        var pemKeyStoreEntityV2 = io.gravitee.definition.model.ssl.pem.PEMKeyStore.builder()
            .type(io.gravitee.definition.model.ssl.KeyStoreType.PEM)
            .keyPath("key-path")
            .keyContent("key-content")
            .certPath("cert-path")
            .certContent("cert-content")
            .build();

        var pemKeyStore = keyStoreMapper.map(pemKeyStoreEntityV2);
        assertThat(pemKeyStore).isNotNull();
        assertThat(pemKeyStore.getType()).isEqualTo(KeyStoreType.valueOf(pemKeyStoreEntityV2.getType().name()));
        assertThat(pemKeyStore.getKeyPath()).isEqualTo(pemKeyStoreEntityV2.getKeyPath());
        assertThat(pemKeyStore.getKeyContent()).isEqualTo(pemKeyStoreEntityV2.getKeyContent());
        assertThat(pemKeyStore.getCertPath()).isEqualTo(pemKeyStoreEntityV2.getCertPath());
        assertThat(pemKeyStore.getCertContent()).isEqualTo(pemKeyStoreEntityV2.getCertContent());
    }

    @Test
    void shouldMapFromNoneKeyStoreV2() {
        var noneKeyStoreEntityV2 = io.gravitee.definition.model.ssl.none.NoneKeyStore.builder()
            .type(io.gravitee.definition.model.ssl.KeyStoreType.None)
            .build();

        var noneKeyStore = keyStoreMapper.map(noneKeyStoreEntityV2);
        assertThat(noneKeyStore).isNotNull();
        assertThat(noneKeyStore.getType()).isEqualTo(KeyStoreType.NONE);
    }
}
