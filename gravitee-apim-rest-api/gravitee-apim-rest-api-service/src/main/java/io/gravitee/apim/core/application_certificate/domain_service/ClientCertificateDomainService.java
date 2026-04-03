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

/**
 * Domain service for managing client certificates and syncing mTLS subscriptions.
 *
 * @author GraviteeSource Team
 */
public interface ClientCertificateDomainService {
    /**
     * Creates a new client certificate for the given application and syncs active mTLS subscriptions.
     *
     * @param applicationId the application ID
     * @param certificate the certificate to create (pre-validated and enriched)
     * @return the persisted certificate
     */
    ClientCertificate create(String applicationId, ClientCertificate certificate);

    /**
     * Updates an existing client certificate and syncs active mTLS subscriptions.
     * Performs an ownership check when applicationId is non-null.
     *
     * @param applicationId the application ID for ownership verification (may be null to skip)
     * @param certificateId the certificate ID to update
     * @param certificate the certificate data to apply
     * @return the updated certificate
     */
    ClientCertificate update(String applicationId, String certificateId, ClientCertificate certificate);

    /**
     * Deletes a client certificate and syncs active mTLS subscriptions.
     *
     * @param applicationId the application ID for ownership verification (may be null to skip; resolved from the certificate)
     * @param certificateId the certificate ID to delete
     */
    void delete(String applicationId, String certificateId);
}
