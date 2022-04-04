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
package io.gravitee.rest.api.service.converter;

import static io.gravitee.repository.management.model.AlertEventType.API_CREATE;
import static io.gravitee.repository.management.model.AlertEventType.APPLICATION_CREATE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.repository.management.model.AlertEventRule;
import io.gravitee.repository.management.model.AlertEventType;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.AlertEventRuleEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AlertTriggerConverterTest {

    @InjectMocks
    private AlertTriggerConverter converter;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void should_convert_alertTriggerEntity_to_alertTrigger() throws Exception {
        AlertTriggerEntity alertTriggerEntity = mock(AlertTriggerEntity.class);
        when(alertTriggerEntity.getEnvironmentId()).thenReturn("env-id");
        when(alertTriggerEntity.getReferenceType()).thenReturn(AlertReferenceType.APPLICATION);
        when(alertTriggerEntity.getReferenceId()).thenReturn("application-id");
        when(alertTriggerEntity.getSeverity()).thenReturn(Trigger.Severity.INFO);
        when(alertTriggerEntity.getDescription()).thenReturn("description");
        when(alertTriggerEntity.isEnabled()).thenReturn(true);
        when(alertTriggerEntity.isTemplate()).thenReturn(false);
        when(alertTriggerEntity.getEventRules())
            .thenReturn(List.of(buildAlertEventRuleEntity("API_CREATE"), buildAlertEventRuleEntity("APPLICATION_CREATE")));

        when(objectMapper.writeValueAsString(alertTriggerEntity)).thenReturn("alertTriggerEntity in json format");

        AlertTrigger alertTrigger = converter.toAlertTrigger(alertTriggerEntity);

        assertEquals("env-id", alertTrigger.getEnvironmentId());
        assertEquals("APPLICATION", alertTrigger.getReferenceType());
        assertEquals("application-id", alertTrigger.getReferenceId());
        assertEquals("INFO", alertTrigger.getSeverity());
        assertEquals("description", alertTrigger.getDescription());
        assertEquals("alertTriggerEntity in json format", alertTrigger.getDefinition());
        assertEventRules(List.of(API_CREATE, APPLICATION_CREATE), alertTrigger.getEventRules());
        assertTrue(alertTrigger.isEnabled());
        assertFalse(alertTrigger.isTemplate());
    }

    @Test
    public void should_convert_newAlertTriggerEntity_to_alertTrigger() throws Exception {
        NewAlertTriggerEntity newAlertTriggerEntity = new NewAlertTriggerEntity();
        newAlertTriggerEntity.setReferenceType(AlertReferenceType.APPLICATION);
        newAlertTriggerEntity.setReferenceId("application-id");
        newAlertTriggerEntity.setSeverity(Trigger.Severity.INFO);
        newAlertTriggerEntity.setDescription("description");
        newAlertTriggerEntity.setEnabled(true);
        newAlertTriggerEntity.setTemplate(false);
        newAlertTriggerEntity.setEventRules(
            List.of(buildAlertEventRuleEntity("API_CREATE"), buildAlertEventRuleEntity("APPLICATION_CREATE"))
        );

        when(objectMapper.writeValueAsString(newAlertTriggerEntity)).thenReturn("NewAlertTriggerEntity in json format");

        AlertTrigger alertTrigger = converter.toAlertTrigger(GraviteeContext.getExecutionContext(), newAlertTriggerEntity);

        assertEquals("DEFAULT", alertTrigger.getEnvironmentId());
        assertEquals("APPLICATION", alertTrigger.getReferenceType());
        assertEquals("application-id", alertTrigger.getReferenceId());
        assertEquals("INFO", alertTrigger.getSeverity());
        assertEquals("description", alertTrigger.getDescription());
        assertEquals("NewAlertTriggerEntity in json format", alertTrigger.getDefinition());
        assertEventRules(List.of(API_CREATE, APPLICATION_CREATE), alertTrigger.getEventRules());
        assertTrue(alertTrigger.isEnabled());
        assertFalse(alertTrigger.isTemplate());
    }

    @Test
    public void should_convert_updateAlertTriggerEntity_to_alertTrigger() throws Exception {
        UpdateAlertTriggerEntity updateAlertTriggerEntity = new UpdateAlertTriggerEntity();
        updateAlertTriggerEntity.setSeverity(Trigger.Severity.INFO);
        updateAlertTriggerEntity.setDescription("description");
        updateAlertTriggerEntity.setEnabled(true);
        updateAlertTriggerEntity.setEventRules(
            List.of(buildAlertEventRuleEntity("API_CREATE"), buildAlertEventRuleEntity("APPLICATION_CREATE"))
        );

        when(objectMapper.writeValueAsString(updateAlertTriggerEntity)).thenReturn("UpdateAlertTriggerEntity in json format");

        AlertTrigger alertTrigger = converter.toAlertTrigger(GraviteeContext.getExecutionContext(), updateAlertTriggerEntity);

        assertEquals("DEFAULT", alertTrigger.getEnvironmentId());
        assertEquals("INFO", alertTrigger.getSeverity());
        assertEquals("description", alertTrigger.getDescription());
        assertEquals("UpdateAlertTriggerEntity in json format", alertTrigger.getDefinition());
        assertEventRules(List.of(API_CREATE, APPLICATION_CREATE), alertTrigger.getEventRules());
        assertTrue(alertTrigger.isEnabled());
        assertFalse(alertTrigger.isTemplate());
    }

    @Test
    public void should_convert_alertTrigger_to_alertTriggerEntity_should_convert() throws Exception {
        AlertTrigger alertTrigger = new AlertTrigger();
        alertTrigger.setEnvironmentId("env-id");
        alertTrigger.setId("id");
        alertTrigger.setReferenceType("APPLICATION");
        alertTrigger.setReferenceId("application-id");
        alertTrigger.setSeverity("INFO");
        alertTrigger.setDescription("description");
        alertTrigger.setEnabled(true);
        alertTrigger.setTemplate(false);
        alertTrigger.setDefinition(
            "{\"id\":\"id\",\"name\":null,\"description\":\"description\",\"type\":null,\"definition\":null,\"enabled\":true,\"referenceType\":\"APPLICATION\",\"referenceId\":\"application-id\",\"createdAt\":null,\"updatedAt\":null,\"severity\":\"INFO\",\"parentId\":null,\"eventRules\":null,\"template\":false,\"environmentId\":\"env-id\",\"source\":\"source\"}"
        );
        alertTrigger.setEventRules(List.of(buildAlertEventRule(API_CREATE), buildAlertEventRule(APPLICATION_CREATE)));

        when(objectMapper.readValue(anyString(), any(Trigger.class.getClass())))
            .thenAnswer(i -> new ObjectMapper().readValue((String) i.getArgument(0), Trigger.class));

        AlertTriggerEntity alertTriggerEntity = converter.toAlertTriggerEntity(alertTrigger);

        assertEquals("env-id", alertTriggerEntity.getEnvironmentId());
        assertEquals("id", alertTriggerEntity.getId());
        assertEquals(AlertReferenceType.APPLICATION, alertTriggerEntity.getReferenceType());
        assertEquals("application-id", alertTriggerEntity.getReferenceId());
        assertEquals(Trigger.Severity.INFO, alertTriggerEntity.getSeverity());
        assertEquals("description", alertTriggerEntity.getDescription());
        assertEventRulesEntities(List.of("API_CREATE", "APPLICATION_CREATE"), alertTriggerEntity.getEventRules());
        assertTrue(alertTriggerEntity.isEnabled());
        assertFalse(alertTriggerEntity.isTemplate());
    }

    private void assertEventRulesEntities(List<String> events, List<AlertEventRuleEntity> eventRules) {
        assertTrue(events.containsAll(eventRules.stream().map(AlertEventRuleEntity::getEvent).collect(Collectors.toList())));
    }

    private void assertEventRules(List<AlertEventType> events, List<AlertEventRule> eventRules) {
        assertTrue(events.containsAll(eventRules.stream().map(AlertEventRule::getEvent).collect(Collectors.toList())));
    }

    private AlertEventRuleEntity buildAlertEventRuleEntity(String event) {
        AlertEventRuleEntity alertEventRuleEntity = new AlertEventRuleEntity();
        alertEventRuleEntity.setEvent(event);
        return alertEventRuleEntity;
    }

    private AlertEventRule buildAlertEventRule(AlertEventType event) {
        AlertEventRule alertEventRule = new AlertEventRule();
        alertEventRule.setEvent(event);
        return alertEventRule;
    }
}
