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
package io.gravitee.apim.core.application_certificate.model;

import java.util.Date;

/**
 * Domain model for client certificate.
 *
 * @author GraviteeSource Team
 */
public record ClientCertificate(
    String id,
    String crossId,
    String applicationId,
    String name,
    Date startsAt,
    Date endsAt,
    Date createdAt,
    Date updatedAt,
    String certificate,
    Date certificateExpiration,
    String subject,
    String issuer,
    String fingerprint,
    String environmentId,
    ClientCertificateStatus status
) {
    /** For create: only the fields the caller controls */
    public ClientCertificate(String name, String certificate, Date startsAt, Date endsAt) {
        this(null, null, null, name, startsAt, endsAt, null, null, certificate, null, null, null, null, null, null);
    }

    /** For update: only name and date bounds */
    public ClientCertificate(String name, Date startsAt, Date endsAt) {
        this(null, null, null, name, startsAt, endsAt, null, null, null, null, null, null, null, null, null);
    }
}
