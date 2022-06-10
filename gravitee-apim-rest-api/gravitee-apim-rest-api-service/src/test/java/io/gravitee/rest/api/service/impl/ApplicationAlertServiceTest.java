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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.alert.api.condition.Condition;
import io.gravitee.alert.api.condition.RateCondition;
import io.gravitee.alert.api.condition.StringCondition;
import io.gravitee.alert.api.condition.ThresholdRangeCondition;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.notifier.api.Notification;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.notification.NotificationTemplateEntity;
import io.gravitee.rest.api.model.notification.NotificationTemplateEvent;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.AlertHook;
import io.gravitee.rest.api.service.notification.HookScope;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationAlertServiceTest {

    private ApplicationAlertService cut;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private AlertService alertService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Spy
    private ObjectMapper mapper = new GraviteeMapper();

    private static final String EMAIL_FROM = "mail@from.io";
    private static final String ALERT_ID = "alert-id";
    private static final String APPLICATION_ID = "app-id";
    private static final String APPLICATION_NAME = "app-name";
    private static final String API_ID = "api-id";

    @Before
    public void setUp() throws Exception {
        cut =
            new ApplicationAlertServiceImpl(
                applicationService,
                alertService,
                membershipService,
                userService,
                mapper,
                notificationTemplateService,
                EMAIL_FROM
            );
    }

    @Test
    public void shouldCreateStatusAlert() {
        NewAlertTriggerEntity newAlert = new NewAlertTriggerEntity();
        newAlert.setId(ALERT_ID);
        newAlert.setType("METRICS_RATE");

        ApplicationEntity application = getApplication();
        prepareForCreation(newAlert);

        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        cut.create(GraviteeContext.getExecutionContext(), APPLICATION_ID, newAlert);

        verify(alertService, times(1)).create(GraviteeContext.getExecutionContext(), newAlert);
        verify(notificationTemplateService, times(1))
            .findByHookAndScope(
                GraviteeContext.getCurrentOrganization(),
                AlertHook.CONSUMER_HTTP_STATUS.name(),
                HookScope.TEMPLATES_FOR_ALERT.name()
            );
    }

    @Test
    public void shouldCreateResponseTimeAlert() {
        NewAlertTriggerEntity newAlert = new NewAlertTriggerEntity();
        newAlert.setId(ALERT_ID);
        newAlert.setType("METRICS_AGGREGATION");

        ApplicationEntity application = getApplication();
        prepareForCreation(newAlert);

        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        cut.create(GraviteeContext.getExecutionContext(), APPLICATION_ID, newAlert);

        verify(alertService, times(1)).create(GraviteeContext.getExecutionContext(), newAlert);
        verify(notificationTemplateService, times(1))
            .findByHookAndScope(
                GraviteeContext.getCurrentOrganization(),
                AlertHook.CONSUMER_RESPONSE_TIME.name(),
                HookScope.TEMPLATES_FOR_ALERT.name()
            );
    }

    @Test
    public void shouldCreateAlertWithApiFilter() {
        NewAlertTriggerEntity newAlert = new NewAlertTriggerEntity();
        newAlert.setId(ALERT_ID);
        newAlert.setType("METRICS_RATE");

        final StringCondition stringFilter = mock(StringCondition.class);
        newAlert.setFilters(singletonList(stringFilter));
        when(stringFilter.getProperty()).thenReturn("api");
        when(stringFilter.getPattern()).thenReturn(API_ID);

        ApplicationEntity application = getApplication();
        prepareForCreation(newAlert);

        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        cut.create(GraviteeContext.getExecutionContext(), APPLICATION_ID, newAlert);

        ArgumentCaptor<NewAlertTriggerEntity> alertCaptor = ArgumentCaptor.forClass(NewAlertTriggerEntity.class);

        verify(alertService, times(1)).create(eq(GraviteeContext.getExecutionContext()), alertCaptor.capture());
        assertThat(((StringCondition) alertCaptor.getValue().getFilters().get(0)).getProperty()).isEqualTo("application");
        assertThat(((StringCondition) alertCaptor.getValue().getFilters().get(0)).getPattern()).isEqualTo(APPLICATION_ID);

        assertThat(((StringCondition) alertCaptor.getValue().getFilters().get(1)).getProperty()).isEqualTo("api");
        assertThat(((StringCondition) alertCaptor.getValue().getFilters().get(1)).getPattern()).isEqualTo(API_ID);
    }

    @Test
    public void shouldCreateAlertWithNotificationsAndAddDefaultEmailNotification() {
        NewAlertTriggerEntity newAlert = new NewAlertTriggerEntity();
        newAlert.setId(ALERT_ID);
        newAlert.setType("METRICS_RATE");

        Notification notification1 = new Notification();
        notification1.setType("notification1-type");
        Notification notification2 = new Notification();
        notification2.setType("notification2-type");
        newAlert.setNotifications(List.of(notification1, notification2));

        ApplicationEntity application = getApplication();
        prepareForCreation(newAlert);

        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        cut.create(GraviteeContext.getExecutionContext(), APPLICATION_ID, newAlert);

        ArgumentCaptor<NewAlertTriggerEntity> alertCaptor = ArgumentCaptor.forClass(NewAlertTriggerEntity.class);

        verify(alertService, times(1)).create(eq(GraviteeContext.getExecutionContext()), alertCaptor.capture());

        assertThat(alertCaptor.getValue().getNotifications().size()).isEqualTo(3);

        assertThat(alertCaptor.getValue().getNotifications().get(0).getType()).isEqualTo("notification1-type");
        assertThat(alertCaptor.getValue().getNotifications().get(1).getType()).isEqualTo("notification2-type");
        assertThat(alertCaptor.getValue().getNotifications().get(2).getType()).isEqualTo("default-email");
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateMappingError() throws Exception {
        NewAlertTriggerEntity newAlert = new NewAlertTriggerEntity();
        newAlert.setId(ALERT_ID);
        newAlert.setType("METRICS_RATE");

        ApplicationEntity application = getApplication();
        prepareForCreation(newAlert);

        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID))).thenReturn(application);
        when(mapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

        cut.create(GraviteeContext.getExecutionContext(), APPLICATION_ID, newAlert);
    }

    @Test
    public void shouldUpdate() throws Exception {
        final AlertTriggerEntity alertTrigger = mock(AlertTriggerEntity.class);
        when(alertService.findById(ALERT_ID)).thenReturn(alertTrigger);

        UpdateAlertTriggerEntity updating = new UpdateAlertTriggerEntity();
        updating.setId(ALERT_ID);

        cut.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updating);

        verify(alertService, times(1)).update(GraviteeContext.getExecutionContext(), updating);
    }

    @Test
    public void shouldUpdateAlertWithApiFilter() {
        final AlertTriggerEntity alertTrigger = mock(AlertTriggerEntity.class);
        alertTrigger.setFilters(singletonList(StringCondition.equals("application", APPLICATION_ID).build()));
        when(alertService.findById(ALERT_ID)).thenReturn(alertTrigger);

        UpdateAlertTriggerEntity updating = new UpdateAlertTriggerEntity();
        updating.setId(ALERT_ID);
        updating.setFilters(singletonList(StringCondition.equals("api", API_ID).build()));

        cut.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updating);

        ArgumentCaptor<UpdateAlertTriggerEntity> alertCaptor = ArgumentCaptor.forClass(UpdateAlertTriggerEntity.class);

        verify(alertService, times(1)).update(eq(GraviteeContext.getExecutionContext()), alertCaptor.capture());
        assertThat(((StringCondition) alertCaptor.getValue().getFilters().get(0)).getProperty()).isEqualTo("application");
        assertThat(((StringCondition) alertCaptor.getValue().getFilters().get(0)).getPattern()).isEqualTo(APPLICATION_ID);

        assertThat(((StringCondition) alertCaptor.getValue().getFilters().get(1)).getProperty()).isEqualTo("api");
        assertThat(((StringCondition) alertCaptor.getValue().getFilters().get(1)).getPattern()).isEqualTo(API_ID);
    }

    @Test
    public void shouldUpdateAlertAndReplaceOldWebhooksNotifications() {
        final AlertTriggerEntity alertTrigger = mock(AlertTriggerEntity.class);
        Notification existingNotification1 = new Notification();
        existingNotification1.setType("existing-notification-type");
        Notification existingNotification2 = new Notification();
        existingNotification2.setType("webhook-notifier");
        when(alertTrigger.getNotifications()).thenReturn(new ArrayList<>(List.of(existingNotification1, existingNotification2)));

        when(alertService.findById(ALERT_ID)).thenReturn(alertTrigger);

        UpdateAlertTriggerEntity updating = new UpdateAlertTriggerEntity();
        updating.setId(ALERT_ID);
        Notification notification1 = new Notification();
        notification1.setType("notification1-type");
        Notification notification2 = new Notification();
        notification2.setType("notification2-type");
        updating.setNotifications(List.of(notification1, notification2));

        cut.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updating);

        ArgumentCaptor<UpdateAlertTriggerEntity> alertCaptor = ArgumentCaptor.forClass(UpdateAlertTriggerEntity.class);

        verify(alertService, times(1)).update(eq(GraviteeContext.getExecutionContext()), alertCaptor.capture());

        assertThat(alertCaptor.getValue().getNotifications().size()).isEqualTo(3);

        assertThat(alertCaptor.getValue().getNotifications().get(0).getType()).isEqualTo("notification1-type");
        assertThat(alertCaptor.getValue().getNotifications().get(1).getType()).isEqualTo("notification2-type");
        assertThat(alertCaptor.getValue().getNotifications().get(2).getType()).isEqualTo("existing-notification-type");
    }

    @Test
    public void shouldAddMember() throws Exception {
        AlertTriggerEntity trigger1 = mock(AlertTriggerEntity.class);
        Notification notification = new Notification();
        notification.setType("default-email");
        notification.setConfiguration("");
        Notification notification2 = new Notification();
        notification2.setType("default-email");
        notification2.setConfiguration("");
        when(trigger1.getNotifications()).thenReturn(Collections.singletonList(notification));
        AlertTriggerEntity trigger2 = mock(AlertTriggerEntity.class);
        when(trigger2.getNotifications()).thenReturn(Collections.singletonList(notification2));

        List<AlertTriggerEntity> triggers = new ArrayList<>();
        triggers.add(trigger1);
        triggers.add(trigger2);

        when(alertService.findByReference(AlertReferenceType.APPLICATION, APPLICATION_ID)).thenReturn(triggers);

        JsonNode emailNode = JsonNodeFactory.instance
            .objectNode()
            .put("to", "to")
            .put("from", "from")
            .put("subject", "subject")
            .put("body", "body");
        when(mapper.readTree(notification.getConfiguration())).thenReturn(emailNode);
        when(mapper.readTree(notification2.getConfiguration())).thenReturn(emailNode);

        cut.addMemberToApplication(GraviteeContext.getExecutionContext(), APPLICATION_ID, "add@mail.gio");

        ArgumentCaptor<UpdateAlertTriggerEntity> updatingCaptor = ArgumentCaptor.forClass(UpdateAlertTriggerEntity.class);

        verify(alertService, times(2)).update(eq(GraviteeContext.getExecutionContext()), updatingCaptor.capture());

        final UpdateAlertTriggerEntity updating = updatingCaptor.getValue();
        assertThat(updating.getNotifications().get(0).getConfiguration()).contains("to,add@mail.gio");
    }

    @Test
    public void shouldAddMemberWhenNoOne() throws Exception {
        AlertTriggerEntity trigger1 = mock(AlertTriggerEntity.class);
        when(trigger1.getNotifications()).thenReturn(Collections.emptyList());

        List<AlertTriggerEntity> triggers = new ArrayList<>();
        triggers.add(trigger1);

        when(alertService.findByReference(AlertReferenceType.APPLICATION, APPLICATION_ID)).thenReturn(triggers);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(getApplication());

        cut.addMemberToApplication(GraviteeContext.getExecutionContext(), APPLICATION_ID, "add@mail.gio");

        ArgumentCaptor<List<Notification>> notificationsCaptor = ArgumentCaptor.forClass(List.class);

        verify(trigger1, times(1)).setNotifications(notificationsCaptor.capture());

        final List<Notification> notifications = notificationsCaptor.getValue();
        assertThat(notifications.get(0).getConfiguration()).contains("\"add@mail.gio\"");

        verify(alertService, times(1)).update(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldNotAddMemberEmptyMail() throws Exception {
        cut.addMemberToApplication(GraviteeContext.getExecutionContext(), APPLICATION_ID, "");
        verify(alertService, never()).update(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotAddMemberMappingError() throws Exception {
        AlertTriggerEntity trigger1 = mock(AlertTriggerEntity.class);
        Notification notification = new Notification();
        notification.setType("default-email");
        notification.setConfiguration("");
        when(trigger1.getNotifications()).thenReturn(Collections.singletonList(notification));

        List<AlertTriggerEntity> triggers = new ArrayList<>();
        triggers.add(trigger1);

        when(alertService.findByReference(AlertReferenceType.APPLICATION, APPLICATION_ID)).thenReturn(triggers);

        when(mapper.readTree(notification.getConfiguration())).thenThrow(JsonProcessingException.class);

        cut.addMemberToApplication(GraviteeContext.getExecutionContext(), APPLICATION_ID, "add@mail.gio");
    }

    @Test
    public void shouldDeleteMember() throws Exception {
        AlertTriggerEntity trigger1 = mock(AlertTriggerEntity.class);
        Notification notification = new Notification();
        notification.setType("default-email");
        notification.setConfiguration("");
        Notification notification2 = new Notification();
        notification2.setType("default-email");
        notification2.setConfiguration("");
        when(trigger1.getNotifications()).thenReturn(Collections.singletonList(notification));
        AlertTriggerEntity trigger2 = mock(AlertTriggerEntity.class);
        when(trigger2.getNotifications()).thenReturn(Collections.singletonList(notification2));

        List<AlertTriggerEntity> triggers = new ArrayList<>();
        triggers.add(trigger1);
        triggers.add(trigger2);

        when(alertService.findByReference(AlertReferenceType.APPLICATION, APPLICATION_ID)).thenReturn(triggers);

        JsonNode emailNode = JsonNodeFactory.instance
            .objectNode()
            .put("to", "to,delete@mail.gio")
            .put("from", "from")
            .put("subject", "subject")
            .put("body", "body");
        when(mapper.readTree(notification.getConfiguration())).thenReturn(emailNode);
        when(mapper.readTree(notification2.getConfiguration())).thenReturn(emailNode);

        cut.deleteMemberFromApplication(GraviteeContext.getExecutionContext(), APPLICATION_ID, "delete@mail.gio");

        ArgumentCaptor<UpdateAlertTriggerEntity> updatingCaptor = ArgumentCaptor.forClass(UpdateAlertTriggerEntity.class);

        verify(alertService, times(2)).update(eq(GraviteeContext.getExecutionContext()), updatingCaptor.capture());

        final UpdateAlertTriggerEntity updating = updatingCaptor.getValue();
        assertThat(updating.getNotifications().get(0).getConfiguration()).contains("to");
        assertThat(updating.getNotifications().get(0).getConfiguration()).doesNotContain("delete@mail.gio");
    }

    @Test
    public void shouldNotDeleteMemberEmptyMail() throws Exception {
        cut.deleteMemberFromApplication(GraviteeContext.getExecutionContext(), APPLICATION_ID, "");
        verify(alertService, never()).update(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeleteMemberMappingError() throws Exception {
        AlertTriggerEntity trigger1 = mock(AlertTriggerEntity.class);
        Notification notification = new Notification();
        notification.setType("default-email");
        notification.setConfiguration("");
        when(trigger1.getNotifications()).thenReturn(Collections.singletonList(notification));

        List<AlertTriggerEntity> triggers = new ArrayList<>();
        triggers.add(trigger1);

        when(alertService.findByReference(AlertReferenceType.APPLICATION, APPLICATION_ID)).thenReturn(triggers);

        when(mapper.readTree(notification.getConfiguration())).thenThrow(JsonProcessingException.class);

        cut.deleteMemberFromApplication(GraviteeContext.getExecutionContext(), APPLICATION_ID, "delete@mail.gio");
    }

    @Test
    public void shouldHandleNotificationTemplateUpdatedEventStatusAlert() throws Exception {
        NotificationTemplateEntity notificationTemplate = new NotificationTemplateEntity();
        notificationTemplate.setHook(AlertHook.CONSUMER_HTTP_STATUS.name());
        notificationTemplate.setTitle("notification-template-title");
        notificationTemplate.setContent("notification-template-content");
        NotificationTemplateEvent notificationTemplateEvent = new NotificationTemplateEvent("org-id", notificationTemplate);
        final SimpleEvent<ApplicationAlertEventType, Object> event = new SimpleEvent<>(
            ApplicationAlertEventType.NOTIFICATION_TEMPLATE_UPDATE,
            notificationTemplateEvent
        );

        ApplicationListItem app1 = new ApplicationListItem();
        app1.setId("app1");
        ApplicationListItem app2 = new ApplicationListItem();
        app2.setId("app2");
        when(applicationService.findByOrganization("org-id")).thenReturn(new HashSet<>(Arrays.asList(app1, app2)));

        final AlertTriggerEntity alertTrigger = mock(AlertTriggerEntity.class);
        when(alertTrigger.getType()).thenReturn("METRICS_RATE");
        when(alertService.findByReferences(AlertReferenceType.APPLICATION, Arrays.asList("app1", "app2")))
            .thenReturn(Collections.singletonList(alertTrigger));

        Notification notification = new Notification();
        notification.setType("default-email");
        notification.setConfiguration("");
        when(alertTrigger.getNotifications()).thenReturn(Collections.singletonList(notification));

        JsonNode emailNode = JsonNodeFactory.instance
            .objectNode()
            .put("to", "to")
            .put("from", "from")
            .put("subject", "subject")
            .put("body", "body");
        when(mapper.readTree(notification.getConfiguration())).thenReturn(emailNode);

        cut.handleEvent(event);

        ArgumentCaptor<UpdateAlertTriggerEntity> updatingCaptor = ArgumentCaptor.forClass(UpdateAlertTriggerEntity.class);

        verify(alertService, times(1)).update(any(), updatingCaptor.capture());

        final UpdateAlertTriggerEntity updating = updatingCaptor.getValue();
        assertThat(updating.getNotifications().get(0).getConfiguration()).contains("notification-template-content");
        assertThat(updating.getNotifications().get(0).getConfiguration()).contains("notification-template-title");
    }

    @Test
    public void shouldHandleNotificationTemplateUpdatedEventResponseTimeAlert() throws Exception {
        NotificationTemplateEntity notificationTemplate = new NotificationTemplateEntity();
        notificationTemplate.setHook(AlertHook.CONSUMER_RESPONSE_TIME.name());
        notificationTemplate.setTitle("notification-template-title");
        notificationTemplate.setContent("notification-template-content");
        NotificationTemplateEvent notificationTemplateEvent = new NotificationTemplateEvent("org-id", notificationTemplate);
        final SimpleEvent<ApplicationAlertEventType, Object> event = new SimpleEvent<>(
            ApplicationAlertEventType.NOTIFICATION_TEMPLATE_UPDATE,
            notificationTemplateEvent
        );

        ApplicationListItem app1 = new ApplicationListItem();
        app1.setId("app1");
        ApplicationListItem app2 = new ApplicationListItem();
        app2.setId("app2");
        when(applicationService.findByOrganization("org-id")).thenReturn(new HashSet<>(Arrays.asList(app1, app2)));

        final AlertTriggerEntity alertTrigger = mock(AlertTriggerEntity.class);
        when(alertTrigger.getType()).thenReturn("METRICS_AGGREGATION");
        when(alertService.findByReferences(AlertReferenceType.APPLICATION, Arrays.asList("app1", "app2")))
            .thenReturn(Collections.singletonList(alertTrigger));

        Notification notification = new Notification();
        notification.setType("default-email");
        notification.setConfiguration("");
        when(alertTrigger.getNotifications()).thenReturn(Collections.singletonList(notification));

        JsonNode emailNode = JsonNodeFactory.instance
            .objectNode()
            .put("to", "to")
            .put("from", "from")
            .put("subject", "subject")
            .put("body", "body");
        when(mapper.readTree(notification.getConfiguration())).thenReturn(emailNode);

        cut.handleEvent(event);

        ArgumentCaptor<UpdateAlertTriggerEntity> updatingCaptor = ArgumentCaptor.forClass(UpdateAlertTriggerEntity.class);

        verify(alertService, times(1)).update(any(), updatingCaptor.capture());

        final UpdateAlertTriggerEntity updating = updatingCaptor.getValue();
        assertThat(updating.getNotifications().get(0).getConfiguration()).contains("notification-template-content");
        assertThat(updating.getNotifications().get(0).getConfiguration()).contains("notification-template-title");
    }

    @Test
    public void shouldHandleNotificationTemplateUpdatedEventEmptyNotification() throws Exception {
        NotificationTemplateEntity notificationTemplate = new NotificationTemplateEntity();
        notificationTemplate.setHook(AlertHook.CONSUMER_RESPONSE_TIME.name());
        notificationTemplate.setTitle("notification-template-title");
        notificationTemplate.setContent("notification-template-content");
        NotificationTemplateEvent notificationTemplateEvent = new NotificationTemplateEvent("org-id", notificationTemplate);
        final SimpleEvent<ApplicationAlertEventType, Object> event = new SimpleEvent<>(
            ApplicationAlertEventType.NOTIFICATION_TEMPLATE_UPDATE,
            notificationTemplateEvent
        );

        ApplicationListItem app1 = new ApplicationListItem();
        app1.setId("app1");
        ApplicationListItem app2 = new ApplicationListItem();
        app2.setId("app2");
        when(applicationService.findByOrganization("org-id")).thenReturn(new HashSet<>(Arrays.asList(app1, app2)));

        final AlertTriggerEntity alertTrigger = mock(AlertTriggerEntity.class);
        when(alertTrigger.getType()).thenReturn("METRICS_AGGREGATION");
        when(alertService.findByReferences(AlertReferenceType.APPLICATION, Arrays.asList("app1", "app2")))
            .thenReturn(Collections.singletonList(alertTrigger));

        when(alertTrigger.getNotifications()).thenReturn(Collections.emptyList());

        cut.handleEvent(event);

        verify(alertService, times(0)).update(eq(GraviteeContext.getExecutionContext()), any());
    }

    private ApplicationEntity getApplication() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId(APPLICATION_ID);
        application.setName(APPLICATION_NAME);
        application.setGroups(new HashSet<>());
        return application;
    }

    @NotNull
    private void prepareForCreation(NewAlertTriggerEntity newAlert) {
        // recipients
        when(membershipService.getMembershipsByReference(MembershipReferenceType.APPLICATION, APPLICATION_ID))
            .thenReturn(Collections.emptySet());
        UserEntity user1 = new UserEntity();
        user1.setEmail("user1@mail.gio");
        UserEntity user2 = new UserEntity();
        user2.setEmail("user2@mail.gio");
        Set<UserEntity> users = new HashSet<>();
        users.add(user1);
        users.add(user2);
        when(userService.findByIds(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(users);

        // body
        List<Condition> conditions = new ArrayList<>();
        conditions.add(
            RateCondition
                .of(ThresholdRangeCondition.between("response.status", 200D, 299D).build())
                .duration(10L, TimeUnit.MINUTES)
                .greaterThan(5D)
                .build()
        );
        newAlert.setConditions(conditions);
    }
}
