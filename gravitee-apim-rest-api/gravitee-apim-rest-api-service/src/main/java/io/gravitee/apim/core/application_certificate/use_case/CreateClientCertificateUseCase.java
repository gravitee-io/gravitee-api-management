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
import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.domain_service.ApplicationCertificatesUpdateDomainService;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateClientCertificateUseCase {

    private final ClientCertificateCrudService clientCertificateCrudService;
    private final ApplicationCertificatesUpdateDomainService applicationCertificatesUpdateDomainService;

    public Output execute(Input input) {
        ClientCertificate certificate = clientCertificateCrudService.create(input.applicationId(), input.createClientCertificate());
        applicationCertificatesUpdateDomainService.updateActiveMTLSSubscriptions(input.applicationId());
        return new Output(certificate);
    }

    public record Input(String applicationId, CreateClientCertificate createClientCertificate) {}

    public record Output(ClientCertificate clientCertificate) {}
}
