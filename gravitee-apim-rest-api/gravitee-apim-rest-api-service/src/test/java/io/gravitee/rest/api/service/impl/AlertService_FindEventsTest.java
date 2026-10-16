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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.AlertEvent;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.AlertEventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.alert.AlertEventEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.service.exceptions.AlertNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class AlertService_FindEventsTest extends AlertServiceTest {

    @Test
    public void must_search_alert_event_entity_by_event_types() throws TechnicalException {
        AlertEvent entity = newAlertEvent();

        final String alertId = UUID.randomUUID().toString();
        when(alertTriggerRepository.findById(alertId)).thenReturn(Optional.of(scopedAlertTrigger(alertId)));
        when(alertEventRepository.search(any(), any())).thenReturn(new Page<>(List.of(entity), 0, 1, 1));

        final Page<AlertEventEntity> page = alertService.findEvents(
            alertId,
            AlertReferenceType.ENVIRONMENT,
            "DEFAULT",
            new AlertEventQuery.Builder().pageNumber(0).pageSize(1).type(EventType.ALERT_NOTIFICATION).build()
        );

        assertEquals(0, page.getPageNumber());
        assertEquals(1, page.getPageElements());
        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }

    @Test
    public void must_search_alert_event_entity_by_event_type_and_return_empty() throws TechnicalException {
        final String alertId = UUID.randomUUID().toString();
        when(alertTriggerRepository.findById(alertId)).thenReturn(Optional.of(scopedAlertTrigger(alertId)));
        when(alertEventRepository.search(any(), any())).thenReturn(new Page<>(List.of(), 0, 0, 0));

        final Page<AlertEventEntity> page = alertService.findEvents(
            alertId,
            AlertReferenceType.ENVIRONMENT,
            "DEFAULT",
            new AlertEventQuery.Builder().pageNumber(0).pageSize(1).type(EventType.ALERT_NOTIFICATION).build()
        );

        assertEquals(1, page.getPageNumber());
        assertEquals(0, page.getPageElements());
        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getContent().size());
    }

    @Test
    public void must_search_events_when_alert_belongs_to_the_reference() throws TechnicalException {
        final String alertId = UUID.randomUUID().toString();

        when(alertTriggerRepository.findById(alertId)).thenReturn(Optional.of(scopedAlertTrigger(alertId)));
        when(alertEventRepository.search(any(), any())).thenReturn(new Page<>(List.of(newAlertEvent()), 0, 1, 1));

        final Page<AlertEventEntity> page = alertService.findEvents(
            alertId,
            AlertReferenceType.ENVIRONMENT,
            "DEFAULT",
            new AlertEventQuery.Builder().pageNumber(0).pageSize(1).build()
        );

        assertEquals(1, page.getContent().size());
    }

    @Test
    public void must_throw_AlertNotFoundException_when_alert_belongs_to_another_environment() throws TechnicalException {
        final String alertId = UUID.randomUUID().toString();
        final AlertTrigger alertTrigger = new AlertTrigger();
        alertTrigger.setId(alertId);
        alertTrigger.setReferenceType(AlertReferenceType.ENVIRONMENT.name());
        alertTrigger.setReferenceId("ANOTHER_ENVIRONMENT");

        when(alertTriggerRepository.findById(alertId)).thenReturn(Optional.of(alertTrigger));

        assertThrows(AlertNotFoundException.class, () ->
            alertService.findEvents(
                alertId,
                AlertReferenceType.ENVIRONMENT,
                "DEFAULT",
                new AlertEventQuery.Builder().pageNumber(0).pageSize(1).build()
            )
        );

        verify(alertEventRepository, never()).search(any(), any());
    }

    @Test
    public void must_throw_AlertNotFoundException_when_scoped_alert_is_not_found() throws TechnicalException {
        final String alertId = UUID.randomUUID().toString();

        when(alertTriggerRepository.findById(alertId)).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class, () ->
            alertService.findEvents(
                alertId,
                AlertReferenceType.ENVIRONMENT,
                "DEFAULT",
                new AlertEventQuery.Builder().pageNumber(0).pageSize(1).build()
            )
        );

        verify(alertEventRepository, never()).search(any(), any());
    }

    private static AlertTrigger scopedAlertTrigger(String alertId) {
        AlertTrigger alertTrigger = new AlertTrigger();
        alertTrigger.setId(alertId);
        alertTrigger.setReferenceType(AlertReferenceType.ENVIRONMENT.name());
        alertTrigger.setReferenceId("DEFAULT");
        return alertTrigger;
    }
}
