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
package io.gravitee.rest.api.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.model.AlertEvent;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.AlertAnalyticsQuery;
import io.gravitee.rest.api.model.alert.AlertAnalyticsEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AlertAnalyticsServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertAnalyticsServiceTest {

    private static final String REFERENCE_TYPE = "PLATFORM";
    private static final String REFERENCE_ID = "default";

    private AlertAnalyticsService cut;

    @Mock
    private AlertTriggerRepository alertTriggerRepository;

    @Mock
    private AlertEventRepository alertEventRepository;

    @Before
    public void setUp() throws Exception {
        cut = new AlertAnalyticsServiceImpl(alertTriggerRepository, alertEventRepository);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByReferenceWhenException() throws Exception {
        when(alertTriggerRepository.findByReference(REFERENCE_TYPE, REFERENCE_ID)).thenThrow(new TechnicalException());
        cut.findByReference(AlertReferenceType.PLATFORM, REFERENCE_ID, new AlertAnalyticsQuery.Builder().from(0).to(1).build());
    }

    @Test
    public void shouldNotFindByReferenceWhenNoAlert() throws Exception {
        when(alertTriggerRepository.findByReference(REFERENCE_TYPE, REFERENCE_ID)).thenReturn(Collections.emptyList());

        AlertAnalyticsEntity result = cut.findByReference(AlertReferenceType.PLATFORM, REFERENCE_ID, null);

        assertThat(result.getAlerts()).isEmpty();
        assertThat(result.getBySeverity()).isEmpty();
    }

    @Test
    public void shouldNotFindByReferenceWhenNoEvent() throws Exception {
        when(alertTriggerRepository.findByReference(REFERENCE_TYPE, REFERENCE_ID)).thenReturn(alertTriggerProvider());
        when(alertEventRepository.search(any(), any())).thenReturn(new Page(Collections.emptyList(), 0, 1, 0));

        AlertAnalyticsEntity result = cut.findByReference(
                AlertReferenceType.PLATFORM,
                REFERENCE_ID,
                new AlertAnalyticsQuery.Builder().from(0).to(1).build());

        assertThat(result.getAlerts()).isEmpty();
        assertThat(result.getBySeverity()).isEmpty();
    }

    @Test
    public void shouldFindByReference() throws Exception {
        when(alertTriggerRepository.findByReference(REFERENCE_TYPE, REFERENCE_ID)).thenReturn(alertTriggerProvider());
        when(alertEventRepository.search(any(), any()))
                .thenReturn(alertEventsProvider(10, "alert1"))
                .thenReturn(alertEventsProvider(50, "alert2"))
                .thenReturn(alertEventsProvider(12, "alert3"))
                .thenReturn(alertEventsProvider(0, "alert4"));

        AlertAnalyticsEntity result = cut.findByReference(
                AlertReferenceType.PLATFORM,
                REFERENCE_ID,
                new AlertAnalyticsQuery.Builder().from(0).to(1).build());

        assertThat(result.getAlerts()).hasSize(3);
        // Checking sorting on Severity then event count
        assertThat(result.getAlerts().get(0).getId()).isEqualTo("alert2");
        assertThat(result.getAlerts().get(1).getId()).isEqualTo("alert3");
        assertThat(result.getAlerts().get(2).getId()).isEqualTo("alert1");
        assertThat(result.getBySeverity().get("CRITICAL")).isEqualTo(62);
        assertThat(result.getBySeverity().get("INFO")).isEqualTo(10);
        assertThat(result.getBySeverity().containsKey("WARNING")).isFalse();
    }

    private List<AlertTrigger> alertTriggerProvider() {
        ArrayList<AlertTrigger> alerts = new ArrayList<>();
        AlertTrigger alert1 = new AlertTrigger();
        alert1.setId("alert1");
        alert1.setSeverity("INFO");
        alerts.add(alert1);

        AlertTrigger alert2 = new AlertTrigger();
        alert2.setId("alert2");
        alert2.setSeverity("CRITICAL");
        alerts.add(alert2);

        AlertTrigger alert3 = new AlertTrigger();
        alert3.setId("alert3");
        alert3.setSeverity("CRITICAL");
        alerts.add(alert3);

        AlertTrigger alert4 = new AlertTrigger();
        alert4.setId("alert4");
        alert4.setSeverity("WARNING");
        alerts.add(alert4);

        return alerts;
    }

    private Page<AlertEvent> alertEventsProvider(int eventNumber, String alert) {

        ArrayList<AlertEvent> events = new ArrayList<>();

        for (int i = 0; i < eventNumber; i++) {
            AlertEvent event = new AlertEvent();
            event.setAlert(alert);
            events.add(event);
        }

        return new Page(events, 0, events.size(), events.size());
    }
}