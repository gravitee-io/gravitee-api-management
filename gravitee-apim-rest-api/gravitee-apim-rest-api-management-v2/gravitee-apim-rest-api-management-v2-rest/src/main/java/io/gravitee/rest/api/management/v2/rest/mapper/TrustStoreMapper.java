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
    io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore mapToJKSTrustStoreV4(JKSTrustStore jksTrustStore);
    io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore mapToPKCS12TrustStoreV4(PKCS12TrustStore pkcs12TrustStore);
    io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore mapToPEMTrustStoreV4(PEMTrustStore pemTrustStore);
    io.gravitee.definition.model.v4.ssl.none.NoneTrustStore mapToNoneTrustStoreV4(NoneTrustStore noneTrustStore);

    default io.gravitee.definition.model.v4.ssl.TrustStore mapToTrustStoreV4(TrustStore trustStore) {
        if (trustStore == null) {
            return null;
        }

        BaseTrustStore baseTrustStore = (BaseTrustStore) trustStore.getActualInstance();
        switch (baseTrustStore.getType()) {
            case JKS:
                return mapToJKSTrustStoreV4(trustStore.getJKSTrustStore());
            case PKCS12:
                return mapToPKCS12TrustStoreV4(trustStore.getPKCS12TrustStore());
            case PEM:
                return mapToPEMTrustStoreV4(trustStore.getPEMTrustStore());
            case NONE:
            default:
                return mapToNoneTrustStoreV4(trustStore.getNoneTrustStore());
        }
    }

    JKSTrustStore mapFromJKSTrustStoreV4(io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore jksTrustStore);
    PKCS12TrustStore mapFromPKCS12TrustStoreV4(io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore pkcs12TrustStore);
    PEMTrustStore mapFromPEMTrustStoreV4(io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore pemTrustStore);
    NoneTrustStore mapFromNoneTrustStoreV4(io.gravitee.definition.model.v4.ssl.none.NoneTrustStore noneTrustStore);

    default TrustStore mapFromTrustStoreV4(io.gravitee.definition.model.v4.ssl.TrustStore trustStore) {
        if (trustStore == null) {
            return null;
        }

        switch (trustStore.getType()) {
            case JKS:
                return new TrustStore(mapFromJKSTrustStoreV4((io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore) trustStore));
            case PKCS12:
                return new TrustStore(mapFromPKCS12TrustStoreV4((io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore) trustStore));
            case PEM:
                return new TrustStore(mapFromPEMTrustStoreV4((io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore) trustStore));
            case NONE:
            default:
                return new TrustStore(mapFromNoneTrustStoreV4((io.gravitee.definition.model.v4.ssl.none.NoneTrustStore) trustStore));
        }
    }

    // V2
    io.gravitee.definition.model.ssl.jks.JKSTrustStore mapToJKSTrustStoreV2(JKSTrustStore jksTrustStore);
    io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore mapToPKCS12TrustStoreV2(PKCS12TrustStore pkcs12TrustStore);
    io.gravitee.definition.model.ssl.pem.PEMTrustStore mapToPEMTrustStoreV2(PEMTrustStore pemTrustStore);
    io.gravitee.definition.model.ssl.none.NoneTrustStore mapToNoneTrustStoreV2(NoneTrustStore noneTrustStore);

    default io.gravitee.definition.model.ssl.TrustStoreType mapToTrustStoreType(TrustStoreType type) {
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

    default io.gravitee.definition.model.ssl.TrustStore mapToTrustStoreV2(TrustStore trustStore) {
        if (trustStore == null) {
            return null;
        }

        BaseTrustStore baseTrustStore = (BaseTrustStore) trustStore.getActualInstance();
        switch (baseTrustStore.getType()) {
            case JKS:
                return mapToJKSTrustStoreV2(trustStore.getJKSTrustStore());
            case PKCS12:
                return mapToPKCS12TrustStoreV2(trustStore.getPKCS12TrustStore());
            case PEM:
                return mapToPEMTrustStoreV2(trustStore.getPEMTrustStore());
            case NONE:
            default:
                return mapToNoneTrustStoreV2(trustStore.getNoneTrustStore());
        }
    }

    JKSTrustStore mapFromJKSTrustStoreV2(io.gravitee.definition.model.ssl.jks.JKSTrustStore jksTrustStore);
    PKCS12TrustStore mapFromPKCS12TrustStoreV2(io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore pkcs12TrustStore);
    PEMTrustStore mapFromPEMTrustStoreV2(io.gravitee.definition.model.ssl.pem.PEMTrustStore pemTrustStore);
    NoneTrustStore mapFromNoneTrustStoreV2(io.gravitee.definition.model.ssl.none.NoneTrustStore noneTrustStore);

    default TrustStoreType mapFromTrustStoreType(io.gravitee.definition.model.ssl.TrustStoreType type) {
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

    default TrustStore mapFromTrustStoreV2(io.gravitee.definition.model.ssl.TrustStore trustStore) {
        if (trustStore == null) {
            return null;
        }

        switch (trustStore.getType()) {
            case JKS:
                return new TrustStore(mapFromJKSTrustStoreV2((io.gravitee.definition.model.ssl.jks.JKSTrustStore) trustStore));
            case PKCS12:
                return new TrustStore(mapFromPKCS12TrustStoreV2((io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore) trustStore));
            case PEM:
                return new TrustStore(mapFromPEMTrustStoreV2((io.gravitee.definition.model.ssl.pem.PEMTrustStore) trustStore));
            case None:
            default:
                return new TrustStore(mapFromNoneTrustStoreV2((io.gravitee.definition.model.ssl.none.NoneTrustStore) trustStore));
        }
    }
}
