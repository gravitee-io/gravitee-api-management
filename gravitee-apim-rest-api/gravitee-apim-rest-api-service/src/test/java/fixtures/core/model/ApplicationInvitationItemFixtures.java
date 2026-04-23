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

import io.gravitee.apim.core.invitation.model.ApplicationInvitationId;
import io.gravitee.apim.core.invitation.model.ApplicationInvitationItem;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ApplicationInvitationItemFixtures {

    private static final Instant INSTANT_NOW = Instant.parse("2020-02-01T20:22:02.00Z");

    private ApplicationInvitationItemFixtures() {}

    public static ApplicationInvitationItem anApplicationInvitationItem(String id, String email) {
        return anApplicationInvitationItem(id, email, "USER");
    }

    public static ApplicationInvitationItem anApplicationInvitationItem(String id, String email, String role) {
        return new ApplicationInvitationItem(ApplicationInvitationId.of(id), email, role, date(), date());
    }

    private static ZonedDateTime date() {
        return INSTANT_NOW.atZone(ZoneOffset.UTC);
    }
}
