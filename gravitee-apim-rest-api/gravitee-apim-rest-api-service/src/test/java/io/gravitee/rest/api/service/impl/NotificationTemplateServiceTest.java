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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.CommandTags;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.NotificationTemplateRepository;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.MessageRecipient;
import io.gravitee.repository.management.model.NotificationTemplate;
import io.gravitee.repository.management.model.NotificationTemplateReferenceType;
import io.gravitee.repository.management.model.NotificationTemplateType;
import io.gravitee.rest.api.model.notification.NotificationTemplateEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidTemplateException;
import io.gravitee.rest.api.service.exceptions.NotificationTemplateNotFoundException;
import io.gravitee.rest.api.service.exceptions.TemplateProcessingException;
import io.gravitee.rest.api.service.notification.HookScope;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.v4.mapper.NotificationTemplateMapper;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationTemplateServiceTest {

    private static final String ORGANIZATION_ID = "ORG_ID";

    private static final String NOTIFICATION_TEMPLATE_ID = "my-notif-template-id";
    private static final String NOTIFICATION_TEMPLATE_HOOK = "my-notif-template-hook";
    private static final String NOTIFICATION_TEMPLATE_SCOPE = HookScope.TEMPLATES_FOR_ALERT.name();
    private static final String NOTIFICATION_TEMPLATE_REFERENCE_ID = ORGANIZATION_ID;
    private static final NotificationTemplateReferenceType NOTIFICATION_TEMPLATE_REFERENCE_TYPE =
        NotificationTemplateReferenceType.ORGANIZATION;
    private static final String NOTIFICATION_TEMPLATE_NAME = "my-notif-template-name";
    private static final String NOTIFICATION_TEMPLATE_DESCRIPTION = "my-notif-template-description";
    private static final String NOTIFICATION_TEMPLATE_TITLE = "my-notif-template-title";
    private static final String NOTIFICATION_TEMPLATE_CONTENT = "my-notif-template-content";
    private static final NotificationTemplateType NOTIFICATION_TEMPLATE_TYPE = NotificationTemplateType.EMAIL;
    private static final Date NOTIFICATION_TEMPLATE_CREATED_AT = new Date(100000000L);
    private static final Date NOTIFICATION_TEMPLATE_UPDATED_AT = new Date(110000000L);
    private static final boolean NOTIFICATION_TEMPLATE_ENABLED = true;

    @Mock
    private NotificationTemplateRepository notificationTemplateRepository;

    @Mock
    private CommandRepository commandRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private EventManager eventManager;

    @Mock
    private Node node;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationTemplateMapper notificationTemplateMapper;

    @InjectMocks
    private NotificationTemplateService notificationTemplateService = new NotificationTemplateServiceImpl();

    NotificationTemplate notificationTemplate;

    @Before
    public void init() {
        notificationTemplate = new NotificationTemplate();
        notificationTemplate.setId(NOTIFICATION_TEMPLATE_ID);
        notificationTemplate.setHook(NOTIFICATION_TEMPLATE_HOOK);
        notificationTemplate.setScope(NOTIFICATION_TEMPLATE_SCOPE);
        notificationTemplate.setReferenceId(NOTIFICATION_TEMPLATE_REFERENCE_ID);
        notificationTemplate.setReferenceType(NOTIFICATION_TEMPLATE_REFERENCE_TYPE);
        notificationTemplate.setName(NOTIFICATION_TEMPLATE_NAME);
        notificationTemplate.setDescription(NOTIFICATION_TEMPLATE_DESCRIPTION);
        notificationTemplate.setTitle(NOTIFICATION_TEMPLATE_TITLE);
        notificationTemplate.setContent(NOTIFICATION_TEMPLATE_CONTENT);
        notificationTemplate.setType(NOTIFICATION_TEMPLATE_TYPE);
        notificationTemplate.setCreatedAt(NOTIFICATION_TEMPLATE_CREATED_AT);
        notificationTemplate.setUpdatedAt(NOTIFICATION_TEMPLATE_UPDATED_AT);
        notificationTemplate.setEnabled(NOTIFICATION_TEMPLATE_ENABLED);

        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test(expected = NotificationTemplateNotFoundException.class)
    public void shouldNotFindNotificationTemplate() throws TechnicalException {
        when(notificationTemplateRepository.findById(NOTIFICATION_TEMPLATE_ID)).thenReturn(Optional.empty());
        notificationTemplateService.findById(GraviteeContext.getCurrentOrganization(), NOTIFICATION_TEMPLATE_ID);
    }

    @Test(expected = NotificationTemplateNotFoundException.class)
    public void shouldNotFindNotificationTemplateBecauseDoesNotBelongToOrganization() throws TechnicalException {
        notificationTemplate.setReferenceId("Another_organization");
        when(notificationTemplateRepository.findById(NOTIFICATION_TEMPLATE_ID)).thenReturn(Optional.of(notificationTemplate));
        notificationTemplateService.findById(GraviteeContext.getCurrentOrganization(), NOTIFICATION_TEMPLATE_ID);
    }

    @Test
    public void shouldFindNotificationTemplate() throws TechnicalException {
        when(notificationTemplateRepository.findById(NOTIFICATION_TEMPLATE_ID)).thenReturn(Optional.of(notificationTemplate));
        final NotificationTemplateEntity foundTemplate = notificationTemplateService.findById(
            GraviteeContext.getCurrentOrganization(),
            NOTIFICATION_TEMPLATE_ID
        );

        assertNotNull(foundTemplate);
        assertEquals(notificationTemplate.getId(), foundTemplate.getId());
        assertEquals(notificationTemplate.getHook(), foundTemplate.getHook());
        assertEquals(notificationTemplate.getScope(), foundTemplate.getScope());
        assertEquals(notificationTemplate.getName(), foundTemplate.getName());
        assertEquals(notificationTemplate.getDescription(), foundTemplate.getDescription());
        assertEquals(notificationTemplate.getTitle(), foundTemplate.getTitle());
        assertEquals(notificationTemplate.getContent(), foundTemplate.getContent());
        assertEquals(notificationTemplate.getType().name(), foundTemplate.getType().name());
        assertEquals(notificationTemplate.getCreatedAt(), foundTemplate.getCreatedAt());
        assertEquals(notificationTemplate.getUpdatedAt(), foundTemplate.getUpdatedAt());
    }

    @Test
    public void shouldFindAllNotificationTemplate() throws TechnicalException {
        NotificationTemplate temp1 = new NotificationTemplate();
        temp1.setId("TEMP1");
        temp1.setType(NotificationTemplateType.PORTAL);
        NotificationTemplate temp2 = new NotificationTemplate();
        temp2.setId("TEMP2");
        temp2.setType(NotificationTemplateType.EMAIL);
        when(
            notificationTemplateRepository.findAllByReferenceIdAndReferenceType(
                NOTIFICATION_TEMPLATE_REFERENCE_ID,
                NOTIFICATION_TEMPLATE_REFERENCE_TYPE
            )
        ).thenReturn(Sets.newSet(temp1, temp2));

        final Set<NotificationTemplateEntity> all = notificationTemplateService.findAll(GraviteeContext.getCurrentOrganization());
        assertNotNull(all);
        assertEquals(2, all.size());
    }

    @Test
    public void shouldFindNotificationTemplateByType() throws TechnicalException {
        NotificationTemplate temp1 = new NotificationTemplate();
        temp1.setId("TEMP1");
        temp1.setType(NotificationTemplateType.PORTAL);
        when(
            notificationTemplateRepository.findByTypeAndReferenceIdAndReferenceType(
                NotificationTemplateType.PORTAL,
                NOTIFICATION_TEMPLATE_REFERENCE_ID,
                NOTIFICATION_TEMPLATE_REFERENCE_TYPE
            )
        ).thenReturn(Sets.newSet(temp1));

        final Set<NotificationTemplateEntity> byType = notificationTemplateService.findByType(
            GraviteeContext.getCurrentOrganization(),
            io.gravitee.rest.api.model.notification.NotificationTemplateType.PORTAL
        );
        assertNotNull(byType);
        assertEquals(1, byType.size());
    }

    @Test
    public void shouldCreateNotificationTemplate() throws TechnicalException {
        NotificationTemplateEntity newNotificationTemplateEntity = new NotificationTemplateEntity();
        newNotificationTemplateEntity.setName(NOTIFICATION_TEMPLATE_NAME);
        newNotificationTemplateEntity.setType(
            io.gravitee.rest.api.model.notification.NotificationTemplateType.valueOf(NOTIFICATION_TEMPLATE_TYPE.name())
        );
        newNotificationTemplateEntity.setEnabled(NOTIFICATION_TEMPLATE_ENABLED);

        when(notificationTemplateRepository.create(any())).thenReturn(notificationTemplate);

        notificationTemplateService.create(GraviteeContext.getExecutionContext(), newNotificationTemplateEntity);
        verify(notificationTemplateRepository, times(1)).create(any());
        verify(auditService, times(1)).createOrganizationAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            eq(GraviteeContext.getCurrentOrganization()),
            any(),
            eq(NotificationTemplate.AuditEvent.NOTIFICATION_TEMPLATE_CREATED),
            any(),
            isNull(),
            any()
        );
    }

    @Test
    public void shouldUpdateNotificationTemplate() throws TechnicalException {
        NotificationTemplateEntity updatingNotificationTemplateEntity = new NotificationTemplateEntity();
        updatingNotificationTemplateEntity.setId(NOTIFICATION_TEMPLATE_ID);
        updatingNotificationTemplateEntity.setName("New Name");
        updatingNotificationTemplateEntity.setType(
            io.gravitee.rest.api.model.notification.NotificationTemplateType.valueOf(NOTIFICATION_TEMPLATE_TYPE.name())
        );
        updatingNotificationTemplateEntity.setEnabled(NOTIFICATION_TEMPLATE_ENABLED);

        final NotificationTemplate toUpdate = mock(NotificationTemplate.class);
        when(toUpdate.getReferenceType()).thenReturn(NotificationTemplateReferenceType.ORGANIZATION);
        when(toUpdate.getReferenceId()).thenReturn(ORGANIZATION_ID);
        when(notificationTemplateRepository.findById(NOTIFICATION_TEMPLATE_ID)).thenReturn(Optional.of(toUpdate));
        when(notificationTemplateRepository.update(any())).thenReturn(notificationTemplate);
        when(commandRepository.create(any())).thenReturn(null);
        when(node.id()).thenReturn("nodeId");

        notificationTemplateService.update(GraviteeContext.getExecutionContext(), updatingNotificationTemplateEntity);

        verify(notificationTemplateRepository, times(1)).update(any());
        verify(auditService, times(1)).createOrganizationAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            eq(GraviteeContext.getCurrentOrganization()),
            any(),
            eq(NotificationTemplate.AuditEvent.NOTIFICATION_TEMPLATE_UPDATED),
            any(),
            eq(toUpdate),
            eq(notificationTemplate)
        );

        ArgumentCaptor<Command> captor = ArgumentCaptor.forClass(Command.class);
        verify(commandRepository, times(1)).create(captor.capture());
        var command = captor.getValue();
        assertThat(command).isNotNull();
        assertThat(command.getFrom()).isEqualTo("nodeId");
        assertThat(command.getTo()).isEqualTo(MessageRecipient.MANAGEMENT_APIS.name());
        assertThat(command.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(command.getTags()).containsExactly(CommandTags.EMAIL_TEMPLATE_UPDATE.name());
    }

    @Test(expected = NotificationTemplateNotFoundException.class)
    public void shouldNotUpdateNotificationTemplateBecauseDoesNotBelongToOrganization() throws TechnicalException {
        NotificationTemplateEntity updatingNotificationTemplateEntity = new NotificationTemplateEntity();
        updatingNotificationTemplateEntity.setId(NOTIFICATION_TEMPLATE_ID);
        updatingNotificationTemplateEntity.setName("New Name");
        updatingNotificationTemplateEntity.setType(
            io.gravitee.rest.api.model.notification.NotificationTemplateType.valueOf(NOTIFICATION_TEMPLATE_TYPE.name())
        );
        updatingNotificationTemplateEntity.setEnabled(NOTIFICATION_TEMPLATE_ENABLED);

        final NotificationTemplate toUpdate = mock(NotificationTemplate.class);
        when(toUpdate.getReferenceType()).thenReturn(NotificationTemplateReferenceType.ORGANIZATION);
        when(toUpdate.getReferenceId()).thenReturn("Another_organization");
        when(notificationTemplateRepository.findById(NOTIFICATION_TEMPLATE_ID)).thenReturn(Optional.of(toUpdate));

        notificationTemplateService.update(GraviteeContext.getExecutionContext(), updatingNotificationTemplateEntity);
        verify(notificationTemplateRepository, never()).update(any());
        verify(auditService, never()).createOrganizationAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            eq(GraviteeContext.getCurrentOrganization()),
            any(),
            eq(NotificationTemplate.AuditEvent.NOTIFICATION_TEMPLATE_UPDATED),
            any(),
            eq(toUpdate),
            eq(notificationTemplate)
        );
    }

    @Test
    public void resolveInlineTemplateWithParamReturnsEvaluatedTemplate() {
        String result = notificationTemplateService.resolveInlineTemplateWithParam(
            ORGANIZATION_ID,
            NOTIFICATION_TEMPLATE_NAME,
            "# Hello ${metadata.test}",
            Map.of("metadata", Map.of("test", "world")),
            true
        );
        assertThat(result).isEqualTo("# Hello world");
    }

    @Test(expected = InvalidTemplateException.class)
    public void resolveInlineTemplateWithParamThrowsWhenTemplateIsInvalid() {
        notificationTemplateService.resolveInlineTemplateWithParam(
            ORGANIZATION_ID,
            NOTIFICATION_TEMPLATE_NAME,
            "# Hello ${metadata.[wrong]}",
            Map.of("metadata", Map.of()),
            false
        );
    }

    @Test
    public void resolveInlineTemplateWithParamReturnsEmptyWhenTemplateIsInvalid() {
        String result = notificationTemplateService.resolveInlineTemplateWithParam(
            ORGANIZATION_ID,
            NOTIFICATION_TEMPLATE_NAME,
            "# Hello ${metadata.[wrong]}",
            Map.of("metadata", Map.of()),
            true
        );
        assertThat(result).isEqualTo("");
    }

    @Test(expected = TemplateProcessingException.class)
    public void resolveInlineTemplateWithParamThrowsWhenTemplateEvaluateUnknownProperties() {
        notificationTemplateService.resolveInlineTemplateWithParam(
            ORGANIZATION_ID,
            NOTIFICATION_TEMPLATE_NAME,
            "# Hello ${metadata.wrong}",
            Map.of("metadata", Map.of()),
            false
        );
    }

    @Test
    public void resolveInlineTemplateWithParamReturnsEmptyWhenTemplateEvaluateUnknownProperties() {
        String result = notificationTemplateService.resolveInlineTemplateWithParam(
            ORGANIZATION_ID,
            NOTIFICATION_TEMPLATE_NAME,
            "# Hello ${metadata.wrong}",
            Map.of("metadata", Map.of()),
            true
        );
        assertThat(result).isEqualTo("");
    }
}
