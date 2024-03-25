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

import io.gravitee.apim.core.template.TemplateProcessor;
import io.gravitee.apim.core.template.TemplateProcessorException;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.settings.Email;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notifiers.EmailNotifierService;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    private final EmailService emailService;

    public EmailNotifierServiceImpl(@Autowired EmailService emailService) {
        this.emailService = emailService;
    }

    public void trigger(
        ExecutionContext executionContext,
        final Hook hook,
        final Map<String, Object> templateData,
        Collection<String> recipients
    ) {
        var emailTemplate = getEmailTemplateOptional(hook);
        if (emailTemplate.isEmpty()) {
            LOGGER.error("Email template not found for hook {}", hook);
            return;
        }

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
    }

    private Optional<EmailNotificationBuilder.EmailTemplate> getEmailTemplateOptional(final Hook hook) {
        if (hook == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(EmailNotificationBuilder.EmailTemplate.fromHook(hook));
    }
}
