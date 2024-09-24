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
package io.gravitee.apim.infra.crud_service.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ScoringRulesetFixture;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringRulesetRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ScoringRulesetCrudServiceImplTest {

    ScoringRulesetRepository scoringRulesetRepository;

    ScoringRulesetCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        scoringRulesetRepository = mock(ScoringRulesetRepository.class);
        service = new ScoringRulesetCrudServiceImpl(scoringRulesetRepository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_scoring_ruleset() {
            // Given
            var ruleset = ScoringRulesetFixture.aRuleset();
            when(scoringRulesetRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            var created = service.create(ruleset);

            // Then
            assertThat(created).isEqualTo(ruleset);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            var ruleset = ScoringRulesetFixture.aRuleset();
            when(scoringRulesetRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(ruleset));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when creating Scoring Ruleset: ruleset-id");
        }
    }

    @Nested
    class FindById {

        @Test
        @SneakyThrows
        void should_find_a_ruleset_by_id() {
            // Given
            var ruleset = ScoringRulesetFixture.aRuleset();
            when(scoringRulesetRepository.findById("ruleset-id"))
                .thenReturn(Optional.of(fixtures.repository.ScoringRulesetFixture.aRuleset()));

            // When
            var found = service.findById("ruleset-id");

            // Then
            assertThat(found)
                .contains(
                    ScoringRuleset
                        .builder()
                        .id("ruleset-id")
                        .name("ruleset-name")
                        .description("ruleset-description")
                        .payload("ruleset-payload")
                        .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .referenceType(ScoringRuleset.ReferenceType.ENVIRONMENT)
                        .referenceId("reference-id")
                        .build()
                );
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(scoringRulesetRepository.findById(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findById("ruleset-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when searching for Scoring Ruleset: ruleset-id");
        }
    }

    @Nested
    class Delete {

        @Test
        @SneakyThrows
        void should_delete_a_ruleset() {
            // Given

            // When
            service.delete("ruleset-id");

            // Then
            verify(scoringRulesetRepository).delete("ruleset-id");
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            doThrow(TechnicalException.class).when(scoringRulesetRepository).delete(any());

            // When
            Throwable throwable = catchThrowable(() -> service.delete("ruleset-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when deleting Scoring Ruleset: ruleset-id");
        }
    }

    @Nested
    class DeleteByReference {

        @Test
        @SneakyThrows
        void should_delete_all_rulesets_of_an_environment() {
            // Given

            // When
            service.deleteByReference("env-id", ScoringRuleset.ReferenceType.ENVIRONMENT);

            // Then
            verify(scoringRulesetRepository).deleteByReferenceId("env-id", "ENVIRONMENT");
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            doThrow(TechnicalException.class).when(scoringRulesetRepository).deleteByReferenceId(any(), any());

            // When
            Throwable throwable = catchThrowable(() -> service.deleteByReference("env-id", ScoringRuleset.ReferenceType.ENVIRONMENT));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("Error when deleting Scoring Ruleset for [ENVIRONMENT:env-id]");
        }
    }
}
