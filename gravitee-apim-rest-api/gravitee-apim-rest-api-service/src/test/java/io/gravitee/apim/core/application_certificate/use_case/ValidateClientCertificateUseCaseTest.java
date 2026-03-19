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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateValidationDomainService;
import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateValidationDomainService.CertificateInfo;
import io.gravitee.rest.api.service.exceptions.ClientCertificateInvalidException;
import java.util.Date;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateClientCertificateUseCaseTest {

    private final ClientCertificateValidationDomainService validationService = mock(ClientCertificateValidationDomainService.class);
    private final ValidateClientCertificateUseCase useCase = new ValidateClientCertificateUseCase(validationService);

    @Test
    void should_return_certificate_info_on_valid_input() {
        var expiration = new Date();
        var certificateInfo = new CertificateInfo(expiration, "CN=test", "CN=issuer", "sha256-fingerprint");
        when(validationService.validate("valid-pem")).thenReturn(certificateInfo);

        var output = useCase.execute(new ValidateClientCertificateUseCase.Input("valid-pem"));

        assertThat(output.certificateInfo()).isEqualTo(certificateInfo);
    }

    @Test
    void should_propagate_exception_from_domain_service() {
        when(validationService.validate("invalid-pem")).thenThrow(new ClientCertificateInvalidException());

        assertThatThrownBy(() -> useCase.execute(new ValidateClientCertificateUseCase.Input("invalid-pem"))).isInstanceOf(
            ClientCertificateInvalidException.class
        );
    }
}
