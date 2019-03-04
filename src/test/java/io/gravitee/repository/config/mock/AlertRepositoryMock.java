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

import io.gravitee.repository.management.api.AlertRepository;
import io.gravitee.repository.management.model.Alert;

import java.util.Date;
import java.util.Set;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertRepositoryMock extends AbstractRepositoryMock<AlertRepository> {

    public AlertRepositoryMock() {
        super(AlertRepository.class);
    }

    @Override
    void prepare(AlertRepository alertRepository) throws Exception {
        final Date date = new Date(1439022010883L);

        final Alert alert = new Alert();
        alert.setId("new-alert");
        alert.setName("Alert name");
        alert.setDescription("Description for the new alert");
        alert.setReferenceType("API");
        alert.setReferenceId("api-id");
        alert.setType("HEALTH_CHECK");
        alert.setCreatedAt(date);
        alert.setUpdatedAt(date);

        final Alert alert2 = new Alert();
        alert2.setId("alert");
        alert2.setName("Health-check");

        final Alert alertBeforUpdate = new Alert();
        alertBeforUpdate.setId("quota80");
        alertBeforUpdate.setName("Quota80");

        final Alert alert2Updated = new Alert();
        alert2Updated.setId("quota80");
        alert2Updated.setName("New name");
        alert2Updated.setDescription("New description");
        alert2Updated.setReferenceType("New reference type");
        alert2Updated.setReferenceId("New reference id");
        alert2Updated.setType("New type");
        alert2Updated.setMetricType("New metric type");
        alert2Updated.setMetric("New metric");
        alert2Updated.setThreshold(99D);
        alert2Updated.setPlan("New plan");
        alert2Updated.setEnabled(true);
        alert2Updated.setCreatedAt(date);
        alert2Updated.setUpdatedAt(date);

        final Alert alertQuota = new Alert();
        alertQuota.setId("quota90");
        alertQuota.setName("Quota90");
        alertQuota.setDescription("Description for alert API quota 90%");
        alertQuota.setReferenceType("APPLICATION");
        alertQuota.setReferenceId("application-id");
        alertQuota.setType("QUOTA");
        alertQuota.setMetricType("QUOTA");
        alertQuota.setThresholdType("PERCENT_RATE");
        alertQuota.setThreshold(90D);
        alertQuota.setPlan("gold");
        alertQuota.setEnabled(true);
        alertQuota.setCreatedAt(date);
        alertQuota.setUpdatedAt(new Date(1439022010883L));

        final Set<Alert> alerts = newSet(alert, alertQuota, alert2Updated);
        final Set<Alert> alertsAfterDelete = newSet(alert, alertQuota);
        final Set<Alert> alertsAfterAdd = newSet(alert, alert2, mock(Alert.class), mock(Alert.class));

        when(alertRepository.findAll()).thenReturn(alerts, alertsAfterAdd, alerts, alertsAfterDelete, alerts);

        when(alertRepository.create(any(Alert.class))).thenReturn(alert);

        when(alertRepository.findById("new-alert")).thenReturn(of(alert));
        when(alertRepository.findById("quota80")).thenReturn(of(alertBeforUpdate), of(alert2Updated));

        when(alertRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());

        when(alertRepository.findByReference("API", "api-id")).thenReturn(singletonList(alert));
    }
}
