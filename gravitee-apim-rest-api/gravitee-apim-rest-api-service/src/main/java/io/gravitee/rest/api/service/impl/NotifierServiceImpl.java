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
package io.gravitee.rest.api.service.impl;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.notifier.NotifierPlugin;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PluginEntity;
import io.gravitee.rest.api.model.notification.NotifierEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.EmailRecipientsService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.NotifierNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.gravitee.rest.api.service.notifiers.EmailNotifierService;
import io.gravitee.rest.api.service.notifiers.WebhookNotifierService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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

    private static final io.gravitee.rest.api.model.NotifierEntity DEFAULT_EMAIL_NOTIFIER;

    static {
        DEFAULT_EMAIL_NOTIFIER = new io.gravitee.rest.api.model.NotifierEntity();
        DEFAULT_EMAIL_NOTIFIER.setId(DEFAULT_EMAIL_NOTIFIER_ID);
        DEFAULT_EMAIL_NOTIFIER.setName("System email");
        DEFAULT_EMAIL_NOTIFIER.setDescription("System email notifier");
    }

    private final ConfigurablePluginManager<NotifierPlugin> notifierManager;
    private final PortalNotificationConfigRepository portalNotificationConfigRepository;
    private final PortalNotificationService portalNotificationService;
    private final GenericNotificationConfigRepository genericNotificationConfigRepository;
    private final EmailNotifierService emailNotifierService;
    private final WebhookNotifierService webhookNotifierService;
    private final EmailRecipientsService emailRecipientsService;
    private final ParameterService parameterService;
    private final MembershipService membershipService;

    public NotifierServiceImpl(
        ConfigurablePluginManager<NotifierPlugin> notifierManager,
        @Lazy PortalNotificationConfigRepository portalNotificationConfigRepository,
        PortalNotificationService portalNotificationService,
        @Lazy GenericNotificationConfigRepository genericNotificationConfigRepository,
        @Lazy EmailNotifierService emailNotifierService,
        @Lazy WebhookNotifierService webhookNotifierService,
        @Lazy EmailRecipientsService emailRecipientsService,
        @Lazy ParameterService parameterService,
        @Lazy MembershipService membershipService
    ) {
        this.notifierManager = notifierManager;
        this.portalNotificationConfigRepository = portalNotificationConfigRepository;
        this.portalNotificationService = portalNotificationService;
        this.genericNotificationConfigRepository = genericNotificationConfigRepository;
        this.emailNotifierService = emailNotifierService;
        this.webhookNotifierService = webhookNotifierService;
        this.emailRecipientsService = emailRecipientsService;
        this.parameterService = parameterService;
        this.membershipService = membershipService;
    }

    @AllArgsConstructor
    @Builder
    static class TriggerNotificationsData {

        public Hook hook;
        public NotificationReferenceType referenceType;
        public String referenceId;
        public String orgId;
        public Map<String, Object> params;
        public List<Recipient> recipients;

        @Override
        public String toString() {
            return "{ hook=" + hook + ", referenceType=" + referenceType + ", referenceId=" + referenceId + ", orgId=" + orgId + " }";
        }
    }

    @Override
    @Async
    public void trigger(final ExecutionContext executionContext, final ApiHook hook, final String apiId, Map<String, Object> params) {
        var triggerNotificationsData = TriggerNotificationsData
            .builder()
            .hook(hook)
            .referenceType(NotificationReferenceType.API)
            .referenceId(apiId)
            .params(params)
            .build();
        triggerPortalNotifications(executionContext, triggerNotificationsData);
        triggerGenericNotifications(executionContext, triggerNotificationsData);
    }

    @Override
    @Async
    public void trigger(
        final ExecutionContext executionContext,
        final ApplicationHook hook,
        final String applicationId,
        Map<String, Object> params
    ) {
        var data = TriggerNotificationsData
            .builder()
            .hook(hook)
            .referenceType(NotificationReferenceType.APPLICATION)
            .referenceId(applicationId)
            .params(params)
            .build();
        triggerPortalNotifications(executionContext, data);
        triggerGenericNotifications(executionContext, data);
    }

    @Override
    @Async
    public void trigger(
        final ExecutionContext executionContext,
        final ApplicationHook hook,
        final String applicationId,
        Map<String, Object> params,
        List<Recipient> recipients
    ) {
        var data = TriggerNotificationsData
            .builder()
            .hook(hook)
            .referenceType(NotificationReferenceType.APPLICATION)
            .referenceId(applicationId)
            .params(params)
            .build();
        triggerPortalNotifications(executionContext, data);
        triggerGenericNotifications(executionContext, data);
    }

    @Override
    @Async
    public void trigger(final ExecutionContext executionContext, final PortalHook hook, Map<String, Object> params) {
        var dataBuilder = TriggerNotificationsData.builder().hook(hook).params(params);
        if (executionContext.hasEnvironmentId()) {
            dataBuilder.referenceType(NotificationReferenceType.ENVIRONMENT).referenceId(executionContext.getEnvironmentId());
        } else {
            dataBuilder.orgId(executionContext.getOrganizationId());
        }
        var data = dataBuilder.build();
        triggerPortalNotifications(executionContext, data);
        triggerGenericNotifications(executionContext, data);
    }

    private void triggerPortalNotifications(final ExecutionContext executionContext, final TriggerNotificationsData data) {
        try {
            List<PortalNotificationConfig> portalNotificationConfigs;
            if (data.referenceId != null) {
                portalNotificationConfigs =
                    portalNotificationConfigRepository.findByReferenceAndHook(data.hook.name(), data.referenceType, data.referenceId);
            } else {
                portalNotificationConfigs = portalNotificationConfigRepository.findByHookAndOrganizationId(data.hook.name(), data.orgId);
            }
            List<String> userIds = portalNotificationConfigs
                .stream()
                .flatMap(n -> users(executionContext, n))
                .distinct()
                .collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                portalNotificationService.create(executionContext, data.hook, userIds, data.params);
            }
        } catch (TechnicalException e) {
            LOGGER.error("Error looking for PortalNotificationConfig with {}", data, e);
        }
    }

    private Stream<String> users(final ExecutionContext executionContext, final PortalNotificationConfig portalNotificationConfig) {
        List<String> userIds = new ArrayList<>();
        userIds.add(portalNotificationConfig.getUser());
        if (portalNotificationConfig.getGroups() != null) {
            membershipService
                .getMembersByReferencesAndRole(
                    executionContext,
                    MembershipReferenceType.GROUP,
                    List.copyOf(portalNotificationConfig.getGroups()),
                    null
                )
                .stream()
                .map(MemberEntity::getId)
                .forEach(userIds::add);
        }
        return userIds.stream();
    }

    @VisibleForTesting
    void triggerGenericNotifications(ExecutionContext executionContext, TriggerNotificationsData data) {
        try {
            List<GenericNotificationConfig> genericNotificationConfigs;
            if (data.orgId != null) {
                genericNotificationConfigs = genericNotificationConfigRepository.findByHookAndOrganizationId(data.hook.name(), data.orgId);
            } else {
                genericNotificationConfigs =
                    genericNotificationConfigRepository.findByReferenceAndHook(data.hook.name(), data.referenceType, data.referenceId);
            }
            var notificationConfigs = genericNotificationConfigs
                .stream()
                .collect(Collectors.groupingBy(GenericNotificationConfig::getNotifier));
            if (!notificationConfigs.isEmpty()) {
                list()
                    .forEach(notifier -> {
                        switch (notifier.type()) {
                            case EMAIL -> {
                                var recipients = notificationConfigs
                                    .getOrDefault(notifier.getId(), Collections.emptyList())
                                    .stream()
                                    .map(GenericNotificationConfig::getConfig)
                                    .collect(Collectors.toList());

                                if (!CollectionUtils.isEmpty(data.recipients)) {
                                    var emailAdditionalRecipients = data.recipients
                                        .stream()
                                        .filter(r -> r.type().equals(DEFAULT_EMAIL_NOTIFIER_ID))
                                        .map(Recipient::value)
                                        .toList();
                                    recipients.addAll(emailAdditionalRecipients);
                                }

                                // extract emails from templated string (eg: ${api.primaryOwner.email})
                                var processedRecipients = emailRecipientsService.processTemplatedRecipients(recipients, data.params);
                                // extract emails of opted-in users if trial instance
                                var validRecipients = parameterService.findAsBoolean(
                                        executionContext,
                                        Key.TRIAL_INSTANCE,
                                        ParameterReferenceType.SYSTEM
                                    )
                                    ? emailRecipientsService.filterRegisteredUser(executionContext, processedRecipients)
                                    : processedRecipients;

                                emailNotifierService.trigger(executionContext, data.hook, data.params, validRecipients);
                            }
                            case WEBHOOK -> {
                                notificationConfigs
                                    .getOrDefault(notifier.getId(), Collections.emptyList())
                                    .forEach(config -> webhookNotifierService.trigger(data.hook, config, data.params));
                            }
                            default -> LOGGER.error("Unknown notifier {}", notifier.getType());
                        }
                    });
            }
        } catch (TechnicalException e) {
            LOGGER.error("Error looking for GenericNotificationConfig with {}", data, e);
        }
    }

    @Override
    public List<NotifierEntity> list() {
        return Arrays.asList(
            new NotifierEntity(DEFAULT_EMAIL_NOTIFIER_ID, NotifierEntity.Type.EMAIL, "Default Email Notifier"),
            new NotifierEntity(DEFAULT_WEBHOOK_NOTIFIER_ID, NotifierEntity.Type.WEBHOOK, "Default Webhook Notifier")
        );
    }

    @Override
    public Set<io.gravitee.rest.api.model.NotifierEntity> findAll() {
        try {
            LOGGER.debug("List all notifiers");
            final Collection<NotifierPlugin> plugins = notifierManager.findAll();

            Set<io.gravitee.rest.api.model.NotifierEntity> notifiers = plugins
                .stream()
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
    public io.gravitee.rest.api.model.NotifierEntity findById(String notifier) {
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
                final URL url = getClass().getResource("/notifiers/" + DEFAULT_EMAIL_NOTIFIER_ID + ".json");

                if (url == null) {
                    throw new IOException("Resource not found for: /notifiers/" + DEFAULT_EMAIL_NOTIFIER_ID + ".json");
                }

                try (
                    final InputStream notifierInputStream = url.openStream();
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(notifierInputStream, StandardCharsets.UTF_8))
                ) {
                    return reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            } else {
                return notifierManager.getSchema(notifier);
            }
        } catch (IOException ioex) {
            LOGGER.error("An error occurs while trying to get notifier schema for notifier {}", notifier, ioex);
            throw new TechnicalManagementException("An error occurs while trying to get notifier schema for notifier " + notifier, ioex);
        }
    }

    private io.gravitee.rest.api.model.NotifierEntity convert(NotifierPlugin plugin, boolean withPlugin) {
        io.gravitee.rest.api.model.NotifierEntity entity = new io.gravitee.rest.api.model.NotifierEntity();

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
