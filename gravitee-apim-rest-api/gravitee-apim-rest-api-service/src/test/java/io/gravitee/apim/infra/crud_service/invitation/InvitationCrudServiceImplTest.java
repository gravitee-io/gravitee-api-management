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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.management.model.InvitationReferenceType;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
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
    private static final String INVITATION_ID_2 = "00000000-0000-0000-0000-000000000002";

    @Mock
    private InvitationRepository invitationRepository;

    private InvitationCrudServiceImpl cut;

    @BeforeEach
    void setUp() {
        var index = new AtomicInteger(0);
        UuidString.overrideGenerator(() -> List.of(INVITATION_ID_1, INVITATION_ID_2).get(index.getAndIncrement()));
        cut = new InvitationCrudServiceImpl(invitationRepository);
    }

    @AfterEach
    void tearDown() {
        UuidString.reset();
    }

    @Test
    void should_create_application_invitations() throws TechnicalException {
        when(invitationRepository.create(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = cut.createApplicationInvitations(APPLICATION_ID, ROLE, List.of("alice@example.com", "bob@example.com"));

        assertThat(result).extracting(invitation -> invitation.id().toString()).containsExactly(INVITATION_ID_1, INVITATION_ID_2);
        assertThat(result).extracting("email").containsExactly("alice@example.com", "bob@example.com");

        var captor = ArgumentCaptor.forClass(Invitation.class);
        org.mockito.Mockito.verify(invitationRepository, org.mockito.Mockito.times(2)).create(captor.capture());
        SoftAssertions.assertSoftly(soft -> {
            var firstInvitation = captor.getAllValues().get(0);
            soft.assertThat(firstInvitation.getId()).isEqualTo(INVITATION_ID_1);
            soft.assertThat(firstInvitation.getReferenceType()).isEqualTo(InvitationReferenceType.APPLICATION.name());
            soft.assertThat(firstInvitation.getReferenceId()).isEqualTo(APPLICATION_ID);
            soft.assertThat(firstInvitation.getEmail()).isEqualTo("alice@example.com");
            soft.assertThat(firstInvitation.getApplicationRole()).isEqualTo(ROLE);
            soft.assertThat(firstInvitation.getApiRole()).isNull();
            soft.assertThat(firstInvitation.getCreatedAt()).isNotNull();
            soft.assertThat(firstInvitation.getUpdatedAt()).isNull();
        });
    }

    @Test
    void should_throw_technical_domain_exception_when_repository_fails() throws TechnicalException {
        when(invitationRepository.create(any(Invitation.class))).thenThrow(new TechnicalException("error"));

        var throwable = catchThrowable(() -> cut.createApplicationInvitations(APPLICATION_ID, ROLE, List.of("alice@example.com")));

        assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessageContaining("create application invitations");
    }
}
