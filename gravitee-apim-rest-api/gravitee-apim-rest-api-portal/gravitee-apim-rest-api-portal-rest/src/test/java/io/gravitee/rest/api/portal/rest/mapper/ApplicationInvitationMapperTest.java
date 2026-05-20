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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.ApplicationInvitationId;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class ApplicationInvitationMapperTest {

    private final ApplicationInvitationMapper mapper = ApplicationInvitationMapper.INSTANCE;

    @Test
    void should_map_application_invitation_item() {
        var invitationId = "00000000-0000-0000-0000-000000000001";
        var invitationItem = new ApplicationInvitation(
            ApplicationInvitationId.of(invitationId),
            "application-id",
            "alice@example.com",
            "USER",
            ZonedDateTime.parse("2026-04-23T09:30:00Z"),
            ZonedDateTime.parse("2026-04-23T09:45:00Z")
        );

        var invitation = mapper.toInvitation(invitationItem);

        assertThat(invitation.getId()).isEqualTo(invitationId);
        assertThat(invitation.getEmail()).isEqualTo("alice@example.com");
        assertThat(invitation.getRole()).isEqualTo("USER");
        assertThat(invitation.getCreatedAt()).isEqualTo(OffsetDateTime.parse("2026-04-23T09:30:00Z"));
        assertThat(invitation.getUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-04-23T09:45:00Z"));
    }
}
