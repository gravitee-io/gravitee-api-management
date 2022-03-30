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
package io.gravitee.rest.api.service.notifiers.impl;

import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.notifiers.EmailNotifierService;
import java.util.*;
import java.util.ArrayList;
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

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    @Override
    public void trigger(
        ExecutionContext executionContext,
        final Hook hook,
        GenericNotificationConfig genericNotificationConfig,
        final Map<String, Object> params
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

        String[] mails = getMails(executionContext, genericNotificationConfig, params).toArray(new String[0]);
        emailService.sendAsyncEmailNotification(
            executionContext,
            new EmailNotificationBuilder().to(mails).template(emailTemplate).params(params).build()
        );
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

        return EmailNotificationBuilder.EmailTemplate.fromHook(hook);
    }
}
