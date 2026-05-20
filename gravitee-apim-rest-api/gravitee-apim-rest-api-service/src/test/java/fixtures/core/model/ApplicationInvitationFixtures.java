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
package fixtures.core.model;

import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ApplicationInvitationFixtures {

    private static final Instant INSTANT_NOW = Instant.parse("2020-02-01T20:22:02.00Z");
    private static final String APPLICATION_ID = "application-id";

    private ApplicationInvitationFixtures() {}

    public static ApplicationInvitation anApplicationInvitation(String id, String email) {
        return anApplicationInvitation(id, email, "USER");
    }

    public static ApplicationInvitation anApplicationInvitation(String id, String email, String role) {
        return anApplicationInvitation(id, APPLICATION_ID, email, role);
    }

    public static ApplicationInvitation anApplicationInvitation(String id, String applicationId, String email, String role) {
        return new ApplicationInvitation(InvitationId.of(id), applicationId, email, role, date(), date());
    }

    private static ZonedDateTime date() {
        return INSTANT_NOW.atZone(ZoneOffset.UTC);
    }
}
