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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.alert.api.condition.Filter;
import io.gravitee.alert.api.condition.StringCondition;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.common.utils.UUID;
import io.gravitee.rest.api.model.alert.*;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.exceptions.AlertNotFoundException;
import io.gravitee.rest.api.service.exceptions.AlertUnavailableException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.alert.EmailNotifierConfiguration;
import io.gravitee.notifier.api.Notification;
import io.gravitee.plugin.alert.AlertTriggerProviderManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AlertServiceImpl extends TransactionalService implements AlertService, InitializingBean {

    private final Logger LOGGER = LoggerFactory.getLogger(AlertServiceImpl.class);

    @Value("${notifiers.email.subject:[Gravitee.io] %s}")
    private String subject;
    @Value("${notifiers.email.host}")
    private String host;
    @Value("${notifiers.email.port}")
    private String port;
    @Value("${notifiers.email.username:#{null}}")
    private String username;
    @Value("${notifiers.email.password:#{null}}")
    private String password;
    @Value("${notifiers.email.from}")
    private String defaultFrom;
    @Value("${notifiers.email.starttls.enabled:false}")
    private boolean startTLSEnabled;
    @Value("${notifiers.email.ssl.trustAll:false}")
    private boolean sslTrustAll;
    @Value("${notifiers.email.ssl.keyStore:#{null}}")
    private String sslKeyStore;
    @Value("${notifiers.email.ssl.keyStorePassword:#{null}}")
    private String sslKeyStorePassword;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private AlertTriggerRepository alertTriggerRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private TriggerProvider triggerProvider;

    @Autowired
    private AlertTriggerProviderManager triggerProviderManager;

    @Autowired
    private ParameterService parameterService;

    @Override
    public AlertStatusEntity getStatus() {
        AlertStatusEntity status = new AlertStatusEntity();

        status.setEnabled(parameterService.findAsBoolean(Key.ALERT_ENABLED));
        status.setPlugins(triggerProviderManager.findAll().size());

        return status;
    }

    @Override
    public AlertTriggerEntity create(final NewAlertTriggerEntity newAlertTrigger) {
        checkAlert();

        try {
            // Get trigger
            AlertTrigger alertTrigger = convert(newAlertTrigger);

            alertTrigger.setCreatedAt(new Date());
            alertTrigger.setUpdatedAt(alertTrigger.getCreatedAt());

            final AlertTrigger createdAlert = alertTriggerRepository.create(alertTrigger);
            final AlertTriggerEntity alertTriggerEntity = convert(createdAlert);

            enhance(alertTriggerEntity, newAlertTrigger.getReferenceType(), newAlertTrigger.getReferenceId());
            triggerOrCancelAlert(alertTriggerEntity);

            return alertTriggerEntity;
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to create an alert " + newAlertTrigger;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public AlertTriggerEntity update(final UpdateAlertTriggerEntity updateAlertTrigger) {
        checkAlert();

        try {
            final Optional<AlertTrigger> alertOptional = alertTriggerRepository.findById(updateAlertTrigger.getId());
            if (alertOptional.isPresent()) {
                final AlertTrigger alertToUpdate = alertOptional.get();
                if (!alertToUpdate.getReferenceId().equals(updateAlertTrigger.getReferenceId())) {
                    throw new AlertNotFoundException(updateAlertTrigger.getId());
                }

                AlertTrigger trigger = convert(updateAlertTrigger);
                trigger.setId(updateAlertTrigger.getId());
                trigger.setReferenceId(alertOptional.get().getReferenceId());
                trigger.setReferenceType(alertOptional.get().getReferenceType());
                trigger.setCreatedAt(alertOptional.get().getCreatedAt());
                trigger.setType(alertOptional.get().getType());
                trigger.setUpdatedAt(new Date());

                final AlertTriggerEntity alertTriggerEntity = convert(alertTriggerRepository.update(trigger));

                enhance(alertTriggerEntity, updateAlertTrigger.getReferenceType(), updateAlertTrigger.getReferenceId());
                triggerOrCancelAlert(alertTriggerEntity);

                return alertTriggerEntity;
            } else {
                throw new AlertNotFoundException(updateAlertTrigger.getId());
            }
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to update an alert " + updateAlertTrigger;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public List<AlertTriggerEntity> findAll() {
        try {
            final Set<AlertTrigger> triggers = alertTriggerRepository.findAll();
            return triggers.stream().map(this::convert).collect(toList());
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list all alerts";
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public List<AlertTriggerEntity> findByReference(final AlertReferenceType referenceType, final String referenceId) {
        try {
            final List<AlertTrigger> triggers = alertTriggerRepository.findByReference(referenceType.name(), referenceId);
            return triggers.stream().map(this::convert).sorted(comparing(AlertTriggerEntity::getName)).collect(toList());
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list alerts by reference " + referenceType
                    + '/' + referenceId;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public void delete(final String alertId, final String referenceId) {
        checkAlert();
        try {
            final Optional<AlertTrigger> optionalAlert = alertTriggerRepository.findById(alertId);
            if (!optionalAlert.isPresent() || !optionalAlert.get().getReferenceId().equals(referenceId)) {
                throw new AlertNotFoundException(alertId);
            }
            final AlertTriggerEntity alert = convert(optionalAlert.get());

            // Remove from repository
            alertTriggerRepository.delete(alertId);

            // Notify alert plugins
            disableTrigger(alert);
        } catch (TechnicalException te) {
            final String msg = "An error occurs while trying to delete the alert " + alertId;
            LOGGER.error(msg, te);
            throw new TechnicalManagementException(msg, te);
        }
    }

    /*
    protected Notification createDefaultNotification(final AlertTriggerEntity trigger) {
        final String ownerEmail = getOwnerEmail(trigger);

        return createDefaultNotification(trigger, ownerEmail);
    }

    protected Notification createDefaultNotification(final AlertTriggerEntity trigger, final String ownerEmail) {
        if (ownerEmail == null) {
            LOGGER.warn("Trigger cannot be sent cause the owner of the {} '{}' has no configured email",
                    trigger.getReferenceType(), trigger.getReferenceId());

            return null;
        }

        final Notification notification = new Notification();
        notification.setType("email-notifier");

        final String triggerName = trigger.getDescription() == null ? trigger.getName() : trigger.getDescription();

        final JsonObject configuration = new JsonObject();
        configuration.put("subject", format(subject, "Alert: " + triggerName));
        configuration.put("from", defaultFrom);
        configuration.put("to", ownerEmail);
        configuration.put("host", host);
        configuration.put("port", port);
        configuration.put("username", username);
        configuration.put("password", password);
        configuration.put("startTLSEnabled", startTLSEnabled);
        configuration.put("sslTrustAll", sslTrustAll);
        configuration.put("sslKeyStore", sslKeyStore);
        configuration.put("sslKeyStorePassword", sslKeyStorePassword);

        notification.setConfiguration(configuration.encodePrettily());
        return notification;
    }
     */

    private String getOwnerEmail(final AlertTriggerEntity alert) {
        switch (alert.getReferenceType()) {
            case API:
                return apiService.findById(alert.getReferenceId()).getPrimaryOwner().getEmail();
            case APPLICATION:
                return applicationService.findById(alert.getReferenceId()).getPrimaryOwner().getEmail();
            default:
                return null;
        }
    }

    private void enhance(final AlertTriggerEntity trigger, final AlertReferenceType referenceType, final String referenceId) {
        // Notifications
        List<Notification> notifications = trigger.getNotifications();
        if (notifications == null) {
            notifications = new ArrayList<>();
            trigger.setNotifications(notifications);
        }

        // Set the email notifier configuration in case
        notifications.forEach(new Consumer<Notification>() {
            @Override
            public void accept(Notification notification) {
                if (NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID.equalsIgnoreCase(notification.getType())) {
                    setDefaultEmailNotifier(notification);
                }
            }
        });

        // Filters
        List<Filter> filters = trigger.getFilters();
        if (filters == null) {
            filters = new ArrayList<>();
            trigger.setFilters(filters);
        }

        switch (referenceType) {
            case API:
            case APPLICATION:
                filters.add(StringCondition.equals(referenceType.name().toLowerCase(), referenceId).build());
                break;
        }
    }

    private void setDefaultEmailNotifier(Notification notification) {
        EmailNotifierConfiguration configuration = new EmailNotifierConfiguration();

        configuration.setHost(host);
        configuration.setPort(Integer.parseInt(port));
        configuration.setUsername(username);
        configuration.setPassword(password);
        configuration.setStartTLSEnabled(startTLSEnabled);
        configuration.setSslKeyStore(sslKeyStore);
        configuration.setSslKeyStorePassword(sslKeyStorePassword);
        configuration.setSslTrustAll(sslTrustAll);

        try {
            JsonNode emailNode = mapper.readTree(notification.getConfiguration());
            configuration.setFrom(emailNode.path("from").asText());
            configuration.setTo(emailNode.path("to").asText());
            configuration.setSubject(emailNode.path("subject").asText());
            configuration.setBody(emailNode.path("body").asText());

            notification.setConfiguration(mapper.writeValueAsString(configuration));
            notification.setType("email-notifier");
        } catch (IOException e) {
            LOGGER.error("Unexpected error while converting system email configuration to email notifier");
        }
    }

    private void checkAlert() {
        if (!parameterService.findAsBoolean(Key.ALERT_ENABLED) || triggerProviderManager.findAll().isEmpty()) {
            throw new AlertUnavailableException();
        }
    }

    private void triggerOrCancelAlert(final Trigger trigger) {
        if (trigger.isEnabled()) {
            pushTrigger(trigger);
        } else {
            disableTrigger(trigger);
        }
    }

    private void pushTrigger(final Trigger trigger) {
        triggerProvider.register(trigger);
    }

    private void disableTrigger(Trigger trigger) {
        trigger.setEnabled(false);
        triggerProvider.unregister(trigger);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        triggerProvider.addListener(new TriggerProvider.OnConnectionListener() {
            @Override
            public void doOnConnect() {
                LOGGER.info("Connected to alerting system. Sync alert triggers...");
                // On reconnect, ensure to push all the triggers again
                findAll().forEach(new Consumer<AlertTriggerEntity>() {
                    @Override
                    public void accept(AlertTriggerEntity alertTriggerEntity) {
                        enhance(alertTriggerEntity, alertTriggerEntity.getReferenceType(), alertTriggerEntity.getReferenceId());
                        triggerOrCancelAlert(alertTriggerEntity);
                    }
                });
                LOGGER.info("Alert triggers synchronized with the alerting system.");
            }
        });

        triggerProvider.addListener(new TriggerProvider.OnDisconnectionListener() {
            @Override
            public void doOnDisconnect() {
                LOGGER.error("Connection with the alerting system has been loose.");
            }
        });
    }

    private AlertTrigger convert(final NewAlertTriggerEntity alertEntity) {
        final AlertTrigger alert = new AlertTrigger();
        alert.setId(RandomString.generate());
        alertEntity.setId(alert.getId());
        alert.setName(alertEntity.getName());
        alert.setDescription(alertEntity.getDescription());
        alert.setReferenceId(alertEntity.getReferenceId());
        alert.setReferenceType(alertEntity.getReferenceType().name());
        alert.setEnabled(alertEntity.isEnabled());
        alert.setType(alertEntity.getType());
        alert.setSeverity(alertEntity.getSeverity().name());

        try {
            String definition = mapper.writeValueAsString(alertEntity);
            alert.setDefinition(definition);
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurs while transforming the alert trigger definition into string", ex);
        }

        return alert;
    }

    private AlertTrigger convert(final UpdateAlertTriggerEntity alertEntity) {
        final AlertTrigger alert = new AlertTrigger();
        alert.setId(RandomString.generate());
        alert.setName(alertEntity.getName());
        alert.setDescription(alertEntity.getDescription());
        alert.setEnabled(alertEntity.isEnabled());
        alert.setSeverity(alertEntity.getSeverity().name());

        try {
            String definition = mapper.writeValueAsString(alertEntity);
            alert.setDefinition(definition);
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurs while transforming the alert trigger definition into string", ex);
        }

        return alert;
    }

    private AlertTriggerEntity convert(final AlertTrigger alert) {
        try {
            Trigger trigger = mapper.readValue(alert.getDefinition(), Trigger.class);

            final AlertTriggerEntity alertTriggerEntity = new AlertTriggerEntityWrapper(trigger);
            alertTriggerEntity.setDescription(alert.getDescription());
            alertTriggerEntity.setReferenceId(alert.getReferenceId());
            alertTriggerEntity.setReferenceType(AlertReferenceType.valueOf(alert.getReferenceType()));
            alertTriggerEntity.setCreatedAt(alert.getCreatedAt());
            alertTriggerEntity.setUpdatedAt(alert.getUpdatedAt());
            alertTriggerEntity.setType(alert.getType());
            if (alert.getSeverity() != null) {
                alertTriggerEntity.setSeverity(Trigger.Severity.valueOf(alert.getSeverity()));
            } else {
                alertTriggerEntity.setSeverity(Trigger.Severity.INFO);
            }

            return alertTriggerEntity;
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurs while transforming the alert trigger from its definition", ex);
        }

        return null;
    }
}
