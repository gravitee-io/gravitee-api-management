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
package io.gravitee.apim.core.scoring.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.ScoringRulesetFixture;
import inmemory.InMemoryAlternative;
import inmemory.ScoringRulesetCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.scoring.exception.RulesetNotFoundException;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetEnvironmentRulesetUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    ScoringRulesetCrudServiceInMemory scoringRulesetCrudService = new ScoringRulesetCrudServiceInMemory();

    GetEnvironmentRulesetUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetEnvironmentRulesetUseCase(scoringRulesetCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(scoringRulesetCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_get_a_ruleset() {
        // Given
        var rulesetId = "ruleset-id";
        givenExistingRulesets(ScoringRulesetFixture.aRuleset(rulesetId).withReferenceId(ENVIRONMENT_ID));

        // When
        var result = useCase.execute(new GetEnvironmentRulesetUseCase.Input(rulesetId, AUDIT_INFO));

        // Then
        assertThat(result.scoringRuleset())
            .isEqualTo(
                ScoringRuleset
                    .builder()
                    .id(rulesetId)
                    .name("ruleset-name")
                    .description("ruleset-description")
                    .referenceId(ENVIRONMENT_ID)
                    .referenceType(ScoringRuleset.ReferenceType.ENVIRONMENT)
                    .payload("payload-ruleset-id")
                    .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_throw_exception_when_ruleset_not_found() {
        // Given
        var rulesetId = "ruleset-id";

        // When
        Throwable thrown = catchThrowable(() -> useCase.execute(new GetEnvironmentRulesetUseCase.Input(rulesetId, AUDIT_INFO)));

        //Then
        assertThat(thrown).isInstanceOf(RulesetNotFoundException.class).hasMessage("Ruleset with id ruleset-id not found");
    }

    @Test
    void should_throw_exception_when_ruleset_found_is_from_another_env() {
        // Given
        var rulesetId = "ruleset-id";
        givenExistingRulesets(ScoringRulesetFixture.aRuleset(rulesetId).withReferenceId("other"));

        // When
        Throwable thrown = catchThrowable(() -> useCase.execute(new GetEnvironmentRulesetUseCase.Input(rulesetId, AUDIT_INFO)));

        //Then
        assertThat(thrown).isInstanceOf(RulesetNotFoundException.class).hasMessage("Ruleset with id ruleset-id not found");
    }

    private void givenExistingRulesets(ScoringRuleset... rulesets) {
        scoringRulesetCrudService.initWith(List.of(rulesets));
    }
}
