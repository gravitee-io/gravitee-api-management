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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.assertEquals;

import io.gravitee.repository.management.model.Dashboard;
import io.gravitee.repository.management.model.DashboardReferenceType;
import java.util.*;
import org.junit.Test;

public class DashboardRepositoryTest extends AbstractManagementRepositoryTest {

    public static final String ENVIRONMENT = "ENVIRONMENT";

    @Override
    protected String getTestCasesPath() {
        return "/data/dashboard-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Dashboard> dashboards = dashboardRepository.findAll();
        assertThat(dashboards).hasSize(8);
    }

    @Test
    public void shouldFindAllByReference() throws Exception {
        final List<Dashboard> dashboards = dashboardRepository.findByReference(ENVIRONMENT, "OTHER_ENV");
        assertThat(dashboards).hasSize(2);
    }

    @Test
    public void shouldFindByReferenceAndId() throws Exception {
        final Optional<Dashboard> dashboard = dashboardRepository.findByReferenceAndId(
            ENVIRONMENT,
            "OTHER_ENV",
            "c9d5390e-6387-4943-a123-613df16a419f"
        );

        assertThat(dashboard)
            .hasValueSatisfying(result -> {
                assertThat(result.getName()).isEqualTo("Geo dashboard - the only one of OTHER_ENV");
            });
    }

    @Test
    public void shouldFindByReferenceType() throws Exception {
        final List<Dashboard> dashboards = dashboardRepository.findByReferenceAndType(ENVIRONMENT, "DEFAULT", "PLATFORM");

        assertThat(dashboards).hasSize(3);
        final Dashboard dashboardProduct = dashboards
            .stream()
            .filter(dashboard -> "4eeb1c56-6f4a-4925-ab1c-566f4aa925b8".equals(dashboard.getId()))
            .findAny()
            .get();
        assertThat(dashboardProduct.getName()).isEqualTo("Geo dashboard");
        final Iterator<Dashboard> iterator = dashboards.iterator();
        assertThat(iterator.next().getName()).isEqualTo("Global dashboard");
        assertThat(iterator.next().getName()).isEqualTo("Geo dashboard");
        assertThat(iterator.next().getName()).isEqualTo("Device dashboard");
    }

    @Test
    public void shouldCreate() throws Exception {
        final Dashboard dashboard = new Dashboard();
        dashboard.setId("new-dashboard");
        dashboard.setReferenceType(ENVIRONMENT);
        dashboard.setReferenceId("DEFAULT");
        dashboard.setType("API");
        dashboard.setName("Dashboard name");
        dashboard.setQueryFilter("api:apiId");
        dashboard.setOrder(1);
        dashboard.setEnabled(true);
        dashboard.setDefinition("{\"def\": \"value\"}");
        dashboard.setCreatedAt(new Date(1000000000000L));
        dashboard.setUpdatedAt(new Date(1111111111111L));

        int nbDashboardsBeforeCreation = dashboardRepository.findByReferenceAndType(ENVIRONMENT, "DEFAULT", "API").size();
        dashboardRepository.create(dashboard);
        int nbDashboardsAfterCreation = dashboardRepository.findByReferenceAndType(ENVIRONMENT, "DEFAULT", "API").size();

        assertThat(nbDashboardsAfterCreation).isEqualTo(nbDashboardsBeforeCreation + 1);

        Optional<Dashboard> optional = dashboardRepository.findById("new-dashboard");
        assertThat(optional).isPresent();
        final Dashboard dashboardSaved = optional.get();
        assertThat(dashboard.getId()).as("Invalid saved dashboard id.").isEqualTo(dashboardSaved.getId());
        assertThat(dashboard.getReferenceId()).as("Invalid saved dashboard reference id.").isEqualTo(dashboardSaved.getReferenceId());
        assertThat(dashboard.getReferenceType()).as("Invalid saved dashboard reference type.").isEqualTo(dashboardSaved.getReferenceType());
        assertThat(dashboard.getName()).as("Invalid saved dashboard name.").isEqualTo(dashboardSaved.getName());
        assertThat(dashboard.getQueryFilter()).as("Invalid saved dashboard query filter.").isEqualTo(dashboardSaved.getQueryFilter());
        assertThat(dashboard.getOrder()).as("Invalid saved dashboard order.").isEqualTo(dashboardSaved.getOrder());
        assertThat(dashboard.isEnabled()).as("Invalid saved dashboard enabled.").isEqualTo(dashboardSaved.isEnabled());
        assertThat(dashboard.getDefinition()).as("Invalid saved dashboard definition.").isEqualTo(dashboardSaved.getDefinition());
        assertThat(compareDate(dashboard.getCreatedAt(), dashboardSaved.getCreatedAt())).as("Invalid saved dashboard created at.").isTrue();
        assertThat(compareDate(dashboard.getUpdatedAt(), dashboardSaved.getUpdatedAt())).as("Invalid saved dashboard updated at.").isTrue();
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Dashboard> optional = dashboardRepository.findById("6e0d09f0-ba5d-4571-8d09-f0ba5d7571c3");
        assertThat(optional).as("Dashboard to update not found").isPresent();
        assertThat(optional.get().getName()).as("Invalid saved dashboard name.").isEqualTo("Device dashboard");

        final Dashboard dashboard = optional.get();
        dashboard.setType("PLATFORM");
        dashboard.setReferenceType(ENVIRONMENT);
        dashboard.setReferenceId("1");
        dashboard.setName("New dashboard");
        dashboard.setQueryFilter("api:apiId");
        dashboard.setOrder(3);
        dashboard.setEnabled(true);
        dashboard.setDefinition("{\"def\": \"new value\"}");
        dashboard.setCreatedAt(new Date(1111111111111L));
        dashboard.setUpdatedAt(new Date(1000000000000L));

        int nbDashboardsBeforeUpdate = dashboardRepository.findByReferenceAndType(ENVIRONMENT, "DEFAULT", "PLATFORM").size();
        int nbAllDashboardsBeforeUpdate = dashboardRepository
            .findAll()
            .stream()
            .filter(d -> d.getType().equals("PLATFORM"))
            .toList()
            .size();
        dashboardRepository.update(dashboard);
        int nbDashboardsAfterUpdate = dashboardRepository.findByReferenceAndType(ENVIRONMENT, "DEFAULT", "PLATFORM").size();
        int nbAllDashboardsAfterUpdate = dashboardRepository.findAll().stream().filter(d -> d.getType().equals("PLATFORM")).toList().size();

        assertThat(nbDashboardsAfterUpdate).isEqualTo(nbDashboardsBeforeUpdate - 1);
        assertThat(nbAllDashboardsBeforeUpdate).isEqualTo(nbAllDashboardsAfterUpdate);

        Optional<Dashboard> optionalUpdated = dashboardRepository.findById("6e0d09f0-ba5d-4571-8d09-f0ba5d7571c3");
        assertThat(optionalUpdated).as("Dashboard to update not found").isPresent();

        final Dashboard dashboardSaved = optionalUpdated.get();
        assertThat(dashboardSaved.getReferenceId()).as("Invalid saved dashboard reference id.").isEqualTo("1");
        assertThat(dashboardSaved.getName()).as("Invalid saved dashboard name.").isEqualTo("New dashboard");
        assertThat(dashboardSaved.getQueryFilter()).as("Invalid saved dashboard query filter.").isEqualTo("api:apiId");
        assertThat(dashboardSaved.getOrder()).as("Invalid saved dashboard order.", 3).isEqualTo(3);
        assertThat(dashboardSaved.isEnabled()).as("Invalid saved dashboard enabled.").isTrue();
        assertThat(dashboardSaved.getDefinition()).as("Invalid saved dashboard definition.").isEqualTo("{\"def\": \"new value\"}");
        assertThat(compareDate(new Date(1111111111111L), dashboardSaved.getCreatedAt())).as("Invalid saved dashboard created at.").isTrue();
        assertThat(compareDate(new Date(1000000000000L), dashboardSaved.getUpdatedAt())).as("Invalid saved dashboard updated at.").isTrue();
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbDashboardsBeforeDeletion = dashboardRepository.findByReferenceAndType(ENVIRONMENT, "DEFAULT", "API").size();
        dashboardRepository.delete("7589ef82-02ae-4fc7-89ef-8202ae3fc7cf");
        int nbDashboardsAfterDeletion = dashboardRepository.findByReferenceAndType(ENVIRONMENT, "DEFAULT", "API").size();

        assertThat(nbDashboardsAfterDeletion).isEqualTo(nbDashboardsBeforeDeletion - 1);
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        var nbBeforeDeletion = dashboardRepository.findByReferenceAndType(ENVIRONMENT, "ToBeDeleted", "API").size();
        var deleted = dashboardRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", DashboardReferenceType.ENVIRONMENT).size();
        var nbAfterDeletion = dashboardRepository.findByReferenceAndType(ENVIRONMENT, "ToBeDeleted", "API").size();

        assertEquals(2, nbBeforeDeletion);
        assertEquals(2, deleted);
        assertEquals(0, nbAfterDeletion);
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
