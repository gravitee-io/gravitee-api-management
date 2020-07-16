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

import io.gravitee.management.model.*;
import io.gravitee.management.model.parameters.Key;
import io.gravitee.management.service.*;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.exceptions.EmailRequiredException;
import io.gravitee.management.service.exceptions.SupportUnavailableException;
import io.gravitee.management.service.impl.upgrade.DefaultMetadataUpgrader;
import io.gravitee.management.service.notification.ApiHook;
import io.gravitee.management.service.notification.ApplicationHook;
import io.gravitee.management.service.notification.NotificationParamsBuilder;
import io.gravitee.management.service.notification.PortalHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static io.gravitee.management.service.builder.EmailNotificationBuilder.EmailTemplate.SUPPORT_TICKET;

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
    @Inject
    private ParameterService parameterService;
    @Inject
    private NotifierService notifierService;

    private boolean isEnabled() {
        return parameterService.findAsBoolean(Key.PORTAL_SUPPORT_ENABLED);
    }

    @Override
    public void create(final String userId, final NewTicketEntity ticketEntity) {
        if (!isEnabled()) {
            throw new SupportUnavailableException();
        }
        LOGGER.info("Creating a support ticket: {}", ticketEntity);

        final Map<String, Object> parameters = new HashMap<>();

        final UserEntity user = userService.findById(userId);
        if (user.getEmail() == null) {
            throw new EmailRequiredException(userId);
        }
        parameters.put("user", user);

        final String emailTo;
        final ApiModelEntity api;
        final ApplicationEntity applicationEntity;
        if (ticketEntity.getApi() == null) {
            api = null;
            final MetadataEntity emailMetadata = metadataService.findDefaultByKey(DefaultMetadataUpgrader.METADATA_EMAIL_SUPPORT_KEY);
            if (emailMetadata == null) {
                throw new IllegalStateException("The support email metadata has not been found");
            }
            emailTo = emailMetadata.getValue();
        } else {
            api = apiService.findByIdForTemplates(ticketEntity.getApi(), true);
            final String apiMetadataEmailSupport = api.getMetadata().get(DefaultMetadataUpgrader.METADATA_EMAIL_SUPPORT_KEY);
            if (apiMetadataEmailSupport == null) {
                throw new IllegalStateException("The support email API metadata has not been found");
            }
            emailTo = apiMetadataEmailSupport;
            parameters.put("api", api);
        }

        if (DefaultMetadataUpgrader.DEFAULT_METADATA_EMAIL_SUPPORT.equals(emailTo)) {
            throw new IllegalStateException("The support email API metadata has not been changed");
        }

        if (ticketEntity.getApplication() != null) {
            applicationEntity = applicationService.findById(ticketEntity.getApplication());
            parameters.put("application", applicationEntity);
        } else {
            applicationEntity = null;
        }

        parameters.put("content", ticketEntity.getContent().replaceAll("(\r\n|\n)", "<br />"));
        final String fromName = user.getFirstname() == null ? user.getEmail() : user.getFirstname() + ' ' + user.getLastname();
        emailService.sendEmailNotification(
                new EmailNotificationBuilder()
                        .replyTo(user.getEmail())
                        .fromName(fromName)
                        .to(emailTo)
                        .subject(ticketEntity.getSubject())
                        .copyToSender(ticketEntity.isCopyToSender())
                        .template(SUPPORT_TICKET)
                        .params(parameters)
                        .build());
        sendUserNotification(user, api, applicationEntity);
    }

    private void sendUserNotification(final UserEntity user, final ApiModelEntity api, final ApplicationEntity application) {
        notifierService.trigger(PortalHook.NEW_SUPPORT_TICKET, new NotificationParamsBuilder()
                .user(user)
                .api(api)
                .application(application)
                .build());

        if (api != null) {
            notifierService.trigger(ApiHook.NEW_SUPPORT_TICKET, api.getId(), new NotificationParamsBuilder()
                    .user(user)
                    .api(api)
                    .application(application)
                    .build());
        }

        if (application != null) {
            notifierService.trigger(ApplicationHook.NEW_SUPPORT_TICKET, application.getId(), new NotificationParamsBuilder()
                    .user(user)
                    .api(api)
                    .application(application)
                    .build());
        }
    }
}
