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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.NotificationTemplateRepository;
import io.gravitee.repository.management.model.NotificationTemplate;
import io.gravitee.repository.management.model.NotificationTemplateReferenceType;
import io.gravitee.repository.management.model.NotificationTemplateType;
import io.gravitee.rest.api.model.notification.NotificationTemplateEntity;
import io.gravitee.rest.api.service.exceptions.NotificationTemplateNotFoundException;
import io.gravitee.rest.api.service.impl.NotificationTemplateServiceImpl;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    private static final String NOTIFICATION_TEMPLATE_ID = "my-notif-template-id";
    private static final String NOTIFICATION_TEMPLATE_HOOK = "my-notif-template-hook";
    private static final String NOTIFICATION_TEMPLATE_SCOPE = "my-notif-template-scope";
    private static final String NOTIFICATION_TEMPLATE_REFERENCE_ID = "DEFAULT";
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

    @InjectMocks
    private NotificationTemplateService notificationTemplateService = new NotificationTemplateServiceImpl();

    @Mock
    private NotificationTemplateRepository notificationTemplateRepository;

    @Mock
    private AuditService auditService;

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
    }

    @Test(expected = NotificationTemplateNotFoundException.class)
    public void shouldNotFindNotificationTemplate() throws TechnicalException {
        when(notificationTemplateRepository.findById(NOTIFICATION_TEMPLATE_ID)).thenReturn(Optional.empty());
        notificationTemplateService.findById(NOTIFICATION_TEMPLATE_ID);
    }

    @Test
    public void shouldFindNotificationTemplate() throws TechnicalException {
        when(notificationTemplateRepository.findById(NOTIFICATION_TEMPLATE_ID)).thenReturn(Optional.of(notificationTemplate));
        final NotificationTemplateEntity foundTemplate = notificationTemplateService.findById(NOTIFICATION_TEMPLATE_ID);

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
        )
            .thenReturn(Sets.newSet(temp1, temp2));

        final Set<NotificationTemplateEntity> all = notificationTemplateService.findAll();
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
        )
            .thenReturn(Sets.newSet(temp1));

        final Set<NotificationTemplateEntity> byType = notificationTemplateService.findByType(
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

        notificationTemplateService.create(newNotificationTemplateEntity);
        verify(notificationTemplateRepository, times(1)).create(any());
        verify(auditService, times(1))
            .createOrganizationAuditLog(any(), eq(NotificationTemplate.AuditEvent.NOTIFICATION_TEMPLATE_CREATED), any(), isNull(), any());
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
        when(notificationTemplateRepository.findById(NOTIFICATION_TEMPLATE_ID)).thenReturn(Optional.of(toUpdate));
        when(notificationTemplateRepository.update(any())).thenReturn(notificationTemplate);

        notificationTemplateService.update(updatingNotificationTemplateEntity);
        verify(notificationTemplateRepository, times(1)).update(any());
        verify(auditService, times(1))
            .createOrganizationAuditLog(
                any(),
                eq(NotificationTemplate.AuditEvent.NOTIFICATION_TEMPLATE_UPDATED),
                any(),
                eq(toUpdate),
                eq(notificationTemplate)
            );
    }
}
