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
package io.gravitee.rest.api.service.notifiers.impl;

<<<<<<< HEAD
import io.gravitee.repository.management.model.GenericNotificationConfig;
=======
import io.gravitee.apim.core.template.TemplateProcessor;
import io.gravitee.apim.core.template.TemplateProcessorException;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.settings.Email;
>>>>>>> 6a6c2d18e3 (fix: prevent emails to be sent to non opted in user in trial instance)
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.notifiers.EmailNotifierService;
<<<<<<< HEAD
import java.util.*;
import java.util.ArrayList;
=======
import java.util.Arrays;
import java.util.Collection;
>>>>>>> 6a6c2d18e3 (fix: prevent emails to be sent to non opted in user in trial instance)
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class EmailNotifierServiceImpl implements EmailNotifierService {

    private final Logger LOGGER = LoggerFactory.getLogger(EmailNotifierServiceImpl.class);

    @Autowired
    EmailService emailService;

<<<<<<< HEAD
    @Autowired
    private NotificationTemplateService notificationTemplateService;
=======
    public EmailNotifierServiceImpl(@Autowired EmailService emailService) {
        this.emailService = emailService;
    }
>>>>>>> 6a6c2d18e3 (fix: prevent emails to be sent to non opted in user in trial instance)

    @Override
    public void trigger(
        ExecutionContext executionContext,
        final Hook hook,
<<<<<<< HEAD
        GenericNotificationConfig genericNotificationConfig,
        final Map<String, Object> params
=======
        final Map<String, Object> templateData,
        Collection<String> recipients
>>>>>>> 6a6c2d18e3 (fix: prevent emails to be sent to non opted in user in trial instance)
    ) {
        if (
            genericNotificationConfig == null ||
            genericNotificationConfig.getConfig() == null ||
            genericNotificationConfig.getConfig().isEmpty()
        ) {
            LOGGER.error("Email Notifier configuration is empty");
            return;
        }
        EmailNotificationBuilder.EmailTemplate emailTemplate = getEmailTemplate(hook);
        if (emailTemplate == null) {
            LOGGER.error("Email template not found for hook {}", hook);
            return;
        }

<<<<<<< HEAD
        String[] mails = getMails(executionContext, genericNotificationConfig, params).toArray(new String[0]);
        emailService.sendAsyncEmailNotification(
            executionContext,
            new EmailNotificationBuilder().to(mails).template(emailTemplate).params(params).build()
        );
=======
        if (recipients.isEmpty()) {
            LOGGER.error("No emails extracted from {}", recipients);
            return;
        }

        EmailNotificationBuilder template = new EmailNotificationBuilder().template(emailTemplate.get()).params(templateData);
        if (recipients.size() == 1) {
            template.to(recipients.toArray(new String[0]));
        } else {
            template.bcc(recipients.toArray(new String[0]));
        }
        emailService.sendAsyncEmailNotification(executionContext, template.build());
>>>>>>> 6a6c2d18e3 (fix: prevent emails to be sent to non opted in user in trial instance)
    }

    public List<String> getMails(
        ExecutionContext executionContext,
        final GenericNotificationConfig genericNotificationConfig,
        final Map<String, Object> params
    ) {
        if (
            genericNotificationConfig == null ||
            genericNotificationConfig.getConfig() == null ||
            genericNotificationConfig.getConfig().isEmpty()
        ) {
            LOGGER.error("Email Notifier configuration is empty");
            return Collections.emptyList();
        }

        String[] mails = genericNotificationConfig.getConfig().split(",|;|\\s");
        List<String> result = new ArrayList<>();
        for (String mail : mails) {
            if (!mail.isEmpty()) {
                if (mail.contains("$")) {
                    String tmpMail =
                        this.notificationTemplateService.resolveInlineTemplateWithParam(
                                executionContext.getOrganizationId(),
                                mail,
                                mail,
                                params
                            );
                    if (!tmpMail.isEmpty()) {
                        result.add(tmpMail);
                    }
                } else {
                    result.add(mail);
                }
            }
        }
        if (result.isEmpty()) {
            LOGGER.warn("Email recipient not found with: {}", genericNotificationConfig.getConfig());
        }
        return result;
    }

    private EmailNotificationBuilder.EmailTemplate getEmailTemplate(final Hook hook) {
        if (hook == null) {
            return null;
        }

<<<<<<< HEAD
        return EmailNotificationBuilder.EmailTemplate.fromHook(hook);
=======
        return Optional.ofNullable(EmailNotificationBuilder.EmailTemplate.fromHook(hook));
>>>>>>> 6a6c2d18e3 (fix: prevent emails to be sent to non opted in user in trial instance)
    }
}
