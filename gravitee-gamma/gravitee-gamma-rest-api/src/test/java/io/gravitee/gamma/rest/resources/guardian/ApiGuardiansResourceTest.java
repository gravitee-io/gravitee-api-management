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
package io.gravitee.gamma.rest.resources.guardian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.GuardianFixtures;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.guardian.GuardiansResourceTest.GuardiansTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * The Guardians of an API over HTTP, under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/apis/{apiId}/guardians}. The use cases run for real against
 * in-memory ports.
 *
 * @author GraviteeSource Team
 */
@ContextConfiguration(classes = { ResourceContextConfiguration.class, GuardiansTestConfiguration.class })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiGuardiansResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final String API_ID = GuardianFixtures.API_ID;
    private static final String GUARDIAN_ID = GuardianFixtures.GUARDIAN_ID;

    @Inject
    private InMemoryGuardianRepository guardianRepository;

    @Inject
    private InMemoryGuardedApiPort apis;

    @Inject
    private GuardianPublicationFixture guardianPublications;

    @Inject
    private GuardianAuditFixture audits;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/guardians";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
    }

    @AfterEach
    void resetPorts() {
        guardianRepository.reset();
        apis.reset();
        guardianPublications.reset();
        audits.reset();
    }

    @Nested
    class ListApiGuardians {

        @Test
        void should_return_the_guardians_enabled_on_the_api() {
            guardianRepository.givenGuardian(
                GuardianFixtures.aGuardian(d -> d.environmentId(ENVIRONMENT).name("Credential leakage")).build()
            );
            apis.givenApi(
                GuardianFixtures.aGuardedApi()
                    .environmentId(ENVIRONMENT)
                    .internalFlow(Flow.builder().request(List.of(GuardianSteps.of(GUARDIAN_ID, "Credential leakage"))).build())
                    .build()
            );

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.OK_200);
            JsonNode row = response.readEntity(JsonNode.class).get("data").get(0);
            assertSoftly(softly -> {
                softly.assertThat(row.get("id").asText()).as("id").isEqualTo(GUARDIAN_ID);
                softly.assertThat(row.get("name").asText()).as("name").isEqualTo("Credential leakage");
                softly.assertThat(row.get("phase").asText()).as("phase").isEqualTo("REQUEST");
                softly.assertThat(row.get("order").asInt()).as("order").isEqualTo(1);
            });
        }

        @Test
        void should_return_404_for_an_api_of_another_environment() {
            apis.givenApi(GuardianFixtures.aGuardedApi().environmentId("other-env").build());

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_the_permission_to_read_the_api_definition() {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_GUARDIAN, ENVIRONMENT);
            apis.givenApi(GuardianFixtures.aGuardedApi().environmentId(ENVIRONMENT).build());

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    @Nested
    class EnableApiGuardian {

        @Test
        void should_enable_a_deployed_guardian() {
            givenDeployedGuardian();
            apis.givenApi(GuardianFixtures.aGuardedApi().environmentId(ENVIRONMENT).build());

            Response response = post(Map.of("guardianId", GUARDIAN_ID));

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.OK_200);
            JsonNode row = response.readEntity(JsonNode.class).get("data").get(0);
            assertSoftly(softly -> {
                softly.assertThat(row.get("id").asText()).as("id").isEqualTo(GUARDIAN_ID);
                softly.assertThat(row.get("order").asInt()).as("order").isEqualTo(1);
            });
        }

        @Test
        void should_return_400_for_a_guardian_that_is_not_deployed() {
            guardianRepository.givenGuardian(GuardianFixtures.aGuardian(d -> d.environmentId(ENVIRONMENT)).build());
            apis.givenApi(GuardianFixtures.aGuardedApi().environmentId(ENVIRONMENT).build());

            Response response = post(Map.of("guardianId", GUARDIAN_ID));

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_409_for_a_guardian_already_enabled() {
            givenDeployedGuardian();
            apis.givenApi(
                GuardianFixtures.aGuardedApi()
                    .environmentId(ENVIRONMENT)
                    .internalFlow(Flow.builder().request(List.of(GuardianSteps.of(GUARDIAN_ID, "Credential leakage"))).build())
                    .build()
            );

            Response response = post(Map.of("guardianId", GUARDIAN_ID));

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.CONFLICT_409);
        }

        @Test
        void should_return_400_without_a_guardian_id() {
            apis.givenApi(GuardianFixtures.aGuardedApi().environmentId(ENVIRONMENT).build());

            Response response = post(Map.of());

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_the_permission_to_read_the_guardians() {
            when(permissionService.hasPermission(any(), eq(RolePermission.ENVIRONMENT_GUARDIAN), any(), any())).thenReturn(false);
            givenDeployedGuardian();
            apis.givenApi(GuardianFixtures.aGuardedApi().environmentId(ENVIRONMENT).build());

            Response response = post(Map.of("guardianId", GUARDIAN_ID));

            assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.FORBIDDEN_403);
                softly.assertThat(apis.findById(ENVIRONMENT, API_ID).orElseThrow().internalFlow()).as("internal flow").isNull();
            });
        }

        @Test
        void should_return_403_without_the_permission_to_update_the_api_definition() {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_GUARDIAN, ENVIRONMENT);
            givenDeployedGuardian();
            apis.givenApi(GuardianFixtures.aGuardedApi().environmentId(ENVIRONMENT).build());

            Response response = post(Map.of("guardianId", GUARDIAN_ID));

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    @Nested
    class ReorderApiGuardians {

        @BeforeEach
        void given_an_api_running_two_request_guardians() {
            guardianRepository.givenGuardian(GuardianFixtures.aGuardian(d -> d.id("guardian-1").environmentId(ENVIRONMENT)).build());
            guardianRepository.givenGuardian(GuardianFixtures.aGuardian(d -> d.id("guardian-2").environmentId(ENVIRONMENT)).build());
            apis.givenApi(
                GuardianFixtures.aGuardedApi()
                    .environmentId(ENVIRONMENT)
                    .internalFlow(
                        Flow.builder()
                            .request(List.of(GuardianSteps.of("guardian-1", "One"), GuardianSteps.of("guardian-2", "Two")))
                            .build()
                    )
                    .build()
            );
        }

        @Test
        void should_reorder_the_guardians_of_a_phase() {
            Response response = put(Map.of("phase", "REQUEST", "guardianIds", List.of("guardian-2", "guardian-1")));

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.OK_200);
            JsonNode data = response.readEntity(JsonNode.class).get("data");
            assertSoftly(softly -> {
                softly.assertThat(data.get(0).get("id").asText()).as("first").isEqualTo("guardian-2");
                softly.assertThat(data.get(1).get("id").asText()).as("second").isEqualTo("guardian-1");
            });
        }

        @Test
        void should_return_400_for_an_order_missing_a_guardian() {
            Response response = put(Map.of("phase", "REQUEST", "guardianIds", List.of("guardian-2")));

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_the_permission_to_read_the_guardians() {
            when(permissionService.hasPermission(any(), eq(RolePermission.ENVIRONMENT_GUARDIAN), any(), any())).thenReturn(false);

            Response response = put(Map.of("phase", "REQUEST", "guardianIds", List.of("guardian-2", "guardian-1")));

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }

        private Response put(Object payload) {
            return rootTarget().path("_order").request().put(Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE));
        }
    }

    @Nested
    class DisableApiGuardian {

        @BeforeEach
        void given_an_api_running_a_guardian() {
            apis.givenApi(
                GuardianFixtures.aGuardedApi()
                    .environmentId(ENVIRONMENT)
                    .internalFlow(Flow.builder().request(List.of(GuardianSteps.of(GUARDIAN_ID, "Credential leakage"))).build())
                    .build()
            );
        }

        @Test
        void should_turn_the_guardian_off() {
            Response response = rootTarget(GUARDIAN_ID).request().delete();

            assertSoftly(softly -> {
                softly.assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.OK_200);
                softly.assertThat(response.readEntity(JsonNode.class).get("data")).as("enabled guardians").isEmpty();
            });
        }

        @Test
        void should_return_404_for_a_guardian_that_is_not_enabled() {
            Response response = rootTarget("unknown-guardian").request().delete();

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_the_permission_to_read_the_guardians() {
            when(permissionService.hasPermission(any(), eq(RolePermission.ENVIRONMENT_GUARDIAN), any(), any())).thenReturn(false);

            Response response = rootTarget(GUARDIAN_ID).request().delete();

            assertThat(response.getStatus()).as("status").isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    private void givenDeployedGuardian() {
        var guardian = GuardianFixtures.aGuardian(d -> d.environmentId(ENVIRONMENT).name("Credential leakage")).build();
        guardianRepository.givenGuardian(guardian);
        guardianPublications.givenPublication(guardian.getDefinition(), Instant.parse("2026-11-09T10:00:00Z"));
    }

    private Response post(Object payload) {
        return rootTarget().request().post(Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE));
    }
}
