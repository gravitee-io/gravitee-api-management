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
package io.gravitee.rest.api.management.v2.rest.resource.api.debug;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.apim.core.gateway.model.Instance.DEBUG_PLUGIN_ID;
import static io.gravitee.common.http.HttpStatusCode.ACCEPTED_202;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.InstanceQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.gateway.model.Instance;
import io.gravitee.rest.api.management.v2.rest.model.DebugEvent;
import io.gravitee.rest.api.management.v2.rest.model.DebugHttpRequest;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import io.gravitee.rest.api.model.PluginEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiDebugResourceTest extends ApiResourceTest {

    WebTarget target;

    @Inject
    ApiCrudServiceInMemory apiCrudService;

    @Inject
    PlanQueryServiceInMemory planQueryService;

    @Inject
    InstanceQueryServiceInMemory instanceQueryService;

    @BeforeEach
    void setup() {
        target = rootTarget();

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        Stream.of(apiCrudService, instanceQueryService, planQueryService).forEach(InMemoryAlternative::reset);
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/debug";
    }

    @Nested
    class SendDebugRequest {

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_DEFINITION),
                    eq(API),
                    eq(RolePermissionAction.UPDATE)
                )
            ).thenReturn(false);

            final Response response = target.request().post(null);

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_debug_event_created() {
            apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().setId(API)));
            planQueryService.initWith(List.of(fixtures.core.model.PlanFixtures.aPlanHttpV4().setApiId(API)));
            instanceQueryService.initWith(List.of(validInstance()));

            final Response response = target.request().post(Entity.json(new DebugHttpRequest().path("/").method("GET")));

            assertThat(response)
                .hasStatus(ACCEPTED_202)
                .asEntity(DebugEvent.class)
                .satisfies(event -> {
                    SoftAssertions.assertSoftly(softly -> {
                        softly.assertThat(event.getId()).isNotNull();
                        softly.assertThat(event.getType()).isEqualTo("DEBUG_API");
                    });
                });
        }
    }

    private static Instance validInstance() {
        return Instance.builder()
            .id("gateway-instance-id")
            .startedAt(new Date())
            .clusterPrimaryNode(true)
            .environments(Set.of(ENVIRONMENT))
            .plugins(Set.of(PluginEntity.builder().id(DEBUG_PLUGIN_ID).build()))
            .build();
    }
}
