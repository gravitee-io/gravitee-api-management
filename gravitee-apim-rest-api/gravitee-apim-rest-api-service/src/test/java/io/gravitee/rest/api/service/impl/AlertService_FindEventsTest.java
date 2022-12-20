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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.model.AlertEvent;
import io.gravitee.rest.api.model.AlertEventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.alert.AlertEventEntity;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AlertService_FindEventsTest extends AlertServiceTest {

    @Test
    public void must_search_alert_event_entity_by_event_types() {
        AlertEvent entity = newAlertEvent();

        when(alertEventRepository.search(any(), any())).thenReturn(new Page<>(List.of(entity), 0, 1, 1));

        final Page<AlertEventEntity> page = alertService.findEvents(
            UUID.randomUUID().toString(),
            new AlertEventQuery.Builder().pageNumber(0).pageSize(1).type(EventType.ALERT_NOTIFICATION).build()
        );

        assertEquals(0, page.getPageNumber());
        assertEquals(1, page.getPageElements());
        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }

    @Test
    public void must_search_alert_event_entity_by_event_type_and_return_empty() {
        when(alertEventRepository.search(any(), any())).thenReturn(new Page<>(List.of(), 0, 0, 0));

        final Page<AlertEventEntity> page = alertService.findEvents(
            UUID.randomUUID().toString(),
            new AlertEventQuery.Builder().pageNumber(0).pageSize(1).type(EventType.ALERT_NOTIFICATION).build()
        );

        assertEquals(1, page.getPageNumber());
        assertEquals(0, page.getPageElements());
        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getContent().size());
    }
}
