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
package io.gravitee.repository;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.*;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.AlertTrigger;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;

public class AlertRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/alerttrigger-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<AlertTrigger> alerts = alertRepository.findAll();

        assertNotNull(alerts);
        assertEquals(4, alerts.size());
        final Optional<AlertTrigger> optionalAlert = alerts.stream().filter(alert -> "quota90".equals(alert.getId())).findAny();
        assertTrue(optionalAlert.isPresent());
        assertEquals("Quota90", optionalAlert.get().getName());
        assertEquals("Description for alert API quota 90%", optionalAlert.get().getDescription());
        assertEquals("APPLICATION", optionalAlert.get().getReferenceType());
        assertEquals("application-id", optionalAlert.get().getReferenceId());
        assertEquals("QUOTA", optionalAlert.get().getType());
        assertEquals("{}", optionalAlert.get().getDefinition());
        assertTrue(optionalAlert.get().isEnabled());
        assertTrue(compareDate(1439022010883L, optionalAlert.get().getCreatedAt().getTime()));
        assertTrue(compareDate(1439022010883L, optionalAlert.get().getUpdatedAt().getTime()));
        assertTrue(optionalAlert.get().isTemplate());
        assertEquals(1, optionalAlert.get().getEventRules().size());
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<AlertTrigger> alert = alertRepository.findById("health-check");

        assertNotNull(alert);
        assertTrue(alert.isPresent());
        assertEquals("health-check", alert.get().getId());
        assertEquals("Health-check", alert.get().getName());
        assertEquals(1, alert.get().getEventRules().size());
    }

    @Test
    public void shouldCreate() throws Exception {
        final AlertTrigger alert = new AlertTrigger();
        alert.setId("new-alert");
        alert.setName("Alert name");
        alert.setDescription("Description for the new alert");
        alert.setReferenceType("API");
        alert.setReferenceId("api-id");
        alert.setType("HEALTH_CHECK");
        alert.setDefinition("{}");
        final Date date = new Date(1439022010883L);
        alert.setCreatedAt(date);
        alert.setUpdatedAt(date);

        int nbAlertsBeforeCreation = alertRepository.findAll().size();
        alertRepository.create(alert);
        int nbAlertsAfterCreation = alertRepository.findAll().size();

        assertEquals(nbAlertsBeforeCreation + 1, nbAlertsAfterCreation);

        Optional<AlertTrigger> optional = alertRepository.findById("new-alert");
        Assert.assertTrue("Alert saved not found", optional.isPresent());
        final AlertTrigger fetchedAlert = optional.get();
        assertEquals(alert.getName(), fetchedAlert.getName());
        assertEquals(alert.getDescription(), fetchedAlert.getDescription());
        assertEquals(alert.getReferenceType(), fetchedAlert.getReferenceType());
        assertEquals(alert.getReferenceId(), fetchedAlert.getReferenceId());
        assertEquals(alert.getType(), fetchedAlert.getType());
        assertEquals(alert.getDefinition(), fetchedAlert.getDefinition());
        assertEquals(alert.isEnabled(), fetchedAlert.isEnabled());
        assertTrue(compareDate(alert.getCreatedAt(), fetchedAlert.getCreatedAt()));
        assertTrue(compareDate(alert.getUpdatedAt(), fetchedAlert.getUpdatedAt()));
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<AlertTrigger> optional = alertRepository.findById("quota80");
        Assert.assertTrue("Alert to update not found", optional.isPresent());
        assertEquals("Invalid saved alert name.", "Quota80", optional.get().getName());

        final AlertTrigger alert = optional.get();
        alert.setName("New name");
        alert.setDescription("New description");
        alert.setReferenceType("New reference type");
        alert.setReferenceId("New reference id");
        alert.setType("New type");
        alert.setDefinition("{}");
        alert.setEnabled(true);
        final Date date = new Date(1439022010883L);
        alert.setCreatedAt(date);
        alert.setUpdatedAt(date);

        int nbAlertsBeforeUpdate = alertRepository.findAll().size();
        alertRepository.update(alert);
        int nbAlertsAfterUpdate = alertRepository.findAll().size();

        assertEquals(nbAlertsBeforeUpdate, nbAlertsAfterUpdate);

        Optional<AlertTrigger> optionalUpdated = alertRepository.findById("quota80");
        Assert.assertTrue("Alert to update not found", optionalUpdated.isPresent());
        final AlertTrigger fetchedAlert = optionalUpdated.get();
        assertEquals(alert.getName(), fetchedAlert.getName());
        assertEquals(alert.getDescription(), fetchedAlert.getDescription());
        assertEquals(alert.getReferenceType(), fetchedAlert.getReferenceType());
        assertEquals(alert.getReferenceId(), fetchedAlert.getReferenceId());
        assertEquals(alert.getType(), fetchedAlert.getType());
        assertEquals(alert.getDefinition(), fetchedAlert.getDefinition());
        assertEquals(alert.isEnabled(), fetchedAlert.isEnabled());
        assertTrue(compareDate(alert.getCreatedAt(), fetchedAlert.getCreatedAt()));
        assertTrue(compareDate(alert.getUpdatedAt(), fetchedAlert.getUpdatedAt()));
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbAlertsBeforeDeletion = alertRepository.findAll().size();
        alertRepository.delete("quota80");
        int nbAlertsAfterDeletion = alertRepository.findAll().size();

        assertEquals(nbAlertsBeforeDeletion - 1, nbAlertsAfterDeletion);
    }

    @Test
    public void shouldFindByReference() throws Exception {
        final List<AlertTrigger> alerts = alertRepository.findByReference("API", "api-id");
        assertNotNull(alerts);
        assertEquals(1, alerts.size());
    }

    @Test
    public void shouldFindByReferenceAndReferenceIds() throws Exception {
        final List<AlertTrigger> alerts = alertRepository.findByReferenceAndReferenceIds("API", Arrays.asList("api-id", "application-id"));
        assertNotNull(alerts);
        assertEquals(2, alerts.size());
    }

    @Test
    public void shouldFindByReferenceAndReferenceIds_andReturnEmptyListWhenInputListIsEmpty() throws Exception {
        final List<AlertTrigger> alerts = alertRepository.findByReferenceAndReferenceIds("API", Collections.emptyList());
        assertNotNull(alerts);
        assertEquals(0, alerts.size());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownAlert() throws Exception {
        AlertTrigger unknownAlert = new AlertTrigger();
        unknownAlert.setId("unknown");
        alertRepository.update(unknownAlert);
        fail("An unknown alert should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        alertRepository.update(null);
        fail("A null alert should not be updated");
    }
}
