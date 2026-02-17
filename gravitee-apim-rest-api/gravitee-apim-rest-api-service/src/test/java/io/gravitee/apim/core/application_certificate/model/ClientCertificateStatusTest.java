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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.audit.model.event.ClientCertificateAuditEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientCertificateStatusTest {

    static Stream<Arguments> computeStatusCases() {
        var now = Instant.parse("2025-01-15T12:00:00Z");
        var past = Date.from(now.minus(1, ChronoUnit.DAYS));
        var future = Date.from(now.plus(1, ChronoUnit.DAYS));

        return Stream.of(
            // endsAt in past → REVOKED regardless of startsAt
            Arguments.of(null, past, now, ClientCertificateStatus.REVOKED),
            Arguments.of(past, past, now, ClientCertificateStatus.REVOKED),
            // startsAt in future (and endsAt not expired) → SCHEDULED
            Arguments.of(future, null, now, ClientCertificateStatus.SCHEDULED),
            Arguments.of(future, future, now, ClientCertificateStatus.SCHEDULED),
            // endsAt in future, startsAt not in future → ACTIVE_WITH_END
            Arguments.of(null, future, now, ClientCertificateStatus.ACTIVE_WITH_END),
            Arguments.of(past, future, now, ClientCertificateStatus.ACTIVE_WITH_END),
            // no endsAt, startsAt not in future → ACTIVE
            Arguments.of(null, null, now, ClientCertificateStatus.ACTIVE),
            Arguments.of(past, null, now, ClientCertificateStatus.ACTIVE),
            // Boundary cases: exactly at 'now'
            // endsAt == now → ACTIVE_WITH_END (isBefore(now) is false)
            Arguments.of(null, Date.from(now), now, ClientCertificateStatus.ACTIVE_WITH_END),
            Arguments.of(past, Date.from(now), now, ClientCertificateStatus.ACTIVE_WITH_END),
            // startsAt == now → ACTIVE (isAfter(now) is false)
            Arguments.of(Date.from(now), null, now, ClientCertificateStatus.ACTIVE),
            // startsAt == now, endsAt in future → ACTIVE_WITH_END
            Arguments.of(Date.from(now), future, now, ClientCertificateStatus.ACTIVE_WITH_END)
        );
    }

    @ParameterizedTest
    @MethodSource("computeStatusCases")
    void should_compute_correct_status(Date startsAt, Date endsAt, Instant now, ClientCertificateStatus expected) {
        assertThat(ClientCertificateStatus.computeStatus(startsAt, endsAt, now)).isEqualTo(expected);
    }

    @Test
    void should_return_revoked_audit_event_for_revoked_status() {
        assertThat(ClientCertificateStatus.REVOKED.toAuditEvent()).isEqualTo(ClientCertificateAuditEvent.CLIENT_CERTIFICATE_REVOKED);
    }

    @Test
    void should_return_activated_audit_event_for_active_status() {
        assertThat(ClientCertificateStatus.ACTIVE.toAuditEvent()).isEqualTo(ClientCertificateAuditEvent.CLIENT_CERTIFICATE_ACTIVATED);
    }

    @Test
    void should_return_activated_audit_event_for_active_with_end_status() {
        assertThat(ClientCertificateStatus.ACTIVE_WITH_END.toAuditEvent()).isEqualTo(
            ClientCertificateAuditEvent.CLIENT_CERTIFICATE_ACTIVATED
        );
    }

    @Test
    void should_return_scheduled_audit_event_for_scheduled_status() {
        assertThat(ClientCertificateStatus.SCHEDULED.toAuditEvent()).isEqualTo(ClientCertificateAuditEvent.CLIENT_CERTIFICATE_SCHEDULED);
    }
}
