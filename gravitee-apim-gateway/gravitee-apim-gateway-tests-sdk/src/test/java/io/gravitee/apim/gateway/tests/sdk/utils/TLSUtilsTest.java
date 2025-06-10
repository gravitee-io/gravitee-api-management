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
package io.gravitee.apim.gateway.tests.sdk.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TLSUtilsTest {

    protected static final char[] PASSWORD = "awesome password".toCharArray();

    @Test
    void should_create_key_pair() throws Exception {
        TLSUtils.X509Pair localhost = TLSUtils.createKeyPair("localhost");
        assertThat(localhost.certificate()).isNotNull();
        assertThat(localhost.certificate().data()).isNotNull();
        assertThat(localhost.certificate().data().getSubjectX500Principal().getName()).startsWith("CN=localhost");
        assertThat(localhost.privateKey()).isNotNull();
        assertThat(localhost.privateKey().data()).isNotNull();
        assertThat(localhost.privateKey().data().getAlgorithm()).isEqualTo("RSA");

        assertThat(localhost.certificate().toPem()).startsWith("-----BEGIN CERTIFICATE").endsWith("END CERTIFICATE-----\n");
        assertThat(localhost.privateKey().toPem()).startsWith("-----BEGIN RSA PRIVATE KEY").endsWith("END RSA PRIVATE KEY-----\n");

        Path certTempFile = Files.createTempFile("test", "pem");
        localhost.certificate().writeToDisk(certTempFile);
        assertThat(certTempFile).content().startsWith("-----BEGIN CERTIFICATE").endsWith("END CERTIFICATE-----\n");
        Path keyTempFile = Files.createTempFile("test", "pem");
        localhost.privateKey().writeToDisk(keyTempFile);
        assertThat(keyTempFile).content().startsWith("-----BEGIN RSA PRIVATE KEY").endsWith("END RSA PRIVATE KEY-----\n");
    }

    @Test
    void should_create_and_update_keystore() throws Exception {
        TLSUtils.X509Pair foo = TLSUtils.createKeyPair("foo");
        KeyStore keyStore = TLSUtils.createKeyStore("foo", foo, PASSWORD);
        assertThat(keyStore.containsAlias("foo")).isTrue();
        assertThat(keyStore.isKeyEntry("foo")).isTrue();
        assertThat(keyStore.getCertificate("foo")).isInstanceOf(X509Certificate.class);

        TLSUtils.X509Pair bar = TLSUtils.createKeyPair("bar");
        TLSUtils.appendToKeyStore(keyStore, "bar", bar, PASSWORD);
        assertThat(keyStore.size()).isEqualTo(2);
        assertThat(keyStore.containsAlias("bar")).isTrue();
        assertThat(keyStore.isKeyEntry("bar")).isTrue();
        assertThat(keyStore.getCertificate("bar")).isInstanceOf(X509Certificate.class);

        TLSUtils.appendToKeyStore(keyStore, "barCert", bar.certificate(), null);
        assertThat(keyStore.size()).isEqualTo(3);
        assertThat(keyStore.containsAlias("barCert")).isTrue();
        assertThat(keyStore.isKeyEntry("barCert")).isFalse();

        assertThatCode(() -> TLSUtils.appendToKeyStore(keyStore, "error", "a simple string", PASSWORD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("a simple string");
        assertThatCode(() -> TLSUtils.appendToKeyStore(keyStore, "bar", bar, PASSWORD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bar");
    }

    @Test
    void should_convert_keystore_to_truststore() throws Exception {
        TLSUtils.X509Pair foo = TLSUtils.createKeyPair("foo");
        KeyStore keyStore = TLSUtils.createKeyStore("foo", foo, PASSWORD);
        assertThat(keyStore.isKeyEntry("foo")).isTrue();

        KeyStore trustStore = TLSUtils.toTrustStore(keyStore, PASSWORD);
        assertThat(keyStore.size()).isEqualTo(trustStore.size());
        assertThat(trustStore.isKeyEntry("foo")).isFalse();
        assertThatCode(() -> trustStore.getEntry("foo", null)).doesNotThrowAnyException();
    }
}
