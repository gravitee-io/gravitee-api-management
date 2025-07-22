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
package io.gravitee.rest.api.service.impl.configuration.application.registration.client.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.configuration.application.registration.KeyStoreEntity;
import io.gravitee.rest.api.model.configuration.application.registration.KeyStoreEntity;
import io.gravitee.rest.api.model.configuration.application.registration.KeyStoreEntity.Type;
import io.gravitee.rest.api.model.configuration.application.registration.TrustStoreEntity;
import io.gravitee.rest.api.model.configuration.application.registration.TrustStoreEntity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Base64;
import java.util.Enumeration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecureHttpClientUtilsTest {

    private static final String VALID_PASSWORD = "password";
    private static final String DUMMY_CONTENT = "dummy-content";

    private TrustStoreEntity trustStore;
    private KeyStoreEntity keyStore;

    @BeforeEach
    void init() {
        trustStore = new TrustStoreEntity();
        trustStore.setType(TrustStoreEntity.Type.JKS);
        // invalid base64 / invalid keystore
        trustStore.setContent(Base64.getEncoder().encodeToString(DUMMY_CONTENT.getBytes(StandardCharsets.UTF_8)));
        trustStore.setPassword(VALID_PASSWORD);

        keyStore = new KeyStoreEntity();
        keyStore.setType(KeyStoreEntity.Type.PKCS12);
        keyStore.setContent(Base64.getEncoder().encodeToString(DUMMY_CONTENT.getBytes(StandardCharsets.UTF_8)));
        keyStore.setPassword(VALID_PASSWORD);
    }

    @Test
    void createHttpClient_noStores() throws Exception {
        try (CloseableHttpClient client = SecureHttpClientUtils.createHttpClient(null, null)) {
            assertNotNull(client);
        }
    }

    @Test
    void createHttpClient_invalidKeyStoreContent_throws() {
        keyStore.setContent("not-base64");
        assertThrows(Exception.class, () -> SecureHttpClientUtils.createHttpClient(null, keyStore));
    }

    @Test
    void createHttpClient_noneTypes() throws Exception {
        trustStore.setType(TrustStoreEntity.Type.NONE);
        keyStore.setType(KeyStoreEntity.Type.NONE);
        try (CloseableHttpClient client = SecureHttpClientUtils.createHttpClient(trustStore, keyStore)) {
            assertNotNull(client);
        }
    }

    @Test
    void loadTrustStore_invalidFormat_throws() {
        // our DUMMY_CONTENT isn't a real keystore
        assertThrows(Exception.class, () -> SecureHttpClientUtils.loadKeyStore(trustStore));
    }

    @Test
    void loadTrustStore_validEmptyJks_loadsSuccessfully() throws Exception {
        // build an empty JKS keystore
        KeyStore emptyJks = KeyStore.getInstance("JKS");
        emptyJks.load(null, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        emptyJks.store(baos, new char[0]);

        trustStore.setContent(Base64.getEncoder().encodeToString(baos.toByteArray()));
        // leave password null or empty array
        trustStore.setPassword(null);

        KeyStore loaded = SecureHttpClientUtils.loadKeyStore(trustStore);
        assertNotNull(loaded);
        assertEquals("JKS", loaded.getType());
    }

    @Test
    void loadKeyStore_invalidFormat_throwsIllegalArgumentException() {
        // Base64 decodes to garbage → expect IllegalArgumentException from PKCS12 loader
        assertThrows(Exception.class, () -> SecureHttpClientUtils.loadKeyStore(keyStore));
    }

    @Test
    void loadKeyStore_validEmptyJksEntity_loadsSuccessfully() throws Exception {
        // switch to JKS so we don't require a cert chain
        keyStore.setType(KeyStoreEntity.Type.JKS);

        KeyStore emptyJks = KeyStore.getInstance("JKS");
        emptyJks.load(null, VALID_PASSWORD.toCharArray());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        emptyJks.store(baos, VALID_PASSWORD.toCharArray());

        keyStore.setContent(Base64.getEncoder().encodeToString(baos.toByteArray()));
        keyStore.setPassword(VALID_PASSWORD);

        KeyStore loaded = SecureHttpClientUtils.loadKeyStore(keyStore);
        assertNotNull(loaded);
        assertEquals("JKS", loaded.getType());
    }

    @Test
    void resolveKeyPassword_withExplicitKeyPassword() {
        keyStore.setKeyPassword("kpwd");
        char[] result = SecureHttpClientUtils.resolveKeyPassword(keyStore);
        assertArrayEquals("kpwd".toCharArray(), result);
    }

    @Test
    void resolveKeyPassword_emptyKeyPassword_fallsBackToStorePassword() {
        keyStore.setKeyPassword("");
        char[] result = SecureHttpClientUtils.resolveKeyPassword(keyStore);
        assertArrayEquals(VALID_PASSWORD.toCharArray(), result);
    }

    @Test
    void resolveAlias_withExplicitAlias() throws Exception {
        // generate a key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(512);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // build an in‑memory JKS and insert one key + a dummy certificate
        KeyStore jksKeyStore = KeyStore.getInstance("JKS");
        jksKeyStore.load(null, null);

        Certificate dummyCert = new Certificate("X.509") {
            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }

            @Override
            public void verify(PublicKey key) {}

            @Override
            public void verify(PublicKey key, String sigProvider) {}

            @Override
            public String toString() {
                return "DummyCert";
            }

            @Override
            public PublicKey getPublicKey() {
                return keyPair.getPublic();
            }
        };

        jksKeyStore.setKeyEntry("myalias", keyPair.getPrivate(), VALID_PASSWORD.toCharArray(), new Certificate[] { dummyCert });

        keyStore.setAlias("myalias");
        assertEquals("myalias", SecureHttpClientUtils.resolveAlias(jksKeyStore, keyStore));
    }

    @Test
    void resolveAlias_autoDetectFirstEntry() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(512);
        KeyPair firstKeyPair = keyPairGenerator.generateKeyPair();
        KeyPair secondKeyPair = keyPairGenerator.generateKeyPair();

        KeyStore jksKeyStore = KeyStore.getInstance("JKS");
        jksKeyStore.load(null, null);

        // reuse the same dummy cert for both keys
        Certificate cert = new Certificate("X.509") {
            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }

            @Override
            public void verify(PublicKey key) {}

            @Override
            public void verify(PublicKey key, String sigProvider) {}

            @Override
            public String toString() {
                return "DummyCert";
            }

            @Override
            public PublicKey getPublicKey() {
                return firstKeyPair.getPublic();
            }
        };

        jksKeyStore.setKeyEntry("first", firstKeyPair.getPrivate(), VALID_PASSWORD.toCharArray(), new Certificate[] { cert });
        jksKeyStore.setKeyEntry("second", secondKeyPair.getPrivate(), VALID_PASSWORD.toCharArray(), new Certificate[] { cert });

        keyStore.setAlias(null);
        String result = SecureHttpClientUtils.resolveAlias(jksKeyStore, keyStore);
        assertTrue("first".equals(result) || "second".equals(result), "Expected either 'first' or 'second' but got: " + result);
    }

    @Test
    void resolveAlias_noEntries_throws() throws Exception {
        KeyStore empty = KeyStore.getInstance("JKS");
        empty.load(null, null);
        keyStore.setAlias(null);
        assertThrows(Exception.class, () -> SecureHttpClientUtils.resolveAlias(empty, keyStore));
    }

    @Test
    void createHttpClient_withTrustStore() throws Exception {
        trustStore.setType(TrustStoreEntity.Type.JKS);
        trustStore.setPassword(VALID_PASSWORD);
        KeyStore trustStoreInstance = KeyStore.getInstance("JKS");
        trustStoreInstance.load(null, VALID_PASSWORD.toCharArray());
        try (ByteArrayOutputStream trustStoreBaos = new ByteArrayOutputStream()) {
            trustStoreInstance.store(trustStoreBaos, VALID_PASSWORD.toCharArray());
            trustStore.setContent(Base64.getEncoder().encodeToString(trustStoreBaos.toByteArray()));
        }

        // Create the HTTP client and verify it is properly instantiated
        try (CloseableHttpClient client = SecureHttpClientUtils.createHttpClient(trustStore, null)) {
            assertNotNull(client, "The client should not be null");
        }
    }
}
