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
package io.gravitee.apim.infra.domain_service.application_certificates;

import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.domain_service.ClientCertificateValidationDomainService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.common.security.CertificateUtils;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.rest.api.service.exceptions.ClientCertificateAlreadyUsedException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateAuthorityException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateDateBoundsInvalidException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateEmptyException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateInvalidException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
@Service
public class ClientCertificateValidationDomainServiceImpl implements ClientCertificateValidationDomainService {

    private final ClientCertificateCrudService clientCertificateCrudService;

    @Override
    public CertificateInfo validate(String pemCertificate) {
        X509Certificate x509Certificate = parseCertificate(pemCertificate);

        String fingerprint = CertificateUtils.generateThumbprint(x509Certificate, "SHA-256");
        if (fingerprint == null) {
            throw new ClientCertificateInvalidException();
        }

        return new CertificateInfo(
            x509Certificate.getNotAfter(),
            x509Certificate.getSubjectX500Principal().getName(),
            x509Certificate.getIssuerX500Principal().getName(),
            fingerprint
        );
    }

    @Override
    public CertificateInfo validateForCreation(ClientCertificate clientCertificate, String environmentId) {
        CertificateInfo info = validate(clientCertificate.certificate());

        if (clientCertificateCrudService.existsByFingerprintAndActiveApplication(info.fingerprint(), environmentId)) {
            throw new ClientCertificateAlreadyUsedException(info.fingerprint());
        }

        validateDateBounds(clientCertificate);

        return info;
    }

    private X509Certificate parseCertificate(String pemCertificate) {
        Certificate[] certificates;
        try {
            certificates = KeyStoreUtils.loadPemCertificates(pemCertificate);
        } catch (Exception e) {
            throw new ClientCertificateInvalidException();
        }

        if (certificates.length == 0) {
            throw new ClientCertificateEmptyException();
        }
        if (certificates.length > 1) {
            log.debug("Certificate chain contains multiple certificates, using the first one");
        }

        X509Certificate certificate = (X509Certificate) certificates[0];
        if (certificate.getBasicConstraints() != -1) {
            throw new ClientCertificateAuthorityException();
        }
        return certificate;
    }

    private void validateDateBounds(ClientCertificate clientCertificate) {
        if (
            !Duration.between(
                Optional.ofNullable(clientCertificate.startsAt()).map(Date::toInstant).orElse(Instant.ofEpochMilli(Long.MIN_VALUE)),
                Optional.ofNullable(clientCertificate.endsAt()).map(Date::toInstant).orElse(Instant.ofEpochMilli(Long.MAX_VALUE))
            ).isPositive()
        ) {
            throw new ClientCertificateDateBoundsInvalidException();
        }
    }
}
