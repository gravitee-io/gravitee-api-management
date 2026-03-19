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
package io.gravitee.apim.core.application_certificate.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.common.security.CertificateUtils;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.rest.api.service.exceptions.ClientCertificateAuthorityException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateEmptyException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateInvalidException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import lombok.CustomLog;

/**
 * Validates a PEM-encoded client certificate and extracts its metadata.
 *
 * @author GraviteeSource Team
 */
@DomainService
@CustomLog
public class ClientCertificateValidationDomainService {

    /**
     * Parses and validates the given PEM certificate, then returns its metadata.
     *
     * @param pemCertificate the PEM-encoded certificate string
     * @return extracted certificate information
     * @throws ClientCertificateInvalidException if the PEM cannot be parsed or fingerprint generation fails
     * @throws ClientCertificateEmptyException if the PEM contains no certificates
     * @throws ClientCertificateAuthorityException if the certificate is a CA certificate
     */
    public CertificateInfo validate(String pemCertificate) {
        Certificate[] certificates;
        try {
            certificates = KeyStoreUtils.loadPemCertificates(pemCertificate);
        } catch (Exception e) {
            throw new ClientCertificateInvalidException(e);
        }

        if (certificates.length == 0) {
            throw new ClientCertificateEmptyException();
        }
        if (certificates.length > 1) {
            log.debug("Certificate chain contains multiple certificates, using the first one");
        }

        X509Certificate x509Certificate = (X509Certificate) certificates[0];

        if (x509Certificate.getBasicConstraints() != -1) {
            throw new ClientCertificateAuthorityException();
        }

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

    public record CertificateInfo(Date certificateExpiration, String subject, String issuer, String fingerprint) {}
}
