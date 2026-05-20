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
package io.gravitee.apim.infra.query_service.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.invitation.model.ApplicationInvitationId;
import io.gravitee.apim.core.invitation.model.InvitationReference;
import io.gravitee.apim.core.invitation.model.SearchApplicationInvitationsCriteria;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.api.InvitationRepository.InvitationCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.management.model.InvitationReferenceType;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvitationQueryServiceImplTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String INVITATION_ID_1 = "00000000-0000-0000-0000-000000000001";
    private static final String INVITATION_ID_2 = "00000000-0000-0000-0000-000000000002";

    @Mock
    private InvitationRepository invitationRepository;

    private InvitationQueryServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new InvitationQueryServiceImpl(invitationRepository);
    }

    @Test
    void should_search_using_repository_criteria_sortable_and_pageable() throws Exception {
        var createdAt = OffsetDateTime.parse("2026-04-23T09:30:00Z");
        var updatedAt = OffsetDateTime.parse("2026-04-23T09:45:00Z");
        var alphaInvitation = anInvitation(INVITATION_ID_2, "john.alpha@example.com", null, createdAt, updatedAt);
        var zedInvitation = anInvitation(INVITATION_ID_1, "John.Zed@example.com", "USER", createdAt, updatedAt);
        var criteria = new SearchApplicationInvitationsCriteria(Optional.of("JOHN"));
        var pageable = new PageableImpl(2, 10);
        var repositoryCriteria = new InvitationCriteria(APPLICATION_ID, InvitationReferenceType.APPLICATION, "JOHN");
        var sortable = new SortableBuilder().field("email").order(Order.ASC).build();
        var repositoryPageable = new PageableBuilder().pageNumber(1).pageSize(10).build();

        when(invitationRepository.search(repositoryCriteria, sortable, repositoryPageable)).thenReturn(
            new Page<>(List.of(alphaInvitation, zedInvitation), 1, 2, 3)
        );

        var result = cut.findByApplicationId(APPLICATION_ID, criteria, pageable);
        var expectedCreatedAt = createdAt.toInstant().atZone(TimeProvider.clock().getZone());
        var expectedUpdatedAt = updatedAt.toInstant().atZone(TimeProvider.clock().getZone());

        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getPageElements()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).id()).isEqualTo(ApplicationInvitationId.of(INVITATION_ID_2));
        assertThat(result.getContent().get(0).applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.getContent().get(0).roleName()).isNull();
        assertThat(result.getContent().get(0).createdAt()).isEqualTo(expectedCreatedAt);
        assertThat(result.getContent().get(0).updatedAt()).isEqualTo(expectedUpdatedAt);
        assertThat(result.getContent().get(1).id()).isEqualTo(ApplicationInvitationId.of(INVITATION_ID_1));
        assertThat(result.getContent().get(1).applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.getContent().get(1).roleName()).isEqualTo("USER");
        assertThat(result.getContent().get(1).createdAt()).isEqualTo(expectedCreatedAt);
        assertThat(result.getContent().get(1).updatedAt()).isEqualTo(expectedUpdatedAt);
        verify(invitationRepository).search(repositoryCriteria, sortable, repositoryPageable);
    }

    @Test
    void should_find_invitations_by_reference() throws Exception {
        var createdAt = OffsetDateTime.parse("2026-04-23T09:30:00Z");
        var invitation = anInvitation(INVITATION_ID_1, "john@example.com", "USER", createdAt, createdAt);
        when(invitationRepository.findByReferenceIdAndReferenceType(APPLICATION_ID, InvitationReferenceType.APPLICATION)).thenReturn(
            List.of(invitation)
        );

        var result = cut.findByReference(InvitationReference.application(APPLICATION_ID));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(ApplicationInvitationId.of(INVITATION_ID_1));
        assertThat(result.getFirst().applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(result.getFirst().email()).isEqualTo("john@example.com");
        assertThat(result.getFirst().roleName()).isEqualTo("USER");
        assertThat(result.getFirst().createdAt()).isEqualTo(createdAt.toInstant().atZone(TimeProvider.clock().getZone()));
        verify(invitationRepository).findByReferenceIdAndReferenceType(APPLICATION_ID, InvitationReferenceType.APPLICATION);
    }

    @Test
    void should_find_invitations_by_api_reference() throws Exception {
        var apiId = "api-id";
        when(invitationRepository.findByReferenceIdAndReferenceType(apiId, InvitationReferenceType.API)).thenReturn(List.of());

        var result = cut.findByReference(InvitationReference.api(apiId));

        assertThat(result).isEmpty();
        verify(invitationRepository).findByReferenceIdAndReferenceType(apiId, InvitationReferenceType.API);
    }

    @Test
    void should_wrap_technical_exception() throws Exception {
        var repositoryCriteria = new InvitationCriteria(APPLICATION_ID, InvitationReferenceType.APPLICATION, null);
        var sortable = new SortableBuilder().field("email").order(Order.ASC).build();
        var repositoryPageable = new PageableBuilder().pageNumber(0).pageSize(10).build();
        when(invitationRepository.search(repositoryCriteria, sortable, repositoryPageable)).thenThrow(new TechnicalException("error"));

        assertThatThrownBy(() ->
            cut.findByApplicationId(APPLICATION_ID, new SearchApplicationInvitationsCriteria(Optional.empty()), new PageableImpl(1, 10))
        )
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessage("An error occurs while trying to find application invitations");
    }

    private Invitation anInvitation(String id, String email, String applicationRole, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        var invitation = new Invitation();
        invitation.setId(id);
        invitation.setReferenceType(InvitationReferenceType.APPLICATION.name());
        invitation.setReferenceId(APPLICATION_ID);
        invitation.setEmail(email);
        invitation.setApplicationRole(applicationRole);
        invitation.setCreatedAt(Date.from(createdAt.toInstant()));
        invitation.setUpdatedAt(Date.from(updatedAt.toInstant()));
        return invitation;
    }
}
