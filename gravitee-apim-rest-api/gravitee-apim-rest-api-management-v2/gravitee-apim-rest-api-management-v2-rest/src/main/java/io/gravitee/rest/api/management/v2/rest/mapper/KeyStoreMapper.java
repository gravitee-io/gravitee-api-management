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
    io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore mapToV4(JKSKeyStore jksKeyStore);
    io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore mapToV4(PKCS12KeyStore pkcs12KeyStore);
    io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore mapToV4(PEMKeyStore pemKeyStore);
    io.gravitee.definition.model.v4.ssl.none.NoneKeyStore mapToV4(NoneKeyStore noneKeyStore);

    default io.gravitee.definition.model.v4.ssl.KeyStore mapToV4(KeyStore keyStore) {
        if (keyStore == null) {
            return null;
        }

        BaseKeyStore baseKeyStore = (BaseKeyStore) keyStore.getActualInstance();
        switch (baseKeyStore.getType()) {
            case JKS:
                return mapToV4(keyStore.getJKSKeyStore());
            case PKCS12:
                return mapToV4(keyStore.getPKCS12KeyStore());
            case PEM:
                return mapToV4(keyStore.getPEMKeyStore());
            case NONE:
            default:
                return mapToV4(keyStore.getNoneKeyStore());
        }
    }

    JKSKeyStore map(io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore jksKeyStore);
    PKCS12KeyStore map(io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore pkcs12KeyStore);
    PEMKeyStore map(io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore pemKeyStore);
    NoneKeyStore map(io.gravitee.definition.model.v4.ssl.none.NoneKeyStore noneKeyStore);

    default KeyStore map(io.gravitee.definition.model.v4.ssl.KeyStore keyStore) {
        if (keyStore == null) {
            return null;
        }

        switch (keyStore.getType()) {
            case JKS:
                return new KeyStore(map((io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore) keyStore));
            case PKCS12:
                return new KeyStore(map((io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore) keyStore));
            case PEM:
                return new KeyStore(map((io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore) keyStore));
            case NONE:
            default:
                return new KeyStore(map((io.gravitee.definition.model.v4.ssl.none.NoneKeyStore) keyStore));
        }
    }

    // V2
    io.gravitee.definition.model.ssl.jks.JKSKeyStore mapToV2(JKSKeyStore jksKeyStore);
    io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore mapToV2(PKCS12KeyStore pkcs12KeyStore);
    io.gravitee.definition.model.ssl.pem.PEMKeyStore mapToV2(PEMKeyStore pemKeyStore);
    io.gravitee.definition.model.ssl.none.NoneKeyStore mapToV2(NoneKeyStore noneKeyStore);

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

    default io.gravitee.definition.model.ssl.KeyStore mapToV2(KeyStore keyStore) {
        if (keyStore == null) {
            return null;
        }

        BaseKeyStore baseKeyStore = (BaseKeyStore) keyStore.getActualInstance();
        switch (baseKeyStore.getType()) {
            case JKS:
                return mapToV2(keyStore.getJKSKeyStore());
            case PKCS12:
                return mapToV2(keyStore.getPKCS12KeyStore());
            case PEM:
                return mapToV2(keyStore.getPEMKeyStore());
            case NONE:
            default:
                return mapToV2(keyStore.getNoneKeyStore());
        }
    }

    JKSKeyStore map(io.gravitee.definition.model.ssl.jks.JKSKeyStore jksKeyStore);
    PKCS12KeyStore map(io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore pkcs12KeyStore);
    PEMKeyStore map(io.gravitee.definition.model.ssl.pem.PEMKeyStore pemKeyStore);
    NoneKeyStore map(io.gravitee.definition.model.ssl.none.NoneKeyStore noneKeyStore);

    default KeyStoreType mapKeyStoreType(io.gravitee.definition.model.ssl.KeyStoreType type) {
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

    default KeyStore map(io.gravitee.definition.model.ssl.KeyStore keyStore) {
        if (keyStore == null) {
            return null;
        }

        switch (keyStore.getType()) {
            case JKS:
                return new KeyStore(map((io.gravitee.definition.model.ssl.jks.JKSKeyStore) keyStore));
            case PKCS12:
                return new KeyStore(map((io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore) keyStore));
            case PEM:
                return new KeyStore(map((io.gravitee.definition.model.ssl.pem.PEMKeyStore) keyStore));
            case None:
            default:
                return new KeyStore(map((io.gravitee.definition.model.ssl.none.NoneKeyStore) keyStore));
        }
    }
}
