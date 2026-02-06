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

package io.gravitee.rest.api.model.clientcertificate;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;

/**
 * DTO representing a client certificate with all properties.
 */
@Schema(name = "ClientCertificate", description = "Client certificate in PEM format")
public record ClientCertificate(
    @Schema(description = "Generated UUID") String id,
    @Schema(description = "Generated UUID for all environment.") String crossId,
    @Schema(description = "Application owning that client certificate.") String applicationId,
    @Schema(description = "Name of this client certificate.") String name,
    @Schema(
        description = "Date when this certificate will become available. If null it will be deployed along with current or future subscription."
    )
    Date startsAt,
    @Schema(
        description = "Date when this certificate will be removed from existing subscriptions. If null it will stay until the subscription is removed."
    )
    Date endsAt,
    @Schema(description = "When this object was created") Date createdAt,
    @Schema(description = "When this object was last updated") Date updatedAt,
    @Schema(description = "The certificate in PEM format.") String certificate,
    @Schema(description = "Expiration date from the certificate itself") Date certificateExpiration,
    @Schema(description = "Subject of the certificate") String subject,
    @Schema(description = "Issuer of the certificate") String issuer,
    @Schema(description = "Fingerprint of the certificate") String fingerprint,
    @Schema(description = "Environment ID in which the certificate was created") String environmentId,
    @Schema(
        description = "Status of the certificate. It is computed depending on start and end dates. ACTIVE means it is available. ACTIVE_WITH_END means it is available but will be removed when endsAt is reached. SCHEDULED means that startsAt is not yet passed. REVOKED means endsAt is passed."
    )
    ClientCertificateStatus status
) {}
