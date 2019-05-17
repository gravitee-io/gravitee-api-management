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

import io.gravitee.management.model.notification.NotifierEntity;
import io.gravitee.management.service.NotifierService;
import io.gravitee.management.service.PortalNotificationService;
import io.gravitee.management.service.notification.ApiHook;
import io.gravitee.management.service.notification.ApplicationHook;
import io.gravitee.management.service.notification.Hook;
import io.gravitee.management.service.notification.PortalHook;
import io.gravitee.management.service.notifiers.EmailNotifierService;
import io.gravitee.management.service.notifiers.WebhookNotifierService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.repository.management.model.PortalNotificationDefaultReferenceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class NotifierServiceImpl extends AbstractService implements NotifierService {

    /**
     * Default Notifier IDs
     */
    public static final String DEFAULT_EMAIL_NOTIFIER_ID = "default-email";
    private static final String DEFAULT_WEBHOOK_NOTIFIER_ID = "default-webhook";


    private final Logger LOGGER = LoggerFactory.getLogger(NotifierServiceImpl.class);

    @Autowired
    PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Autowired
    PortalNotificationService portalNotificationService;

    @Autowired
    GenericNotificationConfigRepository genericNotificationConfigRepository;

    @Autowired
    @Lazy
    EmailNotifierService emailNotifierService;

    @Autowired
    @Lazy
    WebhookNotifierService webhookNotifierService;

    @Override
    @Async
    public void trigger(final ApiHook hook, final String apiId, Map<String, Object> params) {
        triggerPortalNotifications(hook, NotificationReferenceType.API, apiId, params);
        triggerGenericNotifications(hook, NotificationReferenceType.API, apiId, params);
    }

    @Override
    @Async
    public void trigger(final ApplicationHook hook, final String applicationId, Map<String, Object> params) {
        triggerPortalNotifications(hook, NotificationReferenceType.APPLICATION, applicationId, params);
        triggerGenericNotifications(hook, NotificationReferenceType.APPLICATION, applicationId, params);
    }

    @Override
    @Async
    public void trigger(final PortalHook hook, Map<String, Object> params) {
        triggerPortalNotifications(hook, NotificationReferenceType.PORTAL, PortalNotificationDefaultReferenceId.DEFAULT.name(), params);
        triggerGenericNotifications(hook, NotificationReferenceType.PORTAL, PortalNotificationDefaultReferenceId.DEFAULT.name(), params);
    }

    private void triggerPortalNotifications(final Hook hook, final NotificationReferenceType refType, final String refId, final Map<String, Object> params) {
        try {
            List<String> userIds = portalNotificationConfigRepository.findByReferenceAndHook(hook.name(), refType, refId).
                    stream().
                    map(PortalNotificationConfig::getUser).
                    collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                portalNotificationService.create(hook, userIds, params);
            }
        } catch (TechnicalException e) {
            LOGGER.error("Error looking for PortalNotificationConfig with {}/{}/{}", hook, refType, refId, e);
        }
    }

    private void triggerGenericNotifications(final Hook hook, final NotificationReferenceType refType, final String refId, final Map<String, Object> params) {
        try {
            for (GenericNotificationConfig genericNotificationConfig : genericNotificationConfigRepository.findByReferenceAndHook(hook.name(), refType, refId)) {
                switch (genericNotificationConfig.getNotifier()) {
                    case DEFAULT_EMAIL_NOTIFIER_ID:
                        emailNotifierService.trigger(hook, genericNotificationConfig, params);
                        break;
                    case DEFAULT_WEBHOOK_NOTIFIER_ID:
                        webhookNotifierService.trigger(hook, genericNotificationConfig, params);
                        break;
                    default:
                        LOGGER.error("Unknown notifier {}", genericNotificationConfig.getNotifier());
                        break;
                }
            }
        } catch (TechnicalException e) {
            LOGGER.error("Error looking for GenericNotificationConfig with {}/{}/{}", hook, refType, refId, e);
        }
    }

    @Override
    public List<NotifierEntity> list(NotificationReferenceType referenceType, String referenceId) {
        NotifierEntity emailNotifier = new NotifierEntity();
        emailNotifier.setId(DEFAULT_EMAIL_NOTIFIER_ID);
        emailNotifier.setName("Default Email Notifier");
        emailNotifier.setType("EMAIL");

        NotifierEntity webHookNotifier = new NotifierEntity();
        webHookNotifier.setId(DEFAULT_WEBHOOK_NOTIFIER_ID);
        webHookNotifier.setName("Default Webhook Notifier");
        webHookNotifier.setType("WEBHOOK");
        return Arrays.asList(emailNotifier, webHookNotifier);
    }
}
