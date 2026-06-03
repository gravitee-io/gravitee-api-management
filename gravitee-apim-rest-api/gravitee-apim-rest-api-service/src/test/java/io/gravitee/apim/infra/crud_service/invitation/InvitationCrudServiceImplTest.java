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

import inmemory.InvitationRepositoryInMemory;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.GroupInvitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.InvitationReferenceType;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InvitationCrudServiceImplTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String ROLE = "USER";
    private static final String INVITATION_ID_1 = "00000000-0000-0000-0000-000000000001";

    InvitationRepositoryInMemory invitationRepository = new InvitationRepositoryInMemory();

    InvitationCrudServiceImpl cut;

    @BeforeEach
    void setUp() {
        invitationRepository.reset();
        cut = new InvitationCrudServiceImpl(invitationRepository);
    }

    @Test
    void should_create_application_invitation() throws TechnicalException {
        var invitationToCreate = anApplicationInvitation(INVITATION_ID_1, APPLICATION_ID, "alice@example.com", ROLE);

        var result = cut.create(invitationToCreate);

        assertThat(result.id().toString()).isEqualTo(INVITATION_ID_1);
        assertThat(result.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.email()).isEqualTo("alice@example.com");

        var stored = invitationRepository.storage().get(INVITATION_ID_1);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(stored.getId()).isEqualTo(INVITATION_ID_1);
            soft.assertThat(stored.getReferenceType()).isEqualTo(InvitationReferenceType.APPLICATION.name());
            soft.assertThat(stored.getReferenceId()).isEqualTo(APPLICATION_ID);
            soft.assertThat(stored.getEmail()).isEqualTo("alice@example.com");
            soft.assertThat(stored.getApplicationRole()).isEqualTo(ROLE);
            soft.assertThat(stored.getApiRole()).isNull();
            soft.assertThat(stored.getCreatedAt()).isNotNull();
            soft.assertThat(stored.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void should_throw_technical_domain_exception_when_repository_fails() {
        invitationRepository.failsWith(new TechnicalException("error"));

        var throwable = catchThrowable(() ->
            cut.create(anApplicationInvitation(INVITATION_ID_1, APPLICATION_ID, "alice@example.com", ROLE))
        );

        assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessageContaining("create application invitation");
    }

    @Test
    void should_update_application_invitation() {
        invitationRepository.initWith(List.of(aRepositoryApplicationInvitation(INVITATION_ID_1, APPLICATION_ID, "alice@example.com")));
        var invitationToUpdate = anApplicationInvitation(INVITATION_ID_1, APPLICATION_ID, "alice@example.com", "OWNER");

        var result = cut.update(invitationToUpdate);

        assertThat(result.id().toString()).isEqualTo(INVITATION_ID_1);
        assertThat(result.roleName()).isEqualTo("OWNER");
        assertThat(invitationRepository.storage().get(INVITATION_ID_1).getApplicationRole()).isEqualTo("OWNER");
    }

    @Test
    void should_throw_technical_domain_exception_when_update_fails() {
        invitationRepository.failsWith(new TechnicalException("error"));

        var throwable = catchThrowable(() ->
            cut.update(anApplicationInvitation(INVITATION_ID_1, APPLICATION_ID, "alice@example.com", ROLE))
        );

        assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessageContaining("update application invitation");
    }

    @Nested
    class FindByEmail {

        private static final String INVITATION_ID_2 = "00000000-0000-0000-0000-000000000002";
        private static final String INVITATION_ID_3 = "00000000-0000-0000-0000-000000000003";

        @Test
        void should_return_group_and_application_invitations_for_email() {
            invitationRepository.initWith(
                List.of(
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
        void should_return_empty_list_when_no_invitations_match_email() {
            invitationRepository.initWith(List.of(aRepositoryGroupInvitation(INVITATION_ID_1, "group-1", "bob@example.com")));

            var result = cut.findByEmail("alice@example.com");

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_fails() {
            invitationRepository.failsWith(new TechnicalException("error"));

            var throwable = catchThrowable(() -> cut.findByEmail("alice@example.com"));

            assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessageContaining("find invitations by email");
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_invitation_by_id() {
            invitationRepository.initWith(List.of(aRepositoryGroupInvitation(INVITATION_ID_1, "group-1", "alice@example.com")));

            cut.delete(InvitationId.of(INVITATION_ID_1));

            assertThat(invitationRepository.storage()).doesNotContainKey(INVITATION_ID_1);
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_fails() {
            invitationRepository.failsWith(new TechnicalException("error"));

            var throwable = catchThrowable(() -> cut.delete(InvitationId.of(INVITATION_ID_1)));

            assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessageContaining("delete invitation");
        }
    }
}
