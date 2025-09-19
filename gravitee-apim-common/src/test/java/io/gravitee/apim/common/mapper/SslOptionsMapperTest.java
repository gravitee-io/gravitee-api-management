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
package io.gravitee.apim.common.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.gravitee.definition.model.v4.ssl.SslOptions;
import io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.v4.ssl.none.NoneKeyStore;
import io.gravitee.definition.model.v4.ssl.none.NoneTrustStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore;
import io.gravitee.node.vertx.client.ssl.KeyStore;
import io.gravitee.node.vertx.client.ssl.KeyStoreType;
import io.gravitee.node.vertx.client.ssl.TrustStore;
import io.gravitee.node.vertx.client.ssl.TrustStoreType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SslOptionsMapperTest {

    @Test
    void should_map_base_fields() {
        final SslOptions sslOptions = SslOptions.builder().trustAll(false).build();

        final io.gravitee.node.vertx.client.ssl.SslOptions result = SslOptionsMapper.INSTANCE.map(sslOptions);
        assertThat(result.isTrustAll()).isFalse();
        assertThat(result.isHostnameVerifier()).isTrue();
        assertThat(result.trustStore()).isEmpty();
        assertThat(result.keyStore()).isEmpty();
    }

    @Nested
    class PEM {

        @Test
        void should_map_pem_keyStore() {
            final SslOptions sslOptions = SslOptions.builder()
                .trustAll(false)
                .keyStore(
                    PEMKeyStore.builder()
                        .keyPath("keyPath")
                        .keyContent("keyContent")
                        .certPath("certPath")
                        .certContent("certContent")
                        .build()
                )
                .build();
            final var result = SslOptionsMapper.INSTANCE.map(sslOptions);
            assertThat(result.keyStore()).hasValueSatisfying(resultKeystore -> {
                assertThat(resultKeystore.getType()).isEqualTo(KeyStoreType.PEM);
                final var resultPEMKeyStore = castInto(resultKeystore, io.gravitee.node.vertx.client.ssl.pem.PEMKeyStore.class);
                assertThat(resultPEMKeyStore.getKeyPath()).isEqualTo("keyPath");
                assertThat(resultPEMKeyStore.getKeyContent()).isEqualTo("keyContent");
                assertThat(resultPEMKeyStore.getCertPath()).isEqualTo("certPath");
                assertThat(resultPEMKeyStore.getCertContent()).isEqualTo("certContent");
            });
        }

        @Test
        void should_map_pem_trustStore() {
            final SslOptions sslOptions = SslOptions.builder()
                .trustAll(false)
                .trustStore(PEMTrustStore.builder().path("path").content("content").build())
                .build();
            final var result = SslOptionsMapper.INSTANCE.map(sslOptions);
            assertThat(result.trustStore()).hasValueSatisfying(resultTrustStore -> {
                assertThat(resultTrustStore.getType()).isEqualTo(TrustStoreType.PEM);
                final var resultPEMTrustStore = castInto(resultTrustStore, io.gravitee.node.vertx.client.ssl.pem.PEMTrustStore.class);
                assertThat(resultPEMTrustStore.getPath()).isEqualTo("path");
                assertThat(resultPEMTrustStore.getContent()).isEqualTo("content");
            });
        }
    }

    @Nested
    class JKS {

        @Test
        void should_map_jks_keyStore() {
            final SslOptions sslOptions = SslOptions.builder()
                .trustAll(false)
                .keyStore(
                    JKSKeyStore.builder()
                        .path("path")
                        .content("content")
                        .password("password")
                        .alias("alias")
                        .keyPassword("keyPassword")
                        .build()
                )
                .build();
            final var result = SslOptionsMapper.INSTANCE.map(sslOptions);
            assertThat(result.keyStore()).hasValueSatisfying(resultKeystore -> {
                assertThat(resultKeystore.getType()).isEqualTo(KeyStoreType.JKS);
                final var resultJKSKeyStore = castInto(resultKeystore, io.gravitee.node.vertx.client.ssl.jks.JKSKeyStore.class);
                assertThat(resultJKSKeyStore.getPath()).isEqualTo("path");
                assertThat(resultJKSKeyStore.getContent()).isEqualTo("content");
                assertThat(resultJKSKeyStore.getPassword()).isEqualTo("password");
                assertThat(resultJKSKeyStore.getAlias()).isEqualTo("alias");
                assertThat(resultJKSKeyStore.getKeyPassword()).isEqualTo("keyPassword");
            });
        }

        @Test
        void should_map_jks_trustStore() {
            final SslOptions sslOptions = SslOptions.builder()
                .trustAll(false)
                .trustStore(JKSTrustStore.builder().path("path").content("content").password("password").alias("alias").build())
                .build();
            final var result = SslOptionsMapper.INSTANCE.map(sslOptions);
            assertThat(result.trustStore()).hasValueSatisfying(resultTrustStore -> {
                assertThat(resultTrustStore.getType()).isEqualTo(TrustStoreType.JKS);
                final var resultJKSTrustStore = castInto(resultTrustStore, io.gravitee.node.vertx.client.ssl.jks.JKSTrustStore.class);
                assertThat(resultJKSTrustStore.getPath()).isEqualTo("path");
                assertThat(resultJKSTrustStore.getContent()).isEqualTo("content");
                assertThat(resultJKSTrustStore.getPassword()).isEqualTo("password");
                assertThat(resultJKSTrustStore.getAlias()).isEqualTo("alias");
            });
        }
    }

    @Nested
    class PKCS12 {

        @Test
        void should_map_pkcs12_keyStore() {
            final SslOptions sslOptions = SslOptions.builder()
                .trustAll(false)
                .keyStore(
                    PKCS12KeyStore.builder()
                        .path("path")
                        .content("content")
                        .password("password")
                        .alias("alias")
                        .keyPassword("keyPassword")
                        .build()
                )
                .build();
            final var result = SslOptionsMapper.INSTANCE.map(sslOptions);
            assertThat(result.keyStore()).hasValueSatisfying(resultKeystore -> {
                assertThat(resultKeystore.getType()).isEqualTo(KeyStoreType.PKCS12);
                final var resultPKCS12KeyStore = castInto(resultKeystore, io.gravitee.node.vertx.client.ssl.pkcs12.PKCS12KeyStore.class);
                assertThat(resultPKCS12KeyStore.getPath()).isEqualTo("path");
                assertThat(resultPKCS12KeyStore.getContent()).isEqualTo("content");
                assertThat(resultPKCS12KeyStore.getPassword()).isEqualTo("password");
                assertThat(resultPKCS12KeyStore.getAlias()).isEqualTo("alias");
                assertThat(resultPKCS12KeyStore.getKeyPassword()).isEqualTo("keyPassword");
            });
        }

        @Test
        void should_map_pkcs12_trustStore() {
            final SslOptions sslOptions = SslOptions.builder()
                .trustAll(false)
                .trustStore(PKCS12TrustStore.builder().path("path").content("content").password("password").alias("alias").build())
                .build();
            final var result = SslOptionsMapper.INSTANCE.map(sslOptions);
            assertThat(result.trustStore()).hasValueSatisfying(resultTrustStore -> {
                assertThat(resultTrustStore.getType()).isEqualTo(TrustStoreType.PKCS12);
                final var resultPKCS12TrustStore = castInto(
                    resultTrustStore,
                    io.gravitee.node.vertx.client.ssl.pkcs12.PKCS12TrustStore.class
                );
                assertThat(resultPKCS12TrustStore.getPath()).isEqualTo("path");
                assertThat(resultPKCS12TrustStore.getContent()).isEqualTo("content");
                assertThat(resultPKCS12TrustStore.getPassword()).isEqualTo("password");
                assertThat(resultPKCS12TrustStore.getAlias()).isEqualTo("alias");
            });
        }
    }

    @Nested
    class None {

        @Test
        void should_map_none_keyStore() {
            final SslOptions sslOptions = SslOptions.builder().trustAll(false).keyStore(NoneKeyStore.builder().build()).build();
            final var result = SslOptionsMapper.INSTANCE.map(sslOptions);
            assertThat(result.keyStore()).hasValueSatisfying(resultKeystore -> {
                assertThat(resultKeystore.getType()).isEqualTo(KeyStoreType.NONE);
            });
        }

        @Test
        void should_map_none_trustStore() {
            final SslOptions sslOptions = SslOptions.builder().trustAll(false).trustStore(NoneTrustStore.builder().build()).build();
            final var result = SslOptionsMapper.INSTANCE.map(sslOptions);
            assertThat(result.trustStore()).hasValueSatisfying(resultTrustStore -> {
                assertThat(resultTrustStore.getType()).isEqualTo(TrustStoreType.NONE);
            });
        }
    }

    <K extends KeyStore> K castInto(KeyStore keyStore, Class<K> keyStoreSubclass) {
        if (keyStoreSubclass.isAssignableFrom(keyStore.getClass())) {
            return keyStoreSubclass.cast(keyStore);
        } else {
            fail(
                String.format(
                    "Cannot map keyStore of type %s into type %s",
                    keyStore.getClass().getSimpleName(),
                    keyStoreSubclass.getSimpleName()
                )
            );
        }
        return null;
    }

    <T extends TrustStore> T castInto(TrustStore trustStore, Class<T> trustStoreSubclass) {
        if (trustStoreSubclass.isAssignableFrom(trustStore.getClass())) {
            return trustStoreSubclass.cast(trustStore);
        } else {
            fail(
                String.format(
                    "Cannot map trustStore of type %s into type %s",
                    trustStore.getClass().getSimpleName(),
                    trustStoreSubclass.getSimpleName()
                )
            );
        }
        return null;
    }
}
