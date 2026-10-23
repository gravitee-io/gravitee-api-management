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
import static jakarta.ws.rs.client.Entity.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ResolveFilterLabelsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.api.AbstractNonAdminApiResourceTest;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Checks that the observability filter endpoints answer a non-admin user from the rows they read, whatever their
 * environment permissions: here a user who holds none and reads no API.
 *
 * @author GraviteeSource Team
 */
class ObservabilityFiltersResourcePermissionsTest extends AbstractNonAdminApiResourceTest {

    @Autowired
    AnalyticsQueryContextLoaderResolver analyticsQueryContextLoader;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/observability/filters";
    }

    @BeforeEach
    void givenNoEnvironmentPermissionNorReadableApi() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(membershipService.getMembershipsByMemberAndReference(any(), any(), any())).thenReturn(Set.of());
        when(analyticsQueryContextLoader.load(any(), any(), any())).thenReturn(
            new AnalyticsQueryContext(
                null,
                new ExecutionContext(ORGANIZATION, ENVIRONMENT),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Optional.empty()
            )
        );
    }

    /** The catalog holds no traffic data. */
    @Test
    void should_describe_the_catalog() {
        var response = rootTarget("definition").request().get();

        assertThat(response).hasStatus(200);
    }

    @Test
    void should_list_values() {
        var response = rootTarget("HTTP_METHOD/values").request().get();

        assertThat(response).hasStatus(200);
    }

    @Test
    void should_label_no_api_the_user_does_not_read() {
        var response = rootTarget("resolve")
            .request()
            .post(json(Map.of("entries", List.of(Map.of("filterName", "API", "ids", List.of("api-id-1"))))));

        assertThat(response)
            .hasStatus(200)
            .asEntity(ResolveFilterLabelsResponse.class)
            .extracting(ResolveFilterLabelsResponse::getEntries)
            .satisfies(entries -> assertThat(entries.get(0).getLabels()).isEmpty());
    }
}
