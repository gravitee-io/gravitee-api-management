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

import io.gravitee.rest.api.management.v2.rest.model.BaseTrustStore;
import io.gravitee.rest.api.management.v2.rest.model.JKSTrustStore;
import io.gravitee.rest.api.management.v2.rest.model.NoneTrustStore;
import io.gravitee.rest.api.management.v2.rest.model.PEMTrustStore;
import io.gravitee.rest.api.management.v2.rest.model.PKCS12TrustStore;
import io.gravitee.rest.api.management.v2.rest.model.TrustStore;
import io.gravitee.rest.api.management.v2.rest.model.TrustStoreType;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TrustStoreMapper {
    TrustStoreMapper INSTANCE = Mappers.getMapper(TrustStoreMapper.class);

    // V4
    io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore mapToV4(JKSTrustStore jksTrustStore);
    io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore mapToV4(PKCS12TrustStore pkcs12TrustStore);
    io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore mapToV4(PEMTrustStore pemTrustStore);
    io.gravitee.definition.model.v4.ssl.none.NoneTrustStore mapToV4(NoneTrustStore noneTrustStore);

    default io.gravitee.definition.model.v4.ssl.TrustStore mapToV4(TrustStore trustStore) {
        if (trustStore == null) {
            return null;
        }

        BaseTrustStore baseTrustStore = (BaseTrustStore) trustStore.getActualInstance();
        switch (baseTrustStore.getType()) {
            case JKS:
                return mapToV4(trustStore.getJKSTrustStore());
            case PKCS12:
                return mapToV4(trustStore.getPKCS12TrustStore());
            case PEM:
                return mapToV4(trustStore.getPEMTrustStore());
            case NONE:
            default:
                return mapToV4(trustStore.getNoneTrustStore());
        }
    }

    JKSTrustStore map(io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore jksTrustStore);
    PKCS12TrustStore map(io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore pkcs12TrustStore);
    PEMTrustStore map(io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore pemTrustStore);
    NoneTrustStore map(io.gravitee.definition.model.v4.ssl.none.NoneTrustStore noneTrustStore);

    default TrustStore map(io.gravitee.definition.model.v4.ssl.TrustStore trustStore) {
        if (trustStore == null) {
            return null;
        }

        switch (trustStore.getType()) {
            case JKS:
                return new TrustStore(map((io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore) trustStore));
            case PKCS12:
                return new TrustStore(map((io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore) trustStore));
            case PEM:
                return new TrustStore(map((io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore) trustStore));
            case NONE:
            default:
                return new TrustStore(map((io.gravitee.definition.model.v4.ssl.none.NoneTrustStore) trustStore));
        }
    }

    // V2
    io.gravitee.definition.model.ssl.jks.JKSTrustStore mapToV2(JKSTrustStore jksTrustStore);
    io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore mapToV2(PKCS12TrustStore pkcs12TrustStore);
    io.gravitee.definition.model.ssl.pem.PEMTrustStore mapToV2(PEMTrustStore pemTrustStore);
    io.gravitee.definition.model.ssl.none.NoneTrustStore mapToV2(NoneTrustStore noneTrustStore);

    default io.gravitee.definition.model.ssl.TrustStoreType mapTrustStoreType(TrustStoreType type) {
        switch (type) {
            case PKCS12:
                return io.gravitee.definition.model.ssl.TrustStoreType.PKCS12;
            case JKS:
                return io.gravitee.definition.model.ssl.TrustStoreType.JKS;
            case PEM:
                return io.gravitee.definition.model.ssl.TrustStoreType.PEM;
            case NONE:
            default:
                return io.gravitee.definition.model.ssl.TrustStoreType.None;
        }
    }

    default io.gravitee.definition.model.ssl.TrustStore mapToV2(TrustStore trustStore) {
        if (trustStore == null) {
            return null;
        }

        BaseTrustStore baseTrustStore = (BaseTrustStore) trustStore.getActualInstance();
        switch (baseTrustStore.getType()) {
            case JKS:
                return mapToV2(trustStore.getJKSTrustStore());
            case PKCS12:
                return mapToV2(trustStore.getPKCS12TrustStore());
            case PEM:
                return mapToV2(trustStore.getPEMTrustStore());
            case NONE:
            default:
                return mapToV2(trustStore.getNoneTrustStore());
        }
    }

    JKSTrustStore map(io.gravitee.definition.model.ssl.jks.JKSTrustStore jksTrustStore);
    PKCS12TrustStore map(io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore pkcs12TrustStore);
    PEMTrustStore map(io.gravitee.definition.model.ssl.pem.PEMTrustStore pemTrustStore);
    NoneTrustStore map(io.gravitee.definition.model.ssl.none.NoneTrustStore noneTrustStore);

    default TrustStoreType mapTrustStoreType(io.gravitee.definition.model.ssl.TrustStoreType type) {
        switch (type) {
            case PKCS12:
                return TrustStoreType.PKCS12;
            case JKS:
                return TrustStoreType.JKS;
            case PEM:
                return TrustStoreType.PEM;
            case None:
            default:
                return TrustStoreType.NONE;
        }
    }

    default TrustStore map(io.gravitee.definition.model.ssl.TrustStore trustStore) {
        if (trustStore == null) {
            return null;
        }

        switch (trustStore.getType()) {
            case JKS:
                return new TrustStore(map((io.gravitee.definition.model.ssl.jks.JKSTrustStore) trustStore));
            case PKCS12:
                return new TrustStore(map((io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore) trustStore));
            case PEM:
                return new TrustStore(map((io.gravitee.definition.model.ssl.pem.PEMTrustStore) trustStore));
            case None:
            default:
                return new TrustStore(map((io.gravitee.definition.model.ssl.none.NoneTrustStore) trustStore));
        }
    }
}
