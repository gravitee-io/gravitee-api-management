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

import io.gravitee.apim.core.dashboard.use_case.ListDashboardsUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.portal.rest.fixture.PortalAnalyticsDashboardFixtures;
import io.gravitee.rest.api.portal.rest.model.AnalyticsDashboardsResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@code GET /portal/environments/{envId}/analytics/dashboards} list endpoint.
 *
 * @author GraviteeSource Team
 */
@DisplayName("Portal analytics dashboards list")
public class PortalAnalyticsDashboardsResourceTest extends AbstractResourceTest {

    @Autowired
    private ListDashboardsUseCase listDashboardsUseCase;

    @Override
    protected String contextPath() {
        return "analytics/dashboards";
    }

    @BeforeEach
    void setUp() {
        resetAllMocks();
        reset(listDashboardsUseCase);
        when(
            parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_ANALYTICS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);
    }

    @Test
    @DisplayName("returns paginated data + metadata + links and delegates to ListDashboardsUseCase")
    void returnsPaginatedList() {
        when(listDashboardsUseCase.execute(any())).thenReturn(
            new ListDashboardsUseCase.Output(
                List.of(
                    PortalAnalyticsDashboardFixtures.aDashboard("dashboard-1", "Dashboard 1"),
                    PortalAnalyticsDashboardFixtures.aDashboard("dashboard-2", "Dashboard 2")
                )
            )
        );

        final var response = target().queryParam("page", 1).queryParam("size", 10).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        final var dashboardsResponse = response.readEntity(AnalyticsDashboardsResponse.class);
        assertNotNull(dashboardsResponse.getData());
        assertEquals(2, dashboardsResponse.getData().size());
        assertEquals("dashboard-1", dashboardsResponse.getData().get(0).getId());
        assertEquals("Dashboard 2", dashboardsResponse.getData().get(1).getName());
        assertNotNull(dashboardsResponse.getMetadata());
        assertNotNull(dashboardsResponse.getLinks());

        verify(listDashboardsUseCase).execute(any());
    }

    @Test
    @DisplayName("403 when PORTAL_NEXT_ANALYTICS_ENABLED is false — use case is not invoked")
    void forbiddenWhenAnalyticsDisabled() {
        when(
            parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_ANALYTICS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(false);

        final var response = target().request().get();

        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        verify(listDashboardsUseCase, never()).execute(any());
    }
}
