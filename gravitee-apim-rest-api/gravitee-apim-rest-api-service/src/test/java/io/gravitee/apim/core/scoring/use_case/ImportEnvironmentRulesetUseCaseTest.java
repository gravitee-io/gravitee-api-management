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

import static assertions.CoreAssertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.InMemoryAlternative;
import inmemory.ScoringRulesetCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportEnvironmentRulesetUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String USER_ID = "user-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    ScoringRulesetCrudServiceInMemory scoringRulesetCrudService = new ScoringRulesetCrudServiceInMemory();

    ImportEnvironmentRulesetUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        useCase = new ImportEnvironmentRulesetUseCase(scoringRulesetCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(scoringRulesetCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_create_ruleset() {
        // Given
        ImportEnvironmentRulesetUseCase.Input input = new ImportEnvironmentRulesetUseCase.Input(
            new ImportEnvironmentRulesetUseCase.NewRuleset("name", "description", "payload"),
            AUDIT_INFO
        );

        // When
        useCase.execute(input);

        // Then
        assertThat(scoringRulesetCrudService.storage())
            .contains(
                ScoringRuleset
                    .builder()
                    .id("generated-id")
                    .name("name")
                    .description("description")
                    .payload("payload")
                    .referenceId(ENVIRONMENT_ID)
                    .referenceType(ScoringRuleset.ReferenceType.ENVIRONMENT)
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_ruleset_id() {
        // Given
        ImportEnvironmentRulesetUseCase.Input input = new ImportEnvironmentRulesetUseCase.Input(
            new ImportEnvironmentRulesetUseCase.NewRuleset("name", "description", "payload"),
            AUDIT_INFO
        );

        // When
        ImportEnvironmentRulesetUseCase.Output output = useCase.execute(input);

        // Then
        assertThat(output.rulesetId()).isEqualTo("generated-id");
    }
}
