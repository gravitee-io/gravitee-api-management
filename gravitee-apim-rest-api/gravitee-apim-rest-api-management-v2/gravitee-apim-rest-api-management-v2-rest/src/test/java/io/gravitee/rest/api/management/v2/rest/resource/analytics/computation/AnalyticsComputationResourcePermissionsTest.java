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
package io.gravitee.rest.api.management.v2.rest.resource.analytics.computation;

import static assertions.MAPIAssertions.assertThat;
import static fixtures.AnalyticsEngineFixtures.*;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.domain_service.QueryFilterTransformer;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricMeasuresResult;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Checks the permission guard of {@link AnalyticsComputationResource} with a non-admin user.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsComputationResourcePermissionsTest extends ApiResourceTest {

    @Autowired
    AnalyticsRepository analyticsRepository;

    @Autowired
    QueryFilterTransformer queryFilterTransformer;

    @Autowired
    AnalyticsQueryContextLoaderResolver analyticsQueryContextLoader;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/analytics";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(
            (ContainerRequestFilter) requestContext ->
                requestContext.setSecurityContext(
                    new SecurityContext() {
                        @Override
                        public Principal getUserPrincipal() {
                            var userDetails = new io.gravitee.rest.api.management.v2.rest.UserDetails(USER_NAME, "", List.of());
                            userDetails.setOrganizationId(ORGANIZATION);
                            var principal = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userDetails,
                                new Object()
                            );
                            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(principal);
                            return principal;
                        }

                        @Override
                        public boolean isUserInRole(String string) {
                            return false;
                        }

                        @Override
                        public boolean isSecure() {
                            return true;
                        }

                        @Override
                        public String getAuthenticationScheme() {
                            return "BASIC";
                        }
                    }
                ),
            5
        );
        resourceConfig.register(GraviteeContextRequestFilter.class);
        var mockResponse = Mockito.mock(HttpServletResponse.class);
        resourceConfig.register(
            new org.glassfish.hk2.utilities.binding.AbstractBinder() {
                @Override
                protected void configure() {
                    bind(mockResponse).to(HttpServletResponse.class);
                }
            }
        );
    }

    @BeforeEach
    void init2() {
        Mockito.reset(analyticsRepository, queryFilterTransformer, analyticsQueryContextLoader);
    }

    private void givenNoDashboardNorApiPermission() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DASHBOARD,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(false);
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_API,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(false);
        when(membershipService.getMembershipsByMemberAndReference(any(), any(), any())).thenReturn(Set.of());
    }

    @Test
    void should_return_403_on_measures_when_user_has_no_dashboard_nor_api_permission() {
        givenNoDashboardNorApiPermission();

        var response = rootTarget().path("measures").request().post(Entity.json(aCountMeasureRequest()));

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_return_403_on_facets_when_user_has_no_dashboard_nor_api_permission() {
        givenNoDashboardNorApiPermission();

        var response = rootTarget().path("facets").request().post(Entity.json(aRequestCountFacetRequest()));

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_return_403_on_time_series_when_user_has_no_dashboard_nor_api_permission() {
        givenNoDashboardNorApiPermission();

        var response = rootTarget().path("time-series").request().post(Entity.json(aRequestCountTimeSeries()));

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_return_measures_when_user_has_only_environment_api_read_permission() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DASHBOARD,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(false);
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_API,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        var queryContext = new QueryContext(ORGANIZATION, ENVIRONMENT);
        when(analyticsRepository.searchHTTPMeasures(eq(queryContext), any())).thenReturn(
            new MeasuresResult(
                List.of(
                    new MetricMeasuresResult(
                        Metric.HTTP_REQUESTS,
                        Map.of(io.gravitee.repository.analytics.engine.api.metric.Measure.COUNT, 42)
                    )
                )
            )
        );
        when(analyticsRepository.searchMessageMeasures(eq(queryContext), any())).thenReturn(
            new MeasuresResult(
                List.of(
                    new MetricMeasuresResult(Metric.MESSAGES, Map.of(io.gravitee.repository.analytics.engine.api.metric.Measure.COUNT, 42))
                )
            )
        );
        when(analyticsQueryContextLoader.load(any(), any())).thenReturn(
            new AnalyticsQueryContext(null, new ExecutionContext(ORGANIZATION, ENVIRONMENT), Set.of(), Map.of(), Map.of(), Map.of())
        );
        when(queryFilterTransformer.transform(any(AnalyticsQueryContext.class), any())).thenAnswer(inv -> inv.getArgument(1));

        var response = rootTarget().path("measures").request().post(Entity.json(aCountMeasureRequest()));

        assertThat(response).hasStatus(OK_200);
    }
}
