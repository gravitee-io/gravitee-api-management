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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.NotificationTemplateRepository;
import io.gravitee.repository.management.model.NotificationTemplate;
import io.gravitee.repository.management.model.NotificationTemplateReferenceType;
import io.gravitee.repository.management.model.NotificationTemplateType;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.Set;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NotificationTemplateRepositoryMock extends AbstractRepositoryMock<NotificationTemplateRepository> {

    public NotificationTemplateRepositoryMock() {
        super(NotificationTemplateRepository.class);
    }

    @Override
    protected void prepare(NotificationTemplateRepository notificationTemplateRepository) throws Exception {
        final NotificationTemplate newNotificationTemplate = mock(NotificationTemplate.class);
        when(newNotificationTemplate.getHook()).thenReturn("MY_NEW_HOOK");
        when(newNotificationTemplate.getScope()).thenReturn("API");
        when(newNotificationTemplate.getReferenceId()).thenReturn("DEFAULT");
        when(newNotificationTemplate.getReferenceType()).thenReturn(NotificationTemplateReferenceType.ORGANIZATION);
        when(newNotificationTemplate.getName()).thenReturn("My notif 1");
        when(newNotificationTemplate.getDescription()).thenReturn("Description for my notif 1");
        when(newNotificationTemplate.getTitle()).thenReturn("Title of my notif");
        when(newNotificationTemplate.getContent()).thenReturn("Content of my notif");
        when(newNotificationTemplate.getCreatedAt()).thenReturn(new Date(1000000000000L));
        when(newNotificationTemplate.getUpdatedAt()).thenReturn(new Date(1439032010883L));
        when(newNotificationTemplate.getType()).thenReturn(NotificationTemplateType.PORTAL);
        when(newNotificationTemplate.isEnabled()).thenReturn(true);

        final NotificationTemplate notif1 = new NotificationTemplate();
        notif1.setId("notif-1");
        notif1.setName("My notif 1");
        notif1.setDescription("Description for my notif 1");
        notif1.setCreatedAt(new Date(1000000000000L));
        notif1.setUpdatedAt(new Date(1439032010883L));

        final NotificationTemplate notificationTemplateUpdated = mock(NotificationTemplate.class);
        when(notificationTemplateUpdated.getHook()).thenReturn("MY_HOOK");
        when(notificationTemplateUpdated.getScope()).thenReturn("API");
        when(notificationTemplateUpdated.getReferenceId()).thenReturn("DEFAULT");
        when(notificationTemplateUpdated.getReferenceType()).thenReturn(NotificationTemplateReferenceType.ORGANIZATION);
        when(notificationTemplateUpdated.getName()).thenReturn("My notif 1");
        when(notificationTemplateUpdated.getDescription()).thenReturn("Description for my notif 1");
        when(notificationTemplateUpdated.getTitle()).thenReturn("Title of my notif");
        when(notificationTemplateUpdated.getContent()).thenReturn("Content of my notif");
        when(notificationTemplateUpdated.getCreatedAt()).thenReturn(new Date(1000000000000L));
        when(notificationTemplateUpdated.getUpdatedAt()).thenReturn(new Date(1486771200000L));
        when(notificationTemplateUpdated.getType()).thenReturn(NotificationTemplateType.PORTAL);
        when(notificationTemplateUpdated.isEnabled()).thenReturn(true);

        final Set<NotificationTemplate> notificationTemplates = newSet(
            newNotificationTemplate,
            notif1,
            mock(NotificationTemplate.class),
            mock(NotificationTemplate.class)
        );
        final Set<NotificationTemplate> notificationTemplatesAfterDelete = newSet(
            newNotificationTemplate,
            notif1,
            mock(NotificationTemplate.class)
        );
        final Set<NotificationTemplate> notificationTemplatesAfterAdd = newSet(
            newNotificationTemplate,
            notif1,
            mock(NotificationTemplate.class),
            mock(NotificationTemplate.class),
            mock(NotificationTemplate.class)
        );

        when(notificationTemplateRepository.findAll())
            .thenReturn(
                notificationTemplates,
                notificationTemplatesAfterAdd,
                notificationTemplates,
                notificationTemplatesAfterDelete,
                notificationTemplates
            );
        when(notificationTemplateRepository.findAllByReferenceIdAndReferenceType("DEFAULT", NotificationTemplateReferenceType.ORGANIZATION))
            .thenReturn(notificationTemplates);
        when(
            notificationTemplateRepository.findByTypeAndReferenceIdAndReferenceType(
                NotificationTemplateType.PORTAL,
                "DEFAULT",
                NotificationTemplateReferenceType.ORGANIZATION
            )
        )
            .thenReturn(newSet(newNotificationTemplate, notif1, mock(NotificationTemplate.class)));
        when(
            notificationTemplateRepository.findByHookAndScopeAndReferenceIdAndReferenceType(
                "MY_HOOK_3",
                "API",
                "DEFAULT",
                NotificationTemplateReferenceType.ORGANIZATION
            )
        )
            .thenReturn(newSet(mock(NotificationTemplate.class), mock(NotificationTemplate.class)));

        when(notificationTemplateRepository.create(any(NotificationTemplate.class))).thenReturn(newNotificationTemplate);

        when(notificationTemplateRepository.findById("new-notificationTemplate")).thenReturn(of(newNotificationTemplate));
        when(notificationTemplateRepository.findById("unknown")).thenReturn(empty());
        when(notificationTemplateRepository.findById("notif-1")).thenReturn(of(notif1), of(notificationTemplateUpdated));

        when(notificationTemplateRepository.update(argThat(o -> o == null || o.getId().equals("unknown"))))
            .thenThrow(new IllegalStateException());
    }
}
