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

import static io.gravitee.rest.api.model.alert.AlertReferenceType.API;
import static io.gravitee.rest.api.model.alert.AlertReferenceType.APPLICATION;
import static io.gravitee.rest.api.service.common.ReferenceContext.Type.ENVIRONMENT;
import static io.gravitee.rest.api.service.common.ReferenceContext.Type.ORGANIZATION;
import static io.gravitee.rest.api.service.impl.AbstractService.convert;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.alert.api.condition.Filter;
import io.gravitee.alert.api.condition.StringCondition;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.alert.api.trigger.command.AlertNotificationCommand;
import io.gravitee.alert.api.trigger.command.Command;
import io.gravitee.alert.api.trigger.command.Handler;
import io.gravitee.alert.api.trigger.command.ResolvePropertyCommand;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.notifier.api.Notification;
import io.gravitee.plugin.alert.AlertTriggerProviderManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.AlertEventCriteria;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.AlertEvent;
import io.gravitee.repository.management.model.AlertEventRule;
import io.gravitee.repository.management.model.AlertEventType;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.AlertEventQuery;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.alert.*;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.converter.AlertTriggerConverter;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.alert.EmailNotifierConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AlertServiceImpl extends TransactionalService implements AlertService, InitializingBean {

    private final Logger LOGGER = LoggerFactory.getLogger(AlertServiceImpl.class);

    private static final String UNKNOWN_SERVICE = "1";
    private static final String FIELD_API = "api";
    private static final String FIELD_APPLICATION = "application";
    private static final String FIELD_PLAN = "plan";

    private static final String METADATA_NAME = "name";
    private static final String METADATA_DELETED = "deleted";
    private static final String METADATA_UNKNOWN = "unknown";
    private static final String METADATA_UNKNOWN_API_NAME = "Unknown API (not found)";
    private static final String METADATA_UNKNOWN_APPLICATION_NAME = "Unknown application (keyless)";
    private static final String METADATA_DELETED_API_NAME = "Deleted API";
    private static final String METADATA_DELETED_APPLICATION_NAME = "Deleted application";
    private static final String METADATA_DELETED_PLAN_NAME = "Deleted plan";

    private final Configuration configuration;
    private final ObjectMapper mapper;
    private final AlertTriggerRepository alertTriggerRepository;
    private final ApiService apiService;
    private final ApplicationService applicationService;
    private final PlanService planService;
    private final AlertEventRepository alertEventRepository;
    private final TriggerProvider triggerProvider;
    private final AlertTriggerProviderManager triggerProviderManager;
    private final ParameterService parameterService;
    private final ApiRepository apiRepository;

    private final AlertTriggerConverter alertTriggerConverter;

    private final EnvironmentService environmentService;

    @Autowired
    public AlertServiceImpl(
        Configuration configuration,
        ObjectMapper mapper,
        @Lazy AlertTriggerRepository alertTriggerRepository,
        @Lazy ApiService apiService,
        ApplicationService applicationService,
        PlanService planService,
        @Lazy AlertEventRepository alertEventRepository,
        TriggerProvider triggerProvider,
        AlertTriggerProviderManager triggerProviderManager,
        ParameterService parameterService,
        @Lazy ApiRepository apiRepository,
        AlertTriggerConverter alertTriggerConverter,
        EnvironmentService environmentService
    ) {
        this.configuration = configuration;
        this.mapper = mapper;
        this.alertTriggerRepository = alertTriggerRepository;
        this.apiService = apiService;
        this.applicationService = applicationService;
        this.planService = planService;
        this.alertEventRepository = alertEventRepository;
        this.triggerProvider = triggerProvider;
        this.triggerProviderManager = triggerProviderManager;
        this.parameterService = parameterService;
        this.apiRepository = apiRepository;
        this.alertTriggerConverter = alertTriggerConverter;
        this.environmentService = environmentService;
    }

    @Override
    public AlertStatusEntity getStatus(final ExecutionContext executionContext) {
        AlertStatusEntity status = new AlertStatusEntity();

        status.setEnabled(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION));
        status.setPlugins(triggerProviderManager.findAll().size());

        return status;
    }

    @Override
    public AlertTriggerEntity create(final ExecutionContext executionContext, final NewAlertTriggerEntity newAlertTrigger) {
        checkAlert(executionContext);

        try {
            // Get trigger
            AlertTrigger alertTrigger = alertTriggerConverter.toAlertTrigger(executionContext, newAlertTrigger);

            alertTrigger.setCreatedAt(new Date());
            alertTrigger.setUpdatedAt(alertTrigger.getCreatedAt());

            return create(executionContext, alertTrigger);
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to create an alert " + newAlertTrigger;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    private AlertTriggerEntity create(final ExecutionContext executionContext, AlertTrigger alertTrigger) throws TechnicalException {
        final AlertTrigger createdAlert = alertTriggerRepository.create(alertTrigger);

        final AlertTriggerEntity alertTriggerEntity = alertTriggerConverter.toAlertTriggerEntity(createdAlert);

        fillNotificationsAndFilters(
            executionContext,
            alertTriggerEntity,
            alertTriggerEntity.getReferenceType(),
            alertTriggerEntity.getReferenceId()
        );

        // Obviously, we are not deploying rule templates :)
        if (!alertTriggerEntity.isTemplate()) {
            triggerOrCancelAlert(alertTriggerEntity);
        }

        return alertTriggerEntity;
    }

    @Override
    public AlertTriggerEntity update(final ExecutionContext executionContext, final UpdateAlertTriggerEntity updateAlertTrigger) {
        checkAlert(executionContext);

        try {
            final AlertTrigger alertToUpdate = alertTriggerRepository
                .findById(updateAlertTrigger.getId())
                .orElseThrow(() -> new AlertNotFoundException(updateAlertTrigger.getId()));

            if (!alertToUpdate.getReferenceId().equals(updateAlertTrigger.getReferenceId())) {
                throw new AlertNotFoundException(updateAlertTrigger.getId());
            }

            AlertTrigger trigger = alertTriggerConverter.toAlertTrigger(executionContext, updateAlertTrigger);
            trigger.setReferenceId(alertToUpdate.getReferenceId());
            trigger.setReferenceType(alertToUpdate.getReferenceType());
            trigger.setCreatedAt(alertToUpdate.getCreatedAt());
            trigger.setType(alertToUpdate.getType());
            trigger.setUpdatedAt(new Date());

            final AlertTriggerEntity alertTriggerEntity = alertTriggerConverter.toAlertTriggerEntity(
                alertTriggerRepository.update(trigger)
            );

            fillNotificationsAndFilters(
                executionContext,
                alertTriggerEntity,
                updateAlertTrigger.getReferenceType(),
                updateAlertTrigger.getReferenceId()
            );

            // Obviously, we are not deploying rule templates :)
            if (!alertTriggerEntity.isTemplate()) {
                triggerOrCancelAlert(alertTriggerEntity);
            }

            return alertTriggerEntity;
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to update an alert " + updateAlertTrigger;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public AlertTriggerEntity findById(String alertId) {
        try {
            return alertTriggerRepository
                .findById(alertId)
                .map(alertTriggerConverter::toAlertTriggerEntity)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while trying to find alert by id {}", alertId, e);
            throw new TechnicalManagementException("An error occurs while trying to find alert with id", e);
        }
    }

    @Override
    public List<AlertTriggerEntity> findAll() {
        try {
            return alertTriggerConverter.toAlertTriggerEntities(new ArrayList<>(alertTriggerRepository.findAll()));
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list all alerts";
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public List<AlertTriggerEntity> findByReference(final AlertReferenceType referenceType, final String referenceId) {
        try {
            return alertTriggerConverter.toAlertTriggerEntities(
                alertTriggerRepository.findByReferenceAndReferenceId(referenceType.name(), referenceId)
            );
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list alerts by reference " + referenceType + '/' + referenceId;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public List<AlertTriggerEntity> findByReferences(final AlertReferenceType referenceType, final List<String> referenceIds) {
        try {
            return alertTriggerConverter.toAlertTriggerEntities(
                alertTriggerRepository.findByReferenceAndReferenceIds(referenceType.name(), referenceIds)
            );
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list alerts by references " + referenceType + '/' + referenceIds;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public List<AlertTriggerEntity> findByReferenceWithEventCounts(final AlertReferenceType referenceType, final String referenceId) {
        try {
            final List<AlertTrigger> triggers = alertTriggerRepository.findByReferenceAndReferenceId(referenceType.name(), referenceId);
            return triggers
                .stream()
                .map(alertTrigger -> {
                    AlertTriggerEntity entity = alertTriggerConverter.toAlertTriggerEntity(alertTrigger);

                    getLastEvent(entity.getId())
                        .ifPresent(alertEvent -> {
                            entity.setLastAlertAt(alertEvent.getCreatedAt());
                            entity.setLastAlertMessage(alertEvent.getMessage());
                        });

                    final Date from = new Date(System.currentTimeMillis());

                    Map<String, Integer> counters = Map
                        .of(
                            "5m",
                            from.toInstant().minus(Duration.ofMinutes(5)),
                            "1h",
                            from.toInstant().minus(Duration.ofHours(1)),
                            "1d",
                            from.toInstant().minus(Duration.ofDays(1)),
                            "1M",
                            from.toInstant().minus(Duration.ofDays(30))
                        )
                        .entrySet()
                        // Get the count of events for each time period in parallel to speed up the process
                        .parallelStream()
                        .map(entry ->
                            new AbstractMap.SimpleEntry<>(
                                entry.getKey(),
                                countEvents(entity.getId(), entry.getValue().toEpochMilli(), from.getTime())
                            )
                        )
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    entity.setCounters(counters);
                    return entity;
                })
                .sorted(comparing(AlertTriggerEntity::getName))
                .collect(toList());
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list alerts by reference " + referenceType + '/' + referenceId;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public void delete(final String alertId, final String referenceId) {
        try {
            final AlertTriggerEntity alert = alertTriggerRepository
                .findById(alertId)
                .filter(a -> a.getReferenceId().equals(referenceId))
                .map(alertTriggerConverter::toAlertTriggerEntity)
                .orElseThrow(() -> new AlertNotFoundException(alertId));

            // Remove from repository
            alertTriggerRepository.delete(alertId);
            alertEventRepository.deleteAll(alertId);

            // Notify alert plugins
            disableTrigger(alert);
        } catch (TechnicalException te) {
            final String msg = "An error occurs while trying to delete the alert " + alertId;
            LOGGER.error(msg, te);
            throw new TechnicalManagementException(msg, te);
        }
    }

    @Override
    public Page<AlertEventEntity> findEvents(final String alertId, final AlertEventQuery eventQuery) {
        Page<AlertEvent> alertEventsRepo = alertEventRepository.search(
            new AlertEventCriteria.Builder().alert(alertId).from(eventQuery.getFrom()).to(eventQuery.getTo()).build(),
            new PageableBuilder().pageNumber(eventQuery.getPageNumber()).pageSize(eventQuery.getPageSize()).build()
        );

        if (alertEventsRepo.getPageElements() == 0) {
            return new Page<>(List.of(), 1, 0, 0);
        }

        return alertEventsRepo.map(alertEventRepo -> {
            AlertEventEntity alertEvent = new AlertEventEntity();
            alertEvent.setCreatedAt(alertEventRepo.getCreatedAt());
            alertEvent.setMessage(alertEventRepo.getMessage());
            return alertEvent;
        });
    }

    private Set<AlertTriggerEntity> findByEvent(AlertEventType event) {
        try {
            LOGGER.debug("findByEvent: {}", event);
            Set<AlertTriggerEntity> set = alertTriggerRepository
                .findAll()
                .stream()
                .filter(alert ->
                    alert.isTemplate() &&
                    alert.getEventRules() != null &&
                    alert.getEventRules().stream().map(AlertEventRule::getEvent).collect(Collectors.toList()).contains(event)
                )
                .map(alertTriggerConverter::toAlertTriggerEntity)
                .sorted(Comparator.comparing(AlertTriggerEntity::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            LOGGER.debug("findByEvent : {} - DONE", set);
            return set;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find alert triggers by event", ex);
            throw new TechnicalManagementException("An error occurs while trying to find alert triggers by event", ex);
        }
    }

    @Override
    public void createDefaults(ExecutionContext executionContext, AlertReferenceType referenceType, String referenceId) {
        if (getStatus(executionContext).isEnabled()) {
            Set<AlertTriggerEntity> defaultAlerts = findByEvent(AlertEventType.API_CREATE);

            for (AlertTriggerEntity alert : defaultAlerts) {
                AlertTrigger trigger = alertTriggerConverter.toAlertTrigger(alert);
                AlertTriggerEntity triggerEntity = alertTriggerConverter.toAlertTriggerEntity(trigger);
                triggerEntity.setId(UUID.toString(UUID.random()));
                triggerEntity.setReferenceType(AlertReferenceType.API);
                triggerEntity.setReferenceId(referenceId);
                triggerEntity.setTemplate(false);
                triggerEntity.setEnabled(true);
                triggerEntity.setEventRules(null);
                triggerEntity.setParentId(alert.getId());
                triggerEntity.setCreatedAt(new Date());
                triggerEntity.setUpdatedAt(trigger.getCreatedAt());

                try {
                    create(executionContext, alertTriggerConverter.toAlertTrigger(triggerEntity));
                } catch (TechnicalException te) {
                    LOGGER.error("Unable to create default alert", te);
                }
            }
        }
    }

    @Override
    public void applyDefaults(final ExecutionContext executionContext, final String alertId, final AlertReferenceType referenceType) {
        try {
            final AlertTriggerEntity alert = alertTriggerRepository
                .findById(alertId)
                .map(alertTriggerConverter::toAlertTriggerEntity)
                .orElseThrow(() -> new AlertNotFoundException(alertId));

            if (!alert.isTemplate()) {
                throw new AlertTemplateInvalidException(alertId);
            }

            if (referenceType == API) {
                List<String> apiIds = apiRepository
                    .searchIds(
                        List.of(new ApiCriteria.Builder().environmentId(executionContext.getEnvironmentId()).build()),
                        convert(new PageableImpl(0, Integer.MAX_VALUE)),
                        null
                    )
                    .getContent();
                if (apiIds != null) {
                    apiIds
                        .stream()
                        .forEach(apiId -> {
                            try {
                                boolean create = alertTriggerRepository
                                    .findByReferenceAndReferenceId(API.name(), apiId)
                                    .stream()
                                    .noneMatch(alertTrigger -> alertId.equals(alertTrigger.getParentId()));

                                if (create) {
                                    AlertTrigger trigger = alertTriggerConverter.toAlertTrigger(alert);
                                    AlertTriggerEntity triggerEntity = alertTriggerConverter.toAlertTriggerEntity(trigger);
                                    triggerEntity.setId(UUID.toString(UUID.random()));
                                    triggerEntity.setReferenceType(API);
                                    triggerEntity.setReferenceId(apiId);
                                    triggerEntity.setTemplate(false);
                                    triggerEntity.setEnabled(true);
                                    triggerEntity.setEventRules(null);
                                    triggerEntity.setParentId(alertId);
                                    triggerEntity.setCreatedAt(new Date());
                                    triggerEntity.setUpdatedAt(trigger.getCreatedAt());

                                    create(executionContext, alertTriggerConverter.toAlertTrigger(triggerEntity));
                                }
                            } catch (TechnicalException te) {
                                LOGGER.error("Unable to create default alert for API {}", apiId, te);
                            }
                        });
                }
            }
        } catch (TechnicalException te) {
            final String msg = "An error occurs while trying to apply template alert " + alertId;
            LOGGER.error(msg, te);
            throw new TechnicalManagementException(msg, te);
        }
    }

    private void fillNotificationsAndFilters(
        final ExecutionContext executionContext,
        final AlertTriggerEntity trigger,
        final AlertReferenceType referenceType,
        final String referenceId
    ) {
        // Notifications
        List<Notification> notifications = trigger.getNotifications();
        if (notifications == null) {
            notifications = new ArrayList<>();
            trigger.setNotifications(notifications);
        }

        // Set the email notifier configuration in case
        notifications.forEach(notification -> {
            if (NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID.equalsIgnoreCase(notification.getType())) {
                setDefaultEmailNotifier(executionContext, notification);
            }
        });

        // Filters
        List<Filter> filters = trigger.getFilters();
        if (filters == null) {
            filters = new ArrayList<>();
            trigger.setFilters(filters);
        }

        // add filter matching reference type (api, application or environment)
        if (referenceType == APPLICATION || referenceType == API) {
            filters.add(StringCondition.equals(referenceType.name().toLowerCase(), referenceId).build());
        } else if (referenceType == AlertReferenceType.ENVIRONMENT) {
            EnvironmentEntity environment = environmentService.findById(referenceId);
            filters.add(stringMatchesCondition(ENVIRONMENT, environment.getId()));
            filters.add(stringMatchesCondition(ORGANIZATION, environment.getOrganizationId()));
        }
    }

    private void setDefaultEmailNotifier(final ExecutionContext executionContext, Notification notification) {
        EmailNotifierConfiguration configuration = new EmailNotifierConfiguration();

        if (host() == null) {
            configuration.setHost(parameterService.find(executionContext, Key.EMAIL_HOST, ParameterReferenceType.ORGANIZATION));
            final String emailPort = parameterService.find(executionContext, Key.EMAIL_PORT, ParameterReferenceType.ORGANIZATION);
            if (emailPort != null) {
                configuration.setPort(Integer.parseInt(emailPort));
            }
            configuration.setUsername(parameterService.find(executionContext, Key.EMAIL_USERNAME, ParameterReferenceType.ORGANIZATION));
            configuration.setPassword(parameterService.find(executionContext, Key.EMAIL_PASSWORD, ParameterReferenceType.ORGANIZATION));
            configuration.setStartTLSEnabled(
                parameterService.findAsBoolean(executionContext, Key.EMAIL_HOST, ParameterReferenceType.ORGANIZATION)
            );
        } else {
            configuration.setHost(host());
            configuration.setPort(port());
            configuration.setUsername(username());
            configuration.setPassword(password());
            configuration.setStartTLSEnabled(startTLSEnabled());
            configuration.setSslKeyStore(sslKeyStore());
            configuration.setSslKeyStorePassword(sslKeyStorePassword());
            configuration.setSslTrustAll(sslTrustAll());
        }

        configuration.setAuthMethods(authMethods());

        try {
            JsonNode emailNode = mapper.readTree(notification.getConfiguration());
            configuration.setFrom(emailNode.path("from").asText());
            configuration.setTo(emailNode.path("to").asText());
            configuration.setSubject(String.format(subject(), emailNode.path("subject").asText()));
            configuration.setBody(emailNode.path("body").asText());

            notification.setConfiguration(mapper.writeValueAsString(configuration));
            notification.setType("email-notifier");
        } catch (IOException e) {
            LOGGER.error("Unexpected error while converting system email configuration to email notifier");
        }
    }

    private void checkAlert(final ExecutionContext executionContext) {
        if (
            !parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION) ||
            triggerProviderManager.findAll().isEmpty()
        ) {
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
        triggerProvider.addListener(
            (TriggerProvider.OnConnectionListener) () -> {
                LOGGER.info("Connected to alerting system. Sync alert triggers...");
                // On reconnect, ensure to push all the triggers again
                findAll()
                    .stream()
                    .filter(alertTriggerEntity -> !alertTriggerEntity.isTemplate())
                    .forEach(alertTriggerEntity -> {
                        ExecutionContext executionContext = new ExecutionContext(
                            environmentService.findById(alertTriggerEntity.getEnvironmentId())
                        );

                        fillNotificationsAndFilters(
                            executionContext,
                            alertTriggerEntity,
                            alertTriggerEntity.getReferenceType(),
                            alertTriggerEntity.getReferenceId()
                        );
                        triggerOrCancelAlert(alertTriggerEntity);
                    });
                LOGGER.info("Alert triggers synchronized with the alerting system.");
            }
        );

        triggerProvider.addListener(
            (TriggerProvider.OnDisconnectionListener) () -> LOGGER.error("Connection with the alerting system has been loose.")
        );

        triggerProvider.addListener(
            (TriggerProvider.OnCommandListener) command -> {
                if (command instanceof AlertNotificationCommand) {
                    handleAlertNotificationCommand((AlertNotificationCommand) command);
                } else {
                    LOGGER.warn("Unknown alert command: {}", command);
                }
            }
        );

        triggerProvider.addListener(
            new TriggerProvider.OnCommandResultListener() {
                @Override
                public <T> void doOnCommand(Command command, Handler<T> resultHandler) {
                    Supplier<T> supplier = null;

                    if (command instanceof ResolvePropertyCommand) {
                        supplier = (Supplier<T>) new ResolvePropertyCommandHandler((ResolvePropertyCommand) command);
                    } else {
                        LOGGER.warn("Unknown alert command: {}", command);
                    }

                    if (supplier != null) {
                        resultHandler.handle(supplier.get());
                    } else {
                        resultHandler.handle(null);
                    }
                }
            }
        );
    }

    private void handleAlertNotificationCommand(AlertNotificationCommand command) {
        try {
            AlertEvent alertEvent = new AlertEvent();

            alertEvent.setId(UUID.toString(UUID.random()));
            alertEvent.setAlert(command.getTrigger());
            alertEvent.setCreatedAt(new Date(command.getTimestamp()));
            alertEvent.setUpdatedAt(alertEvent.getCreatedAt());
            alertEvent.setMessage(command.getMessage());

            alertEventRepository.create(alertEvent);
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to create an alert event from command {}" + command;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    private final class ResolvePropertyCommandHandler implements Supplier<Map<String, Map<String, Object>>> {

        private final ResolvePropertyCommand command;

        ResolvePropertyCommandHandler(ResolvePropertyCommand command) {
            this.command = command;
        }

        @Override
        public Map<String, Map<String, Object>> get() {
            Map<String, String> properties = command.getProperties();
            Map<String, Map<String, Object>> values = new HashMap<>();

            if (properties != null) {
                properties.forEach((key, value) -> {
                    switch (key) {
                        case FIELD_API:
                            values.put(key, getAPIMetadata(value));
                            break;
                        case FIELD_APPLICATION:
                            values.put(key, getApplicationMetadata(value));
                            break;
                        case FIELD_PLAN:
                            values.put(key, getPlanMetadata(value));
                            break;
                    }
                });
            }

            return values;
        }

        private Map<String, Object> getAPIMetadata(String api) {
            Map<String, Object> metadata = new HashMap<>();

            try {
                if (api.equals(UNKNOWN_SERVICE)) {
                    metadata.put(METADATA_NAME, METADATA_UNKNOWN_API_NAME);
                    metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
                } else {
                    return apiService.findByIdAsMap(api);
                }
            } catch (ApiNotFoundException anfe) {
                metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                metadata.put(METADATA_NAME, METADATA_DELETED_API_NAME);
            } catch (TechnicalException e) {
                LOGGER.error("Failed to retrieve API {} metadata", api, e);
            }

            return metadata;
        }

        private Map<String, Object> getApplicationMetadata(String application) {
            Map<String, Object> metadata = new HashMap<>();

            try {
                if (application.equals(UNKNOWN_SERVICE)) {
                    metadata.put(METADATA_NAME, METADATA_UNKNOWN_APPLICATION_NAME);
                    metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
                } else {
                    return applicationService.findByIdAsMap(application);
                }
            } catch (ApplicationNotFoundException anfe) {
                metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                metadata.put(METADATA_NAME, METADATA_DELETED_APPLICATION_NAME);
            } catch (TechnicalException e) {
                LOGGER.error("Failed to retrieve application {} metadata", application, e);
            }

            return metadata;
        }

        private Map<String, Object> getPlanMetadata(String plan) {
            Map<String, Object> metadata = new HashMap<>();

            try {
                return planService.findByIdAsMap(plan);
            } catch (PlanNotFoundException anfe) {
                metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                metadata.put(METADATA_NAME, METADATA_DELETED_PLAN_NAME);
            } catch (TechnicalException e) {
                LOGGER.error("Failed to retrieve plan {} metadata", plan, e);
            }

            return metadata;
        }
    }

    private int countEvents(final String triggerId, final long from, final long to) {
        AlertEventCriteria criteria = new AlertEventCriteria.Builder().alert(triggerId).from(from).to(to).build();
        return Math.toIntExact(alertEventRepository.count(criteria));
    }

    private Optional<AlertEvent> getLastEvent(final String triggerId) {
        Page<AlertEvent> alertEventsRepo = alertEventRepository.search(
            new AlertEventCriteria.Builder().alert(triggerId).from(0).to(0).build(),
            new PageableBuilder().pageNumber(0).pageSize(1).build()
        );

        return (alertEventsRepo.getPageElements() == 0) ? Optional.empty() : Optional.of(alertEventsRepo.getContent().get(0));
    }

    private StringCondition stringMatchesCondition(ReferenceContext.Type key, String value) {
        return StringCondition.matches(key.name().toLowerCase(), "(?:.*,|^)" + value + "(?:,.*|$)|\\*").build();
    }

    private String subject() {
        return this.configuration.getProperty("notifiers.email.subject", "[Gravitee.io] %s");
    }

    private String host() {
        return this.configuration.getProperty("notifiers.email.host");
    }

    private Integer port() {
        return this.configuration.getProperty("notifiers.email.port", Integer.class);
    }

    private String username() {
        return this.configuration.getProperty("notifiers.email.username");
    }

    private String password() {
        return this.configuration.getProperty("notifiers.email.password");
    }

    private Set<String> authMethods() {
        return Optional.ofNullable(this.configuration.getProperty("notifiers.email.authMethods", String[].class)).map(Set::of).orElse(null);
    }

    private boolean startTLSEnabled() {
        return this.configuration.getProperty("notifiers.email.starttls.enabled", Boolean.class, false);
    }

    private boolean sslTrustAll() {
        return this.configuration.getProperty("notifiers.email.ssl.trustAll", Boolean.class, false);
    }

    private String sslKeyStore() {
        return this.configuration.getProperty("notifiers.email.ssl.keyStore");
    }

    private String sslKeyStorePassword() {
        return this.configuration.getProperty("notifiers.email.ssl.keyStorePassword");
    }
}
