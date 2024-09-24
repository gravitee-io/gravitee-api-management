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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static assertions.MAPIAssertions.assertThat;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doReturn;

import inmemory.InMemoryAlternative;
import inmemory.ScoringRulesetCrudServiceInMemory;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.ImportScoringRuleset;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EnvironmentScoringRulesetsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";

    WebTarget target;

    @Inject
    ScoringRulesetCrudServiceInMemory scoringRulesetCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/scoring/rulesets";
    }

    @BeforeEach
    void setup() {
        target = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        doReturn(environmentEntity).when(environmentService).findById(ENVIRONMENT);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        UuidString.reset();
        GraviteeContext.cleanContext();

        Stream.of(scoringRulesetCrudService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class ImportEnvironmentRuleset {

        @Test
        void should_create_ruleset() {
            // Given
            var request = ImportScoringRuleset
                .builder()
                .name("ruleset-name")
                .description("ruleset-description")
                .payload("ruleset-payload")
                .build();

            // When
            target.request().post(json(request));

            // Then
            assertThat(scoringRulesetCrudService.storage())
                .extracting(ScoringRuleset::name, ScoringRuleset::description, ScoringRuleset::payload)
                .containsExactly(tuple("ruleset-name", "ruleset-description", "ruleset-payload"));
        }

        @Test
        void should_set_location_header_with_created_ruleset_url() {
            // Given
            UuidString.overrideGenerator(() -> "generated-id");
            var request = ImportScoringRuleset
                .builder()
                .name("ruleset-name")
                .description("ruleset-description")
                .payload("ruleset-payload")
                .build();

            // When
            Response response = target.request().post(json(request));

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.CREATED_201)
                .hasHeader("Location", target.path("generated-id").getUri().toString());
        }
    }
}
