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
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.model.Dashboard;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DashboardRepositoryMock extends AbstractRepositoryMock<DashboardRepository> {

    public DashboardRepositoryMock() {
        super(DashboardRepository.class);
    }

    @Override
    protected void prepare(DashboardRepository dashboardRepository) throws Exception {
        final Dashboard dashboard1 = mockDashboard(
            "2535437f-ee99-4ab8-b543-7fee995ab847",
            "PLATFORM",
            "DEFAULT",
            "Global dashboard",
            null,
            0,
            true,
            "{\"def\": \"value\"}",
            false
        );
        final Dashboard dashboard2 = mockDashboard(
            "4eeb1c56-6f4a-4925-ab1c-566f4aa925b8",
            "PLATFORM",
            "DEFAULT",
            "Geo dashboard",
            null,
            1,
            true,
            "{\"def\": \"value\"}",
            false
        );
        final Dashboard dashboard3 = mockDashboard(
            "6e0d09f0-ba5d-4571-8d09-f0ba5d7571c3",
            "PLATFORM",
            "DEFAULT",
            "Device dashboard",
            null,
            2,
            false,
            "{\"def\": \"value\"}",
            false
        );
        final Dashboard dashboard4 = mockDashboard(
            "2535437f-ee99-4ab8-b543-7fee995ab847",
            "API",
            "DEFAULT",
            "Global dashboard",
            null,
            0,
            true,
            "{\"def\": \"value\"}",
            false
        );

        final Dashboard createdDashboard = mockDashboard(
            "new-dashboard",
            "API",
            "DEFAULT",
            "Dashboard name",
            "api:apiId",
            1,
            true,
            "{\"def\": \"value\"}",
            false
        );
        final Dashboard updatedDashboard = mockDashboard(
            "6e0d09f0-ba5d-4571-8d09-f0ba5d7571c3",
            "PLATFORM",
            "1",
            "New dashboard",
            "api:apiId",
            3,
            true,
            "{\"def\": \"new value\"}",
            true
        );

        when(dashboardRepository.create(any(Dashboard.class))).thenReturn(createdDashboard);

        when(dashboardRepository.findAll()).thenReturn(newSet(dashboard1, dashboard2, dashboard3, dashboard4));
        when(dashboardRepository.findById("new-dashboard")).thenReturn(of(createdDashboard));
        when(dashboardRepository.findById("6e0d09f0-ba5d-4571-8d09-f0ba5d7571c3")).thenReturn(of(dashboard3), of(updatedDashboard));
        when(dashboardRepository.findByReferenceType("PLATFORM")).thenReturn(asList(dashboard1, dashboard2, dashboard3));
        when(dashboardRepository.findByReferenceType("API"))
            .thenReturn(singletonList(dashboard4), asList(dashboard4, createdDashboard), singletonList(dashboard4), emptyList());
        when(dashboardRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());
    }

    private Dashboard mockDashboard(
        final String id,
        final String referenceType,
        final String referenceId,
        final String name,
        final String queryFilter,
        final int order,
        final boolean enabled,
        final String definition,
        final boolean switchDates
    ) {
        final Dashboard dashboard = mock(Dashboard.class);
        when(dashboard.getId()).thenReturn(id);
        when(dashboard.getReferenceType()).thenReturn(referenceType);
        when(dashboard.getReferenceId()).thenReturn(referenceId);
        when(dashboard.getName()).thenReturn(name);
        when(dashboard.getQueryFilter()).thenReturn(queryFilter);
        when(dashboard.getOrder()).thenReturn(order);
        when(dashboard.isEnabled()).thenReturn(enabled);
        when(dashboard.getDefinition()).thenReturn(definition);
        if (switchDates) {
            when(dashboard.getCreatedAt()).thenReturn(new Date(1111111111111L));
            when(dashboard.getUpdatedAt()).thenReturn(new Date(1000000000000L));
        } else {
            when(dashboard.getCreatedAt()).thenReturn(new Date(1000000000000L));
            when(dashboard.getUpdatedAt()).thenReturn(new Date(1111111111111L));
        }
        return dashboard;
    }
}
