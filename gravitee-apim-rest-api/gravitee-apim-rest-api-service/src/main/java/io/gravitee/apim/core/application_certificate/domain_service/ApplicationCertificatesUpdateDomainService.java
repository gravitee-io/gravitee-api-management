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

/**
 * Domain service for updating application certificates on mTLS subscriptions.
 *
 * @author GraviteeSource Team
 */

public interface ApplicationCertificatesUpdateDomainService {
    /**
     * Updates the client certificates for all active mTLS subscriptions of the given application.
     * <p>
     * This method:
     * <ul>
     *   <li>Gets all active subscriptions for the application with an mTLS plan</li>
     *   <li>Gets all active certificates (ACTIVE and ACTIVE_WITH_END) of the application</li>
     *   <li>If there are multiple certificates, creates a PKCS7 bundle from the certificates</li>
     *   <li>If there is only one certificate, uses the PEM directly</li>
     *   <li>Encodes the result with base64 and updates the clientCertificate field of all subscriptions
     *       only if the value is different</li>
     * </ul>
     *
     * @param applicationId the application ID
     */
    void updateActiveMTLSSubscriptions(String applicationId);
}
