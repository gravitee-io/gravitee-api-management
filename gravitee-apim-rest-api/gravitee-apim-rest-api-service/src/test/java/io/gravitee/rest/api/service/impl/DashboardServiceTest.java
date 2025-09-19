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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.DASHBOARD;
import static io.gravitee.rest.api.model.DashboardReferenceType.ENVIRONMENT;
import static io.gravitee.rest.api.model.DashboardType.API;
import static io.gravitee.rest.api.model.DashboardType.APPLICATION;
import static io.gravitee.rest.api.model.DashboardType.PLATFORM;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.model.Dashboard;
import io.gravitee.repository.management.model.DashboardType;
import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.model.NewDashboardEntity;
import io.gravitee.rest.api.model.UpdateDashboardEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.DashboardService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DashboardNotFoundException;
import java.util.Date;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DashboardServiceTest {

    private static final String DASHBOARD_ID = "id-dashboard";

    @InjectMocks
    private DashboardService dashboardService = new DashboardServiceImpl();

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private AuditService auditService;

    @Test
    void should_find_by_reference_and_type() throws TechnicalException {
        final Dashboard dashboard = Dashboard.builder()
            .id(DASHBOARD_ID)
            .name("NAME")
            .definition("DEFINITION")
            .order(1)
            .referenceId("REF_ID")
            .referenceType(ENVIRONMENT.name())
            .type(PLATFORM.name())
            .queryFilter("QUERY FILTER")
            .createdAt(new Date(1))
            .updatedAt(new Date(2))
            .build();
        when(dashboardRepository.findByReferenceAndType("ENVIRONMENT", "REF_ID", PLATFORM.name())).thenReturn(singletonList(dashboard));

        final List<DashboardEntity> dashboards = dashboardService.findByReferenceAndType(ENVIRONMENT, "REF_ID", DashboardType.PLATFORM);
        final DashboardEntity dashboardEntity = dashboards.iterator().next();
        assertThat(dashboardEntity.getId()).isEqualTo(DASHBOARD_ID);
        assertThat(dashboardEntity.getName()).isEqualTo("NAME");
        assertThat(dashboardEntity.getDefinition()).isEqualTo("DEFINITION");
        assertThat(dashboardEntity.getOrder()).isOne();
        assertThat(dashboardEntity.getReferenceId()).isEqualTo("REF_ID");
        assertThat(dashboardEntity.getReferenceType()).isEqualTo(ENVIRONMENT.name());
        assertThat(dashboardEntity.getType()).isEqualTo(PLATFORM.name());
        assertThat(dashboardEntity.getQueryFilter()).isEqualTo("QUERY FILTER");
        assertThat(dashboardEntity.getCreatedAt()).isEqualTo(new Date(1));
        assertThat(dashboardEntity.getUpdatedAt()).isEqualTo(new Date(2));
    }

    @Test
    void should_find_by_reference_and_id() throws TechnicalException {
        final Dashboard dashboard = Dashboard.builder()
            .id(DASHBOARD_ID)
            .name("NAME")
            .definition("DEFINITION")
            .order(1)
            .referenceId("ENV_ID")
            .referenceType(ENVIRONMENT.name())
            .type(PLATFORM.name())
            .queryFilter("QUERY FILTER")
            .createdAt(new Date(1))
            .updatedAt(new Date(2))
            .build();
        when(dashboardRepository.findByReferenceAndId("ENVIRONMENT", "ENV_ID", DASHBOARD_ID)).thenReturn(of(dashboard));

        final DashboardEntity dashboardEntity = dashboardService.findByReferenceAndId(ENVIRONMENT, "ENV_ID", DASHBOARD_ID);
        assertThat(dashboardEntity.getId()).isEqualTo(DASHBOARD_ID);
        assertThat(dashboardEntity.getName()).isEqualTo("NAME");
        assertThat(dashboardEntity.getDefinition()).isEqualTo("DEFINITION");
        assertThat(dashboardEntity.getOrder()).isOne();
        assertThat(dashboardEntity.getReferenceId()).isEqualTo("ENV_ID");
        assertThat(dashboardEntity.getReferenceType()).isEqualTo(ENVIRONMENT.name());
        assertThat(dashboardEntity.getType()).isEqualTo(PLATFORM.name());
        assertThat(dashboardEntity.getQueryFilter()).isEqualTo("QUERY FILTER");
        assertThat(dashboardEntity.getCreatedAt()).isEqualTo(new Date(1));
        assertThat(dashboardEntity.getUpdatedAt()).isEqualTo(new Date(2));
    }

    @Test
    void should_not_find_by_id() throws TechnicalException {
        when(dashboardRepository.findByReferenceAndId(ENVIRONMENT.name(), "ENV_ID", DASHBOARD_ID)).thenReturn(empty());
        Assertions.assertThatThrownBy(() -> dashboardService.findByReferenceAndId(ENVIRONMENT, "ENV_ID", DASHBOARD_ID)).isInstanceOf(
            DashboardNotFoundException.class
        );
    }

    @Test
    void should_find_all() throws TechnicalException {
        final Dashboard dashboard = Dashboard.builder()
            .id(DASHBOARD_ID)
            .name("NAME")
            .definition("DEFINITION")
            .order(1)
            .referenceId("REF_ID")
            .referenceType(ENVIRONMENT.name())
            .type(PLATFORM.name())
            .queryFilter("QUERY FILTER")
            .createdAt(new Date(1))
            .updatedAt(new Date(2))
            .build();
        when(dashboardRepository.findAll()).thenReturn(singleton(dashboard));

        final List<DashboardEntity> dashboards = dashboardService.findAll();
        final DashboardEntity dashboardEntity = dashboards.iterator().next();
        assertThat(dashboardEntity.getId()).isEqualTo(DASHBOARD_ID);
        assertThat(dashboardEntity.getName()).isEqualTo("NAME");
        assertThat(dashboardEntity.getDefinition()).isEqualTo("DEFINITION");
        assertThat(dashboardEntity.getOrder()).isOne();
        assertThat(dashboardEntity.getReferenceId()).isEqualTo("REF_ID");
        assertThat(dashboardEntity.getReferenceType()).isEqualTo(ENVIRONMENT.name());
        assertThat(dashboardEntity.getType()).isEqualTo(PLATFORM.name());
        assertThat(dashboardEntity.getQueryFilter()).isEqualTo("QUERY FILTER");
        assertThat(dashboardEntity.getCreatedAt()).isEqualTo(new Date(1));
        assertThat(dashboardEntity.getUpdatedAt()).isEqualTo(new Date(2));
    }

    @Test
    void should_create() throws TechnicalException {
        final NewDashboardEntity newDashboardEntity = NewDashboardEntity.builder()
            .name("NAME")
            .definition("DEFINITION")
            .referenceId("REF_ID")
            .referenceType(ENVIRONMENT)
            .type(PLATFORM)
            .queryFilter("QUERY FILTER")
            .build();

        final Dashboard createdDashboard = Dashboard.builder()
            .id(DASHBOARD_ID)
            .name("NAME")
            .definition("DEFINITION")
            .order(1)
            .referenceId("REF_ID")
            .referenceType(ENVIRONMENT.name())
            .type(PLATFORM.name())
            .queryFilter("QUERY FILTER")
            .createdAt(new Date())
            .updatedAt(new Date())
            .build();
        when(dashboardRepository.create(any())).thenReturn(createdDashboard);

        final Dashboard d = new Dashboard();
        d.setOrder(0);
        when(dashboardRepository.findByReferenceAndType("ENVIRONMENT", "DEFAULT", PLATFORM.name())).thenReturn(singletonList(d));

        final DashboardEntity dashboardEntity = dashboardService.create(GraviteeContext.getExecutionContext(), newDashboardEntity);

        assertThat(dashboardEntity.getId()).isNotNull();
        assertThat(dashboardEntity.getName()).isEqualTo("NAME");
        assertThat(dashboardEntity.getDefinition()).isEqualTo("DEFINITION");
        assertThat(dashboardEntity.getOrder()).isOne();
        assertThat(dashboardEntity.getReferenceId()).isEqualTo("REF_ID");
        assertThat(dashboardEntity.getReferenceType()).isEqualTo(ENVIRONMENT.name());
        assertThat(dashboardEntity.getType()).isEqualTo(PLATFORM.name());
        assertThat(dashboardEntity.getQueryFilter()).isEqualTo("QUERY FILTER");
        assertThat(dashboardEntity.getCreatedAt()).isNotNull();
        assertThat(dashboardEntity.getUpdatedAt()).isNotNull();

        verify(dashboardRepository, times(1)).create(
            argThat(
                argument ->
                    "NAME".equals(argument.getName()) &&
                    "DEFINITION".equals(argument.getDefinition()) &&
                    "REF_ID".equals(argument.getReferenceId()) &&
                    ENVIRONMENT.name().equals(argument.getReferenceType()) &&
                    PLATFORM.name().equals(argument.getType()) &&
                    "QUERY FILTER".equals(argument.getQueryFilter()) &&
                    Integer.valueOf(1).equals(argument.getOrder()) &&
                    !argument.getId().isEmpty() &&
                    argument.getCreatedAt() != null &&
                    argument.getUpdatedAt() != null
            )
        );
        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            eq(ImmutableMap.of(DASHBOARD, DASHBOARD_ID)),
            eq(Dashboard.AuditEvent.DASHBOARD_CREATED),
            any(Date.class),
            isNull(),
            any()
        );
    }

    @Test
    void should_update() throws TechnicalException {
        final UpdateDashboardEntity updateDashboardEntity = UpdateDashboardEntity.builder()
            .id(DASHBOARD_ID)
            .name("NAME")
            .definition("DEFINITION")
            .order(1)
            .referenceId("REF_ID")
            .referenceType(ENVIRONMENT)
            .type(PLATFORM)
            .queryFilter("QUERY FILTER")
            .build();

        final Dashboard updatedDashboard = Dashboard.builder()
            .id(DASHBOARD_ID)
            .name("NAME")
            .definition("DEFINITION")
            .order(1)
            .referenceId("REF_ID")
            .referenceType(ENVIRONMENT.name())
            .type(PLATFORM.name())
            .queryFilter("QUERY FILTER")
            .createdAt(new Date())
            .updatedAt(new Date())
            .build();
        when(dashboardRepository.update(any())).thenReturn(updatedDashboard);
        when(dashboardRepository.findByReferenceAndId(ENVIRONMENT.name(), "REF_ID", DASHBOARD_ID)).thenReturn(of(updatedDashboard));

        final DashboardEntity dashboardEntity = dashboardService.update(
            GraviteeContext.getExecutionContext(),
            ENVIRONMENT,
            "REF_ID",
            updateDashboardEntity
        );

        assertThat(dashboardEntity.getId()).isNotNull();
        assertThat(dashboardEntity.getName()).isEqualTo("NAME");
        assertThat(dashboardEntity.getDefinition()).isEqualTo("DEFINITION");
        assertThat(dashboardEntity.getOrder()).isOne();
        assertThat(dashboardEntity.getReferenceId()).isEqualTo("REF_ID");
        assertThat(dashboardEntity.getReferenceType()).isEqualTo("ENVIRONMENT");
        assertThat(dashboardEntity.getType()).isEqualTo(PLATFORM.name());
        assertThat(dashboardEntity.getQueryFilter()).isEqualTo("QUERY FILTER");
        assertThat(dashboardEntity.getCreatedAt()).isNotNull();
        assertThat(dashboardEntity.getUpdatedAt()).isNotNull();

        verify(dashboardRepository, times(1)).update(
            argThat(
                argument ->
                    "NAME".equals(argument.getName()) &&
                    "DEFINITION".equals(argument.getDefinition()) &&
                    "REF_ID".equals(argument.getReferenceId()) &&
                    ENVIRONMENT.name().equals(argument.getReferenceType()) &&
                    PLATFORM.name().equals(argument.getType()) &&
                    "QUERY FILTER".equals(argument.getQueryFilter()) &&
                    Integer.valueOf(1).equals(argument.getOrder()) &&
                    DASHBOARD_ID.equals(argument.getId()) &&
                    argument.getCreatedAt() == null &&
                    argument.getUpdatedAt() != null
            )
        );
        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            eq(ImmutableMap.of(DASHBOARD, DASHBOARD_ID)),
            eq(Dashboard.AuditEvent.DASHBOARD_UPDATED),
            any(Date.class),
            any(),
            any()
        );
    }

    @Test
    void should_not_update() throws TechnicalException {
        final UpdateDashboardEntity updateDashboardEntity = UpdateDashboardEntity.builder().id(DASHBOARD_ID).build();

        when(dashboardRepository.findByReferenceAndId(ENVIRONMENT.name(), "REF_ID", DASHBOARD_ID)).thenReturn(empty());

        Assertions.assertThatThrownBy(() ->
            dashboardService.update(GraviteeContext.getExecutionContext(), ENVIRONMENT, "REF_ID", updateDashboardEntity)
        ).isInstanceOf(DashboardNotFoundException.class);
    }

    @Test
    void should_delete() throws TechnicalException {
        final Dashboard dashboard = mock(Dashboard.class);
        when(dashboardRepository.findByReferenceAndId(ENVIRONMENT.name(), "REF_ID", DASHBOARD_ID)).thenReturn(of(dashboard));

        dashboardService.delete(GraviteeContext.getExecutionContext(), ENVIRONMENT, "REF_ID", DASHBOARD_ID);

        verify(dashboardRepository, times(1)).delete(DASHBOARD_ID);
        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            eq(ImmutableMap.of(DASHBOARD, DASHBOARD_ID)),
            eq(Dashboard.AuditEvent.DASHBOARD_DELETED),
            any(Date.class),
            isNull(),
            eq(dashboard)
        );
    }

    @SneakyThrows
    @Test
    void should_find_all_by_reference() {
        var firstApiDashboard = Dashboard.builder()
            .id("firstApiDashboard")
            .referenceType(ENVIRONMENT.name())
            .referenceId("ENV_ID")
            .type(API.name())
            .order(0)
            .build();
        var secondApiDashboard = firstApiDashboard.toBuilder().id("secondApiDashboard").order(1).build();
        var thirdApiDashboard = firstApiDashboard.toBuilder().id("thirdApiDashboard").order(2).build();
        var firstAppDashboard = firstApiDashboard.toBuilder().id("firstAppDashboard").type(APPLICATION.name()).build();
        var secondAppDashboard = secondApiDashboard.toBuilder().id("secondAppDashboard").type(APPLICATION.name()).build();
        var firstPlatformDashboard = firstApiDashboard.toBuilder().id("firstPlatformDashboard").type(PLATFORM.name()).build();

        when(dashboardRepository.findByReference(ENVIRONMENT.name(), "ENV_ID"))
            // returns an unordered list
            .thenReturn(
                List.of(
                    secondApiDashboard,
                    firstPlatformDashboard,
                    thirdApiDashboard,
                    firstAppDashboard,
                    firstApiDashboard,
                    secondAppDashboard
                )
            );
        final List<DashboardEntity> result = dashboardService.findAllByReference(ENVIRONMENT, "ENV_ID");
        assertThat(result)
            .hasSize(6)
            .extracting(DashboardEntity::getId)
            .containsExactly(
                "firstApiDashboard",
                "secondApiDashboard",
                "thirdApiDashboard",
                "firstAppDashboard",
                "secondAppDashboard",
                "firstPlatformDashboard"
            );
    }

    @SneakyThrows
    @Test
    void should_initialize_environment() {
        when(dashboardRepository.findByReferenceAndType(eq(ENVIRONMENT.name()), eq("ENV_ID"), any())).thenReturn(List.of());
        when(dashboardRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        dashboardService.initialize(new ExecutionContext("ORG_ID", "ENV_ID"));
        final ArgumentCaptor<Dashboard> dashboardArgumentCaptor = ArgumentCaptor.forClass(Dashboard.class);
        verify(dashboardRepository, times(9)).create(dashboardArgumentCaptor.capture());

        assertThat(dashboardArgumentCaptor.getAllValues())
            .hasSize(9)
            .extracting(Dashboard::getReferenceId, Dashboard::getType, Dashboard::getName)
            .containsExactly(
                Tuple.tuple("ENV_ID", PLATFORM.name(), "Global dashboard"),
                Tuple.tuple("ENV_ID", PLATFORM.name(), "Geo dashboard"),
                Tuple.tuple("ENV_ID", PLATFORM.name(), "User dashboard"),
                Tuple.tuple("ENV_ID", API.name(), "Global dashboard"),
                Tuple.tuple("ENV_ID", API.name(), "Geo dashboard"),
                Tuple.tuple("ENV_ID", API.name(), "User dashboard"),
                Tuple.tuple("ENV_ID", APPLICATION.name(), "Global dashboard"),
                Tuple.tuple("ENV_ID", APPLICATION.name(), "Geo dashboard"),
                Tuple.tuple("ENV_ID", APPLICATION.name(), "User dashboard")
            );
    }
}
