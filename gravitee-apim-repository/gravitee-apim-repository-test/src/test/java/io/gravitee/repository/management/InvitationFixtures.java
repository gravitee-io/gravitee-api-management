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
package io.gravitee.repository.management;

import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.management.model.InvitationReferenceType;
import java.util.Date;

final class InvitationFixtures {

    private InvitationFixtures() {}

    static Invitation anInvitation(String id, String referenceId, InvitationReferenceType referenceType, String email) {
        final Invitation invitation = new Invitation();
        invitation.setId(id);
        invitation.setReferenceType(referenceType.name());
        invitation.setReferenceId(referenceId);
        invitation.setEmail(email);
        invitation.setCreatedAt(new Date(1439022010883L));
        invitation.setUpdatedAt(new Date(1439022010883L));
        return invitation;
    }
}
