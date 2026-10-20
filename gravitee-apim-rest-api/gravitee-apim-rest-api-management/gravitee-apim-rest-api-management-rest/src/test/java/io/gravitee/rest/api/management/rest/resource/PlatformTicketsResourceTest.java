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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.NewTicketEntity;
import io.gravitee.rest.api.model.TicketEntity;
import io.gravitee.rest.api.model.api.TicketQuery;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TicketNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlatformTicketsResourceTest extends AbstractResourceTest {

    private static final String TICKET = "my-ticket";
    private static final String API = "my-api";
    private static final String APPLICATION = "my-application";

    private TicketEntity fakeTicketEntity;

    @Override
    protected String contextPath() {
        return "tickets/";
    }

    @BeforeEach
    public void init() {
        reset(ticketService);

        fakeTicketEntity = new TicketEntity();
        fakeTicketEntity.setId(TICKET);
        fakeTicketEntity.setSubject("subject");
    }

    @Test
    public void shouldCreateGenericTicket() {
        Response response = envTarget().request().post(Entity.json(newTicket(null, null)));

        assertEquals(CREATED_201, response.getStatus());
        verify(ticketService).create(any(), eq(USER_NAME), any(), any(), eq(ParameterReferenceType.ORGANIZATION));
    }

    @Test
    public void shouldCreateWhenApiAndApplicationAreReadable() {
        Response response = envTarget().request().post(Entity.json(newTicket(API, APPLICATION)));

        assertEquals(CREATED_201, response.getStatus());
        verify(ticketService).create(
            any(),
            eq(USER_NAME),
            argThat(ticket -> API.equals(ticket.getApi()) && APPLICATION.equals(ticket.getApplication())),
            any(),
            any()
        );
    }

    @Test
    public void shouldNotCreateWhenApiIsNotReadable() {
        denyRead(RolePermission.API_DEFINITION, API);

        Response response = envTarget().request().post(Entity.json(newTicket(API, null)));

        assertEquals(FORBIDDEN_403, response.getStatus());
        verifyNoInteractions(ticketService);
    }

    @Test
    public void shouldNotCreateWhenApplicationIsNotReadable() {
        denyRead(RolePermission.APPLICATION_DEFINITION, APPLICATION);

        Response response = envTarget().request().post(Entity.json(newTicket(null, APPLICATION)));

        assertEquals(FORBIDDEN_403, response.getStatus());
        verifyNoInteractions(ticketService);
    }

    @Test
    public void shouldNotCreateWhenApiIsReadableButApplicationIsNotReadable() {
        denyRead(RolePermission.APPLICATION_DEFINITION, APPLICATION);

        Response response = envTarget().request().post(Entity.json(newTicket(API, APPLICATION)));

        assertEquals(FORBIDDEN_403, response.getStatus());
        verifyNoInteractions(ticketService);
    }

    @Test
    public void shouldNotCreateWhenApplicationIsReadableButApiIsNotReadable() {
        denyRead(RolePermission.API_DEFINITION, API);

        Response response = envTarget().request().post(Entity.json(newTicket(API, APPLICATION)));

        assertEquals(FORBIDDEN_403, response.getStatus());
        verifyNoInteractions(ticketService);
    }

    @Test
    public void shouldFindTicket() {
        when(ticketService.findById(eq(GraviteeContext.getExecutionContext()), any(String.class))).thenReturn(fakeTicketEntity);

        Response ticket = envTarget(TICKET).request().get();

        assertEquals(OK_200, ticket.getStatus());
    }

    @Test
    public void shouldNotFindTicket() {
        when(ticketService.findById(eq(GraviteeContext.getExecutionContext()), any(String.class))).thenThrow(
            new TicketNotFoundException("id")
        );

        Response ticket = envTarget(TICKET).request().get();

        assertEquals(NOT_FOUND_404, ticket.getStatus());
    }

    @Test
    public void shouldSearchTickets() {
        ArgumentCaptor<TicketQuery> queryCaptor = ArgumentCaptor.forClass(TicketQuery.class);
        ArgumentCaptor<SortableImpl> sortableCaptor = ArgumentCaptor.forClass(SortableImpl.class);

        when(
            ticketService.search(eq(GraviteeContext.getExecutionContext()), queryCaptor.capture(), sortableCaptor.capture(), any())
        ).thenReturn(new Page<>(singletonList(fakeTicketEntity), 1, 1, 1));

        Response response = envTarget().queryParam("page", 1).queryParam("size", 10).queryParam("order", "-subject").request().get();

        assertEquals(OK_200, response.getStatus());
        TicketQuery query = queryCaptor.getValue();
        SortableImpl sortable = sortableCaptor.getValue();
        assertEquals(USER_NAME, query.getFromUser(), "Query user");

        assertEquals("subject", sortable.getField(), "Sort field");
        assertEquals(false, sortable.isAscOrder(), "Sort order");

        verify(ticketService, Mockito.times(1)).search(
            eq(GraviteeContext.getExecutionContext()),
            any(),
            argThat(o -> o.getField().equals("subject") && !o.isAscOrder()),
            argThat(o -> o.getPageNumber() == 1 && o.getPageSize() == 10)
        );
    }

    @Test
    public void shouldSearchTicketsWithoutSorting() {
        ArgumentCaptor<TicketQuery> queryCaptor = ArgumentCaptor.forClass(TicketQuery.class);

        when(ticketService.search(eq(GraviteeContext.getExecutionContext()), queryCaptor.capture(), any(), any())).thenReturn(
            new Page<>(singletonList(fakeTicketEntity), 1, 1, 1)
        );

        Response response = envTarget().queryParam("page", 1).queryParam("size", 10).request().get();

        assertEquals(OK_200, response.getStatus());
        TicketQuery query = queryCaptor.getValue();
        assertEquals(USER_NAME, query.getFromUser(), "Query user");

        verify(ticketService, Mockito.times(1)).search(
            eq(GraviteeContext.getExecutionContext()),
            any(),
            isNull(),
            argThat(o -> o.getPageNumber() == 1 && o.getPageSize() == 10)
        );
    }

    private static NewTicketEntity newTicket(String api, String application) {
        NewTicketEntity ticket = new NewTicketEntity();
        ticket.setSubject("subject");
        ticket.setContent("content");
        ticket.setApi(api);
        ticket.setApplication(application);
        return ticket;
    }

    private void denyRead(RolePermission permission, String referenceId) {
        when(
            permissionService.hasPermission(GraviteeContext.getExecutionContext(), permission, referenceId, RolePermissionAction.READ)
        ).thenReturn(false);
    }
}
