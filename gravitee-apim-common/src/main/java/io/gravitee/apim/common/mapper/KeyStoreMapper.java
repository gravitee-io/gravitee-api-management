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

import io.gravitee.definition.model.v4.ssl.KeyStore;
import io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.v4.ssl.none.NoneKeyStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface KeyStoreMapper {
    KeyStoreMapper INSTANCE = Mappers.getMapper(KeyStoreMapper.class);

    default io.gravitee.node.vertx.client.ssl.KeyStore map(KeyStore keystore) {
        if (keystore == null) {
            return null;
        }

        return switch (KeyStoreImplementation.get(keystore.getClass().getName())) {
            case PEM_KEY_STORE -> map((PEMKeyStore) keystore);
            case JKS_KEY_STORE -> map((JKSKeyStore) keystore);
            case PKCS_12_KEY_STORE -> map((PKCS12KeyStore) keystore);
            case NONE_KEY_STORE -> map((NoneKeyStore) keystore);
        };
    }

    io.gravitee.node.vertx.client.ssl.pem.PEMKeyStore map(PEMKeyStore keystore);
    io.gravitee.node.vertx.client.ssl.jks.JKSKeyStore map(JKSKeyStore keystore);
    io.gravitee.node.vertx.client.ssl.pkcs12.PKCS12KeyStore map(PKCS12KeyStore keystore);
    io.gravitee.node.vertx.client.ssl.none.NoneKeyStore map(NoneKeyStore keystore);

    enum KeyStoreImplementation {
        PEM_KEY_STORE(PEMKeyStore.class.getName()),
        JKS_KEY_STORE(JKSKeyStore.class.getName()),
        PKCS_12_KEY_STORE(PKCS12KeyStore.class.getName()),
        NONE_KEY_STORE(NoneKeyStore.class.getName());

        private final String className;

        KeyStoreImplementation(String className) {
            this.className = className;
        }

        private static final Map<String, KeyStoreImplementation> ENUM_MAP;

        static {
            Map<String, KeyStoreImplementation> map = new ConcurrentHashMap<String, KeyStoreImplementation>();
            for (KeyStoreImplementation instance : KeyStoreImplementation.values()) {
                map.put(instance.className, instance);
            }
            ENUM_MAP = Collections.unmodifiableMap(map);
        }

        public static KeyStoreImplementation get(String name) {
            return ENUM_MAP.get(name);
        }
    }
}
