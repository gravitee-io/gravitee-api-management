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
package fixtures.repository;

import io.gravitee.repository.management.model.Invitation;

public class InvitationFixtures {

    private InvitationFixtures() {}

    public static Invitation aRepositoryGroupInvitation(String id, String referenceId, String email) {
        return aRepositoryInvitation(id, referenceId, "GROUP", email);
    }

    public static Invitation aRepositoryApplicationInvitation(String id, String referenceId, String email) {
        return aRepositoryInvitation(id, referenceId, "APPLICATION", email);
    }

    public static Invitation aRepositoryInvitation(String id, String referenceId, String referenceType, String email) {
        var invitation = new Invitation();
        invitation.setId(id);
        invitation.setReferenceId(referenceId);
        invitation.setReferenceType(referenceType);
        invitation.setEmail(email);
        return invitation;
    }
}
