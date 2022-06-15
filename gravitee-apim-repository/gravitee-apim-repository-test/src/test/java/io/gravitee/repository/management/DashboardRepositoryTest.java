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

import io.gravitee.repository.config.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.Dashboard;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;

public class DashboardRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/dashboard-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Dashboard> dashboards = dashboardRepository.findAll();
        assertNotNull(dashboards);
        assertEquals(4, dashboards.size());
    }

    @Test
    public void shouldFindByReferenceType() throws Exception {
        final List<Dashboard> dashboards = dashboardRepository.findByReferenceType("PLATFORM");

        assertNotNull(dashboards);
        assertEquals(3, dashboards.size());
        final Dashboard dashboardProduct = dashboards
            .stream()
            .filter(dashboard -> "4eeb1c56-6f4a-4925-ab1c-566f4aa925b8".equals(dashboard.getId()))
            .findAny()
            .get();
        assertEquals("Geo dashboard", dashboardProduct.getName());
        final Iterator<Dashboard> iterator = dashboards.iterator();
        assertEquals("Global dashboard", iterator.next().getName());
        assertEquals("Geo dashboard", iterator.next().getName());
        assertEquals("Device dashboard", iterator.next().getName());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Dashboard dashboard = new Dashboard();
        dashboard.setId("new-dashboard");
        dashboard.setReferenceType("API");
        dashboard.setReferenceId("DEFAULT");
        dashboard.setName("Dashboard name");
        dashboard.setQueryFilter("api:apiId");
        dashboard.setOrder(1);
        dashboard.setEnabled(true);
        dashboard.setDefinition("{\"def\": \"value\"}");
        dashboard.setCreatedAt(new Date(1000000000000L));
        dashboard.setUpdatedAt(new Date(1111111111111L));

        int nbDashboardsBeforeCreation = dashboardRepository.findByReferenceType("API").size();
        dashboardRepository.create(dashboard);
        int nbDashboardsAfterCreation = dashboardRepository.findByReferenceType("API").size();

        Assert.assertEquals(nbDashboardsBeforeCreation + 1, nbDashboardsAfterCreation);

        Optional<Dashboard> optional = dashboardRepository.findById("new-dashboard");
        Assert.assertTrue("Dashboard saved not found", optional.isPresent());

        final Dashboard dashboardSaved = optional.get();
        Assert.assertEquals("Invalid saved dashboard id.", dashboard.getId(), dashboardSaved.getId());
        Assert.assertEquals("Invalid saved dashboard reference id.", dashboard.getReferenceId(), dashboardSaved.getReferenceId());
        Assert.assertEquals("Invalid saved dashboard reference type.", dashboard.getReferenceType(), dashboardSaved.getReferenceType());
        Assert.assertEquals("Invalid saved dashboard name.", dashboard.getName(), dashboardSaved.getName());
        Assert.assertEquals("Invalid saved dashboard query filter.", dashboard.getQueryFilter(), dashboardSaved.getQueryFilter());
        Assert.assertEquals("Invalid saved dashboard order.", dashboard.getOrder(), dashboardSaved.getOrder());
        Assert.assertEquals("Invalid saved dashboard enabled.", dashboard.isEnabled(), dashboardSaved.isEnabled());
        Assert.assertEquals("Invalid saved dashboard definition.", dashboard.getDefinition(), dashboardSaved.getDefinition());
        Assert.assertTrue("Invalid saved dashboard created at.", compareDate(dashboard.getCreatedAt(), dashboardSaved.getCreatedAt()));
        Assert.assertTrue("Invalid saved dashboard updated at.", compareDate(dashboard.getUpdatedAt(), dashboardSaved.getUpdatedAt()));
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Dashboard> optional = dashboardRepository.findById("6e0d09f0-ba5d-4571-8d09-f0ba5d7571c3");
        Assert.assertTrue("Dashboard to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved dashboard name.", "Device dashboard", optional.get().getName());

        final Dashboard dashboard = optional.get();
        dashboard.setReferenceType("PLATFORM");
        dashboard.setReferenceId("1");
        dashboard.setName("New dashboard");
        dashboard.setQueryFilter("api:apiId");
        dashboard.setOrder(3);
        dashboard.setEnabled(true);
        dashboard.setDefinition("{\"def\": \"new value\"}");
        dashboard.setCreatedAt(new Date(1111111111111L));
        dashboard.setUpdatedAt(new Date(1000000000000L));

        int nbDashboardsBeforeUpdate = dashboardRepository.findByReferenceType("PLATFORM").size();
        dashboardRepository.update(dashboard);
        int nbDashboardsAfterUpdate = dashboardRepository.findByReferenceType("PLATFORM").size();

        Assert.assertEquals(nbDashboardsBeforeUpdate, nbDashboardsAfterUpdate);

        Optional<Dashboard> optionalUpdated = dashboardRepository.findById("6e0d09f0-ba5d-4571-8d09-f0ba5d7571c3");
        Assert.assertTrue("Dashboard to update not found", optionalUpdated.isPresent());

        final Dashboard dashboardSaved = optionalUpdated.get();
        Assert.assertEquals("Invalid saved dashboard reference id.", "1", dashboardSaved.getReferenceId());
        Assert.assertEquals("Invalid saved dashboard name.", "New dashboard", dashboardSaved.getName());
        Assert.assertEquals("Invalid saved dashboard query filter.", "api:apiId", dashboardSaved.getQueryFilter());
        Assert.assertEquals("Invalid saved dashboard order.", 3, dashboardSaved.getOrder());
        assertTrue("Invalid saved dashboard enabled.", dashboardSaved.isEnabled());
        Assert.assertEquals("Invalid saved dashboard definition.", "{\"def\": \"new value\"}", dashboardSaved.getDefinition());
        Assert.assertTrue("Invalid saved dashboard created at.", compareDate(new Date(1111111111111L), dashboardSaved.getCreatedAt()));
        Assert.assertTrue("Invalid saved dashboard updated at.", compareDate(new Date(1000000000000L), dashboardSaved.getUpdatedAt()));
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbDashboardsBeforeDeletion = dashboardRepository.findByReferenceType("API").size();
        dashboardRepository.delete("7589ef82-02ae-4fc7-89ef-8202ae3fc7cf");
        int nbDashboardsAfterDeletion = dashboardRepository.findByReferenceType("API").size();

        Assert.assertEquals(nbDashboardsBeforeDeletion - 1, nbDashboardsAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownDashboard() throws Exception {
        Dashboard unknownDashboard = new Dashboard();
        unknownDashboard.setId("unknown");
        dashboardRepository.update(unknownDashboard);
        fail("An unknown dashboard should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        dashboardRepository.update(null);
        fail("A null dashboard should not be updated");
    }
}
