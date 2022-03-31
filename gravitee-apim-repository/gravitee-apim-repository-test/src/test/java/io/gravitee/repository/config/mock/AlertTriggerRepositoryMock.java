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
package io.gravitee.repository.config.mock;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.model.AlertEventRule;
import io.gravitee.repository.management.model.AlertEventType;
import io.gravitee.repository.management.model.AlertTrigger;
import java.util.Date;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertTriggerRepositoryMock extends AbstractRepositoryMock<AlertTriggerRepository> {

    public AlertTriggerRepositoryMock() {
        super(AlertTriggerRepository.class);
    }

    @Override
    void prepare(AlertTriggerRepository alertRepository) throws Exception {
        final Date date = new Date(1439022010883L);

        final AlertTrigger alert = new AlertTrigger();
        alert.setId("new-alert");
        alert.setName("Alert name");
        alert.setDescription("Description for the new alert");
        alert.setReferenceType("API");
        alert.setReferenceId("api-id");
        alert.setType("HEALTH_CHECK");
        alert.setDefinition("{}");
        alert.setCreatedAt(date);
        alert.setUpdatedAt(date);

        final AlertTrigger alert2 = new AlertTrigger();
        alert2.setId("alert");
        alert2.setName("Health-check");

        final AlertTrigger alertBeforeUpdate = new AlertTrigger();
        alertBeforeUpdate.setId("quota80");
        alertBeforeUpdate.setName("Quota80");

        final AlertTrigger alert2Updated = new AlertTrigger();
        alert2Updated.setId("quota80");
        alert2Updated.setName("New name");
        alert2Updated.setDescription("New description");
        alert2Updated.setReferenceType("New reference type");
        alert2Updated.setReferenceId("New reference id");
        alert2Updated.setType("New type");
        alert2Updated.setDefinition("{}");
        alert2Updated.setEnabled(true);
        alert2Updated.setCreatedAt(date);
        alert2Updated.setUpdatedAt(date);

        final AlertTrigger alertQuota = new AlertTrigger();
        alertQuota.setId("quota90");
        alertQuota.setName("Quota90");
        alertQuota.setDescription("Description for alert API quota 90%");
        alertQuota.setReferenceType("APPLICATION");
        alertQuota.setReferenceId("application-id");
        alertQuota.setType("QUOTA");
        alertQuota.setDefinition("{}");
        alertQuota.setEnabled(true);
        alertQuota.setCreatedAt(date);
        alertQuota.setUpdatedAt(new Date(1439022010883L));
        alertQuota.setTemplate(true);
        alertQuota.setEventRules(singletonList(new AlertEventRule()));

        final AlertTrigger alertQuota100 = new AlertTrigger();
        alertQuota100.setId("quota100");
        alertQuota100.setName("Quota100");
        alertQuota100.setDescription("Description for alert API quota 100%");
        alertQuota100.setReferenceType("API");
        alertQuota100.setReferenceId("application-id");
        alertQuota100.setType("QUOTA");
        alertQuota100.setDefinition("{}");
        alertQuota100.setEnabled(false);
        alertQuota100.setCreatedAt(date);
        alertQuota100.setUpdatedAt(new Date(1439022010883L));

        final AlertTrigger alertTriggerEvents = new AlertTrigger();
        alertTriggerEvents.setId("health-check");
        alertTriggerEvents.setName("Health-check");
        AlertEventRule eventRule1 = new AlertEventRule();
        eventRule1.setEvent(AlertEventType.API_CREATE);
        alertTriggerEvents.setEventRules(asList(eventRule1));

        final Set<AlertTrigger> alerts = newSet(alert, alertQuota, alert2Updated, alertQuota100);
        final Set<AlertTrigger> alertsAfterDelete = newSet(alert, alertQuota, alertQuota100);
        final Set<AlertTrigger> alertsAfterAdd = newSet(alert, alert2, alertQuota100, mock(AlertTrigger.class), mock(AlertTrigger.class));

        when(alertRepository.findAll()).thenReturn(alerts, alertsAfterAdd, alerts, alertsAfterDelete, alerts);

        when(alertRepository.create(any(AlertTrigger.class))).thenReturn(alert);

        when(alertRepository.findById("health-check")).thenReturn(of(alertTriggerEvents));
        when(alertRepository.findById("new-alert")).thenReturn(of(alert));
        when(alertRepository.findById("quota80")).thenReturn(of(alertBeforeUpdate), of(alert2Updated));

        when(alertRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());

        when(alertRepository.findByReference("API", "api-id", "DEFAULT")).thenReturn(singletonList(alert));
        when(alertRepository.findByReferenceAndReferenceIds("API", asList("api-id", "application-id"), "DEFAULT"))
            .thenReturn(asList(alert2, alertQuota100));
    }
}
