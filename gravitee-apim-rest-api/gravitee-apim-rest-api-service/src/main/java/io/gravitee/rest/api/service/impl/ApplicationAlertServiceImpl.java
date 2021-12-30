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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.alert.api.condition.Filter;
import io.gravitee.alert.api.condition.StringCondition;
import io.gravitee.alert.api.trigger.Dampening;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.common.event.Event;
import io.gravitee.notifier.api.Notification;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.AlertStatusEntity;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.model.alert.ApplicationAlertMembershipEvent;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.notification.NotificationTemplateEntity;
import io.gravitee.rest.api.model.notification.NotificationTemplateEvent;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApplicationAlertService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.AlertHook;
import io.gravitee.rest.api.service.notification.HookScope;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class ApplicationAlertServiceImpl implements ApplicationAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationAlertServiceImpl.class);
    public static final String DEFAULT_EMAIL_NOTIFIER = "default-email";
    public static final String STATUS_ALERT = "METRICS_RATE";
    public static final String RESPONSE_TIME_ALERT = "METRICS_AGGREGATION";

    private final ApplicationService applicationService;
    private final AlertService alertService;
    private final AlertTriggerRepository alertTriggerRepository;
    private final MembershipService membershipService;
    private final UserService userService;
    private final ObjectMapper mapper;
    private final NotificationTemplateService notificationTemplateService;
    private final String emailFrom;

    public ApplicationAlertServiceImpl(
        ApplicationService applicationService,
        AlertService alertService,
        AlertTriggerRepository alertTriggerRepository,
        MembershipService membershipService,
        UserService userService,
        ObjectMapper mapper,
        NotificationTemplateService notificationTemplateService,
        @Value("${email.from}") String emailFrom
    ) {
        this.applicationService = applicationService;
        this.alertService = alertService;
        this.alertTriggerRepository = alertTriggerRepository;
        this.membershipService = membershipService;
        this.userService = userService;
        this.mapper = mapper;
        this.notificationTemplateService = notificationTemplateService;
        this.emailFrom = emailFrom;
    }

    @Override
    public AlertTriggerEntity create(final String environmentId, String applicationId, NewAlertTriggerEntity alert) {
        final ApplicationEntity application = applicationService.findById(environmentId, applicationId);

        alert.setName(generateAlertName(application, alert));
        alert.setReferenceType(AlertReferenceType.APPLICATION);
        alert.setReferenceId(application.getId());
        alert.setSource("REQUEST");
        alert.setSeverity(Trigger.Severity.INFO);
        alert.setDampening(Dampening.strictCount(1));
        Filter filter = StringCondition.equals("application", applicationId).build();
        alert.setFilters(singletonList(filter));

        final List<String> recipients = getNotificationRecipients(application.getId(), application.getGroups());

        if (!CollectionUtils.isEmpty(recipients)) {
            alert.setNotifications(createNotification(alert.getType(), recipients));
        }

        return alertService.create(alert);
    }

    private List<Notification> createNotification(String alertType) {
        return createNotification(alertType, new ArrayList<>());
    }

    private List<Notification> createNotification(String alertType, List<String> recipients) {
        try {
            Notification notification = new Notification();
            notification.setType(DEFAULT_EMAIL_NOTIFIER);
            ObjectNode configuration = mapper.createObjectNode();
            configuration.put("from", emailFrom);
            configuration.put("to", String.join(",", recipients));
            generateNotificationBodyFromTemplate(configuration, alertType);
            notification.setPeriods(emptyList());

            notification.setConfiguration(mapper.writeValueAsString(configuration));
            return singletonList(notification);
        } catch (JsonProcessingException e) {
            LOGGER.error("An error occurs while trying to create the Alert notification", e);
            throw new TechnicalManagementException("An error occurs while trying to create the Alert notification");
        }
    }

    @Override
    public List<AlertTriggerEntity> findByApplication(String applicationId) {
        return alertService.findByReference(AlertReferenceType.APPLICATION, applicationId);
    }

    @Override
    public AlertTriggerEntity update(String applicationId, UpdateAlertTriggerEntity alert) {
        final AlertTriggerEntity alertTrigger = alertService.findById(alert.getId());

        alert.setName(alertTrigger.getName());
        alert.setReferenceType(AlertReferenceType.APPLICATION);
        alert.setReferenceId(alertTrigger.getReferenceId());
        alert.setSource("REQUEST");
        alert.setSeverity(Trigger.Severity.INFO);
        alert.setDampening(Dampening.strictCount(1));
        alert.setNotifications(alert.getNotifications());
        alert.setFilters(alertTrigger.getFilters());
        return alertService.update(alert);
    }

    @Override
    public void delete(String alertId, String applicationId) {
        alertService.delete(alertId, applicationId);
    }

    @Override
    public AlertStatusEntity getStatus() {
        return alertService.getStatus();
    }

    @Override
    public void addMemberToApplication(final String environmentId, String applicationId, String email) {
        if (StringUtils.isEmpty(email)) {
            return;
        }

        // check existence of application
        applicationService.findById(environmentId, applicationId);

        alertService
            .findByReference(AlertReferenceType.APPLICATION, applicationId)
            .forEach(
                trigger -> {
                    if (trigger.getNotifications() == null) {
                        trigger.setNotifications(createNotification(trigger.getType()));
                    }
                    final Optional<Notification> notificationOpt = trigger
                        .getNotifications()
                        .stream()
                        .filter(n -> DEFAULT_EMAIL_NOTIFIER.equals(n.getType()))
                        .findFirst();

                    if (notificationOpt.isPresent()) {
                        Notification notification = notificationOpt.get();
                        try {
                            ObjectNode configuration = mapper.createObjectNode();

                            JsonNode emailNode = mapper.readTree(notification.getConfiguration());
                            configuration.put("to", emailNode.path("to").asText() + "," + email);
                            configuration.put("from", emailNode.path("from").asText());
                            configuration.put("subject", emailNode.path("subject").asText());
                            configuration.put("body", emailNode.path("body").asText());
                            notification.setConfiguration(mapper.writeValueAsString(configuration));
                        } catch (JsonProcessingException e) {
                            LOGGER.error("An error occurs while trying to add a recipient to the Alert notification", e);
                            throw new TechnicalManagementException(
                                "An error occurs while trying to add a recipient to the Alert notification"
                            );
                        }
                    } else {
                        trigger.setNotifications(createNotification(trigger.getType(), singletonList(email)));
                    }
                    alertService.update(convert(trigger));
                }
            );
    }

    @Override
    public void deleteMemberFromApplication(final String environmentId, String applicationId, String email) {
        if (StringUtils.isEmpty(email)) {
            return;
        }

        // check existence of application
        applicationService.findById(environmentId, applicationId);

        alertService
            .findByReference(AlertReferenceType.APPLICATION, applicationId)
            .forEach(
                trigger -> {
                    if (trigger.getNotifications() == null) {
                        trigger.setNotifications(createNotification(trigger.getType()));
                    }

                    final Optional<Notification> notificationOpt = trigger
                        .getNotifications()
                        .stream()
                        .filter(n -> DEFAULT_EMAIL_NOTIFIER.equals(n.getType()))
                        .findFirst();

                    if (notificationOpt.isPresent()) {
                        final Notification notification = notificationOpt.get();
                        try {
                            ObjectNode configuration = mapper.createObjectNode();

                            JsonNode emailNode = mapper.readTree(notification.getConfiguration());

                            final String to = Arrays
                                .stream(emailNode.path("to").asText().split(",|;|\\s"))
                                .filter(mailTo -> !mailTo.equals(email))
                                .collect(Collectors.joining(","));

                            if (StringUtils.isEmpty(to)) {
                                trigger.setNotifications(emptyList());
                            } else {
                                configuration.put("to", to);
                                configuration.put("from", emailNode.path("from").asText());
                                configuration.put("subject", emailNode.path("subject").asText());
                                configuration.put("body", emailNode.path("body").asText());
                                notification.setConfiguration(mapper.writeValueAsString(configuration));
                            }
                            alertService.update(convert(trigger));
                        } catch (JsonProcessingException e) {
                            LOGGER.error("An error occurs while trying to add a recipient to the Alert notification", e);
                            throw new TechnicalManagementException(
                                "An error occurs while trying to add a recipient to the Alert notification"
                            );
                        }
                    }
                }
            );
    }

    @Override
    public void deleteAll(String applicationId) {
        alertService
            .findByReference(AlertReferenceType.APPLICATION, applicationId)
            .forEach(trigger -> alertService.delete(trigger.getId(), applicationId));
    }

    @Override
    @Async
    public void handleEvent(Event<ApplicationAlertEventType, Object> event) {
        switch (event.type()) {
            case NOTIFICATION_TEMPLATE_UPDATE:
                final NotificationTemplateEvent notificationEvent = (NotificationTemplateEvent) event.content();
                final NotificationTemplateEntity notificationTemplate = notificationEvent.getNotificationTemplateEntity();
                final String organizationId = notificationEvent.getOrganizationId();
                if (notificationTemplate.getHook().equals(AlertHook.CONSUMER_HTTP_STATUS.name())) {
                    updateAllAlertsBody(organizationId, STATUS_ALERT, notificationTemplate.getContent(), notificationTemplate.getTitle());
                } else if (notificationTemplate.getHook().equals(AlertHook.CONSUMER_RESPONSE_TIME.name())) {
                    updateAllAlertsBody(
                        organizationId,
                        RESPONSE_TIME_ALERT,
                        notificationTemplate.getContent(),
                        notificationTemplate.getTitle()
                    );
                }
                break;
            case APPLICATION_MEMBERSHIP_UPDATE:
                updateAlertsRecipients(
                    (ApplicationAlertMembershipEvent) event.content(),
                    // FIXME: remove this calls to getCurrentXXXX
                    GraviteeContext.getCurrentOrganization(),
                    GraviteeContext.getCurrentEnvironment()
                );
                break;
        }
    }

    private void updateAlertsRecipients(
        ApplicationAlertMembershipEvent alertMembershipEvent,
        final String organizationId,
        final String environmentId
    ) {
        // get all applications ids to update
        final Set<String> applicationIds = new HashSet<>(alertMembershipEvent.getApplicationIds());
        if (!CollectionUtils.isEmpty(alertMembershipEvent.getGroupIds())) {
            applicationService
                .findByGroups(organizationId, new ArrayList<>(alertMembershipEvent.getGroupIds()))
                .stream()
                .map(ApplicationListItem::getId)
                .forEach(applicationIds::add);
        }

        // get recipients for each application
        final Map<String, List<String>> recipientsByApplicationId = applicationService
            .findByIds(organizationId, environmentId, new ArrayList<>(applicationIds))
            .stream()
            .collect(Collectors.toMap(ApplicationListItem::getId, app -> getNotificationRecipients(app.getId(), app.getGroups())));

        // apply new recipients to each AlertTrigger related to applications to update
        alertService
            .findByReferenceAndReferenceIds(AlertReferenceType.APPLICATION, new ArrayList<>(applicationIds))
            .forEach(
                trigger -> {
                    if (trigger.getNotifications() == null) {
                        trigger.setNotifications(createNotification(trigger.getType()));
                    }

                    updateTriggerNotification(trigger, recipientsByApplicationId.get(trigger.getReferenceId()));
                }
            );
    }

    private void updateAllAlertsBody(String organizationId, String type, String body, String subject) {
        final List<String> ids = applicationService
            .findByOrganization(organizationId)
            .stream()
            .map(ApplicationListItem::getId)
            .collect(Collectors.toList());
        alertService
            .findByReferenceAndReferenceIds(AlertReferenceType.APPLICATION, ids)
            .stream()
            .filter(alert -> alert.getType().equals(type))
            .forEach(
                trigger -> {
                    if (trigger.getNotifications() == null) {
                        trigger.setNotifications(createNotification(trigger.getType()));
                    }

                    updateTriggerNotification(trigger, body, subject);
                }
            );
    }

    private String generateAlertName(ApplicationEntity application, NewAlertTriggerEntity alert) {
        return String.format("%s - %s", alert.getType(), application.getId());
    }

    private List<String> getNotificationRecipients(String applicationId, Set<String> groupIds) {
        final List<String> members = membershipService
            .getMembershipsByReference(MembershipReferenceType.APPLICATION, applicationId)
            .stream()
            .map(MembershipEntity::getMemberId)
            .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(groupIds)) {
            groupIds.forEach(
                group -> {
                    members.addAll(
                        membershipService
                            .getMembershipsByReference(MembershipReferenceType.GROUP, group)
                            .stream()
                            .map(MembershipEntity::getMemberId)
                            .collect(Collectors.toList())
                    );
                }
            );
        }

        return userService
            .findByIds(members)
            .stream()
            .map(UserEntity::getEmail)
            .filter(mail -> !StringUtils.isEmpty(mail))
            .collect(Collectors.toList());
    }

    private void generateNotificationBodyFromTemplate(ObjectNode configuration, String alertType) {
        final Consumer<NotificationTemplateEntity> templateConsumer = template -> {
            configuration.put("subject", template.getTitle());
            configuration.put("body", template.getContent());
        };

        if (STATUS_ALERT.equals(alertType)) {
            notificationTemplateService
                .findByHookAndScope(AlertHook.CONSUMER_HTTP_STATUS.name(), HookScope.TEMPLATES_FOR_ALERT.name())
                .stream()
                .findFirst()
                .ifPresent(templateConsumer);
        } else if (RESPONSE_TIME_ALERT.equals(alertType)) {
            notificationTemplateService
                .findByHookAndScope(AlertHook.CONSUMER_RESPONSE_TIME.name(), HookScope.TEMPLATES_FOR_ALERT.name())
                .stream()
                .findFirst()
                .ifPresent(templateConsumer);
        }
    }

    private void updateTriggerNotification(AlertTriggerEntity trigger, List<String> recipients) {
        updateTriggerNotification(trigger, null, null, recipients);
    }

    private void updateTriggerNotification(AlertTriggerEntity trigger, String body, String subject) {
        updateTriggerNotification(trigger, body, subject, null);
    }

    private void updateTriggerNotification(AlertTriggerEntity trigger, String body, String subject, List<String> recipients) {
        if (CollectionUtils.isEmpty(trigger.getNotifications())) {
            return;
        }
        trigger
            .getNotifications()
            .stream()
            .filter(n -> DEFAULT_EMAIL_NOTIFIER.equals(n.getType()))
            .findFirst()
            .ifPresent(
                notification -> {
                    try {
                        ObjectNode configuration = mapper.createObjectNode();

                        JsonNode emailNode = mapper.readTree(notification.getConfiguration());

                        configuration.put("to", recipients == null ? emailNode.path("to").asText() : String.join(",", recipients));
                        configuration.put("from", emailNode.path("from").asText());
                        configuration.put("subject", subject == null ? emailNode.path("subject").asText() : subject);
                        configuration.put("body", body == null ? emailNode.path("body").asText() : body);
                        notification.setConfiguration(mapper.writeValueAsString(configuration));

                        alertService.update(convert(trigger));
                    } catch (JsonProcessingException e) {
                        LOGGER.error("An error occurs while trying to update Alert notification", e);
                        throw new TechnicalManagementException("An error occurs while trying to update Alert notification");
                    }
                }
            );
    }

    private UpdateAlertTriggerEntity convert(AlertTriggerEntity trigger) {
        final UpdateAlertTriggerEntity updating = new UpdateAlertTriggerEntity();
        updating.setId(trigger.getId());
        updating.setName(trigger.getName());
        updating.setEnabled(trigger.isEnabled());
        updating.setDescription(trigger.getDescription());
        updating.setReferenceType(trigger.getReferenceType());
        updating.setReferenceId(trigger.getReferenceId());
        updating.setSeverity(trigger.getSeverity());
        updating.setEventRules(trigger.getEventRules());
        updating.setConditions(trigger.getConditions());
        updating.setDampening(trigger.getDampening());
        updating.setFilters(trigger.getFilters());
        updating.setMetadata(trigger.getMetadata());
        updating.setNotifications(trigger.getNotifications());
        updating.setNotificationPeriods(trigger.getNotificationPeriods());
        updating.setSource(trigger.getSource());

        return updating;
    }
}
