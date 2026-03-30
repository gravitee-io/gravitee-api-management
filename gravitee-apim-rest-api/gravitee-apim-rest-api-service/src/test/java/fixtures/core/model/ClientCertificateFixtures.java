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
package fixtures.core.model;

import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import java.util.Date;
import java.util.function.Supplier;

public class ClientCertificateFixtures {

    private ClientCertificateFixtures() {}

    private static final Supplier<ClientCertificate> BASE = () ->
        new ClientCertificate(
            "certificate-id",
            "cross-certificate-id",
            "application-id",
            "Certificate",
            new Date(),
            new Date(),
            new Date(),
            new Date(),
            "PEM_CONTENT",
            new Date(),
            "CN=Test",
            "CN=Issuer",
            "fingerprint",
            "environment-id",
            ClientCertificateStatus.ACTIVE
        );

    public static ClientCertificate aClientCertificate() {
        return BASE.get();
    }

    public static ClientCertificate aClientCertificate(String id, String applicationId, String fingerprint) {
        return new ClientCertificate(
            id,
            "cross-" + id,
            applicationId,
            "Certificate " + id,
            new Date(),
            new Date(),
            new Date(),
            new Date(),
            "PEM_CONTENT",
            new Date(),
            "CN=Test",
            "CN=Issuer",
            fingerprint,
            "environment-id",
            ClientCertificateStatus.ACTIVE
        );
    }
}
