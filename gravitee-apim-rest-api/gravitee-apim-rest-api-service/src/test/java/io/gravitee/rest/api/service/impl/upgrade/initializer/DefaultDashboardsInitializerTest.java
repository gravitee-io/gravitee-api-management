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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.DashboardService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class DefaultDashboardsInitializerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private EnvironmentService environmentService;

    private DefaultDashboardsInitializer cut;

    @BeforeEach
    void setUp() {
        cut = new DefaultDashboardsInitializer(dashboardService, environmentService);
    }

    @Test
    void should_create_dashboards() {
        when(environmentService.findAllOrInitialize()).thenReturn(
            Set.of(
                EnvironmentEntity.builder()
                    .organizationId("FAKE_ORG_1")
                    .id("ENV_WITH_DASHBOARDS")
                    .description("Contains dashboard")
                    .build(),
                EnvironmentEntity.builder()
                    .organizationId("FAKE_ORG_1")
                    .id("ENV_WITHOUT_DASHBOARDS_1")
                    .description("Must initialize dashboards")
                    .build(),
                EnvironmentEntity.builder()
                    .organizationId("FAKE_ORG_1")
                    .id("ENV_WITHOUT_DASHBOARDS_2")
                    .description("Must initialize dashboards")
                    .build(),
                EnvironmentEntity.builder()
                    .organizationId("FAKE_ORG_2")
                    .id("ENV_WITHOUT_DASHBOARDS_1")
                    .description("Must initialize dashboards")
                    .build()
            )
        );
        when(dashboardService.findAll()).thenReturn(
            List.of(
                DashboardEntity.builder().referenceType("ENVIRONMENT").referenceId("ENV_WITH_DASHBOARDS").build(),
                DashboardEntity.builder().referenceType("ORGANIZATION").referenceId("ENV_WITH_DASHBOARDS").build()
            )
        );
        cut.initialize();
        verify(dashboardService).initialize(eq(new ExecutionContext("FAKE_ORG_1", "ENV_WITHOUT_DASHBOARDS_1")));
        verify(dashboardService).initialize(eq(new ExecutionContext("FAKE_ORG_1", "ENV_WITHOUT_DASHBOARDS_2")));
        verify(dashboardService).initialize(eq(new ExecutionContext("FAKE_ORG_2", "ENV_WITHOUT_DASHBOARDS_1")));
    }

    @Test
    void should_use_correct_order() {
        assertThat(cut.getOrder()).isEqualTo(InitializerOrder.DEFAULT_DASHBOARDS_INITIALIZER);
    }
}
