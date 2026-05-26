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
import static fixtures.repository.InvitationFixtures.aRepositoryApplicationInvitation;
import static fixtures.repository.InvitationFixtures.aRepositoryGroupInvitation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.GroupInvitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.management.model.InvitationReferenceType;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class FindByEmail {

        private static final String INVITATION_ID_2 = "00000000-0000-0000-0000-000000000002";
        private static final String INVITATION_ID_3 = "00000000-0000-0000-0000-000000000003";

        @Test
        void should_return_group_and_application_invitations_for_email() throws TechnicalException {
            when(invitationRepository.findAll()).thenReturn(
                Set.of(
                    aRepositoryGroupInvitation(INVITATION_ID_1, "group-1", "alice@example.com"),
                    aRepositoryApplicationInvitation(INVITATION_ID_2, APPLICATION_ID, "alice@example.com"),
                    aRepositoryGroupInvitation(INVITATION_ID_3, "group-2", "bob@example.com")
                )
            );

            var result = cut.findByEmail("alice@example.com");

            assertThat(result).hasSize(2);
            assertThat(result).anySatisfy(i -> assertThat(i).isInstanceOf(GroupInvitation.class));
            assertThat(result).anySatisfy(i -> assertThat(i).isInstanceOf(ApplicationInvitation.class));
        }

        @Test
        void should_return_empty_list_when_no_invitations_match_email() throws TechnicalException {
            when(invitationRepository.findAll()).thenReturn(
                Set.of(aRepositoryGroupInvitation(INVITATION_ID_1, "group-1", "bob@example.com"))
            );

            var result = cut.findByEmail("alice@example.com");

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_fails() throws TechnicalException {
            when(invitationRepository.findAll()).thenThrow(new TechnicalException("error"));

            var throwable = catchThrowable(() -> cut.findByEmail("alice@example.com"));

            assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessageContaining("find invitations by email");
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_invitation_by_id() throws TechnicalException {
            cut.delete(InvitationId.of(INVITATION_ID_1));

            verify(invitationRepository).delete(INVITATION_ID_1);
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_fails() throws TechnicalException {
            org.mockito.Mockito.doThrow(new TechnicalException("error")).when(invitationRepository).delete(anyString());

            var throwable = catchThrowable(() -> cut.delete(InvitationId.of(INVITATION_ID_1)));

            assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessageContaining("delete invitation");
        }
    }
}
