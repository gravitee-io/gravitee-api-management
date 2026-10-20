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
package io.gravitee.rest.api.portal.rest.resource;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.NewTicketEntity;
import io.gravitee.rest.api.model.TicketEntity;
import io.gravitee.rest.api.model.api.TicketQuery;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.model.TicketInput;
import io.gravitee.rest.api.portal.rest.model.TicketsResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TicketsResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String APPLICATION = "my-application";

    @Override
    protected String contextPath() {
        return "tickets";
    }

    @Test
    public void shouldCreate() {
        resetAllMocks();
        mapTicketInputs();

        TicketInput input = new TicketInput().subject("A").content("B");
        final Response response = target().request().post(Entity.json(input));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());

        assertNull(response.getHeaders().getFirst(HttpHeaders.LOCATION));

        verify(ticketMapper).convert(input);
        verifyNoInteractions(accessControlService, permissionService);
        verify(ticketService).create(
            eq(GraviteeContext.getExecutionContext()),
            eq(USER_NAME),
            any(),
            eq("DEFAULT"),
            eq(ParameterReferenceType.ENVIRONMENT)
        );
    }

    @Test
    public void shouldCreateWhenApiIsAccessibleFromPortal() {
        resetAllMocks();
        mapTicketInputs();
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(true);

        final Response response = target().request().post(Entity.json(new TicketInput().subject("A").content("B").api(API)));

        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        verify(ticketService).create(any(), eq(USER_NAME), argThat(ticket -> API.equals(ticket.getApi())), any(), any());
    }

    @Test
    public void shouldNotCreateWhenApiIsNotAccessibleFromPortal() {
        resetAllMocks();
        mapTicketInputs();
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(false);

        final Response response = target().request().post(Entity.json(new TicketInput().subject("A").content("B").api(API)));

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        verifyNoInteractions(ticketService);
    }

    @Test
    public void shouldCreateWhenApplicationIsReadable() {
        resetAllMocks();
        mapTicketInputs();
        allowApplicationRead(true);

        final Response response = target()
            .request()
            .post(Entity.json(new TicketInput().subject("A").content("B").application(APPLICATION)));

        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        verify(ticketService).create(any(), eq(USER_NAME), argThat(ticket -> APPLICATION.equals(ticket.getApplication())), any(), any());
    }

    @Test
    public void shouldNotCreateWhenApplicationIsNotReadable() {
        resetAllMocks();
        mapTicketInputs();
        allowApplicationRead(false);

        final Response response = target()
            .request()
            .post(Entity.json(new TicketInput().subject("A").content("B").application(APPLICATION)));

        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        verifyNoInteractions(ticketService);
    }

    @Test
    public void shouldNotCreateWhenApiIsAccessibleButApplicationIsNotReadable() {
        resetAllMocks();
        mapTicketInputs();
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(true);
        allowApplicationRead(false);

        final Response response = target()
            .request()
            .post(Entity.json(new TicketInput().subject("A").content("B").api(API).application(APPLICATION)));

        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        verifyNoInteractions(ticketService);
    }

    @Test
    public void shouldNotCreateWhenApplicationIsReadableButApiIsNotAccessible() {
        resetAllMocks();
        mapTicketInputs();
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(false);
        allowApplicationRead(true);

        final Response response = target()
            .request()
            .post(Entity.json(new TicketInput().subject("A").content("B").api(API).application(APPLICATION)));

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        verifyNoInteractions(ticketService);
    }

    @Test
    public void shouldSearchTickets() {
        resetAllMocks();

        TicketEntity ticketEntity = new TicketEntity();
        ticketEntity.setId("1");

        ArgumentCaptor<TicketQuery> queryCaptor = ArgumentCaptor.forClass(TicketQuery.class);
        ArgumentCaptor<SortableImpl> sortableCaptor = ArgumentCaptor.forClass(SortableImpl.class);

        when(
            ticketService.search(eq(GraviteeContext.getExecutionContext()), queryCaptor.capture(), sortableCaptor.capture(), any())
        ).thenReturn(new Page<>(singletonList(ticketEntity), 1, 1, 1));

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
        assertEquals(USER_NAME, query.getFromUser(), "Criteria user");
        assertEquals("apiId", query.getApi(), "Criteria api");
        assertEquals("subject", sortable.getField(), "Query sort field");
        assertEquals(false, sortable.isAscOrder(), "Query sort order");

        verify(ticketService, Mockito.times(1)).search(
            eq(GraviteeContext.getExecutionContext()),
            any(),
            argThat(o -> o.getField().equals("subject") && !o.isAscOrder()),
            argThat(o -> o.getPageNumber() == 1 && o.getPageSize() == 10)
        );

        TicketsResponse ticketsResponse = response.readEntity(TicketsResponse.class);
        assertEquals(1, ticketsResponse.getData().size(), "Ticket list had not the good size");
    }

    @Test
    public void shouldSearchTicketsWithoutSorting() {
        resetAllMocks();

        TicketEntity ticketEntity = new TicketEntity();
        ticketEntity.setId("1");

        ArgumentCaptor<TicketQuery> queryCaptor = ArgumentCaptor.forClass(TicketQuery.class);

        when(ticketService.search(eq(GraviteeContext.getExecutionContext()), queryCaptor.capture(), any(), any())).thenReturn(
            new Page<>(singletonList(ticketEntity), 1, 1, 1)
        );

        Response response = target().queryParam("apiId", "apiId").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        TicketQuery query = queryCaptor.getValue();
        assertEquals(USER_NAME, query.getFromUser(), "Criteria user");
        assertEquals("apiId", query.getApi(), "Criteria api");

        verify(ticketService, Mockito.times(1)).search(
            eq(GraviteeContext.getExecutionContext()),
            any(),
            isNull(),
            argThat(o -> o.getPageNumber() == 1 && o.getPageSize() == 10)
        );

        TicketsResponse ticketsResponse = response.readEntity(TicketsResponse.class);
        assertEquals(1, ticketsResponse.getData().size(), "Ticket list had not the good size");
    }

    private void mapTicketInputs() {
        when(ticketMapper.convert(any())).thenAnswer(invocation -> {
            TicketInput input = invocation.getArgument(0);
            NewTicketEntity ticket = new NewTicketEntity();
            ticket.setApi(input.getApi());
            ticket.setApplication(input.getApplication());
            ticket.setSubject(input.getSubject());
            ticket.setContent(input.getContent());
            return ticket;
        });
    }

    private void allowApplicationRead(boolean allowed) {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.APPLICATION_DEFINITION,
                APPLICATION,
                RolePermissionAction.READ
            )
        ).thenReturn(allowed);
    }
}
