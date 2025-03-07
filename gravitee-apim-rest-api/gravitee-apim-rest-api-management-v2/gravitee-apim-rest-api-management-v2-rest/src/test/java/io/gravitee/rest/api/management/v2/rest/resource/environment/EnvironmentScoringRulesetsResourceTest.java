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
import static org.mockito.Mockito.when;

import fixtures.core.model.ScoringRulesetFixture;
import inmemory.InMemoryAlternative;
import inmemory.ScoringRulesetCrudServiceInMemory;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.ImportScoringRuleset;
import io.gravitee.rest.api.management.v2.rest.model.ScoringAssetFormat;
import io.gravitee.rest.api.management.v2.rest.model.ScoringRulesetsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
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

        var environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

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
            var request = new ImportScoringRuleset()
                .name("ruleset-name")
                .description("ruleset-description")
                .payload("ruleset-payload")
                .format(ScoringAssetFormat.GRAVITEE_PROXY);

            // When
            try (var ignored = target.request().post(json(request))) {
                // Then
                assertThat(scoringRulesetCrudService.storage())
                    .extracting(ScoringRuleset::name, ScoringRuleset::description, ScoringRuleset::payload, ScoringRuleset::format)
                    .containsExactly(tuple("ruleset-name", "ruleset-description", "ruleset-payload", ScoringRuleset.Format.GRAVITEE_PROXY));
            }
        }

        @Test
        void should_create_ruleset_without_description() {
            // Given
            var request = new ImportScoringRuleset()
                .name("ruleset-name")
                .payload("ruleset-payload")
                .format(ScoringAssetFormat.GRAVITEE_PROXY);

            // When
            try (var ignored = target.request().post(json(request))) {
                // Then
                assertThat(scoringRulesetCrudService.storage())
                    .extracting(ScoringRuleset::name, ScoringRuleset::payload, ScoringRuleset::format)
                    .containsExactly(tuple("ruleset-name", "ruleset-payload", ScoringRuleset.Format.GRAVITEE_PROXY));
            }
        }

        @Test
        void should_set_location_header_with_created_ruleset_url() {
            // Given
            UuidString.overrideGenerator(() -> "generated-id");
            var request = new ImportScoringRuleset()
                .name("ruleset-name")
                .description("ruleset-description")
                .payload("ruleset-payload")
                .format(ScoringAssetFormat.GRAVITEE_PROXY);

            // When
            Response response = target.request().post(json(request));

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.CREATED_201)
                .hasHeader("Location", target.path("generated-id").getUri().toString());
        }
    }

    @Nested
    class ListEnvironmentRulesets {

        @Test
        void should_return_rulesets() {
            // Given
            scoringRulesetCrudService.initWith(
                List.of(
                    ScoringRulesetFixture.aRuleset().toBuilder().id("ruleset1").referenceId(ENVIRONMENT).build(),
                    ScoringRulesetFixture.aRuleset().toBuilder().id("ruleset2").referenceId(ENVIRONMENT).build()
                )
            );

            // When
            var response = target.request().get();

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(ScoringRulesetsResponse.class)
                .extracting(ScoringRulesetsResponse::getData)
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly(
                    new io.gravitee.rest.api.management.v2.rest.model.ScoringRuleset()
                        .id("ruleset1")
                        .name("ruleset-name")
                        .description("ruleset-description")
                        .payload("ruleset-payload")
                        .format(ScoringAssetFormat.GRAVITEE_PROXY)
                        .referenceId(ENVIRONMENT)
                        .referenceType(io.gravitee.rest.api.management.v2.rest.model.ScoringRulesetReferenceType.ENVIRONMENT)
                        .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atOffset(ZoneOffset.UTC)),
                    new io.gravitee.rest.api.management.v2.rest.model.ScoringRuleset()
                        .id("ruleset2")
                        .name("ruleset-name")
                        .description("ruleset-description")
                        .payload("ruleset-payload")
                        .format(ScoringAssetFormat.GRAVITEE_PROXY)
                        .referenceId(ENVIRONMENT)
                        .referenceType(io.gravitee.rest.api.management.v2.rest.model.ScoringRulesetReferenceType.ENVIRONMENT)
                        .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atOffset(ZoneOffset.UTC))
                );
        }
    }
}
