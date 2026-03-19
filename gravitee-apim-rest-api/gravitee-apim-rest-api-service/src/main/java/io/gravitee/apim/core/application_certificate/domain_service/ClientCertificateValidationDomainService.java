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

import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import java.util.Date;

/**
 * Validates a PEM-encoded client certificate and extracts its metadata.
 * Core interface whose implementation lives in infra.
 *
 * @author GraviteeSource Team
 */
public interface ClientCertificateValidationDomainService {
    /**
     * Parses and validates the given PEM certificate, then returns its metadata.
     *
     * @param pemCertificate the PEM-encoded certificate string
     * @return extracted certificate information
     */
    CertificateInfo validate(String pemCertificate);

    /**
     * Validates a certificate for creation: PEM validation, duplicate fingerprint check, and date bounds.
     *
     * @param clientCertificate the certificate to validate
     * @param environmentId the environment ID for duplicate fingerprint lookup
     * @return extracted certificate information
     */
    CertificateInfo validateForCreation(ClientCertificate clientCertificate, String environmentId);

    record CertificateInfo(Date certificateExpiration, String subject, String issuer, String fingerprint) {}
}
