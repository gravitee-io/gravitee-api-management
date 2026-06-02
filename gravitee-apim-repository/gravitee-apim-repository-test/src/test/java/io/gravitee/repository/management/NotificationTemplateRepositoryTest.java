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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.management.model.NotificationTemplate;
import io.gravitee.repository.management.model.NotificationTemplateReferenceType;
import io.gravitee.repository.management.model.NotificationTemplateType;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingReferenceType;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NotificationTemplateRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/notificationtemplate-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<NotificationTemplate> notificationTemplates = notificationTemplateRepository.findAll();

        assertNotNull(notificationTemplates);
        assertEquals(6, notificationTemplates.size());
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
        final Set<NotificationTemplate> notificationTemplates =
            notificationTemplateRepository.findByHookAndScopeAndReferenceIdAndReferenceType(
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
        notificationTemplate.setHook("USER_REGISTRATION");
        notificationTemplate.setScope("TEMPLATES_FOR_ACTION");
        notificationTemplate.setReferenceId("DEFAULT");
        notificationTemplate.setReferenceType(NotificationTemplateReferenceType.ORGANIZATION);
        notificationTemplate.setName("My notif 1");
        notificationTemplate.setDescription(
            "Email sent to a user who has self-registered on portal or admin console. Contains a registration link."
        );
        notificationTemplate.setTitle("User ${registrationAction} - ${user.displayName} ");
        notificationTemplate.setContent(
            "<html>\n" +
                "\t<body style=\"text-align: center;\">\n" +
                "\t\t<header>\n" +
                "\t\t\t<#include \"header.html\" />\n" +
                "\t\t</header>\n" +
                "\t\t<div style=\"margin-top: 50px; color: #424e5a;\">\n" +
                "\t\t\t<h3>Hi ${user.displayName},</h3>\n" +
                "\t\t\t<p>You have been successfully registered.</p>\n" +
                "\t\t\t<p><a href=\"${registrationUrl}\">Click here</a> to confirm your registration.</p>\n" +
                "\t\t</div>\n" +
                "\t</body>\n" +
                "</html>\n"
        );
        notificationTemplate.setCreatedAt(new Date(1000000000000L));
        notificationTemplate.setUpdatedAt(new Date(1439032010883L));
        notificationTemplate.setType(NotificationTemplateType.EMAIL);
        notificationTemplate.setEnabled(true);

        int nbNotificationTemplatesBeforeCreation = notificationTemplateRepository.findAll().size();
        notificationTemplateRepository.create(notificationTemplate);
        int nbNotificationTemplatesAfterCreation = notificationTemplateRepository.findAll().size();

        Assertions.assertEquals(nbNotificationTemplatesBeforeCreation + 1, nbNotificationTemplatesAfterCreation);

        Optional<NotificationTemplate> optional = notificationTemplateRepository.findById("new-notificationTemplate");
        Assertions.assertTrue(optional.isPresent(), "NotificationTemplate saved not found");

        final NotificationTemplate notificationTemplateSaved = optional.get();
        Assertions.assertEquals(notificationTemplate.getHook(), notificationTemplateSaved.getHook(), "Invalid saved hook.");
        Assertions.assertEquals(notificationTemplate.getScope(), notificationTemplateSaved.getScope(), "Invalid saved scope.");
        Assertions.assertEquals(
            notificationTemplate.getReferenceId(),
            notificationTemplateSaved.getReferenceId(),
            "Invalid saved reference id."
        );
        Assertions.assertEquals(
            notificationTemplate.getReferenceType(),
            notificationTemplateSaved.getReferenceType(),
            "Invalid saved reference Type"
        );
        Assertions.assertEquals(
            notificationTemplate.getName(),
            notificationTemplateSaved.getName(),
            "Invalid saved notificationTemplate name."
        );
        Assertions.assertEquals(
            notificationTemplate.getDescription(),
            notificationTemplateSaved.getDescription(),
            "Invalid notificationTemplate description."
        );
        Assertions.assertEquals(
            notificationTemplate.getTitle(),
            notificationTemplateSaved.getTitle(),
            "Invalid saved notificationTemplate title."
        );
        Assertions.assertEquals(
            notificationTemplate.getContent(),
            notificationTemplateSaved.getContent(),
            "Invalid notificationTemplate content."
        );
        Assertions.assertTrue(
            compareDate(notificationTemplate.getCreatedAt(), notificationTemplateSaved.getCreatedAt()),
            "Invalid notificationTemplate createdAt."
        );
        Assertions.assertTrue(
            compareDate(notificationTemplate.getUpdatedAt(), notificationTemplateSaved.getUpdatedAt()),
            "Invalid notificationTemplate updatedAt."
        );
        Assertions.assertEquals(notificationTemplate.getType(), notificationTemplateSaved.getType(), "Invalid notificationTemplate type.");
        Assertions.assertEquals(
            notificationTemplate.isEnabled(),
            notificationTemplateSaved.isEnabled(),
            "Invalid notificationTemplate enabled."
        );
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<NotificationTemplate> optional = notificationTemplateRepository.findById("notif-1");
        Assertions.assertTrue(optional.isPresent(), "NotificationTemplate to update not found");
        Assertions.assertEquals("My notif 1", optional.get().getName(), "Invalid saved notificationTemplate name.");

        final NotificationTemplate notificationTemplate = optional.get();
        notificationTemplate.setHook("MY_HOOK");
        notificationTemplate.setScope("API");
        notificationTemplate.setReferenceId("DEFAULT");
        notificationTemplate.setReferenceType(NotificationTemplateReferenceType.ORGANIZATION);
        notificationTemplate.setName("My notif 1");
        notificationTemplate.setDescription(
            "Email sent to a user who has self-registered on portal or admin console. Contains a registration link."
        );
        notificationTemplate.setTitle("User ${registrationAction} - ${user.displayName} ");
        notificationTemplate.setContent(
            "<html>\n" +
                "\t<body style=\"text-align: center;\">\n" +
                "\t\t<header>\n" +
                "\t\t\t<#include \"header.html\" />\n" +
                "\t\t</header>\n" +
                "\t\t<div style=\"margin-top: 50px; color: #424e5a;\">\n" +
                "\t\t\t<h3>Hi ${user.displayName},</h3>\n" +
                "\t\t\t<p>You have been successfully registered.</p>\n" +
                "\t\t\t<p><a href=\"${registrationUrl}\">Click here</a> to confirm your registration.</p>\n" +
                "\t\t</div>\n" +
                "\t</body>\n" +
                "</html>\n"
        );
        notificationTemplate.setCreatedAt(new Date(1000000000000L));
        notificationTemplate.setUpdatedAt(new Date(1486771200000L));
        notificationTemplate.setType(NotificationTemplateType.PORTAL);
        notificationTemplate.setEnabled(true);

        int nbNotificationTemplatesBeforeUpdate = notificationTemplateRepository.findAll().size();
        notificationTemplateRepository.update(notificationTemplate);
        int nbNotificationTemplatesAfterUpdate = notificationTemplateRepository.findAll().size();

        Assertions.assertEquals(nbNotificationTemplatesBeforeUpdate, nbNotificationTemplatesAfterUpdate);

        Optional<NotificationTemplate> optionalUpdated = notificationTemplateRepository.findById("notif-1");
        Assertions.assertTrue(optionalUpdated.isPresent(), "NotificationTemplate to update not found");

        final NotificationTemplate notificationTemplateUpdated = optionalUpdated.get();
        Assertions.assertEquals(notificationTemplate.getHook(), notificationTemplateUpdated.getHook(), "Invalid saved hook.");
        Assertions.assertEquals(notificationTemplate.getScope(), notificationTemplateUpdated.getScope(), "Invalid saved scope.");
        Assertions.assertEquals(
            notificationTemplate.getReferenceId(),
            notificationTemplateUpdated.getReferenceId(),
            "Invalid saved reference id."
        );
        Assertions.assertEquals(
            notificationTemplate.getReferenceType(),
            notificationTemplateUpdated.getReferenceType(),
            "Invalid saved reference type."
        );
        Assertions.assertEquals(
            notificationTemplate.getName(),
            notificationTemplateUpdated.getName(),
            "Invalid saved notificationTemplate name."
        );
        Assertions.assertEquals(
            notificationTemplate.getDescription(),
            notificationTemplateUpdated.getDescription(),
            "Invalid notificationTemplate description."
        );
        Assertions.assertEquals(
            notificationTemplate.getTitle(),
            notificationTemplateUpdated.getTitle(),
            "Invalid saved notificationTemplate title."
        );
        Assertions.assertEquals(
            notificationTemplate.getContent(),
            notificationTemplateUpdated.getContent(),
            "Invalid notificationTemplate content."
        );
        Assertions.assertTrue(
            compareDate(notificationTemplate.getCreatedAt(), notificationTemplateUpdated.getCreatedAt()),
            "Invalid notificationTemplate createdAt."
        );
        Assertions.assertTrue(
            compareDate(notificationTemplate.getUpdatedAt(), notificationTemplateUpdated.getUpdatedAt()),
            "Invalid notificationTemplate updatedAt."
        );
        Assertions.assertEquals(
            notificationTemplate.getType(),
            notificationTemplateUpdated.getType(),
            "Invalid notificationTemplate type."
        );
        Assertions.assertEquals(
            notificationTemplate.isEnabled(),
            notificationTemplateUpdated.isEnabled(),
            "Invalid notificationTemplate enabled."
        );
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbNotificationTemplatesBeforeDeletion = notificationTemplateRepository.findAll().size();
        notificationTemplateRepository.delete("notif-3-email");
        int nbNotificationTemplatesAfterDeletion = notificationTemplateRepository.findAll().size();

        Assertions.assertEquals(nbNotificationTemplatesBeforeDeletion - 1, nbNotificationTemplatesAfterDeletion);
    }

    @Test
    public void shouldNotUpdateUnknownTemplate() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            NotificationTemplate unknownNotificationTemplate = new NotificationTemplate();
            unknownNotificationTemplate.setId("unknown");
            unknownNotificationTemplate.setReferenceId("DEFAULT");
            unknownNotificationTemplate.setReferenceType(NotificationTemplateReferenceType.ORGANIZATION);
            notificationTemplateRepository.update(unknownNotificationTemplate);
            fail("An unknown notificationTemplate should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            notificationTemplateRepository.update(null);
            fail("A null notificationTemplate should not be updated");
        });
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        final var beforeDeletion = notificationTemplateRepository
            .findAllByReferenceIdAndReferenceType("org-to-delete", NotificationTemplateReferenceType.ORGANIZATION)
            .stream()
            .map(NotificationTemplate::getId)
            .toList();
        final var deleted = notificationTemplateRepository.deleteByReferenceIdAndReferenceType(
            "org-to-delete",
            NotificationTemplateReferenceType.ORGANIZATION
        );
        final var nbAfterDeletion = notificationTemplateRepository
            .findAllByReferenceIdAndReferenceType("org-to-delete", NotificationTemplateReferenceType.ORGANIZATION)
            .size();

        assertEquals(beforeDeletion.size(), deleted.size());
        assertTrue(beforeDeletion.containsAll(deleted));
        assertEquals(0, nbAfterDeletion);
    }
}
