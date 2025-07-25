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

import io.gravitee.rest.api.model.configuration.application.registration.KeyStoreEntity;
import io.gravitee.rest.api.model.configuration.application.registration.TrustStoreEntity;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Base64;
import java.util.Enumeration;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

public final class SecureHttpClientUtils {

    public static CloseableHttpClient createHttpClient(TrustStoreEntity trustStore, KeyStoreEntity keyStore) throws Exception {
        HttpClientBuilder builder = HttpClients.custom();
        SSLContextBuilder sslBuilder = SSLContexts.custom();

        boolean hasTrustStore = trustStore != null && trustStore.getType() != TrustStoreEntity.Type.NONE;
        boolean hasKeyStore = keyStore != null && keyStore.getType() != KeyStoreEntity.Type.NONE;

        if (hasTrustStore) {
            sslBuilder.loadTrustMaterial(loadKeyStore(trustStore), null);
        }

        if (hasKeyStore) {
            KeyStore loadedKeyStore = loadKeyStore(keyStore);
            char[] password = resolveKeyPassword(keyStore);
            String alias = resolveAlias(loadedKeyStore, keyStore);
            sslBuilder.loadKeyMaterial(loadedKeyStore, password, (aliases, socket) -> alias);
        }

        if (hasTrustStore || hasKeyStore) {
            SSLContext sslContext = sslBuilder.build();
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[] { "TLSv1.2", "TLSv1.3" },
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier()
            );
            builder.setSSLSocketFactory(sslSocketFactory);
        }

        return builder.build();
    }

    public static KeyStore loadKeyStore(TrustStoreEntity trustStore) throws Exception {
        String type = trustStore.getType() != null ? trustStore.getType().name() : KeyStore.getDefaultType();
        char[] password = trustStore.getPassword() != null ? trustStore.getPassword().toCharArray() : null;

        KeyStore loadedKeyStore = KeyStore.getInstance(type);

        try (
            InputStream inputStream = (trustStore.getPath() != null && !trustStore.getPath().isEmpty())
                ? new FileInputStream(new File(trustStore.getPath()))
                : new ByteArrayInputStream(Base64.getDecoder().decode(trustStore.getContent()))
        ) {
            loadedKeyStore.load(inputStream, password);
        }

        return loadedKeyStore;
    }

    public static KeyStore loadKeyStore(KeyStoreEntity keyStore) throws Exception {
        String type = keyStore.getType().name();
        char[] storePassword = keyStore.getPassword().toCharArray();
        KeyStore loadedKeyStore = KeyStore.getInstance(type);

        try (
            InputStream inputStream = (keyStore.getPath() != null && !keyStore.getPath().isEmpty())
                ? new FileInputStream(new File(keyStore.getPath()))
                : new ByteArrayInputStream(Base64.getDecoder().decode(keyStore.getContent()))
        ) {
            loadedKeyStore.load(inputStream, storePassword);
        }

        return loadedKeyStore;
    }

    public static char[] resolveKeyPassword(KeyStoreEntity keyStore) {
        if (keyStore.getKeyPassword() != null && !keyStore.getKeyPassword().isEmpty()) {
            return keyStore.getKeyPassword().toCharArray();
        }
        return keyStore.getPassword().toCharArray();
    }

    public static String resolveAlias(KeyStore keyStore, KeyStoreEntity keyStoreEntity) throws KeyStoreException {
        // Use specified alias if available
        if (keyStoreEntity.getAlias() != null && !keyStoreEntity.getAlias().isEmpty()) {
            if (!keyStore.containsAlias(keyStoreEntity.getAlias())) {
                throw new KeyStoreException("Alias not found: " + keyStoreEntity.getAlias());
            }
            return keyStoreEntity.getAlias();
        }

        // Auto-detect first suitable alias
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                return alias;
            }
        }

        throw new KeyStoreException("No key entries found in keystore");
    }
}
