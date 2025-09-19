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

import io.gravitee.rest.api.management.v2.rest.model.JKSTrustStore;
import io.gravitee.rest.api.management.v2.rest.model.NoneTrustStore;
import io.gravitee.rest.api.management.v2.rest.model.PEMTrustStore;
import io.gravitee.rest.api.management.v2.rest.model.PKCS12TrustStore;
import io.gravitee.rest.api.management.v2.rest.model.TrustStore;
import io.gravitee.rest.api.management.v2.rest.model.TrustStoreType;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

public class TrustStoreMapperTest {

    private final TrustStoreMapper trustStoreMapper = Mappers.getMapper(TrustStoreMapper.class);

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of(TrustStoreType.JKS, io.gravitee.definition.model.ssl.TrustStoreType.JKS),
            Arguments.of(TrustStoreType.PEM, io.gravitee.definition.model.ssl.TrustStoreType.PEM),
            Arguments.of(TrustStoreType.PKCS12, io.gravitee.definition.model.ssl.TrustStoreType.PKCS12),
            Arguments.of(TrustStoreType.NONE, io.gravitee.definition.model.ssl.TrustStoreType.None)
        );
    }

    @ParameterizedTest(name = "should map trust store type {0} to model {1}")
    @MethodSource("provideParameters")
    void shouldMapToTrustStoreTypeModel(
        TrustStoreType trustStoreType,
        io.gravitee.definition.model.ssl.TrustStoreType trustStoreTypeModel
    ) {
        io.gravitee.definition.model.ssl.TrustStoreType converted = trustStoreMapper.mapTrustStoreType(trustStoreType);

        assertThat(converted).isEqualTo(trustStoreTypeModel);
    }

    @ParameterizedTest(name = "should map trust store model {1} to type {0}")
    @MethodSource("provideParameters")
    void shouldMapFromTrustStoreTypeModel(
        TrustStoreType trustStoreType,
        io.gravitee.definition.model.ssl.TrustStoreType trustStoreTypeModel
    ) {
        TrustStoreType converted = trustStoreMapper.mapTrustStoreType(trustStoreTypeModel);

        assertThat(converted).isEqualTo(trustStoreType);
    }

    @Test
    void shouldMapToJKSTrustStoreV2() {
        var jksTrustStore = (JKSTrustStore) new JKSTrustStore()
            .path("path")
            .content("content")
            .password("password")
            .type(TrustStoreType.JKS);

        var jksTrustStoreEntityV2 = trustStoreMapper.mapToV2(new TrustStore(jksTrustStore));
        assertThat(jksTrustStoreEntityV2).isNotNull();
        assertThat(jksTrustStoreEntityV2).isInstanceOf(io.gravitee.definition.model.ssl.jks.JKSTrustStore.class);
        assertThat(jksTrustStoreEntityV2.getType()).isEqualTo(
            io.gravitee.definition.model.ssl.TrustStoreType.valueOf(jksTrustStore.getType().name())
        );
        assertThat(((io.gravitee.definition.model.ssl.jks.JKSTrustStore) jksTrustStoreEntityV2).getPath()).isEqualTo(
            jksTrustStore.getPath()
        );
        assertThat(((io.gravitee.definition.model.ssl.jks.JKSTrustStore) jksTrustStoreEntityV2).getContent()).isEqualTo(
            jksTrustStore.getContent()
        );
        assertThat(((io.gravitee.definition.model.ssl.jks.JKSTrustStore) jksTrustStoreEntityV2).getPassword()).isEqualTo(
            jksTrustStore.getPassword()
        );
    }

    @Test
    void shouldMapToPKCS12TrustStoreV2() {
        var pkcs12TrustStore = (PKCS12TrustStore) new PKCS12TrustStore()
            .path("path")
            .content("content")
            .password("password")
            .type(TrustStoreType.PKCS12);

        var pkcs12TrustStoreEntityV2 = trustStoreMapper.mapToV2(new TrustStore(pkcs12TrustStore));
        assertThat(pkcs12TrustStoreEntityV2).isNotNull();
        assertThat(pkcs12TrustStoreEntityV2).isInstanceOf(io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore.class);
        assertThat(pkcs12TrustStoreEntityV2.getType()).isEqualTo(
            io.gravitee.definition.model.ssl.TrustStoreType.valueOf(pkcs12TrustStore.getType().name())
        );
        assertThat(((io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore) pkcs12TrustStoreEntityV2).getPath()).isEqualTo(
            pkcs12TrustStore.getPath()
        );
        assertThat(((io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore) pkcs12TrustStoreEntityV2).getContent()).isEqualTo(
            pkcs12TrustStore.getContent()
        );
        assertThat(((io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore) pkcs12TrustStoreEntityV2).getPassword()).isEqualTo(
            pkcs12TrustStore.getPassword()
        );
    }

    @Test
    void shouldMapToPEMTrustStoreV2() {
        var pemTrustStore = (PEMTrustStore) new PEMTrustStore().path("path").content("content").type(TrustStoreType.PEM);

        var pemTrustStoreEntityV2 = trustStoreMapper.mapToV2(new TrustStore(pemTrustStore));
        assertThat(pemTrustStoreEntityV2).isNotNull();
        assertThat(pemTrustStoreEntityV2).isInstanceOf(io.gravitee.definition.model.ssl.pem.PEMTrustStore.class);
        assertThat(pemTrustStoreEntityV2.getType()).isEqualTo(
            io.gravitee.definition.model.ssl.TrustStoreType.valueOf(pemTrustStore.getType().name())
        );
        assertThat(((io.gravitee.definition.model.ssl.pem.PEMTrustStore) pemTrustStoreEntityV2).getPath()).isEqualTo(
            pemTrustStore.getPath()
        );
        assertThat(((io.gravitee.definition.model.ssl.pem.PEMTrustStore) pemTrustStoreEntityV2).getContent()).isEqualTo(
            pemTrustStore.getContent()
        );
    }

    @Test
    void shouldMapToNoneTrustStoreV2() {
        var noneTrustStore = (NoneTrustStore) new NoneTrustStore().type(TrustStoreType.NONE);

        var noneTrustStoreEntityV2 = trustStoreMapper.mapToV2(new TrustStore(noneTrustStore));
        assertThat(noneTrustStoreEntityV2).isNotNull();
        assertThat(noneTrustStoreEntityV2).isInstanceOf(io.gravitee.definition.model.ssl.none.NoneTrustStore.class);
        assertThat(noneTrustStoreEntityV2.getType()).isEqualTo(io.gravitee.definition.model.ssl.TrustStoreType.None);
    }

    @Test
    void shouldMapFromJKSTrustStoreV2() {
        var jksTrustStoreEntityV2 = io.gravitee.definition.model.ssl.jks.JKSTrustStore.builder()
            .type(io.gravitee.definition.model.ssl.TrustStoreType.JKS)
            .path("path")
            .content("content")
            .password("password")
            .build();

        var jksTrustStore = trustStoreMapper.map(jksTrustStoreEntityV2);
        assertThat(jksTrustStore).isNotNull();
        assertThat(jksTrustStore.getType()).isEqualTo(TrustStoreType.valueOf(jksTrustStoreEntityV2.getType().name()));
        assertThat(jksTrustStore.getPath()).isEqualTo(jksTrustStoreEntityV2.getPath());
        assertThat(jksTrustStore.getContent()).isEqualTo(jksTrustStoreEntityV2.getContent());
        assertThat(jksTrustStore.getPassword()).isEqualTo(jksTrustStoreEntityV2.getPassword());
    }

    @Test
    void shouldMapFromPKCS12TrustStoreV2() {
        var pkcs12TrustStoreEntityV2 = io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore.builder()
            .type(io.gravitee.definition.model.ssl.TrustStoreType.PKCS12)
            .path("path")
            .content("content")
            .password("password")
            .build();

        var pkcs12TrustStore = trustStoreMapper.map(pkcs12TrustStoreEntityV2);
        assertThat(pkcs12TrustStore).isNotNull();
        assertThat(pkcs12TrustStore.getType()).isEqualTo(TrustStoreType.valueOf(pkcs12TrustStoreEntityV2.getType().name()));
        assertThat(pkcs12TrustStore.getPath()).isEqualTo(pkcs12TrustStoreEntityV2.getPath());
        assertThat(pkcs12TrustStore.getContent()).isEqualTo(pkcs12TrustStoreEntityV2.getContent());
        assertThat(pkcs12TrustStore.getPassword()).isEqualTo(pkcs12TrustStoreEntityV2.getPassword());
    }

    @Test
    void shouldMapFromPEMTrustStoreV2() {
        var pemTrustStoreEntityV2 = io.gravitee.definition.model.ssl.pem.PEMTrustStore.builder()
            .type(io.gravitee.definition.model.ssl.TrustStoreType.PEM)
            .path("path")
            .content("content")
            .build();

        var pemTrustStore = trustStoreMapper.map(pemTrustStoreEntityV2);
        assertThat(pemTrustStore).isNotNull();
        assertThat(pemTrustStore.getType()).isEqualTo(TrustStoreType.valueOf(pemTrustStoreEntityV2.getType().name()));
        assertThat(pemTrustStore.getPath()).isEqualTo(pemTrustStoreEntityV2.getPath());
        assertThat(pemTrustStore.getContent()).isEqualTo(pemTrustStoreEntityV2.getContent());
    }

    @Test
    void shouldMapFromNoneTrustStoreV2() {
        var noneTrustStoreEntityV2 = io.gravitee.definition.model.ssl.none.NoneTrustStore.builder()
            .type(io.gravitee.definition.model.ssl.TrustStoreType.None)
            .build();

        var noneTrustStore = trustStoreMapper.map(noneTrustStoreEntityV2);
        assertThat(noneTrustStore).isNotNull();
        assertThat(noneTrustStore.getType()).isEqualTo(TrustStoreType.NONE);
    }
}
