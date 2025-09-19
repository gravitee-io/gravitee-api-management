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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.resource.param.AnalyticsType;
import io.gravitee.rest.api.model.analytics.HitsAnalytics;
import io.gravitee.rest.api.model.analytics.query.CountQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class PlatformAnalyticsResource_Admin_GetTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "platform/analytics/";
    }

    @Before
    public void init() {
        reset(applicationService, apiAuthorizationServiceV4, permissionService);
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_PLATFORM,
                "DEFAULT",
                RolePermissionAction.READ
            )
        ).thenReturn(true);
    }

    @Test
    public void should_return_no_content_when_user_admin_and_application_analytics() {
        when(applicationService.findIdsByEnvironment(GraviteeContext.getExecutionContext())).thenReturn(Set.of());

        final Response response = envTarget()
            .queryParam("to", 122222L)
            .queryParam("from", 111111L)
            .queryParam("interval", 1000L)
            .queryParam("type", AnalyticsType.COUNT)
            .queryParam("field", "application")
            .request()
            .get();

        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        verify(permissionService, times(1)).hasPermission(
            GraviteeContext.getExecutionContext(),
            RolePermission.ENVIRONMENT_PLATFORM,
            "DEFAULT",
            RolePermissionAction.READ
        );
        verify(applicationService).findIdsByEnvironment(any(ExecutionContext.class));
    }

    @Test
    public void should_return_no_content_when_user_admin_and_api_analytics() {
        when(apiAuthorizationServiceV4.findIdsByEnvironment(GraviteeContext.getCurrentEnvironment())).thenReturn(Set.of());

        final Response response = envTarget()
            .queryParam("to", 122222L)
            .queryParam("from", 111111L)
            .queryParam("interval", 1000L)
            .queryParam("type", AnalyticsType.COUNT)
            .request()
            .get();

        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        verify(permissionService, times(1)).hasPermission(
            GraviteeContext.getExecutionContext(),
            RolePermission.ENVIRONMENT_PLATFORM,
            "DEFAULT",
            RolePermissionAction.READ
        );
        verify(apiAuthorizationServiceV4).findIdsByEnvironment(any());
    }

    @Test
    public void should_return_analytics_when_user_admin_and_application_analytics() {
        when(applicationService.findIdsByEnvironment(GraviteeContext.getExecutionContext())).thenReturn(Set.of("app-1"));

        HitsAnalytics analytics = new HitsAnalytics();
        analytics.setHits(100L);
        when(analyticsService.execute(any(ExecutionContext.class), any(CountQuery.class))).thenReturn(analytics);

        final Response response = envTarget()
            .queryParam("to", 122222L)
            .queryParam("from", 111111L)
            .queryParam("interval", 1000L)
            .queryParam("type", AnalyticsType.COUNT)
            .queryParam("field", "application")
            .request()
            .get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var body = response.readEntity(HitsAnalytics.class);
        assertEquals(100L, body.getHits());

        verify(permissionService, times(1)).hasPermission(
            GraviteeContext.getExecutionContext(),
            RolePermission.ENVIRONMENT_PLATFORM,
            "DEFAULT",
            RolePermissionAction.READ
        );
        verify(applicationService).findIdsByEnvironment(any(ExecutionContext.class));
    }

    @Test
    public void should_return_analytics_when_user_admin_and_api_analytics() {
        when(apiAuthorizationServiceV4.findIdsByEnvironment(GraviteeContext.getCurrentEnvironment())).thenReturn(Set.of("api-1"));

        HitsAnalytics analytics = new HitsAnalytics();
        analytics.setHits(100L);
        when(analyticsService.execute(any(ExecutionContext.class), any(CountQuery.class))).thenReturn(analytics);

        final Response response = envTarget()
            .queryParam("to", 122222L)
            .queryParam("from", 111111L)
            .queryParam("interval", 1000L)
            .queryParam("type", AnalyticsType.COUNT)
            .request()
            .get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var body = response.readEntity(HitsAnalytics.class);
        assertEquals(100L, body.getHits());

        verify(permissionService, times(1)).hasPermission(
            GraviteeContext.getExecutionContext(),
            RolePermission.ENVIRONMENT_PLATFORM,
            "DEFAULT",
            RolePermissionAction.READ
        );
        verify(apiAuthorizationServiceV4).findIdsByEnvironment(any());
    }
}
