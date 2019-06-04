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
package io.gravitee.rest.api.service.impl;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.repository.management.model.PortalNotificationDefaultReferenceId;
import io.gravitee.rest.api.model.notification.NotifierEntity;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.gravitee.rest.api.service.notifiers.EmailNotifierService;
import io.gravitee.rest.api.service.notifiers.WebhookNotifierService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.*;
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

    private static final io.gravitee.management.model.NotifierEntity DEFAULT_EMAIL_NOTIFIER;

    static {
        DEFAULT_EMAIL_NOTIFIER = new io.gravitee.management.model.NotifierEntity();
        DEFAULT_EMAIL_NOTIFIER.setId(DEFAULT_EMAIL_NOTIFIER_ID);
        DEFAULT_EMAIL_NOTIFIER.setName("System email");
        DEFAULT_EMAIL_NOTIFIER.setDescription("System email notifier");
    }

    @Autowired
    private ConfigurablePluginManager<NotifierPlugin> notifierManager;

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

    @Override
    public Set<io.gravitee.management.model.NotifierEntity> findAll() {
        try {
            LOGGER.debug("List all notifiers");
            final Collection<NotifierPlugin> plugins = notifierManager.findAll();

            Set<io.gravitee.management.model.NotifierEntity> notifiers = plugins.stream()
                    .map(plugin -> convert(plugin, false))
                    .collect(Collectors.toSet());
            notifiers.add(DEFAULT_EMAIL_NOTIFIER);

            return notifiers;
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to list all notifiers", ex);
            throw new TechnicalManagementException("An error occurs while trying to list all notifiers", ex);
        }
    }

    @Override
    public io.gravitee.management.model.NotifierEntity findById(String notifier) {
        LOGGER.debug("Find policy by ID: {}", notifier);

        if (DEFAULT_EMAIL_NOTIFIER_ID.equals(notifier)) {
            return DEFAULT_EMAIL_NOTIFIER;
        }

        NotifierPlugin plugin = notifierManager.get(notifier);

        if (plugin == null) {
            throw new NotifierNotFoundException(notifier);
        }

        return convert(plugin, true);
    }

    @Override
    public String getSchema(String notifier) {
        try {
            LOGGER.debug("Find notifier schema by ID: {}", notifier);
            if (DEFAULT_EMAIL_NOTIFIER_ID.equals(notifier)) {
                URL url = Resources.getResource("notifiers/" + DEFAULT_EMAIL_NOTIFIER_ID + ".json");
                return Resources.toString(url, Charsets.UTF_8);
            } else {
                return notifierManager.getSchema(notifier);
            }
        } catch (IOException ioex) {
            LOGGER.error("An error occurs while trying to get notifier schema for notifier {}", notifier, ioex);
            throw new TechnicalManagementException("An error occurs while trying to get notifier schema for notifier " + notifier, ioex);
        }
    }

    private io.gravitee.management.model.NotifierEntity convert(NotifierPlugin plugin, boolean withPlugin) {
        io.gravitee.management.model.NotifierEntity entity = new io.gravitee.management.model.NotifierEntity();

        entity.setId(plugin.id());
        entity.setDescription(plugin.manifest().description());
        entity.setName(plugin.manifest().name());
        entity.setVersion(plugin.manifest().version());

        if (withPlugin) {
            // Plugin information
            PluginEntity pluginEntity = new PluginEntity();

            pluginEntity.setPlugin(plugin.clazz());
            pluginEntity.setPath(plugin.path().toString());
            pluginEntity.setType(plugin.type().toString().toLowerCase());
            pluginEntity.setDependencies(plugin.dependencies());

            entity.setPlugin(pluginEntity);
        }

        return entity;
    }
}
