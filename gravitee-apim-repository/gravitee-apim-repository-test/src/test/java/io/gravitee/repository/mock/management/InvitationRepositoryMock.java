/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mock.management;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InvitationRepositoryMock extends AbstractRepositoryMock<InvitationRepository> {

    public InvitationRepositoryMock() {
        super(InvitationRepository.class);
    }

    @Override
    protected void prepare(InvitationRepository invitationRepository) throws Exception {
        final Date date = new Date(1439022010883L);

        final Invitation invitation = new Invitation();
        invitation.setId("new-invitation");
        invitation.setReferenceType("API");
        invitation.setReferenceId("api-id");
        invitation.setApiRole("USER");
        invitation.setApplicationRole("OWNER");
        invitation.setEmail("new-invitation@email.com");
        invitation.setCreatedAt(date);
        invitation.setUpdatedAt(date);

        final Invitation invitation2 = new Invitation();
        invitation2.setId("invitation");
        invitation2.setEmail("invitation@email.com");

        final Invitation invitationBeforUpdate = new Invitation();
        invitationBeforUpdate.setId("ec48086d-0bfb-4b9a-a641-d374c6465dd4");
        invitationBeforUpdate.setEmail("invitation@application.com");

        final Invitation invitation2Updated = new Invitation();
        invitation2Updated.setId("ec48086d-0bfb-4b9a-a641-d374c6465dd4");
        invitation2Updated.setReferenceType("New reference type");
        invitation2Updated.setReferenceId("New reference id");
        invitation2Updated.setApiRole("USER");
        invitation2Updated.setApplicationRole("OWNER");
        invitation2Updated.setEmail("new-invitation@application.com");
        invitation2Updated.setCreatedAt(date);
        invitation2Updated.setUpdatedAt(date);

        final Invitation invitationApplication = new Invitation();
        invitationApplication.setId("e6d5e6d0-17e9-4606-83c3-cfef8b91d5ce");
        invitationApplication.setReferenceType("APPLICATION");
        invitationApplication.setReferenceId("application-id");
        invitationApplication.setApiRole("USER");
        invitationApplication.setApplicationRole("OWNER");
        invitationApplication.setEmail("invitation@application.com");
        invitationApplication.setCreatedAt(date);
        invitationApplication.setUpdatedAt(date);

        final Set<Invitation> invitations = newSet(invitation, invitationApplication, invitation2Updated);
        final Set<Invitation> invitationsAfterDelete = newSet(invitation, invitationApplication);
        final Set<Invitation> invitationsAfterAdd = newSet(invitation, invitation2, mock(Invitation.class), mock(Invitation.class));

        when(invitationRepository.findAll()).thenReturn(invitations, invitationsAfterAdd, invitations, invitationsAfterDelete, invitations);

        when(invitationRepository.create(any(Invitation.class))).thenReturn(invitation);

        when(invitationRepository.findById("new-invitation")).thenReturn(of(invitation));
        when(invitationRepository.findById("e6d5e6d0-17e9-4606-83c3-cfef8b91d5ce"))
            .thenReturn(of(invitationBeforUpdate), of(invitation2Updated));

        when(invitationRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());

        when(invitationRepository.findByReference("API", "api-id")).thenReturn(singletonList(invitation));
    }
}
