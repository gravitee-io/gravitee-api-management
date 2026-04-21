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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.dashboard.exception.DashboardNotFoundException;
import io.gravitee.apim.core.dashboard.use_case.GetDashboardUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.portal.rest.fixture.PortalAnalyticsDashboardFixtures;
import io.gravitee.rest.api.portal.rest.model.AnalyticsDashboard;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@code GET /portal/environments/{envId}/analytics/dashboards/{dashboardId}} detail endpoint.
 *
 * @author GraviteeSource Team
 */
@DisplayName("Portal analytics dashboard detail")
public class PortalAnalyticsDashboardResourceTest extends AbstractResourceTest {

    @Autowired
    private GetDashboardUseCase getDashboardUseCase;

    @Override
    protected String contextPath() {
        return "analytics/dashboards";
    }

    @BeforeEach
    void setUp() {
        resetAllMocks();
        reset(getDashboardUseCase);
        when(
            parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_ANALYTICS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);
    }

    @Test
    @DisplayName("returns dashboard body and delegates to GetDashboardUseCase")
    void returnsOneDashboard() {
        when(getDashboardUseCase.execute(any())).thenReturn(
            new GetDashboardUseCase.Output(PortalAnalyticsDashboardFixtures.aDashboard("dashboard-1", "Dashboard 1"))
        );

        final var response = target().path("dashboard-1").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        final var dashboard = response.readEntity(AnalyticsDashboard.class);
        assertEquals("dashboard-1", dashboard.getId());
        assertEquals("Dashboard 1", dashboard.getName());
        assertNotNull(dashboard.getWidgets());
        assertEquals("stats", Objects.requireNonNull(dashboard.getWidgets().getFirst().getType()).getValue());

        verify(getDashboardUseCase).execute(any());
    }

    @Test
    @DisplayName("404 when dashboard does not exist")
    void notFoundWhenMissing() {
        when(getDashboardUseCase.execute(any())).thenThrow(new DashboardNotFoundException("missing-dashboard"));

        final var response = target().path("missing-dashboard").request().get();

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    @DisplayName("404 when dashboard belongs to another environment (prevents cross-env leak)")
    void notFoundWhenWrongEnvironment() {
        when(getDashboardUseCase.execute(any())).thenReturn(
            new GetDashboardUseCase.Output(
                PortalAnalyticsDashboardFixtures.aDashboard("dashboard-1", "Dashboard 1").toBuilder().environmentId("OTHER").build()
            )
        );

        final var response = target().path("dashboard-1").request().get();

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    @DisplayName("403 when PORTAL_NEXT_ANALYTICS_ENABLED is false — use case is not invoked")
    void forbiddenWhenAnalyticsDisabled() {
        when(
            parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_ANALYTICS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(false);

        final var response = target().path("dashboard-1").request().get();

        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        verify(getDashboardUseCase, never()).execute(any());
    }
}
