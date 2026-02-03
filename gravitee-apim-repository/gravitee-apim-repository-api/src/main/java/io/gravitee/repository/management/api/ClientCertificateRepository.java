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
package io.gravitee.repository.management.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.ClientCertificate;
import io.gravitee.repository.management.model.ClientCertificateStatus;
import java.util.Collection;
import java.util.Set;

/**
 * Repository interface for ClientCertificate CRUD operations.
 *
 * @author GraviteeSource Team
 */
public interface ClientCertificateRepository extends CrudRepository<ClientCertificate, String> {
    /**
     * Find all client certificates for a given application with pagination.
     *
     * @param applicationId the application ID
     * @param pageable pagination information
     * @return a page of client certificates
     * @throws TechnicalException if an error occurs
     */
    Page<ClientCertificate> findByApplicationId(String applicationId, Pageable pageable) throws TechnicalException;

    /**
     * Find all client certificates for a given application filtered by statuses.
     *
     * @param applicationId the application ID
     * @param statuses the statuses to filter by
     * @return a set of client certificates matching the criteria
     * @throws TechnicalException if an error occurs
     */
    Set<ClientCertificate> findByApplicationIdAndStatuses(String applicationId, ClientCertificateStatus... statuses)
        throws TechnicalException;

    /**
     * Find all client certificates for given application IDs filtered by statuses.
     * This method handles large amounts of application IDs efficiently by processing them in batches.
     *
     * @param applicationIds the collection of application IDs
     * @param statuses the statuses to filter by
     * @return a set of client certificates matching the criteria
     * @throws TechnicalException if an error occurs
     */
    Set<ClientCertificate> findByApplicationIdsAndStatuses(Collection<String> applicationIds, ClientCertificateStatus... statuses)
        throws TechnicalException;

    /**
     * Check if a client certificate with the given fingerprint exists for an active application,
     * excluding certificates with REVOKED status.
     *
     * @param fingerprint the certificate fingerprint to check
     * @param environmentId the environment ID
     * @return true if such a certificate exists, false otherwise
     * @throws TechnicalException if an error occurs
     */
    boolean existsByFingerprintAndActiveApplication(String fingerprint, String environmentId) throws TechnicalException;

    /**
     * Delete all client certificates for a given application.
     * @param applicationId the application ID
     */
    void deleteByApplicationId(String applicationId);
}
