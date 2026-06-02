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

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.TicketCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Ticket;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TicketRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/ticket-tests/";
    }

    @Test
    public void shouldSearchWithNullCriteria() throws TechnicalException {
        List<Ticket> tickets = ticketRepository
            .search(null, null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();

        assertNotNull(tickets);
        assertEquals(8, tickets.size(), "Invalid tickets numbers in search");
        assertEquals("ticket0", tickets.get(0).getId());
        assertEquals("ticket1", tickets.get(1).getId());
        assertEquals("ticket2", tickets.get(2).getId());
        assertEquals("ticket3", tickets.get(3).getId());
    }

    @Test
    public void shouldSearchAllWithEmptyCriteria() throws Exception {
        List<Ticket> tickets = ticketRepository
            .search(new TicketCriteria.Builder().build(), null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();

        assertNotNull(tickets);
        assertEquals(8, tickets.size(), "Invalid tickets numbers in search");
        assertEquals("ticket0", tickets.get(0).getId());
        assertEquals("ticket1", tickets.get(1).getId());
        assertEquals("ticket2", tickets.get(2).getId());
        assertEquals("ticket3", tickets.get(3).getId());
    }

    @Test
    public void shouldSearchAllWithCriteriaFromUserEmpty() throws Exception {
        List<Ticket> tickets = ticketRepository
            .search(
                new TicketCriteria.Builder().fromUser("").build(),
                null,
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
            )
            .getContent();

        assertNotNull(tickets);
        assertEquals(8, tickets.size(), "Invalid tickets numbers in search");
        assertEquals("ticket0", tickets.get(0).getId());
        assertEquals("ticket1", tickets.get(1).getId());
        assertEquals("ticket2", tickets.get(2).getId());
        assertEquals("ticket3", tickets.get(3).getId());
    }

    @Test
    public void shouldSearchAllWithCriteriaFromUser0SortedBySubjectDescendingOnThreeElements() throws Exception {
        List<Ticket> tickets = ticketRepository
            .search(
                new TicketCriteria.Builder().fromUser("user0").api("api0").build(),
                new SortableBuilder().field("subject").order(Order.DESC).build(),
                new PageableBuilder().pageNumber(0).pageSize(3).build()
            )
            .getContent();

        assertNotNull(tickets);
        assertEquals(3, tickets.size(), "Invalid tickets numbers in search");
        assertEquals("ticket3", tickets.get(0).getId());
        assertEquals("ticket2", tickets.get(1).getId());
        assertEquals("ticket1", tickets.get(2).getId());
    }

    @Test
    public void shouldCreateTicketTest() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setId("createticket1");
        ticket.setApi("api1");
        ticket.setApplication("application1");
        ticket.setFromUser("creationUser");
        ticket.setSubject("createticket1_subject");
        ticket.setContent("createticket1_content");
        ticket.setCreatedAt(new Date(1439052010883L));

        Ticket ticketCreated = ticketRepository.create(ticket);

        assertNotNull(ticketCreated, "Ticket created is null");

        List<Ticket> tickets = ticketRepository
            .search(
                new TicketCriteria.Builder().fromUser("creationUser").build(),
                null,
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
            )
            .getContent();

        assertTrue(tickets.size() == 1, "Unable to find saved ticket");
        Ticket ticketFound = tickets.get(0);

        assertEquals(ticket.getId(), ticketFound.getId(), "Invalid saved ticket id.");
        assertEquals(ticket.getApi(), ticketFound.getApi(), "Invalid saved ticket api.");
        assertEquals(ticket.getApplication(), ticketFound.getApplication(), "Invalid saved ticket application.");
        assertEquals(ticket.getFromUser(), ticketFound.getFromUser(), "Invalid saved ticket fromUser.");
        assertEquals(ticket.getSubject(), ticketFound.getSubject(), "Invalid saved ticket subject.");
        assertEquals(ticket.getContent(), ticketFound.getContent(), "Invalid saved ticket content.");
        assertTrue(compareDate(ticket.getCreatedAt(), ticketFound.getCreatedAt()));
    }

    @Test
    public void should_delete_by_api_id() throws Exception {
        var nbBeforeDeletion = ticketRepository
            .findAll()
            .stream()
            .filter(t -> "api-to-delete".equals(t.getApi()))
            .count();
        var nbDeleted = ticketRepository.deleteByApiId("api-to-delete").size();

        assertThat(nbBeforeDeletion).isEqualTo(2);
        assertThat(nbDeleted).isEqualTo(2);
        assertThat(
            ticketRepository
                .findAll()
                .stream()
                .filter(t -> "api-to-delete".equals(t.getApi()))
                .count()
        ).isEqualTo(0);
    }

    @Test
    public void should_delete_by_application_id() throws Exception {
        var nbBeforeDeletion = ticketRepository
            .findAll()
            .stream()
            .filter(t -> "app-to-delete".equals(t.getApplication()))
            .count();
        var nbDeleted = ticketRepository.deleteByApplicationId("app-to-delete").size();

        assertThat(nbBeforeDeletion).isEqualTo(2);
        assertThat(nbDeleted).isEqualTo(2);
        assertThat(
            ticketRepository
                .findAll()
                .stream()
                .filter(t -> "app-to-delete".equals(t.getApplication()))
                .count()
        ).isEqualTo(0);
    }
}
