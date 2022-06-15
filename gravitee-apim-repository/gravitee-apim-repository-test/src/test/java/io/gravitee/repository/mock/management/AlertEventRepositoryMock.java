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
package io.gravitee.repository.mock.management;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.AlertEvent;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertEventRepositoryMock extends AbstractRepositoryMock<AlertEventRepository> {

    public AlertEventRepositoryMock() {
        super(AlertEventRepository.class);
    }

    @Override
    protected void prepare(AlertEventRepository alertEventRepository) throws Exception {
        final Date date = new Date(1439022010883L);

        final AlertEvent alertEvent = new AlertEvent();
        alertEvent.setId("new-alert-event");
        alertEvent.setAlert("1111-2222-3333-4444");
        alertEvent.setMessage("an alert message");
        alertEvent.setCreatedAt(date);
        alertEvent.setUpdatedAt(date);

        /*
        final AlertEvent alertEvent2 = new AlertEvent();
        alertEvent2.setId("alert");
        alertEvent2.setName("Health-check");
         */

        final AlertEvent alertBeforeUpdate = new AlertEvent();
        alertBeforeUpdate.setId("an-alert-to-update");
        alertBeforeUpdate.setAlert("alert-parent-id");
        alertBeforeUpdate.setMessage("Message of the alert to update");
        alertBeforeUpdate.setCreatedAt(date);
        alertBeforeUpdate.setUpdatedAt(date);

        final AlertEvent alert2Updated = new AlertEvent();
        alert2Updated.setId("an-alert-to-update");
        alert2Updated.setAlert("alert-parent-id");
        alert2Updated.setMessage("An updated message");
        alert2Updated.setCreatedAt(date);
        alert2Updated.setUpdatedAt(date);

        final AlertEvent alertLatest = new AlertEvent();
        alertLatest.setId("latest-alert");
        alertLatest.setAlert("alert-parent-id2");
        alertLatest.setMessage("Message of the latest alert");
        alertLatest.setCreatedAt(date);
        alertLatest.setUpdatedAt(date);

        //        final Set<AlertEvent> events = newSet(alertEvent, alertLatest, alert2Updated);
        //        final Set<AlertTrigger> alertsAfterDelete = newSet(alertEvent, alertQuota);
        //        final Set<AlertTrigger> alertsAfterAdd = newSet(alertEvent, alertEvent2, mock(AlertTrigger.class), mock(AlertTrigger.class));

        final io.gravitee.common.data.domain.Page<AlertEvent> pageAlertEvent = mock(io.gravitee.common.data.domain.Page.class);
        when(pageAlertEvent.getTotalElements()).thenReturn(3L);
        when(pageAlertEvent.getContent()).thenReturn(asList(alertEvent, alertBeforeUpdate, alertLatest));

        final io.gravitee.common.data.domain.Page<AlertEvent> pageAlertEvent2 = mock(io.gravitee.common.data.domain.Page.class);
        when(pageAlertEvent2.getTotalElements()).thenReturn(2L);
        when(pageAlertEvent2.getContent()).thenReturn(asList(alertEvent, alertLatest));

        final io.gravitee.common.data.domain.Page<AlertEvent> pageAlertEvent3 = mock(io.gravitee.common.data.domain.Page.class);
        when(pageAlertEvent3.getTotalElements()).thenReturn(1L);
        when(pageAlertEvent3.getContent()).thenReturn(asList(alertLatest));

        final io.gravitee.common.data.domain.Page<AlertEvent> pageAlertEventEmpty = mock(io.gravitee.common.data.domain.Page.class);
        when(pageAlertEventEmpty.getTotalElements()).thenReturn(0L);
        when(pageAlertEventEmpty.getContent()).thenReturn(emptyList());

        // shouldDelete
        when(alertEventRepository.search(argThat(o -> o != null && o.getAlert() == null), any(Pageable.class)))
            .thenReturn(pageAlertEvent, pageAlertEvent2, pageAlertEvent3);

        //shouldFindByAlert
        when(alertEventRepository.search(argThat(o -> o != null && "alert-parent-id2".equals(o.getAlert())), any(Pageable.class)))
            .thenReturn(pageAlertEvent3);

        //shouldDeleteAll
        when(alertEventRepository.search(argThat(o -> o != null && "alert-id-to-delete".equals(o.getAlert())), any(Pageable.class)))
            .thenReturn(pageAlertEvent2, pageAlertEventEmpty);

        when(alertEventRepository.create(any(AlertEvent.class))).thenReturn(alertEvent);

        when(alertEventRepository.findById("new-alert-event")).thenReturn(of(alertEvent));
        when(alertEventRepository.findById("an-alert-to-update")).thenReturn(of(alertBeforeUpdate), of(alert2Updated));

        when(alertEventRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());
    }
}
