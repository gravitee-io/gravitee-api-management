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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.config.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.search.AlertEventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.AlertEvent;
import java.util.Date;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class AlertEventRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/alertevent-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final AlertEvent event = new AlertEvent();
        event.setId("new-alert-event");
        event.setAlert("1111-2222-3333-4444");
        event.setMessage("an alert message");
        final Date date = new Date(1439022010883L);
        event.setCreatedAt(date);
        event.setUpdatedAt(date);

        alertEventRepository.create(event);

        Optional<AlertEvent> optional = alertEventRepository.findById("new-alert-event");
        Assert.assertTrue("Alert event saved not found", optional.isPresent());
        final AlertEvent fetchedAlert = optional.get();
        assertEquals(event.getId(), fetchedAlert.getId());
        assertEquals(event.getAlert(), fetchedAlert.getAlert());
        assertEquals(event.getMessage(), fetchedAlert.getMessage());
        assertTrue(compareDate(event.getCreatedAt(), fetchedAlert.getCreatedAt()));
        assertTrue(compareDate(event.getUpdatedAt(), fetchedAlert.getUpdatedAt()));
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<AlertEvent> optional = alertEventRepository.findById("an-alert-to-update");
        Assert.assertTrue("Alert event to update not found", optional.isPresent());
        assertEquals("Invalid saved alert name.", "an-alert-to-update", optional.get().getId());

        final AlertEvent event = optional.get();
        event.setMessage("An updated message");
        final Date date = new Date(1439022010883L);
        event.setCreatedAt(date);
        event.setUpdatedAt(date);

        alertEventRepository.update(event);

        Optional<AlertEvent> optionalUpdated = alertEventRepository.findById("an-alert-to-update");
        Assert.assertTrue("Alert event to update not found", optionalUpdated.isPresent());
        final AlertEvent fetchedAlert = optionalUpdated.get();
        assertEquals(event.getId(), fetchedAlert.getId());
        assertEquals(event.getAlert(), fetchedAlert.getAlert());
        assertEquals(event.getMessage(), fetchedAlert.getMessage());
        assertTrue(compareDate(event.getCreatedAt(), fetchedAlert.getCreatedAt()));
        assertTrue(compareDate(event.getUpdatedAt(), fetchedAlert.getUpdatedAt()));
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbAlertsBeforeDeletion = (int) alertEventRepository
            .search(new AlertEventCriteria.Builder().build(), new PageableBuilder().pageNumber(0).pageSize(10).build())
            .getTotalElements();

        alertEventRepository.delete("an-alert-to-update");

        int nbAlertsAfterDeletion = (int) alertEventRepository
            .search(new AlertEventCriteria.Builder().build(), new PageableBuilder().pageNumber(0).pageSize(10).build())
            .getTotalElements();

        assertEquals(nbAlertsBeforeDeletion - 1, nbAlertsAfterDeletion);
    }

    @Test
    public void shouldFindByAlert() throws Exception {
        final Page<AlertEvent> pageEvents = alertEventRepository.search(
            new AlertEventCriteria.Builder().alert("alert-parent-id2").build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertNotNull(pageEvents);
        assertEquals(1, pageEvents.getTotalElements());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownAlert() throws Exception {
        AlertEvent unknownAlertEvent = new AlertEvent();
        unknownAlertEvent.setId("unknown");
        alertEventRepository.update(unknownAlertEvent);
        fail("An unknown alert event should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        alertEventRepository.update(null);
        fail("A null alert event should not be updated");
    }

    @Test
    public void shouldDeleteAll() throws Exception {
        long before = alertEventRepository
            .search(
                new AlertEventCriteria.Builder().alert("alert-id-to-delete").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
            .getTotalElements();

        alertEventRepository.deleteAll("alert-id-to-delete");

        long after = alertEventRepository
            .search(
                new AlertEventCriteria.Builder().alert("alert-id-to-delete").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
            .getTotalElements();

        assertEquals("2 before", 2, before);
        assertEquals("0 after", 0, after);
    }
}
