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
import jakarta.ws.rs.core.Response;
import java.util.List;
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
}
