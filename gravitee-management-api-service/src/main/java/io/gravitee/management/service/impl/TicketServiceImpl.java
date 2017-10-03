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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.ApiModelEntity;
import io.gravitee.management.model.MetadataEntity;
import io.gravitee.management.model.NewTicketEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.service.*;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.exceptions.EmailRequiredException;
import io.gravitee.management.service.exceptions.SupportUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static io.gravitee.management.service.builder.EmailNotificationBuilder.EmailTemplate.SUPPORT_TICKET;
import static io.gravitee.management.service.impl.InitializerServiceImpl.DEFAULT_METADATA_EMAIL_SUPPORT;
import static io.gravitee.management.service.impl.InitializerServiceImpl.METADATA_EMAIL_SUPPORT_KEY;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TicketServiceImpl extends TransactionalService implements TicketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketServiceImpl.class);

    @Inject
    private UserService userService;
    @Inject
    private MetadataService metadataService;
    @Inject
    private ApiService apiService;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private EmailService emailService;

    @Value("${support.enabled:false}")
    private boolean enabled;

    @Override
    public void create(final String username, final NewTicketEntity ticketEntity) {
        if (!enabled) {
            throw new SupportUnavailableException();
        }
        LOGGER.info("Creating a support ticket: {}", ticketEntity);

        final Map<String, Object> parameters = new HashMap<>();

        final UserEntity user = userService.findByName(username, false);
        if (user.getEmail() == null) {
            throw new EmailRequiredException(username);
        }
        parameters.put("user", user);

        final String emailTo;
        if (ticketEntity.getApi() == null) {
            final MetadataEntity emailMetadata = metadataService.findDefaultByKey(METADATA_EMAIL_SUPPORT_KEY);
            if (emailMetadata == null) {
                throw new IllegalStateException("The support email metadata has not been found");
            }
            emailTo = emailMetadata.getValue();
        } else {
            final ApiModelEntity api = apiService.findByIdForTemplates(ticketEntity.getApi());
            final String apiMetadataEmailSupport = api.getMetadata().get(METADATA_EMAIL_SUPPORT_KEY);
            if (apiMetadataEmailSupport == null) {
                throw new IllegalStateException("The support email API metadata has not been found");
            }
            emailTo = apiMetadataEmailSupport;
            parameters.put("api", api);
        }

        if (DEFAULT_METADATA_EMAIL_SUPPORT.equals(emailTo)) {
            throw new IllegalStateException("The support email API metadata has not been changed");
        }

        if (ticketEntity.getApplication() != null) {
            parameters.put("application", applicationService.findById(ticketEntity.getApplication()));
        }

        parameters.put("content", ticketEntity.getContent().replaceAll("(\r\n|\n)", "<br />"));
        emailService.sendEmailNotification(
                new EmailNotificationBuilder()
                        .from(user.getEmail())
                        .fromName(user.getFirstname() + ' ' + user.getLastname())
                        .to(emailTo)
                        .subject(ticketEntity.getSubject())
                        .copyToSender(ticketEntity.isCopyToSender())
                        .template(SUPPORT_TICKET)
                        .params(parameters)
                        .build());
    }
}
