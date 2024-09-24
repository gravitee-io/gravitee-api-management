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
package io.gravitee.apim.infra.query_service.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.repository.ScoringRulesetFixture;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringRulesetRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ScoringRulesetQueryServiceImplTest {

    ScoringRulesetRepository scoringRulesetRepository;

    ScoringRulesetQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        scoringRulesetRepository = mock(ScoringRulesetRepository.class);
        service = new ScoringRulesetQueryServiceImpl(scoringRulesetRepository);
    }

    @Nested
    class FindByReference {

        @Test
        @SneakyThrows
        void should_find_rulesets() {
            when(scoringRulesetRepository.findAllByReferenceId(any(), any()))
                .thenAnswer(invocation ->
                    List.of(
                        ScoringRulesetFixture
                            .aRuleset()
                            .toBuilder()
                            .referenceId(invocation.getArgument(0))
                            .referenceType(invocation.getArgument(1))
                            .build()
                    )
                );

            // When
            var result = service.findByReference("ref-id", ScoringRuleset.ReferenceType.ENVIRONMENT);

            // Then
            assertThat(result)
                .contains(
                    ScoringRuleset
                        .builder()
                        .id("ruleset-id")
                        .name("ruleset-name")
                        .description("ruleset-description")
                        .payload("ruleset-payload")
                        .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .referenceType(ScoringRuleset.ReferenceType.ENVIRONMENT)
                        .referenceId("ref-id")
                        .build()
                );
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(scoringRulesetRepository.findAllByReferenceId(any(), any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findByReference("env-id", ScoringRuleset.ReferenceType.ENVIRONMENT));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurred while finding Scoring ruleset [ENVIRONMENT:env-id] ");
        }
    }
}
