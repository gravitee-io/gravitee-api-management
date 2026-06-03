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
package io.gravitee.apim.core.invitation.crud_service;

import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.Invitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import java.util.List;
import java.util.Optional;

public interface InvitationCrudService {
    ApplicationInvitation create(ApplicationInvitation invitation);

    default ApplicationInvitation update(ApplicationInvitation invitation) {
        throw new UnsupportedOperationException("update not implemented");
    }

    default Optional<ApplicationInvitation> findApplicationInvitationById(InvitationId invitationId) {
        throw new UnsupportedOperationException("findApplicationInvitationById not implemented");
    }

    default List<Invitation> findByEmail(String email) {
        throw new UnsupportedOperationException("findByEmail not implemented");
    }

    default void delete(InvitationId invitationId) {
        throw new UnsupportedOperationException("delete not implemented");
    }
}
