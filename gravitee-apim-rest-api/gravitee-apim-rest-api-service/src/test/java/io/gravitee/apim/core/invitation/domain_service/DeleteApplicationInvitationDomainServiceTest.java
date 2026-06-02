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
package io.gravitee.apim.core.invitation.domain_service;

import static fixtures.core.model.ApplicationInvitationFixtures.anApplicationInvitation;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.exception.ApplicationInvitationNotFoundException;
import io.gravitee.apim.core.invitation.model.InvitationId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteApplicationInvitationDomainServiceTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String OTHER_APPLICATION_ID = "other-application-id";
    private static final String INVITATION_ID = "00000000-0000-0000-0000-000000000001";

    @Mock
    private InvitationCrudService invitationCrudService;

    private DeleteApplicationInvitationDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new DeleteApplicationInvitationDomainService(invitationCrudService);
    }

    @Test
    void should_delete_application_invitation() {
        var invitationId = InvitationId.of(INVITATION_ID);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(
            Optional.of(anApplicationInvitation(INVITATION_ID, APPLICATION_ID, "alice@example.com", "USER"))
        );

        cut.delete(APPLICATION_ID, invitationId);

        verify(invitationCrudService).delete(invitationId);
    }

    @Test
    void should_throw_when_invitation_does_not_exist() {
        var invitationId = InvitationId.of(INVITATION_ID);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cut.delete(APPLICATION_ID, invitationId)).isInstanceOf(ApplicationInvitationNotFoundException.class);

        verify(invitationCrudService, never()).delete(invitationId);
    }

    @Test
    void should_throw_when_invitation_belongs_to_another_application() {
        var invitationId = InvitationId.of(INVITATION_ID);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(
            Optional.of(anApplicationInvitation(INVITATION_ID, OTHER_APPLICATION_ID, "alice@example.com", "USER"))
        );

        assertThatThrownBy(() -> cut.delete(APPLICATION_ID, invitationId)).isInstanceOf(ApplicationInvitationNotFoundException.class);

        verify(invitationCrudService, never()).delete(invitationId);
    }
}
