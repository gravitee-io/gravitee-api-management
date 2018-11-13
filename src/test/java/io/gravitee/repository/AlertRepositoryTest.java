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

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.Alert;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

public class AlertRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/alert-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Alert> alerts = alertRepository.findAll();

        assertNotNull(alerts);
        assertEquals(2, alerts.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Alert alert = new Alert();
        alert.setId("new-alert");
        alert.setName("Alert name");
        alert.setDescription("Description for the new alert");
        alert.setReferenceType("API");
        alert.setReferenceId("api-id");
        alert.setType("HEALTH_CHECK");
        alert.setCreatedAt(new Date());

        int nbAlertsBeforeCreation = alertRepository.findAll().size();
        alertRepository.create(alert);
        int nbAlertsAfterCreation = alertRepository.findAll().size();

        assertEquals(nbAlertsBeforeCreation + 1, nbAlertsAfterCreation);

        Optional<Alert> optional = alertRepository.findById("new-alert");
        Assert.assertTrue("Alert saved not found", optional.isPresent());
        assertEquals("Invalid saved alert.", alert, optional.get());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Alert> optional = alertRepository.findById("health-check");
        Assert.assertTrue("Alert to update not found", optional.isPresent());
        assertEquals("Invalid saved alert name.", "Health-check", optional.get().getName());

        final Alert alert = optional.get();
        alert.setName("New name");
        alert.setDescription("New description");

        int nbAlertsBeforeUpdate = alertRepository.findAll().size();
        alertRepository.update(alert);
        int nbAlertsAfterUpdate = alertRepository.findAll().size();

        assertEquals(nbAlertsBeforeUpdate, nbAlertsAfterUpdate);

        Optional<Alert> optionalUpdated = alertRepository.findById("health-check");
        Assert.assertTrue("Alert to update not found", optionalUpdated.isPresent());
        assertEquals("Invalid saved alert.", alert, optionalUpdated.get());
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
        final List<Alert> alerts = alertRepository.findByReference("API", "api-id");
        assertNotNull(alerts);
        assertEquals(1, alerts.size());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownAlert() throws Exception {
        Alert unknownAlert = new Alert();
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
