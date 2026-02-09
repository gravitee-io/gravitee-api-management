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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model for client certificate.
 *
 * @author GraviteeSource Team
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ClientCertificate {

    private String id;

    private String crossId;

    private String applicationId;

    private String name;

    private Date startsAt;

    private Date endsAt;

    private Date createdAt;

    private Date updatedAt;

    private String certificate;

    private Date certificateExpiration;

    private String subject;

    private String issuer;

    private String fingerprint;

    private String environmentId;

    private ClientCertificateStatus status;
}
