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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.gravitee.repository.config.AbstractManagementRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.TicketCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Ticket;
import java.util.Date;
import java.util.List;
import org.junit.Test;

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
        assertEquals("Invalid tickets numbers in search", 4, tickets.size());
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
        assertEquals("Invalid tickets numbers in search", 4, tickets.size());
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
        assertEquals("Invalid tickets numbers in search", 4, tickets.size());
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
        assertEquals("Invalid tickets numbers in search", 3, tickets.size());
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

        assertNotNull("Ticket created is null", ticketCreated);

        List<Ticket> tickets = ticketRepository
            .search(
                new TicketCriteria.Builder().fromUser("creationUser").build(),
                null,
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
            )
            .getContent();

        assertTrue("Unable to find saved ticket", tickets.size() == 1);
        Ticket ticketFound = tickets.get(0);

        assertEquals("Invalid saved ticket id.", ticket.getId(), ticketFound.getId());
        assertEquals("Invalid saved ticket api.", ticket.getApi(), ticketFound.getApi());
        assertEquals("Invalid saved ticket application.", ticket.getApplication(), ticketFound.getApplication());
        assertEquals("Invalid saved ticket fromUser.", ticket.getFromUser(), ticketFound.getFromUser());
        assertEquals("Invalid saved ticket subject.", ticket.getSubject(), ticketFound.getSubject());
        assertEquals("Invalid saved ticket content.", ticket.getContent(), ticketFound.getContent());
        assertTrue(compareDate(ticket.getCreatedAt(), ticketFound.getCreatedAt()));
    }
}
