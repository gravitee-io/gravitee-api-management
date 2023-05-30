/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.rest.api.management.v2.rest.model.BaseKeyStore;
import io.gravitee.rest.api.management.v2.rest.model.JKSKeyStore;
import io.gravitee.rest.api.management.v2.rest.model.KeyStore;
import io.gravitee.rest.api.management.v2.rest.model.KeyStoreType;
import io.gravitee.rest.api.management.v2.rest.model.NoneKeyStore;
import io.gravitee.rest.api.management.v2.rest.model.PEMKeyStore;
import io.gravitee.rest.api.management.v2.rest.model.PKCS12KeyStore;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface KeyStoreMapper {
    KeyStoreMapper INSTANCE = Mappers.getMapper(KeyStoreMapper.class);

    // V4
    io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore mapToJKSKeyStoreV4(JKSKeyStore jksKeyStore);
    io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore mapToPKCS12KeyStoreV4(PKCS12KeyStore pkcs12KeyStore);
    io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore mapToPEMKeyStoreV4(PEMKeyStore pemKeyStore);
    io.gravitee.definition.model.v4.ssl.none.NoneKeyStore mapToNoneKeyStoreV4(NoneKeyStore noneKeyStore);

    default io.gravitee.definition.model.v4.ssl.KeyStore mapToKeyStoreV4(KeyStore keyStore) {
        if (keyStore == null) {
            return null;
        }

        BaseKeyStore baseKeyStore = (BaseKeyStore) keyStore.getActualInstance();
        switch (baseKeyStore.getType()) {
            case JKS:
                return mapToJKSKeyStoreV4(keyStore.getJKSKeyStore());
            case PKCS12:
                return mapToPKCS12KeyStoreV4(keyStore.getPKCS12KeyStore());
            case PEM:
                return mapToPEMKeyStoreV4(keyStore.getPEMKeyStore());
            case NONE:
            default:
                return mapToNoneKeyStoreV4(keyStore.getNoneKeyStore());
        }
    }

    JKSKeyStore mapFromJKSKeyStoreV4(io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore jksKeyStore);
    PKCS12KeyStore mapFromPKCS12KeyStoreV4(io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore pkcs12KeyStore);
    PEMKeyStore mapFromPEMKeyStoreV4(io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore pemKeyStore);
    NoneKeyStore mapFromNoneKeyStoreV4(io.gravitee.definition.model.v4.ssl.none.NoneKeyStore noneKeyStore);

    default KeyStore mapFromKeyStoreV4(io.gravitee.definition.model.v4.ssl.KeyStore keyStore) {
        if (keyStore == null) {
            return null;
        }

        switch (keyStore.getType()) {
            case JKS:
                return new KeyStore(mapFromJKSKeyStoreV4((io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore) keyStore));
            case PKCS12:
                return new KeyStore(mapFromPKCS12KeyStoreV4((io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore) keyStore));
            case PEM:
                return new KeyStore(mapFromPEMKeyStoreV4((io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore) keyStore));
            case NONE:
            default:
                return new KeyStore(mapFromNoneKeyStoreV4((io.gravitee.definition.model.v4.ssl.none.NoneKeyStore) keyStore));
        }
    }

    // V2
    io.gravitee.definition.model.ssl.jks.JKSKeyStore mapToJKSKeyStoreV2(JKSKeyStore jksKeyStore);
    io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore mapToPKCS12KeyStoreV2(PKCS12KeyStore pkcs12KeyStore);
    io.gravitee.definition.model.ssl.pem.PEMKeyStore mapToPEMKeyStoreV2(PEMKeyStore pemKeyStore);
    io.gravitee.definition.model.ssl.none.NoneKeyStore mapToNoneKeyStoreV2(NoneKeyStore noneKeyStore);

    default io.gravitee.definition.model.ssl.KeyStoreType mapToKeyStoreType(KeyStoreType type) {
        switch (type) {
            case PKCS12:
                return io.gravitee.definition.model.ssl.KeyStoreType.PKCS12;
            case JKS:
                return io.gravitee.definition.model.ssl.KeyStoreType.JKS;
            case PEM:
                return io.gravitee.definition.model.ssl.KeyStoreType.PEM;
            case NONE:
            default:
                return io.gravitee.definition.model.ssl.KeyStoreType.None;
        }
    }

    default io.gravitee.definition.model.ssl.KeyStore mapToKeyStoreV2(KeyStore keyStore) {
        if (keyStore == null) {
            return null;
        }

        BaseKeyStore baseKeyStore = (BaseKeyStore) keyStore.getActualInstance();
        switch (baseKeyStore.getType()) {
            case JKS:
                return mapToJKSKeyStoreV2(keyStore.getJKSKeyStore());
            case PKCS12:
                return mapToPKCS12KeyStoreV2(keyStore.getPKCS12KeyStore());
            case PEM:
                return mapToPEMKeyStoreV2(keyStore.getPEMKeyStore());
            case NONE:
            default:
                return mapToNoneKeyStoreV2(keyStore.getNoneKeyStore());
        }
    }

    JKSKeyStore mapFromJKSKeyStoreV2(io.gravitee.definition.model.ssl.jks.JKSKeyStore jksKeyStore);
    PKCS12KeyStore mapFromPKCS12KeyStoreV2(io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore pkcs12KeyStore);
    PEMKeyStore mapFromPEMKeyStoreV2(io.gravitee.definition.model.ssl.pem.PEMKeyStore pemKeyStore);
    NoneKeyStore mapFromNoneKeyStoreV2(io.gravitee.definition.model.ssl.none.NoneKeyStore noneKeyStore);

    default KeyStoreType mapFromKeyStoreType(io.gravitee.definition.model.ssl.KeyStoreType type) {
        switch (type) {
            case PKCS12:
                return KeyStoreType.PKCS12;
            case JKS:
                return KeyStoreType.JKS;
            case PEM:
                return KeyStoreType.PEM;
            case None:
            default:
                return KeyStoreType.NONE;
        }
    }

    default KeyStore mapFromKeyStoreV2(io.gravitee.definition.model.ssl.KeyStore keyStore) {
        if (keyStore == null) {
            return null;
        }

        switch (keyStore.getType()) {
            case JKS:
                return new KeyStore(mapFromJKSKeyStoreV2((io.gravitee.definition.model.ssl.jks.JKSKeyStore) keyStore));
            case PKCS12:
                return new KeyStore(mapFromPKCS12KeyStoreV2((io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore) keyStore));
            case PEM:
                return new KeyStore(mapFromPEMKeyStoreV2((io.gravitee.definition.model.ssl.pem.PEMKeyStore) keyStore));
            case None:
            default:
                return new KeyStore(mapFromNoneKeyStoreV2((io.gravitee.definition.model.ssl.none.NoneKeyStore) keyStore));
        }
    }
}
