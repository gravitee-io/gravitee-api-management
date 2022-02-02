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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.DASHBOARD;
import static io.gravitee.rest.api.model.DashboardReferenceType.PLATFORM;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.model.Dashboard;
import io.gravitee.repository.management.model.DashboardReferenceType;
import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.model.NewDashboardEntity;
import io.gravitee.rest.api.model.UpdateDashboardEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.DashboardService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DashboardNotFoundException;
import io.gravitee.rest.api.service.impl.DashboardServiceImpl;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DashboardServiceTest {

    private static final String DASHBOARD_ID = "id-dashboard";

    @InjectMocks
    private DashboardService dashboardService = new DashboardServiceImpl();

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private AuditService auditService;

    @Test
    public void shouldFindByReferenceType() throws TechnicalException {
        final Dashboard dashboard = mock(Dashboard.class);
        when(dashboard.getId()).thenReturn(DASHBOARD_ID);
        when(dashboard.getName()).thenReturn("NAME");
        when(dashboard.getDefinition()).thenReturn("DEFINITION");
        when(dashboard.getOrder()).thenReturn(1);
        when(dashboard.getReferenceId()).thenReturn("REF_ID");
        when(dashboard.getReferenceType()).thenReturn(PLATFORM.name());
        when(dashboard.getQueryFilter()).thenReturn("QUERY FILTER");
        when(dashboard.getCreatedAt()).thenReturn(new Date(1));
        when(dashboard.getUpdatedAt()).thenReturn(new Date(2));
        when(dashboardRepository.findByReferenceType(PLATFORM.name())).thenReturn(singletonList(dashboard));

        final List<DashboardEntity> dashboards = dashboardService.findByReferenceType(DashboardReferenceType.PLATFORM);
        final DashboardEntity dashboardEntity = dashboards.iterator().next();
        assertEquals(DASHBOARD_ID, dashboardEntity.getId());
        assertEquals("NAME", dashboardEntity.getName());
        assertEquals("DEFINITION", dashboardEntity.getDefinition());
        assertEquals(1, dashboardEntity.getOrder());
        assertEquals("REF_ID", dashboardEntity.getReferenceId());
        assertEquals(PLATFORM.name(), dashboardEntity.getReferenceType());
        assertEquals("QUERY FILTER", dashboardEntity.getQueryFilter());
        assertEquals(new Date(1), dashboardEntity.getCreatedAt());
        assertEquals(new Date(2), dashboardEntity.getUpdatedAt());
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        final Dashboard dashboard = mock(Dashboard.class);
        when(dashboard.getId()).thenReturn(DASHBOARD_ID);
        when(dashboard.getName()).thenReturn("NAME");
        when(dashboard.getDefinition()).thenReturn("DEFINITION");
        when(dashboard.getOrder()).thenReturn(1);
        when(dashboard.getReferenceId()).thenReturn("REF_ID");
        when(dashboard.getReferenceType()).thenReturn(PLATFORM.name());
        when(dashboard.getQueryFilter()).thenReturn("QUERY FILTER");
        when(dashboard.getCreatedAt()).thenReturn(new Date(1));
        when(dashboard.getUpdatedAt()).thenReturn(new Date(2));
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(of(dashboard));

        final DashboardEntity dashboardEntity = dashboardService.findById(DASHBOARD_ID);
        assertEquals(DASHBOARD_ID, dashboardEntity.getId());
        assertEquals("NAME", dashboardEntity.getName());
        assertEquals("DEFINITION", dashboardEntity.getDefinition());
        assertEquals(1, dashboardEntity.getOrder());
        assertEquals("REF_ID", dashboardEntity.getReferenceId());
        assertEquals(PLATFORM.name(), dashboardEntity.getReferenceType());
        assertEquals("QUERY FILTER", dashboardEntity.getQueryFilter());
        assertEquals(new Date(1), dashboardEntity.getCreatedAt());
        assertEquals(new Date(2), dashboardEntity.getUpdatedAt());
    }

    @Test(expected = DashboardNotFoundException.class)
    public void shouldNotFindById() throws TechnicalException {
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(empty());
        dashboardService.findById(DASHBOARD_ID);
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        final Dashboard dashboard = mock(Dashboard.class);
        when(dashboard.getId()).thenReturn(DASHBOARD_ID);
        when(dashboard.getName()).thenReturn("NAME");
        when(dashboard.getDefinition()).thenReturn("DEFINITION");
        when(dashboard.getOrder()).thenReturn(1);
        when(dashboard.getReferenceId()).thenReturn("REF_ID");
        when(dashboard.getReferenceType()).thenReturn(PLATFORM.name());
        when(dashboard.getQueryFilter()).thenReturn("QUERY FILTER");
        when(dashboard.getCreatedAt()).thenReturn(new Date(1));
        when(dashboard.getUpdatedAt()).thenReturn(new Date(2));
        when(dashboardRepository.findAll()).thenReturn(singleton(dashboard));

        final List<DashboardEntity> dashboards = dashboardService.findAll();
        final DashboardEntity dashboardEntity = dashboards.iterator().next();
        assertEquals(DASHBOARD_ID, dashboardEntity.getId());
        assertEquals("NAME", dashboardEntity.getName());
        assertEquals("DEFINITION", dashboardEntity.getDefinition());
        assertEquals(1, dashboardEntity.getOrder());
        assertEquals("REF_ID", dashboardEntity.getReferenceId());
        assertEquals(PLATFORM.name(), dashboardEntity.getReferenceType());
        assertEquals("QUERY FILTER", dashboardEntity.getQueryFilter());
        assertEquals(new Date(1), dashboardEntity.getCreatedAt());
        assertEquals(new Date(2), dashboardEntity.getUpdatedAt());
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        final NewDashboardEntity newDashboardEntity = new NewDashboardEntity();
        newDashboardEntity.setName("NAME");
        newDashboardEntity.setDefinition("DEFINITION");
        newDashboardEntity.setReferenceId("REF_ID");
        newDashboardEntity.setReferenceType(PLATFORM);
        newDashboardEntity.setQueryFilter("QUERY FILTER");

        final Dashboard createdDashboard = new Dashboard();
        createdDashboard.setId(DASHBOARD_ID);
        createdDashboard.setName("NAME");
        createdDashboard.setDefinition("DEFINITION");
        createdDashboard.setOrder(1);
        createdDashboard.setReferenceId("REF_ID");
        createdDashboard.setReferenceType(PLATFORM.name());
        createdDashboard.setQueryFilter("QUERY FILTER");
        createdDashboard.setCreatedAt(new Date());
        createdDashboard.setUpdatedAt(new Date());
        when(dashboardRepository.create(any())).thenReturn(createdDashboard);

        final Dashboard d = new Dashboard();
        d.setOrder(0);
        when(dashboardRepository.findByReferenceType(PLATFORM.name())).thenReturn(singletonList(d));

        final DashboardEntity dashboardEntity = dashboardService.create(newDashboardEntity);

        assertNotNull(dashboardEntity.getId());
        assertEquals("NAME", dashboardEntity.getName());
        assertEquals("DEFINITION", dashboardEntity.getDefinition());
        assertEquals(1, dashboardEntity.getOrder());
        assertEquals("REF_ID", dashboardEntity.getReferenceId());
        assertEquals(PLATFORM.name(), dashboardEntity.getReferenceType());
        assertEquals("QUERY FILTER", dashboardEntity.getQueryFilter());
        assertNotNull(dashboardEntity.getCreatedAt());
        assertNotNull(dashboardEntity.getUpdatedAt());

        final Dashboard dashboard = new Dashboard();
        dashboard.setName("NAME");
        dashboard.setDefinition("DEFINITION");
        dashboard.setOrder(1);
        dashboard.setReferenceId("REF_ID");
        dashboard.setReferenceType(PLATFORM.name());
        dashboard.setQueryFilter("QUERY FILTER");

        verify(dashboardRepository, times(1))
            .create(
                argThat(
                    argument ->
                        "NAME".equals(argument.getName()) &&
                        "DEFINITION".equals(argument.getDefinition()) &&
                        "REF_ID".equals(argument.getReferenceId()) &&
                        PLATFORM.name().equals(argument.getReferenceType()) &&
                        "QUERY FILTER".equals(argument.getQueryFilter()) &&
                        Integer.valueOf(1).equals(argument.getOrder()) &&
                        !argument.getId().isEmpty() &&
                        argument.getCreatedAt() != null &&
                        argument.getUpdatedAt() != null
                )
            );
        verify(auditService, times(1))
            .createEnvironmentAuditLog(
                eq(GraviteeContext.getCurrentEnvironment()),
                eq(ImmutableMap.of(DASHBOARD, DASHBOARD_ID)),
                eq(Dashboard.AuditEvent.DASHBOARD_CREATED),
                any(Date.class),
                isNull(),
                any()
            );
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        final UpdateDashboardEntity updateDashboardEntity = new UpdateDashboardEntity();
        updateDashboardEntity.setId(DASHBOARD_ID);
        updateDashboardEntity.setName("NAME");
        updateDashboardEntity.setDefinition("DEFINITION");
        updateDashboardEntity.setOrder(1);
        updateDashboardEntity.setReferenceId("REF_ID");
        updateDashboardEntity.setReferenceType(PLATFORM);
        updateDashboardEntity.setQueryFilter("QUERY FILTER");

        final Dashboard updatedDashboard = new Dashboard();
        updatedDashboard.setId(DASHBOARD_ID);
        updatedDashboard.setName("NAME");
        updatedDashboard.setDefinition("DEFINITION");
        updatedDashboard.setOrder(1);
        updatedDashboard.setReferenceId("REF_ID");
        updatedDashboard.setReferenceType(PLATFORM.name());
        updatedDashboard.setQueryFilter("QUERY FILTER");
        updatedDashboard.setCreatedAt(new Date());
        updatedDashboard.setUpdatedAt(new Date());
        when(dashboardRepository.update(any())).thenReturn(updatedDashboard);
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(of(updatedDashboard));

        final DashboardEntity dashboardEntity = dashboardService.update(updateDashboardEntity);

        assertNotNull(dashboardEntity.getId());
        assertEquals("NAME", dashboardEntity.getName());
        assertEquals("DEFINITION", dashboardEntity.getDefinition());
        assertEquals(1, dashboardEntity.getOrder());
        assertEquals("REF_ID", dashboardEntity.getReferenceId());
        assertEquals(PLATFORM.name(), dashboardEntity.getReferenceType());
        assertEquals("QUERY FILTER", dashboardEntity.getQueryFilter());
        assertNotNull(dashboardEntity.getCreatedAt());
        assertNotNull(dashboardEntity.getUpdatedAt());

        final Dashboard dashboard = new Dashboard();
        dashboard.setName("NAME");
        dashboard.setDefinition("DEFINITION");
        dashboard.setOrder(1);
        dashboard.setReferenceId("REF_ID");
        dashboard.setReferenceType(PLATFORM.name());
        dashboard.setQueryFilter("QUERY FILTER");

        verify(dashboardRepository, times(1))
            .update(
                argThat(
                    argument ->
                        "NAME".equals(argument.getName()) &&
                        "DEFINITION".equals(argument.getDefinition()) &&
                        "REF_ID".equals(argument.getReferenceId()) &&
                        PLATFORM.name().equals(argument.getReferenceType()) &&
                        "QUERY FILTER".equals(argument.getQueryFilter()) &&
                        Integer.valueOf(1).equals(argument.getOrder()) &&
                        DASHBOARD_ID.equals(argument.getId()) &&
                        argument.getCreatedAt() == null &&
                        argument.getUpdatedAt() != null
                )
            );
        verify(auditService, times(1))
            .createEnvironmentAuditLog(
                eq(GraviteeContext.getCurrentEnvironment()),
                eq(ImmutableMap.of(DASHBOARD, DASHBOARD_ID)),
                eq(Dashboard.AuditEvent.DASHBOARD_UPDATED),
                any(Date.class),
                any(),
                any()
            );
    }

    @Test(expected = DashboardNotFoundException.class)
    public void shouldNotUpdate() throws TechnicalException {
        final UpdateDashboardEntity updateDashboardEntity = new UpdateDashboardEntity();
        updateDashboardEntity.setId(DASHBOARD_ID);

        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(empty());

        dashboardService.update(updateDashboardEntity);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        final Dashboard dashboard = mock(Dashboard.class);
        when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(of(dashboard));

        dashboardService.delete(DASHBOARD_ID);

        verify(dashboardRepository, times(1)).delete(DASHBOARD_ID);
        verify(auditService, times(1))
            .createEnvironmentAuditLog(
                eq(GraviteeContext.getCurrentEnvironment()),
                eq(ImmutableMap.of(DASHBOARD, DASHBOARD_ID)),
                eq(Dashboard.AuditEvent.DASHBOARD_DELETED),
                any(Date.class),
                isNull(),
                eq(dashboard)
            );
    }
}
