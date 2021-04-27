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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.TicketEntity;
import io.gravitee.rest.api.model.api.TicketQuery;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.service.exceptions.TicketNotFoundException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlatformTicketsResourceTest extends AbstractResourceTest {

    private static final String TICKET = "my-ticket";

    private TicketEntity fakeTicketEntity;

    @Override
    protected String contextPath() {
        return "tickets/";
    }

    @Before
    public void setUp() {
        reset(ticketService);

        fakeTicketEntity = new TicketEntity();
        fakeTicketEntity.setId(TICKET);
        fakeTicketEntity.setSubject("subject");
    }

    @Test
    public void shouldFindTicket() {
        when(ticketService.findById(any(String.class))).thenReturn(fakeTicketEntity);

        Response ticket = envTarget(TICKET).request().get();

        assertEquals(OK_200, ticket.getStatus());
    }

    @Test
    public void shouldNotFindTicket() {
        when(ticketService.findById(any(String.class))).thenThrow(new TicketNotFoundException("id"));

        Response ticket = envTarget(TICKET).request().get();

        assertEquals(NOT_FOUND_404, ticket.getStatus());
    }

    @Test
    public void shouldSearchTickets() {
        ArgumentCaptor<TicketQuery> queryCaptor = ArgumentCaptor.forClass(TicketQuery.class);
        ArgumentCaptor<SortableImpl> sortableCaptor = ArgumentCaptor.forClass(SortableImpl.class);

        when(ticketService.search(queryCaptor.capture(), sortableCaptor.capture(), any()))
            .thenReturn(new Page<>(singletonList(fakeTicketEntity), 1, 1, 1));

        Response response = envTarget().queryParam("page", 1).queryParam("size", 10).queryParam("order", "-subject").request().get();

        assertEquals(OK_200, response.getStatus());
        TicketQuery query = queryCaptor.getValue();
        SortableImpl sortable = sortableCaptor.getValue();
        assertEquals("Query user", USER_NAME, query.getFromUser());

        assertEquals("Sort field", "subject", sortable.getField());
        assertEquals("Sort order", false, sortable.isAscOrder());

        verify(ticketService, Mockito.times(1))
            .search(
                any(),
                argThat(o -> o.getField().equals("subject") && !o.isAscOrder()),
                argThat(o -> o.getPageNumber() == 1 && o.getPageSize() == 10)
            );
    }

    @Test
    public void shouldSearchTicketsWithoutSorting() {
        ArgumentCaptor<TicketQuery> queryCaptor = ArgumentCaptor.forClass(TicketQuery.class);

        when(ticketService.search(queryCaptor.capture(), any(), any())).thenReturn(new Page<>(singletonList(fakeTicketEntity), 1, 1, 1));

        Response response = envTarget().queryParam("page", 1).queryParam("size", 10).request().get();

        assertEquals(OK_200, response.getStatus());
        TicketQuery query = queryCaptor.getValue();
        assertEquals("Query user", USER_NAME, query.getFromUser());

        verify(ticketService, Mockito.times(1)).search(any(), isNull(), argThat(o -> o.getPageNumber() == 1 && o.getPageSize() == 10));
    }
}
