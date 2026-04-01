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
package io.gravitee.apim.core.application_certificate.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateDomainService;
import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateValidationDomainService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.rest.api.service.common.GraviteeContext;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateClientCertificateUseCase {

    private final ClientCertificateDomainService clientCertificateDomainService;
    private final ClientCertificateValidationDomainService clientCertificateValidationDomainService;

    public Output execute(Input input) {
        String environmentId = GraviteeContext.getCurrentEnvironment();
        var certInfo = clientCertificateValidationDomainService.validateForCreation(input.toCreate(), environmentId);

        ClientCertificate enriched = new ClientCertificate(
            null,
            null,
            null,
            input.toCreate().name(),
            input.toCreate().startsAt(),
            input.toCreate().endsAt(),
            null,
            null,
            input.toCreate().certificate(),
            certInfo.certificateExpiration(),
            certInfo.subject(),
            certInfo.issuer(),
            certInfo.fingerprint(),
            null,
            null
        );

        var certificate = clientCertificateDomainService.create(input.applicationId(), enriched);
        return new Output(certificate);
    }

    public record Input(String applicationId, ClientCertificate toCreate) {}

    public record Output(ClientCertificate clientCertificate) {}
}
