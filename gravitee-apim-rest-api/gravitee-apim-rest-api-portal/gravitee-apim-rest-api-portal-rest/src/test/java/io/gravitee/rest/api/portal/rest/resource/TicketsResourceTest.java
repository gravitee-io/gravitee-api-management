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
package io.gravitee.rest.api.portal.rest.resource;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.TicketEntity;
import io.gravitee.rest.api.model.api.TicketQuery;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.portal.rest.model.TicketInput;
import io.gravitee.rest.api.portal.rest.model.TicketsResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TicketsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "tickets";
    }

    @Test
    public void shouldCreate() {
        resetAllMocks();

        TicketInput input = new TicketInput().subject("A").content("B");
        final Response response = target().request().post(Entity.json(input));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());

        assertNull(response.getHeaders().getFirst(HttpHeaders.LOCATION));

        verify(ticketMapper).convert(input);
        verify(ticketService)
            .create(eq(GraviteeContext.getExecutionContext()), eq(USER_NAME), any(), eq("DEFAULT"), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldSearchTickets() {
        resetAllMocks();

        TicketEntity ticketEntity = new TicketEntity();
        ticketEntity.setId("1");

        ArgumentCaptor<TicketQuery> queryCaptor = ArgumentCaptor.forClass(TicketQuery.class);
        ArgumentCaptor<SortableImpl> sortableCaptor = ArgumentCaptor.forClass(SortableImpl.class);

        when(ticketService.search(eq(GraviteeContext.getExecutionContext()), queryCaptor.capture(), sortableCaptor.capture(), any()))
            .thenReturn(new Page<>(singletonList(ticketEntity), 1, 1, 1));

        Response response = target()
            .queryParam("page", 1)
            .queryParam("size", 10)
            .queryParam("apiId", "apiId")
            .queryParam("order", "-subject")
            .request()
            .get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        TicketQuery query = queryCaptor.getValue();
        SortableImpl sortable = sortableCaptor.getValue();
        assertEquals("Criteria user", USER_NAME, query.getFromUser());
        assertEquals("Criteria api", "apiId", query.getApi());
        assertEquals("Query sort field", "subject", sortable.getField());
        assertEquals("Query sort order", false, sortable.isAscOrder());

        verify(ticketService, Mockito.times(1))
            .search(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                argThat(o -> o.getField().equals("subject") && !o.isAscOrder()),
                argThat(o -> o.getPageNumber() == 1 && o.getPageSize() == 10)
            );

        TicketsResponse ticketsResponse = response.readEntity(TicketsResponse.class);
        assertEquals("Ticket list had not the good size", 1, ticketsResponse.getData().size());
    }

    @Test
    public void shouldSearchTicketsWithoutSorting() {
        resetAllMocks();

        TicketEntity ticketEntity = new TicketEntity();
        ticketEntity.setId("1");

        ArgumentCaptor<TicketQuery> queryCaptor = ArgumentCaptor.forClass(TicketQuery.class);

        when(ticketService.search(eq(GraviteeContext.getExecutionContext()), queryCaptor.capture(), any(), any()))
            .thenReturn(new Page<>(singletonList(ticketEntity), 1, 1, 1));

        Response response = target().queryParam("apiId", "apiId").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        TicketQuery query = queryCaptor.getValue();
        assertEquals("Criteria user", USER_NAME, query.getFromUser());
        assertEquals("Criteria api", "apiId", query.getApi());

        verify(ticketService, Mockito.times(1))
            .search(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                isNull(),
                argThat(o -> o.getPageNumber() == 1 && o.getPageSize() == 10)
            );

        TicketsResponse ticketsResponse = response.readEntity(TicketsResponse.class);
        assertEquals("Ticket list had not the good size", 1, ticketsResponse.getData().size());
    }
}
