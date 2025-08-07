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

import io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.v4.ssl.none.NoneKeyStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore;

/**
 * Utility to convert between V2 KeyStore (io.gravitee.definition.model.ssl)
 * and V4 KeyStore (io.gravitee.definition.model.v4.ssl) objects.
 */
public final class KeyStoreMapper {

    private KeyStoreMapper() {
        // Utility class
    }

    public static io.gravitee.definition.model.v4.ssl.KeyStore convert(io.gravitee.definition.model.ssl.KeyStore v2KeyStore) {
        return switch (v2KeyStore) {
            case io.gravitee.definition.model.ssl.jks.JKSKeyStore v2Jks -> convertJKS(v2Jks);
            case io.gravitee.definition.model.ssl.pem.PEMKeyStore v2Pem -> convertPEM(v2Pem);
            case io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore v2Pkcs12 -> convertPKCS12(v2Pkcs12);
            case io.gravitee.definition.model.ssl.none.NoneKeyStore ignored -> convertNone();
            case null -> null;
            default -> throw new IllegalArgumentException("Unsupported key store type: " + v2KeyStore.getClass());
        };
    }

    private static JKSKeyStore convertJKS(io.gravitee.definition.model.ssl.jks.JKSKeyStore v2JKSKeyStore) {
        JKSKeyStore jKSKeyStore = new JKSKeyStore();
        jKSKeyStore.setPath(v2JKSKeyStore.getPath());
        jKSKeyStore.setContent(v2JKSKeyStore.getContent());
        jKSKeyStore.setPassword(v2JKSKeyStore.getPassword());
        return jKSKeyStore;
    }

    private static PEMKeyStore convertPEM(io.gravitee.definition.model.ssl.pem.PEMKeyStore v2PEMKeyStore) {
        PEMKeyStore pemKeyStore = new PEMKeyStore();
        pemKeyStore.setKeyPath(v2PEMKeyStore.getKeyPath());
        pemKeyStore.setKeyContent(v2PEMKeyStore.getKeyContent());
        pemKeyStore.setCertPath(v2PEMKeyStore.getCertPath());
        pemKeyStore.setCertContent(v2PEMKeyStore.getCertContent());
        return pemKeyStore;
    }

    private static PKCS12KeyStore convertPKCS12(io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore v2Pkcs12KeyStore) {
        PKCS12KeyStore pKCS12KeyStore = new PKCS12KeyStore();
        pKCS12KeyStore.setPath(v2Pkcs12KeyStore.getPath());
        pKCS12KeyStore.setPassword(v2Pkcs12KeyStore.getPassword());
        pKCS12KeyStore.setContent(v2Pkcs12KeyStore.getContent());
        return pKCS12KeyStore;
    }

    private static NoneKeyStore convertNone() {
        return new NoneKeyStore();
    }
}
