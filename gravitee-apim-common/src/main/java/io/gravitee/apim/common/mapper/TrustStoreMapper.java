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

import io.gravitee.definition.model.v4.ssl.TrustStore;
import io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.v4.ssl.none.NoneTrustStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore;
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
public interface TrustStoreMapper {
    TrustStoreMapper INSTANCE = Mappers.getMapper(TrustStoreMapper.class);

    default io.gravitee.node.vertx.client.ssl.TrustStore map(TrustStore trustStore) {
        if (trustStore == null) {
            return null;
        }

        return switch (TrustStoreMapper.TrustStoreImplementation.get(trustStore.getClass().getName())) {
            case PEM_TRUST_STORE -> map((PEMTrustStore) trustStore);
            case JKS_TRUST_STORE -> map((JKSTrustStore) trustStore);
            case PKCS_12_TRUST_STORE -> map((PKCS12TrustStore) trustStore);
            case NONE_TRUST_STORE -> map((NoneTrustStore) trustStore);
        };
    }

    io.gravitee.node.vertx.client.ssl.pem.PEMTrustStore map(PEMTrustStore trustStore);
    io.gravitee.node.vertx.client.ssl.jks.JKSTrustStore map(JKSTrustStore trustStore);
    io.gravitee.node.vertx.client.ssl.pkcs12.PKCS12TrustStore map(PKCS12TrustStore trustStore);
    io.gravitee.node.vertx.client.ssl.none.NoneTrustStore map(NoneTrustStore trustStore);

    enum TrustStoreImplementation {
        PEM_TRUST_STORE(PEMTrustStore.class.getName()),
        JKS_TRUST_STORE(JKSTrustStore.class.getName()),
        PKCS_12_TRUST_STORE(PKCS12TrustStore.class.getName()),
        NONE_TRUST_STORE(NoneTrustStore.class.getName());

        private final String className;

        TrustStoreImplementation(String className) {
            this.className = className;
        }

        private static final Map<String, TrustStoreMapper.TrustStoreImplementation> ENUM_MAP;

        static {
            Map<String, TrustStoreMapper.TrustStoreImplementation> map = new ConcurrentHashMap<
                String,
                TrustStoreMapper.TrustStoreImplementation
            >();
            for (TrustStoreMapper.TrustStoreImplementation instance : TrustStoreMapper.TrustStoreImplementation.values()) {
                map.put(instance.className, instance);
            }
            ENUM_MAP = Collections.unmodifiableMap(map);
        }

        public static TrustStoreMapper.TrustStoreImplementation get(String name) {
            return ENUM_MAP.get(name);
        }
    }
}
