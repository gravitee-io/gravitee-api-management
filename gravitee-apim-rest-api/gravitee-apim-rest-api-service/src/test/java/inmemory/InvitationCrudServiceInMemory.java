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
package inmemory;

import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.Invitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class InvitationCrudServiceInMemory implements InvitationCrudService, InMemoryAlternative<Invitation> {

    private final List<Invitation> storage = new ArrayList<>();

    @Override
    public ApplicationInvitation create(ApplicationInvitation invitation) {
        storage.add(invitation);
        return invitation;
    }

    @Override
    public Optional<ApplicationInvitation> findApplicationInvitationById(InvitationId invitationId) {
        return storage
            .stream()
            .filter(ApplicationInvitation.class::isInstance)
            .map(ApplicationInvitation.class::cast)
            .filter(invitation -> invitation.id().equals(invitationId))
            .findFirst();
    }

    @Override
    public List<Invitation> findByEmail(String email) {
        return storage
            .stream()
            .filter(i -> i.email().equals(email))
            .toList();
    }

    @Override
    public void delete(InvitationId invitationId) {
        storage.removeIf(i -> i.id().equals(invitationId));
    }

    @Override
    public void initWith(List<Invitation> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Invitation> storage() {
        return Collections.unmodifiableList(storage);
    }
}
