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

import static io.gravitee.rest.api.model.permissions.RolePermission.API_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.analytics.HistogramAnalytics;
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.model.analytics.query.StatsAnalytics;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentAnalyticsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "analytics";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAdminAuthenticationFilter.class);
    }

    @Before
    public void init() {
        reset(analyticsService, applicationService, apiService, apiAuthorizationService);
    }

    @Test
    public void shouldGetEmptyHistoAnalyticsWhenNotAdminAndNoApp() {
        when(applicationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Collections.emptySet());

        Response response = envTarget()
            .queryParam("type", "date_histo")
            .queryParam("field", "application")
            .queryParam("interval", 1000)
            .queryParam("to", 1000)
            .request()
            .get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        HistogramAnalytics analytics = response.readEntity(HistogramAnalytics.class);
        assertThat(analytics.getValues()).isNull();
        assertThat(analytics.getTimestamp()).isNull();
    }

    @Test
    public void shouldGetEmptyTopHitsAnalyticsWhenNotAdminAndNoApp() {
        when(applicationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Collections.emptySet());

        Response response = envTarget()
            .queryParam("type", "group_by")
            .queryParam("field", "application")
            .queryParam("interval", 1000)
            .queryParam("to", 1000)
            .request()
            .get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        TopHitsAnalytics analytics = response.readEntity(TopHitsAnalytics.class);
        assertThat(analytics.getValues()).isNull();
        assertThat(analytics.getMetadata()).isNull();
    }

    @Test
    public void shouldGetEmptyCountAnalyticsWhenNotAdminAndNoApp() {
        when(applicationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Collections.emptySet());

        Response response = envTarget()
            .queryParam("type", "count")
            .queryParam("field", "application")
            .queryParam("interval", 1000)
            .queryParam("to", 1000)
            .request()
            .get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        StatsAnalytics analytics = response.readEntity(StatsAnalytics.class);
        assertThat(analytics.getCount()).isEqualTo(0);
    }

    @Test
    public void shouldGetEmptyStatsAnalyticsWhenNotAdminAndNoApp() {
        when(applicationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Collections.emptySet());

        Response response = envTarget()
            .queryParam("type", "stats")
            .queryParam("field", "application")
            .queryParam("interval", 1000)
            .queryParam("to", 1000)
            .request()
            .get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        StatsAnalytics analytics = response.readEntity(StatsAnalytics.class);
        assertThat(analytics.getAvg()).isNull();
        assertThat(analytics.getCount()).isNull();
    }

    @Test
    public void shouldGetEmptyHistoAnalyticsWhenNotAdminAndNoApi() {
        when(apiAuthorizationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(null), eq(true)))
            .thenReturn(Collections.emptySet());

        Response response = envTarget()
            .queryParam("type", "date_histo")
            .queryParam("field", "api")
            .queryParam("interval", 1000)
            .queryParam("to", 1000)
            .request()
            .get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        HistogramAnalytics analytics = response.readEntity(HistogramAnalytics.class);
        assertThat(analytics.getValues()).isNull();
        assertThat(analytics.getTimestamp()).isNull();
    }

    @Test
    public void shouldGetEmptyTopHitsAnalyticsWhenNotAdminAndNoApi() {
        when(apiAuthorizationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(null), eq(true)))
            .thenReturn(Collections.emptySet());

        Response response = envTarget()
            .queryParam("type", "group_by")
            .queryParam("field", "api")
            .queryParam("interval", 1000)
            .queryParam("to", 1000)
            .request()
            .get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        TopHitsAnalytics analytics = response.readEntity(TopHitsAnalytics.class);
        assertThat(analytics.getValues()).isNull();
        assertThat(analytics.getMetadata()).isNull();
    }

    @Test
    public void shouldGetEmptyCountAnalyticsWhenNotAdminAndNoApi() {
        when(apiServiceV4.findAll(eq(GraviteeContext.getExecutionContext()), any(), eq(false), any(Pageable.class)))
            .thenReturn(new Page<>(Collections.emptyList(), 0, 0, 0));

        Response response = envTarget()
            .queryParam("type", "count")
            .queryParam("field", "api")
            .queryParam("interval", 1000)
            .queryParam("to", 1000)
            .request()
            .get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        StatsAnalytics analytics = response.readEntity(StatsAnalytics.class);
        assertThat(analytics.getCount()).isEqualTo(0);
    }

    @Test
    public void shouldGetEmptyStatsAnalyticsWhenNotAdminAndNoApi() {
        when(apiAuthorizationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(null), eq(true)))
            .thenReturn(Collections.emptySet());

        Response response = envTarget()
            .queryParam("type", "stats")
            .queryParam("field", "api")
            .queryParam("interval", 1000)
            .queryParam("to", 1000)
            .request()
            .get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        StatsAnalytics analytics = response.readEntity(StatsAnalytics.class);
        assertThat(analytics.getAvg()).isNull();
        assertThat(analytics.getCount()).isNull();
    }

    @Test
    public void shouldGetCountAnalyticsWhenNotAdminAndApi() {
        GenericApiEntity api = new ApiEntity();
        api.setId("apiId");

        when(apiAuthorizationServiceV4.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any(), eq(true)))
            .thenReturn(Collections.singleton(api.getId()));
        when(apiServiceV4.findAll(eq(GraviteeContext.getExecutionContext()), any(), eq(false), any(Pageable.class)))
            .thenReturn(new Page<>(Collections.singletonList(api), 1, 1, 1));

        when(permissionService.hasPermission(GraviteeContext.getExecutionContext(), API_ANALYTICS, api.getId(), READ)).thenReturn(true);

        Response response = envTarget()
            .queryParam("type", "count")
            .queryParam("field", "api")
            .queryParam("interval", 1000)
            .queryParam("to", 1000)
            .request()
            .get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        StatsAnalytics analytics = response.readEntity(StatsAnalytics.class);
        assertThat(analytics.getAvg()).isNull();
        assertThat(analytics.getCount()).isEqualTo(1);
    }

    @Test
    public void shouldGetCountAnalyticsWhenNotAdminAndApp() {
        ApplicationListItem app = new ApplicationListItem();
        app.setId("appId");

        when(applicationService.findIdsByUser(eq(GraviteeContext.getExecutionContext()), any()))
            .thenReturn(Collections.singleton(app.getId()));
        when(
            permissionService.hasPermission(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ANALYTICS), eq(app.getId()), eq(READ))
        )
            .thenReturn(true);

        Response response = envTarget()
            .queryParam("type", "count")
            .queryParam("field", "application")
            .queryParam("interval", 1000)
            .queryParam("to", 1000)
            .request()
            .get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        StatsAnalytics analytics = response.readEntity(StatsAnalytics.class);
        assertThat(analytics.getAvg()).isNull();
        assertThat(analytics.getCount()).isEqualTo(1);
    }
}
