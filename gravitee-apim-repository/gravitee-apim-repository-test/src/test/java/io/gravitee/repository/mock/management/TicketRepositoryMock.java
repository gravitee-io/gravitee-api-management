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

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.TicketRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.TicketCriteria;
import io.gravitee.repository.management.model.Ticket;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TicketRepositoryMock extends AbstractRepositoryMock<TicketRepository> {

    public TicketRepositoryMock() {
        super(TicketRepository.class);
    }

    @Override
    protected void prepare(TicketRepository ticketRepository) throws Exception {
        final Ticket ticket0 = mock(Ticket.class);
        when(ticket0.getId()).thenReturn("ticket0");
        final Ticket ticket1 = mock(Ticket.class);
        when(ticket1.getId()).thenReturn("ticket1");
        final Ticket ticket2 = mock(Ticket.class);
        when(ticket2.getId()).thenReturn("ticket2");
        final Ticket ticket3 = mock(Ticket.class);
        when(ticket3.getId()).thenReturn("ticket3");
        Page<Ticket> searchAllResult = new Page<>(asList(ticket0, ticket1, ticket2, ticket3), 0, 0, 4);

        // shouldSearchWithNullCriteria
        when(ticketRepository.search(nullable(TicketCriteria.class), nullable(Sortable.class), any())).thenReturn(searchAllResult);

        // shouldSearchAllWithEmptyCriteria
        when(ticketRepository.search(argThat(o -> o != null && o.getFromUser() == null), nullable(Sortable.class), any()))
            .thenReturn(searchAllResult);

        // shouldSearchAllWithCriteriaFromUserEmpty
        when(
            ticketRepository.search(
                argThat(o -> o != null && o.getFromUser() != null && o.getFromUser().equals("")),
                nullable(Sortable.class),
                any()
            )
        )
            .thenReturn(searchAllResult);

        // shouldSearchAllWithCriteriaFromUser0SortedBySubjectDescendingOnThreeElements
        Page<Ticket> searchFromUser0SortedBySubject = new Page<>(asList(ticket3, ticket2, ticket1), 0, 3, 3);
        when(
            ticketRepository.search(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getFromUser() != null &&
                        criteria.getFromUser().equals("user0") &&
                        criteria.getApi().equals("api0")
                ),
                argThat(sortable -> sortable != null && sortable.field().equals("subject") && sortable.order().equals(Order.DESC)),
                argThat(page -> page != null && page.pageNumber() == 0 && page.pageSize() == 3)
            )
        )
            .thenReturn(searchFromUser0SortedBySubject);

        // shouldCreateTicketTest
        Ticket ticket = mock(Ticket.class);
        when(ticket.getId()).thenReturn("createticket1");
        when(ticket.getApi()).thenReturn("api1");
        when(ticket.getApplication()).thenReturn("application1");
        when(ticket.getFromUser()).thenReturn("creationUser");
        when(ticket.getSubject()).thenReturn("createticket1_subject");
        when(ticket.getContent()).thenReturn("createticket1_content");
        when(ticket.getCreatedAt()).thenReturn(new Date(1439052010883L));

        when(ticketRepository.create(any(Ticket.class))).thenReturn(ticket);

        Page<Ticket> searchFromUserCreation = new Page<>(asList(ticket), 0, 0, 1);
        when(
            ticketRepository.search(
                argThat(o -> o != null && o.getFromUser() != null && o.getFromUser().equals("creationUser")),
                any(),
                any()
            )
        )
            .thenReturn(searchFromUserCreation);
    }
}
