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

import io.gravitee.definition.model.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.ssl.none.NoneTrustStore;
import io.gravitee.definition.model.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

@UtilityClass
public class TrustStoreMigration {

    public static io.gravitee.definition.model.v4.ssl.@Nullable TrustStore convert(
        io.gravitee.definition.model.ssl.@Nullable TrustStore v2TrustStore
    ) {
        return switch (v2TrustStore) {
            case JKSTrustStore v2Jks -> convertJks(v2Jks);
            case PEMTrustStore v2Pem -> convertPem(v2Pem);
            case PKCS12TrustStore v2Pkcs12 -> convertPkcs12(v2Pkcs12);
            case NoneTrustStore ignored -> convertNone();
            case null -> null;
            default -> throw new IllegalArgumentException("Unsupported trust store type: " + v2TrustStore.getClass());
        };
    }

    private static io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore convertJks(JKSTrustStore v2Jks) {
        return new io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore(v2Jks.getPath(), v2Jks.getContent(), v2Jks.getPassword(), null);
    }

    private static io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore convertPem(PEMTrustStore v2Pem) {
        return new io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore(v2Pem.getPath(), v2Pem.getContent());
    }

    private static io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore convertPkcs12(PKCS12TrustStore v2Pkcs12) {
        io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore pKCS12TrustStore =
            new io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore();
        pKCS12TrustStore.setPath(v2Pkcs12.getPath());
        pKCS12TrustStore.setContent(v2Pkcs12.getContent());
        pKCS12TrustStore.setPassword(v2Pkcs12.getPassword());
        return pKCS12TrustStore;
    }

    private static io.gravitee.definition.model.v4.ssl.none.NoneTrustStore convertNone() {
        return new io.gravitee.definition.model.v4.ssl.none.NoneTrustStore();
    }
}
