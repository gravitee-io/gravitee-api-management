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
package io.gravitee.rest.api.service;

import static io.gravitee.rest.api.service.builder.EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_SUPPORT_TICKET;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TicketRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.TicketCriteria;
import io.gravitee.repository.management.model.Ticket;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.TicketQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.EmailRequiredException;
import io.gravitee.rest.api.service.exceptions.SupportUnavailableException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.TicketNotFoundException;
import io.gravitee.rest.api.service.impl.TicketServiceImpl;
import io.gravitee.rest.api.service.impl.upgrade.DefaultMetadataUpgrader;
import io.gravitee.rest.api.service.notification.PortalHook;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TicketServiceTest {

    private static final String USERNAME = "my-username";
    private static final String USER_EMAIL = "my@email.com";
    private static final String USER_FIRSTNAME = "Firstname";
    private static final String USER_LASTNAME = "Lastname";
    private static final String API_ID = "my-api-id";
    private static final String APPLICATION_ID = "my-application-id";
    private static final String EMAIL_CONTENT = "Email\nContent";
    private static final String EMAIL_SUBJECT = "Email\nSubject";
    private static final boolean EMAIL_COPY_TO_SENDER = false;
    private static final String EMAIL_SUPPORT = "email@support.com";

    private static final String REFERENCE_ID = "DEFAULT";
    private static final ParameterReferenceType REFERENCE_TYPE = ParameterReferenceType.ORGANIZATION;

    @InjectMocks
    private TicketService ticketService = new TicketServiceImpl();

    @Mock
    private UserService userService;

    @Mock
    private MetadataService metadataService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private EmailService emailService;

    @Mock
    private NewTicketEntity newTicketEntity;

    @Mock
    private UserEntity user;

    @Mock
    private ApiModelEntity api;

    @Mock
    private ApplicationEntity application;

    @Mock
    private ParameterService mockParameterService;

    @Mock
    private NotifierService mockNotifierService;

    @Mock
    private TicketRepository ticketRepository;

    @Test(expected = SupportUnavailableException.class)
    public void shouldNotCreateIfSupportDisabled() {
        when(mockParameterService.findAsBoolean(Key.CONSOLE_SUPPORT_ENABLED, REFERENCE_ID, REFERENCE_TYPE)).thenReturn(Boolean.FALSE);
        ticketService.create(USERNAME, newTicketEntity, REFERENCE_ID, REFERENCE_TYPE);
        verify(mockNotifierService, never()).trigger(eq(PortalHook.NEW_SUPPORT_TICKET), anyMap());
    }

    @Test(expected = SupportUnavailableException.class)
    public void shouldNotCreateIfSupportForPortalDisabled() {
        when(mockParameterService.findAsBoolean(Key.PORTAL_SUPPORT_ENABLED, REFERENCE_ID, ParameterReferenceType.ENVIRONMENT))
            .thenReturn(Boolean.FALSE);
        ticketService.create(USERNAME, newTicketEntity, REFERENCE_ID, ParameterReferenceType.ENVIRONMENT);
        verify(mockNotifierService, never()).trigger(eq(PortalHook.NEW_SUPPORT_TICKET), anyMap());
    }

    @Test(expected = EmailRequiredException.class)
    public void shouldNotCreateIfUserEmailIsMissing() {
        when(mockParameterService.findAsBoolean(Key.CONSOLE_SUPPORT_ENABLED, REFERENCE_ID, REFERENCE_TYPE)).thenReturn(Boolean.TRUE);
        when(userService.findById(USERNAME)).thenReturn(user);

        ticketService.create(USERNAME, newTicketEntity, REFERENCE_ID, REFERENCE_TYPE);
        verify(mockNotifierService, never()).trigger(eq(PortalHook.NEW_SUPPORT_TICKET), anyMap());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotCreateIfDefaultEmailSupportIsMissing() {
        when(mockParameterService.findAsBoolean(Key.CONSOLE_SUPPORT_ENABLED, REFERENCE_ID, REFERENCE_TYPE)).thenReturn(Boolean.TRUE);
        when(userService.findById(USERNAME)).thenReturn(user);
        when(user.getEmail()).thenReturn(USER_EMAIL);
        when(newTicketEntity.getApi()).thenReturn(API_ID);
        when(apiService.findByIdForTemplates(API_ID, true)).thenReturn(api);

        ticketService.create(USERNAME, newTicketEntity, REFERENCE_ID, REFERENCE_TYPE);
        verify(mockNotifierService, never()).trigger(eq(PortalHook.NEW_SUPPORT_TICKET), anyMap());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotCreateIfDefaultEmailSupportHasNotBeenChanged() {
        when(mockParameterService.findAsBoolean(Key.CONSOLE_SUPPORT_ENABLED, REFERENCE_ID, REFERENCE_TYPE)).thenReturn(Boolean.TRUE);
        when(newTicketEntity.getApi()).thenReturn(API_ID);

        when(userService.findById(USERNAME)).thenReturn(user);
        when(user.getEmail()).thenReturn(USER_EMAIL);
        when(apiService.findByIdForTemplates(API_ID, true)).thenReturn(api);

        final Map<String, String> metadata = new HashMap<>();
        metadata.put(DefaultMetadataUpgrader.METADATA_EMAIL_SUPPORT_KEY, DefaultMetadataUpgrader.DEFAULT_METADATA_EMAIL_SUPPORT);
        when(api.getMetadata()).thenReturn(metadata);

        ticketService.create(USERNAME, newTicketEntity, REFERENCE_ID, REFERENCE_TYPE);
        verify(mockNotifierService, never()).trigger(eq(PortalHook.NEW_SUPPORT_TICKET), anyMap());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateIfRepositoryThrowTechnicalException() throws TechnicalException {
        when(mockParameterService.findAsBoolean(Key.CONSOLE_SUPPORT_ENABLED, REFERENCE_ID, REFERENCE_TYPE)).thenReturn(Boolean.TRUE);
        when(newTicketEntity.getApi()).thenReturn(API_ID);
        when(newTicketEntity.getApplication()).thenReturn(APPLICATION_ID);
        when(newTicketEntity.isCopyToSender()).thenReturn(EMAIL_COPY_TO_SENDER);
        when(newTicketEntity.getContent()).thenReturn(EMAIL_CONTENT);
        when(newTicketEntity.getSubject()).thenReturn(EMAIL_SUBJECT);

        when(userService.findById(USERNAME)).thenReturn(user);
        when(user.getEmail()).thenReturn(USER_EMAIL);
        when(user.getFirstname()).thenReturn(USER_FIRSTNAME);
        when(user.getLastname()).thenReturn(USER_LASTNAME);
        when(apiService.findByIdForTemplates(API_ID, true)).thenReturn(api);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);

        when(ticketRepository.create(any())).thenThrow(new TechnicalException());

        final Map<String, String> metadata = new HashMap<>();
        metadata.put(DefaultMetadataUpgrader.METADATA_EMAIL_SUPPORT_KEY, EMAIL_SUPPORT);
        when(api.getMetadata()).thenReturn(metadata);

        ticketService.create(USERNAME, newTicketEntity, REFERENCE_ID, REFERENCE_TYPE);
    }

    @Test
    public void shouldCreateWithApi() throws TechnicalException {
        when(mockParameterService.findAsBoolean(Key.CONSOLE_SUPPORT_ENABLED, REFERENCE_ID, REFERENCE_TYPE)).thenReturn(Boolean.TRUE);
        when(newTicketEntity.getApi()).thenReturn(API_ID);
        when(newTicketEntity.getApplication()).thenReturn(APPLICATION_ID);
        when(newTicketEntity.getSubject()).thenReturn(EMAIL_SUBJECT);
        when(newTicketEntity.isCopyToSender()).thenReturn(EMAIL_COPY_TO_SENDER);
        when(newTicketEntity.getContent()).thenReturn(EMAIL_CONTENT);

        when(userService.findById(USERNAME)).thenReturn(user);
        when(user.getEmail()).thenReturn(USER_EMAIL);
        when(user.getFirstname()).thenReturn(USER_FIRSTNAME);
        when(user.getLastname()).thenReturn(USER_LASTNAME);
        when(apiService.findByIdForTemplates(API_ID, true)).thenReturn(api);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);

        Ticket ticketToCreate = new Ticket();
        ticketToCreate.setId("generatedId");
        ticketToCreate.setApi(API_ID);
        ticketToCreate.setApplication(APPLICATION_ID);
        ticketToCreate.setSubject(EMAIL_SUBJECT);
        ticketToCreate.setContent(EMAIL_CONTENT);
        ticketToCreate.setCreatedAt(new Date());
        ticketToCreate.setFromUser(USERNAME);
        when(ticketRepository.create(any(Ticket.class))).thenReturn(ticketToCreate);

        final Map<String, String> metadata = new HashMap<>();
        metadata.put(DefaultMetadataUpgrader.METADATA_EMAIL_SUPPORT_KEY, EMAIL_SUPPORT);
        when(api.getMetadata()).thenReturn(metadata);

        TicketEntity createdTicket = ticketService.create(USERNAME, newTicketEntity, REFERENCE_ID, REFERENCE_TYPE);

        verify(emailService)
            .sendEmailNotification(
                new EmailNotificationBuilder()
                    .replyTo(USER_EMAIL)
                    .fromName(USER_FIRSTNAME + ' ' + USER_LASTNAME)
                    .to(EMAIL_SUPPORT)
                    .copyToSender(EMAIL_COPY_TO_SENDER)
                    .template(TEMPLATES_FOR_ACTION_SUPPORT_TICKET)
                    .params(
                        ImmutableMap.of(
                            "user",
                            user,
                            "api",
                            api,
                            "content",
                            "Email<br />Content",
                            "application",
                            application,
                            "ticketSubject",
                            EMAIL_SUBJECT
                        )
                    )
                    .build()
            );
        verify(mockNotifierService, times(1)).trigger(eq(PortalHook.NEW_SUPPORT_TICKET), anyMap());

        assertEquals("Invalid saved ticket id", createdTicket.getId(), ticketToCreate.getId());
        assertEquals("Invalid saved ticket api", createdTicket.getApi(), ticketToCreate.getApi());
        assertEquals("Invalid saved ticket application", createdTicket.getApplication(), ticketToCreate.getApplication());
        assertEquals("Invalid saved ticket subject", createdTicket.getSubject(), ticketToCreate.getSubject());
        assertEquals("Invalid saved ticket content", createdTicket.getContent(), ticketToCreate.getContent());
        assertEquals("Invalid saved ticket from user", createdTicket.getFromUser(), ticketToCreate.getFromUser());
        assertEquals("Invalid saved ticket created at", createdTicket.getCreatedAt(), ticketToCreate.getCreatedAt());
    }

    @Test
    public void shouldSearchForTicketsFromUser() throws TechnicalException {
        Ticket ticket = new Ticket();
        ticket.setId("generatedId");
        ticket.setApi(API_ID);
        ticket.setApplication(APPLICATION_ID);
        ticket.setSubject(EMAIL_SUBJECT);
        ticket.setContent(EMAIL_CONTENT);
        ticket.setCreatedAt(new Date());
        ticket.setFromUser(USERNAME);

        ApiEntity apiEntity = new ApiEntity();
        ApplicationEntity appEntity = new ApplicationEntity();

        List<Ticket> ticketList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Ticket t = new Ticket(ticket);
            t.setId("ticket" + i);
            ticketList.add(t);
        }

        when(ticketRepository.search(any(TicketCriteria.class), any(Sortable.class), any(Pageable.class)))
            .thenReturn(new Page<>(ticketList, 0, 20, 20));
        when(apiService.findById(API_ID)).thenReturn(apiEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(appEntity);

        TicketQuery query = new TicketQuery();
        query.setFromUser("fromUser");

        Page<TicketEntity> searchResult = ticketService.search(
            query,
            new SortableImpl("subject", true),
            new PageableImpl(1, Integer.MAX_VALUE)
        );

        assertEquals(searchResult.getContent().size(), 20);
        assertEquals(searchResult.getPageNumber(), 1);
        assertEquals(searchResult.getTotalElements(), 20);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotSearchSearchIfRepositoryThrowException() throws TechnicalException {
        Ticket ticket = new Ticket();
        ticket.setId("generatedId");
        ticket.setApi(API_ID);
        ticket.setApplication(APPLICATION_ID);
        ticket.setSubject(EMAIL_SUBJECT);
        ticket.setContent(EMAIL_CONTENT);
        ticket.setCreatedAt(new Date());
        ticket.setFromUser(USERNAME);

        List<Ticket> ticketList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Ticket t = new Ticket(ticket);
            t.setId("ticket" + i);
            ticketList.add(t);
        }

        when(ticketRepository.search(any(TicketCriteria.class), any(Sortable.class), any(Pageable.class)))
            .thenThrow(new TechnicalException());

        TicketQuery query = new TicketQuery();
        query.setFromUser("fromUser");

        ticketService.search(query, new SortableImpl("subject", true), new PageableImpl(1, Integer.MAX_VALUE));
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Ticket ticket = new Ticket();
        ticket.setId("ticket1");
        ticket.setApi(API_ID);
        ticket.setApplication(APPLICATION_ID);
        ticket.setSubject(EMAIL_SUBJECT);
        ticket.setContent(EMAIL_CONTENT);
        ticket.setCreatedAt(new Date());
        ticket.setFromUser(USERNAME);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setName("apiName");
        ApplicationEntity appEntity = new ApplicationEntity();
        appEntity.setName("appName");

        when(ticketRepository.findById("ticket1")).thenReturn(Optional.of(ticket));
        when(apiService.findById(API_ID)).thenReturn(apiEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(appEntity);

        TicketEntity ticketEntity = ticketService.findById("ticket1");

        assertEquals("ticket1", ticketEntity.getId());
        assertEquals("apiName", ticketEntity.getApi());
        assertEquals("appName", ticketEntity.getApplication());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdIfRepositoryThrowException() throws TechnicalException {
        Ticket ticket = new Ticket();
        ticket.setId("ticket1");
        ticket.setApi(API_ID);
        ticket.setApplication(APPLICATION_ID);
        ticket.setSubject(EMAIL_SUBJECT);
        ticket.setContent(EMAIL_CONTENT);
        ticket.setCreatedAt(new Date());
        ticket.setFromUser(USERNAME);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setName("apiName");
        ApplicationEntity appEntity = new ApplicationEntity();
        appEntity.setName("appName");

        when(ticketRepository.findById("ticket1")).thenThrow(new TechnicalException());

        ticketService.findById("ticket1");
    }

    @Test(expected = TicketNotFoundException.class)
    public void shouldThrowNotFoundExceptionWhenNoTicket() throws TechnicalException {
        when(ticketRepository.findById("ticket1")).thenReturn(Optional.empty());

        ticketService.findById("ticket1");
    }
}
