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
package io.gravitee.repository.management;

import static io.gravitee.repository.management.InvitationFixtures.anInvitation;
import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.InvitationRepository.InvitationCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.management.model.InvitationReferenceType;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InvitationRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/invitation-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Invitation> invitations = invitationRepository.findAll();

        assertNotNull(invitations);
        assertEquals(5, invitations.size());
        final Optional<Invitation> optionalInvitation = invitations
            .stream()
            .filter(invitation -> "e6d5e6d0-17e9-4606-83c3-cfef8b91d5ce".equals(invitation.getId()))
            .findAny();
        assertTrue(optionalInvitation.isPresent());
        assertEquals("APPLICATION", optionalInvitation.get().getReferenceType());
        assertEquals("application-id", optionalInvitation.get().getReferenceId());
        assertEquals("invitation@application.com", optionalInvitation.get().getEmail());
        assertTrue(compareDate(1439022010883L, optionalInvitation.get().getCreatedAt().getTime()));
        assertTrue(compareDate(1439022010883L, optionalInvitation.get().getUpdatedAt().getTime()));
    }

    @Test
    public void shouldCreate() throws Exception {
        final Invitation invitation = new Invitation();
        invitation.setId("new-invitation");
        invitation.setReferenceType("API");
        invitation.setReferenceId("api-id");
        invitation.setApiRole("USER");
        invitation.setApplicationRole("OWNER");
        invitation.setEmail("new-invitation@email.com");
        final Date date = new Date(1439022010883L);
        invitation.setCreatedAt(date);
        invitation.setUpdatedAt(date);

        int nbInvitationsBeforeCreation = invitationRepository.findAll().size();
        invitationRepository.create(invitation);
        int nbInvitationsAfterCreation = invitationRepository.findAll().size();

        assertEquals(nbInvitationsBeforeCreation + 1, nbInvitationsAfterCreation);

        Optional<Invitation> optional = invitationRepository.findById("new-invitation");
        Assertions.assertTrue(optional.isPresent(), "Invitation saved not found");
        final Invitation fetchedInvitation = optional.get();
        assertEquals(invitation.getReferenceType(), fetchedInvitation.getReferenceType());
        assertEquals(invitation.getReferenceId(), fetchedInvitation.getReferenceId());
        assertEquals(invitation.getApiRole(), fetchedInvitation.getApiRole());
        assertEquals(invitation.getApplicationRole(), fetchedInvitation.getApplicationRole());
        assertEquals(invitation.getEmail(), fetchedInvitation.getEmail());
        assertTrue(compareDate(invitation.getCreatedAt(), fetchedInvitation.getCreatedAt()));
        assertTrue(compareDate(invitation.getUpdatedAt(), fetchedInvitation.getUpdatedAt()));
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Invitation> optional = invitationRepository.findById("e6d5e6d0-17e9-4606-83c3-cfef8b91d5ce");
        Assertions.assertTrue(optional.isPresent(), "Invitation to update not found");
        assertEquals("invitation@application.com", optional.get().getEmail(), "Invalid saved invitation email.");

        final Invitation invitation = optional.get();
        invitation.setReferenceType("New reference type");
        invitation.setReferenceId("New reference id");
        invitation.setEmail("new-invitation@application.com");
        invitation.setApiRole("USER");
        invitation.setApplicationRole("OWNER");
        final Date date = new Date(1439022010883L);
        invitation.setCreatedAt(date);
        invitation.setUpdatedAt(date);

        int nbInvitationsBeforeUpdate = invitationRepository.findAll().size();
        invitationRepository.update(invitation);
        int nbInvitationsAfterUpdate = invitationRepository.findAll().size();

        assertEquals(nbInvitationsBeforeUpdate, nbInvitationsAfterUpdate);

        Optional<Invitation> optionalUpdated = invitationRepository.findById("e6d5e6d0-17e9-4606-83c3-cfef8b91d5ce");
        Assertions.assertTrue(optionalUpdated.isPresent(), "Invitation to update not found");
        final Invitation fetchedInvitation = optionalUpdated.get();
        assertEquals(invitation.getReferenceType(), fetchedInvitation.getReferenceType());
        assertEquals(invitation.getReferenceId(), fetchedInvitation.getReferenceId());
        assertEquals(invitation.getEmail(), fetchedInvitation.getEmail());
        assertEquals(invitation.getApiRole(), fetchedInvitation.getApiRole());
        assertEquals(invitation.getApplicationRole(), fetchedInvitation.getApplicationRole());
        assertTrue(compareDate(invitation.getCreatedAt(), fetchedInvitation.getCreatedAt()));
        assertTrue(compareDate(invitation.getUpdatedAt(), fetchedInvitation.getUpdatedAt()));
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbInvitationsBeforeDeletion = invitationRepository.findAll().size();
        invitationRepository.delete("e6d5e6d0-17e9-4606-83c3-cfef8b91d5ce");
        int nbInvitationsAfterDeletion = invitationRepository.findAll().size();

        assertEquals(nbInvitationsBeforeDeletion - 1, nbInvitationsAfterDeletion);
    }

    @Test
    public void shouldFindByReferenceIdAndReferenceType() throws Exception {
        final List<Invitation> invitations = invitationRepository.findByReferenceIdAndReferenceType("api-id", InvitationReferenceType.API);
        assertNotNull(invitations);
        assertEquals(1, invitations.size());
    }

    @Test
    public void shouldSearchByCriteriaWithSortableAndPageable() throws Exception {
        invitationRepository.create(
            anInvitation("search-1", "search-application", InvitationReferenceType.APPLICATION, "alpha@example.com")
        );
        invitationRepository.create(
            anInvitation("search-2", "search-application", InvitationReferenceType.APPLICATION, "bravo@example.com")
        );
        invitationRepository.create(
            anInvitation("search-3", "search-application", InvitationReferenceType.APPLICATION, "charlie@example.com")
        );
        invitationRepository.create(
            anInvitation("search-4", "search-application", InvitationReferenceType.APPLICATION, "other@gravitee.io")
        );
        invitationRepository.create(anInvitation("search-5", "search-application", InvitationReferenceType.API, "delta@example.com"));

        Page<Invitation> page = invitationRepository.search(
            new InvitationCriteria("search-application", InvitationReferenceType.APPLICATION, "EXAMPLE.COM"),
            new SortableBuilder().field("email").order(Order.DESC).build(),
            new PageableBuilder().pageNumber(1).pageSize(1).build()
        );

        assertNotNull(page);
        assertEquals(3L, page.getTotalElements());
        assertEquals(1, page.getPageElements());
        assertEquals(1, page.getPageNumber());
        assertEquals("bravo@example.com", page.getContent().get(0).getEmail());
    }

    @Test
    public void shouldNotUpdateUnknownInvitation() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Invitation unknownInvitation = new Invitation();
            unknownInvitation.setId("unknown");
            invitationRepository.update(unknownInvitation);
            fail("An unknown invitation should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            invitationRepository.update(null);
            fail("A null invitation should not be updated");
        });
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        final List<Invitation> beforeDelete = invitationRepository.findByReferenceIdAndReferenceType(
            "ToBeDeleted",
            InvitationReferenceType.APPLICATION
        );

        final List<String> deleted = invitationRepository.deleteByReferenceIdAndReferenceType(
            "ToBeDeleted",
            InvitationReferenceType.APPLICATION
        );

        final List<Invitation> afterDelete = invitationRepository.findByReferenceIdAndReferenceType(
            "ToBeDeleted",
            InvitationReferenceType.APPLICATION
        );

        assertNotNull(beforeDelete);
        assertEquals(2, beforeDelete.size());
        assertEquals(2, deleted.size());
        assertEquals(0, afterDelete.size());
    }
}
