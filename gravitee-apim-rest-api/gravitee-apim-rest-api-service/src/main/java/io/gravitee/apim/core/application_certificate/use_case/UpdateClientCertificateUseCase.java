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
import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateDomainService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateClientCertificateUseCase {

    private final ClientCertificateCrudService clientCertificateCrudService;
    private final ClientCertificateDomainService clientCertificateDomainService;

    public Output execute(Input input) {
        if (input.applicationId().isPresent()) {
            ClientCertificate existing = clientCertificateCrudService.findById(input.clientCertificateId());
            if (!input.applicationId().get().equals(existing.applicationId())) {
                throw new ClientCertificateNotFoundException(input.clientCertificateId());
            }
        }
        var certificate = clientCertificateDomainService.update(input.clientCertificateId(), input.toUpdate());
        return new Output(certificate);
    }

    public record Input(Optional<String> applicationId, String clientCertificateId, ClientCertificate toUpdate) {
        public Input(String clientCertificateId, ClientCertificate toUpdate) {
            this(Optional.empty(), clientCertificateId, toUpdate);
        }
    }

    public record Output(ClientCertificate clientCertificate) {}
}
