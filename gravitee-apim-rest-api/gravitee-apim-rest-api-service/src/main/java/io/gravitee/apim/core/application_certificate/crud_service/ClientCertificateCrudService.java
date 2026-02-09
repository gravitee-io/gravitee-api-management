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
package io.gravitee.apim.core.application_certificate.crud_service;

import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Service for managing client certificates.
 *
 * @author GraviteeSource Team
 */
public interface ClientCertificateCrudService {
    /**
     * Find a client certificate by its ID.
     *
     * @param clientCertificateId the client certificate ID
     * @return the client certificate
     */
    ClientCertificate findById(String clientCertificateId);

    /**
     * Create a new client certificate.
     *
     * @param applicationId the application ID
     * @param clientCertificate the client certificate to create
     * @return the created client certificate
     */
    ClientCertificate create(String applicationId, ClientCertificate clientCertificate);

    /**
     * Update an existing client certificate.
     *
     * @param clientCertificateId the client certificate ID
     * @param clientCertificate the client certificate data to update
     * @return the updated client certificate
     */
    ClientCertificate update(String clientCertificateId, ClientCertificate clientCertificate);

    /**
     * Delete a client certificate by its ID.
     *
     * @param clientCertificateId the client certificate ID
     */
    void delete(String clientCertificateId);

    /**
     * Find all client certificates for a given application with pagination.
     *
     * @param applicationId the application ID
     * @param pageable pagination information
     * @return a page of client certificates
     */
    Page<ClientCertificate> findByApplicationId(String applicationId, Pageable pageable);

    /**
     * Find all client certificates for a given application filtered by statuses.
     *
     * @param applicationId the application ID
     * @param statuses the statuses to filter by
     * @return a set of client certificates matching the criteria
     */
    Set<ClientCertificate> findByApplicationIdAndStatuses(String applicationId, ClientCertificateStatus... statuses);

    /**
     * Find all client certificates for a given set of applications filtered by statuses.
     *
     * @param applicationIds application IDs
     * @param statuses the statuses to filter by
     * @return a set of client certificates matching the criteria
     */
    Set<ClientCertificate> findByApplicationIdsAndStatuses(Collection<String> applicationIds, ClientCertificateStatus... statuses);

    /**
     * Delete all client certificates for a given application.
     *
     * @param applicationId the application ID
     */
    void deleteByApplicationId(String applicationId);

    /**
     * Find the most recently created client certificate for a given application with ACTIVE or ACTIVE_WITH_END status.
     *
     * @param applicationId the application ID
     * @return the most recent active client certificate, or empty if none found
     */
    Optional<ClientCertificate> findMostRecentActiveByApplicationId(String applicationId);
}
