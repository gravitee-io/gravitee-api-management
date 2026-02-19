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

import java.time.Instant;
import java.util.Date;

/**
 * Status of a client certificate.
 *
 * @author GraviteeSource Team
 */
public enum ClientCertificateStatus {
    ACTIVE,
    ACTIVE_WITH_END,
    SCHEDULED,
    REVOKED;

    public static ClientCertificateStatus computeStatus(Date startsAt, Date endsAt) {
        return computeStatus(startsAt, endsAt, Instant.now());
    }

    public static ClientCertificateStatus computeStatus(Date startsAt, Date endsAt, Instant now) {
        if (endsAt != null && endsAt.toInstant().isBefore(now)) {
            return REVOKED;
        }
        if (startsAt != null && startsAt.toInstant().isAfter(now)) {
            return SCHEDULED;
        }
        if (endsAt != null) {
            return ACTIVE_WITH_END;
        }
        return ACTIVE;
    }
}
