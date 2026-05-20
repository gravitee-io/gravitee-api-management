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
package io.gravitee.apim.infra.crud_service.invitation;

import static fixtures.core.model.ApplicationInvitationFixtures.anApplicationInvitation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.management.model.InvitationReferenceType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvitationCrudServiceImplTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String ROLE = "USER";
    private static final String INVITATION_ID_1 = "00000000-0000-0000-0000-000000000001";

    @Mock
    private InvitationRepository invitationRepository;

    private InvitationCrudServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new InvitationCrudServiceImpl(invitationRepository);
    }

    @Test
    void should_create_application_invitation() throws TechnicalException {
        when(invitationRepository.create(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var invitationToCreate = anApplicationInvitation(INVITATION_ID_1, APPLICATION_ID, "alice@example.com", ROLE);

        var result = cut.create(invitationToCreate);

        assertThat(result.id().toString()).isEqualTo(INVITATION_ID_1);
        assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.email()).isEqualTo("alice@example.com");

        var captor = ArgumentCaptor.forClass(Invitation.class);
        org.mockito.Mockito.verify(invitationRepository).create(captor.capture());
        SoftAssertions.assertSoftly(soft -> {
            var repositoryInvitation = captor.getValue();
            soft.assertThat(repositoryInvitation.getId()).isEqualTo(INVITATION_ID_1);
            soft.assertThat(repositoryInvitation.getReferenceType()).isEqualTo(InvitationReferenceType.APPLICATION.name());
            soft.assertThat(repositoryInvitation.getReferenceId()).isEqualTo(APPLICATION_ID);
            soft.assertThat(repositoryInvitation.getEmail()).isEqualTo("alice@example.com");
            soft.assertThat(repositoryInvitation.getApplicationRole()).isEqualTo(ROLE);
            soft.assertThat(repositoryInvitation.getApiRole()).isNull();
            soft.assertThat(repositoryInvitation.getCreatedAt()).isNotNull();
            soft.assertThat(repositoryInvitation.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void should_throw_technical_domain_exception_when_repository_fails() throws TechnicalException {
        when(invitationRepository.create(any(Invitation.class))).thenThrow(new TechnicalException("error"));

        var throwable = catchThrowable(() ->
            cut.create(anApplicationInvitation(INVITATION_ID_1, APPLICATION_ID, "alice@example.com", ROLE))
        );

        assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessageContaining("create application invitation");
    }
}
