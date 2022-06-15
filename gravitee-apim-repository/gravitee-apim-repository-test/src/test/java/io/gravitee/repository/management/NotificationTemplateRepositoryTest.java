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
import static org.junit.Assert.*;

import io.gravitee.repository.config.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.NotificationTemplate;
import io.gravitee.repository.management.model.NotificationTemplateReferenceType;
import io.gravitee.repository.management.model.NotificationTemplateType;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class NotificationTemplateRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/notificationtemplate-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<NotificationTemplate> notificationTemplates = notificationTemplateRepository.findAll();

        assertNotNull(notificationTemplates);
        assertEquals(4, notificationTemplates.size());
    }

    @Test
    public void shouldFindByType() throws Exception {
        final Set<NotificationTemplate> notificationTemplates = notificationTemplateRepository.findByTypeAndReferenceIdAndReferenceType(
            NotificationTemplateType.PORTAL,
            "DEFAULT",
            NotificationTemplateReferenceType.ORGANIZATION
        );

        assertNotNull(notificationTemplates);
        assertEquals(3, notificationTemplates.size());
    }

    @Test
    public void shouldFindAllByReferenceIdAndReferenceType() throws Exception {
        final Set<NotificationTemplate> notificationTemplates = notificationTemplateRepository.findAllByReferenceIdAndReferenceType(
            "DEFAULT",
            NotificationTemplateReferenceType.ORGANIZATION
        );

        assertNotNull(notificationTemplates);
        assertEquals(4, notificationTemplates.size());
    }

    @Test
    public void shouldFindByHookAndReferenceIdAndReferenceType() throws Exception {
        final Set<NotificationTemplate> notificationTemplates = notificationTemplateRepository.findByHookAndScopeAndReferenceIdAndReferenceType(
            "MY_HOOK_3",
            "API",
            "DEFAULT",
            NotificationTemplateReferenceType.ORGANIZATION
        );

        assertNotNull(notificationTemplates);
        assertEquals(2, notificationTemplates.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        final NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setId("new-notificationTemplate");
        notificationTemplate.setHook("MY_NEW_HOOK");
        notificationTemplate.setScope("API");
        notificationTemplate.setReferenceId("DEFAULT");
        notificationTemplate.setReferenceType(NotificationTemplateReferenceType.ORGANIZATION);
        notificationTemplate.setName("My notif 1");
        notificationTemplate.setDescription("Description for my notif 1");
        notificationTemplate.setTitle("Title of my notif");
        notificationTemplate.setContent("Content of my notif");
        notificationTemplate.setCreatedAt(new Date(1000000000000L));
        notificationTemplate.setUpdatedAt(new Date(1439032010883L));
        notificationTemplate.setType(NotificationTemplateType.PORTAL);
        notificationTemplate.setEnabled(true);

        int nbNotificationTemplatesBeforeCreation = notificationTemplateRepository.findAll().size();
        notificationTemplateRepository.create(notificationTemplate);
        int nbNotificationTemplatesAfterCreation = notificationTemplateRepository.findAll().size();

        Assert.assertEquals(nbNotificationTemplatesBeforeCreation + 1, nbNotificationTemplatesAfterCreation);

        Optional<NotificationTemplate> optional = notificationTemplateRepository.findById("new-notificationTemplate");
        Assert.assertTrue("NotificationTemplate saved not found", optional.isPresent());

        final NotificationTemplate notificationTemplateSaved = optional.get();
        Assert.assertEquals("Invalid saved hook.", notificationTemplate.getHook(), notificationTemplateSaved.getHook());
        Assert.assertEquals("Invalid saved scope.", notificationTemplate.getScope(), notificationTemplateSaved.getScope());
        Assert.assertEquals(
            "Invalid saved reference id.",
            notificationTemplate.getReferenceId(),
            notificationTemplateSaved.getReferenceId()
        );
        Assert.assertEquals(
            "Invalid saved reference Type",
            notificationTemplate.getReferenceType(),
            notificationTemplateSaved.getReferenceType()
        );
        Assert.assertEquals(
            "Invalid saved notificationTemplate name.",
            notificationTemplate.getName(),
            notificationTemplateSaved.getName()
        );
        Assert.assertEquals(
            "Invalid notificationTemplate description.",
            notificationTemplate.getDescription(),
            notificationTemplateSaved.getDescription()
        );
        Assert.assertEquals(
            "Invalid saved notificationTemplate title.",
            notificationTemplate.getTitle(),
            notificationTemplateSaved.getTitle()
        );
        Assert.assertEquals(
            "Invalid notificationTemplate content.",
            notificationTemplate.getContent(),
            notificationTemplateSaved.getContent()
        );
        Assert.assertTrue(
            "Invalid notificationTemplate createdAt.",
            compareDate(notificationTemplate.getCreatedAt(), notificationTemplateSaved.getCreatedAt())
        );
        Assert.assertTrue(
            "Invalid notificationTemplate updatedAt.",
            compareDate(notificationTemplate.getUpdatedAt(), notificationTemplateSaved.getUpdatedAt())
        );
        Assert.assertEquals("Invalid notificationTemplate type.", notificationTemplate.getType(), notificationTemplateSaved.getType());
        Assert.assertEquals(
            "Invalid notificationTemplate enabled.",
            notificationTemplate.isEnabled(),
            notificationTemplateSaved.isEnabled()
        );
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<NotificationTemplate> optional = notificationTemplateRepository.findById("notif-1");
        Assert.assertTrue("NotificationTemplate to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved notificationTemplate name.", "My notif 1", optional.get().getName());

        final NotificationTemplate notificationTemplate = optional.get();
        notificationTemplate.setHook("MY_HOOK");
        notificationTemplate.setScope("API");
        notificationTemplate.setReferenceId("DEFAULT");
        notificationTemplate.setReferenceType(NotificationTemplateReferenceType.ORGANIZATION);
        notificationTemplate.setName("My notif 1");
        notificationTemplate.setDescription("Description for my notif 1");
        notificationTemplate.setTitle("Title of my notif");
        notificationTemplate.setContent("Content of my notif");
        notificationTemplate.setCreatedAt(new Date(1000000000000L));
        notificationTemplate.setUpdatedAt(new Date(1486771200000L));
        notificationTemplate.setType(NotificationTemplateType.PORTAL);
        notificationTemplate.setEnabled(true);

        int nbNotificationTemplatesBeforeUpdate = notificationTemplateRepository.findAll().size();
        notificationTemplateRepository.update(notificationTemplate);
        int nbNotificationTemplatesAfterUpdate = notificationTemplateRepository.findAll().size();

        Assert.assertEquals(nbNotificationTemplatesBeforeUpdate, nbNotificationTemplatesAfterUpdate);

        Optional<NotificationTemplate> optionalUpdated = notificationTemplateRepository.findById("notif-1");
        Assert.assertTrue("NotificationTemplate to update not found", optionalUpdated.isPresent());

        final NotificationTemplate notificationTemplateUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved hook.", notificationTemplate.getHook(), notificationTemplateUpdated.getHook());
        Assert.assertEquals("Invalid saved scope.", notificationTemplate.getScope(), notificationTemplateUpdated.getScope());
        Assert.assertEquals(
            "Invalid saved reference id.",
            notificationTemplate.getReferenceId(),
            notificationTemplateUpdated.getReferenceId()
        );
        Assert.assertEquals(
            "Invalid saved reference type.",
            notificationTemplate.getReferenceType(),
            notificationTemplateUpdated.getReferenceType()
        );
        Assert.assertEquals(
            "Invalid saved notificationTemplate name.",
            notificationTemplate.getName(),
            notificationTemplateUpdated.getName()
        );
        Assert.assertEquals(
            "Invalid notificationTemplate description.",
            notificationTemplate.getDescription(),
            notificationTemplateUpdated.getDescription()
        );
        Assert.assertEquals(
            "Invalid saved notificationTemplate title.",
            notificationTemplate.getTitle(),
            notificationTemplateUpdated.getTitle()
        );
        Assert.assertEquals(
            "Invalid notificationTemplate content.",
            notificationTemplate.getContent(),
            notificationTemplateUpdated.getContent()
        );
        Assert.assertTrue(
            "Invalid notificationTemplate createdAt.",
            compareDate(notificationTemplate.getCreatedAt(), notificationTemplateUpdated.getCreatedAt())
        );
        Assert.assertTrue(
            "Invalid notificationTemplate updatedAt.",
            compareDate(notificationTemplate.getUpdatedAt(), notificationTemplateUpdated.getUpdatedAt())
        );
        Assert.assertEquals("Invalid notificationTemplate type.", notificationTemplate.getType(), notificationTemplateUpdated.getType());
        Assert.assertEquals(
            "Invalid notificationTemplate enabled.",
            notificationTemplate.isEnabled(),
            notificationTemplateUpdated.isEnabled()
        );
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbNotificationTemplatesBeforeDeletion = notificationTemplateRepository.findAll().size();
        notificationTemplateRepository.delete("notif-3-email");
        int nbNotificationTemplatesAfterDeletion = notificationTemplateRepository.findAll().size();

        Assert.assertEquals(nbNotificationTemplatesBeforeDeletion - 1, nbNotificationTemplatesAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownTemplate() throws Exception {
        NotificationTemplate unknownNotificationTemplate = new NotificationTemplate();
        unknownNotificationTemplate.setId("unknown");
        unknownNotificationTemplate.setReferenceId("DEFAULT");
        unknownNotificationTemplate.setReferenceType(NotificationTemplateReferenceType.ORGANIZATION);
        notificationTemplateRepository.update(unknownNotificationTemplate);
        fail("An unknown notificationTemplate should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        notificationTemplateRepository.update(null);
        fail("A null notificationTemplate should not be updated");
    }
}
