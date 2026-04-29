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
package io.gravitee.rest.api.management.v2.rest.resource.observability;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static jakarta.ws.rs.client.Entity.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.rest.api.management.v2.rest.UserDetails;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ResolveFilterLabelsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityFilterLabelsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";

    @Inject
    FilterValueNameResolver filterValueNameResolver;

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(
            (ContainerRequestFilter) requestContext ->
                requestContext.setSecurityContext(
                    new SecurityContext() {
                        @Override
                        public Principal getUserPrincipal() {
                            var userDetails = new UserDetails(USER_NAME, "", List.of());
                            userDetails.setOrganizationId(ORGANIZATION);
                            var principal = new UsernamePasswordAuthenticationToken(userDetails, new Object());
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
    void setup() {
        var environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);

        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/observability/filters/resolve";
    }

    @Test
    void should_resolve_filter_labels() {
        when(filterValueNameResolver.resolveNames(ENVIRONMENT, FilterSpec.Name.API, List.of("api-id-1"))).thenReturn(
            Map.of("api-id-1", "Public API")
        );

        var response = rootTarget()
            .request()
            .post(json(Map.of("entries", List.of(Map.of("filterName", "API", "ids", List.of("api-id-1"))))));

        assertThat(response)
            .hasStatus(200)
            .asEntity(ResolveFilterLabelsResponse.class)
            .extracting(ResolveFilterLabelsResponse::getEntries)
            .satisfies(entries -> {
                assertThat(entries).hasSize(1);
                assertThat(entries.get(0).getFilterName().getValue()).isEqualTo("API");
                assertThat(entries.get(0).getLabels()).containsEntry("api-id-1", "Public API");
            });
    }

    @Test
    void should_return_empty_entries_for_empty_request() {
        var response = rootTarget().request().post(json(Map.of("entries", List.of())));

        assertThat(response)
            .hasStatus(200)
            .asEntity(ResolveFilterLabelsResponse.class)
            .extracting(ResolveFilterLabelsResponse::getEntries)
            .satisfies(assertions -> assertThat(assertions).isEmpty());
    }

    @Test
    void should_return_403_when_missing_dashboard_read_permission() {
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

        var response = rootTarget().request().post(json(Map.of("entries", List.of())));

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_return_400_for_validation_error() {
        var entries = IntStream.range(0, 11)
            .mapToObj(index -> Map.of("filterName", "API", "ids", List.of("api-id-" + index)))
            .toList();

        var response = rootTarget().request().post(json(Map.of("entries", entries)));

        assertThat(response).hasStatus(400).asError().hasHttpStatus(400).hasMessage("Too many filter entries to resolve");
    }
}
