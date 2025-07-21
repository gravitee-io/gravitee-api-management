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
package io.gravitee.apim.core.api.model.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api.model.utils.MigrationResult.Issue;
import io.gravitee.apim.core.api.model.utils.MigrationResult.State;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MigrationResultTest {

    @Test
    void should_create_result_with_mapper_that_succeeds() {
        // Given
        Function<Integer, String> mapper = Object::toString;
        Collection<Issue> issues = List.of(new Issue("Test issue", State.MIGRATED));
        var previousResult = new MigrationResult<>(42, issues);

        // When
        MigrationResult<String> result = previousResult.map(mapper);

        // Then
        assertThat(result.value()).isEqualTo("42");
        assertThat(result.issues()).containsExactlyElementsOf(issues);
        assertThat(result.state()).isEqualTo(State.MIGRATED);
    }

    @Test
    void should_create_result_with_mapper_that_fails() {
        // Given
        Function<Integer, String> mapper = i -> {
            throw new RuntimeException("Mapping failed");
        };
        var issues = List.of(new Issue("Test issue", State.CAN_BE_FORCED));
        var previousResult = new MigrationResult<>(42, issues);

        // When
        MigrationResult<String> result = previousResult.map(mapper);

        // Then
        assertThat(result.value()).isNull();

        assertThat(result.issues()).contains(new Issue("Mapping failed", State.IMPOSSIBLE), new Issue("Test issue", State.CAN_BE_FORCED));
        assertThat(result.state()).isEqualTo(State.IMPOSSIBLE);
    }

    @Test
    void should_return_null_when_state_is_impossible() {
        // Given
        Collection<Issue> issues = List.of(new Issue("Test issue", State.IMPOSSIBLE));
        MigrationResult<String> result = new MigrationResult<>("value", issues);

        // When & Then
        assertThat(result.value()).isNull();
    }

    @Nested
    class IssuesMethodTests {

        @Test
        void should_return_copy_of_issues_collection() {
            // Given
            Collection<Issue> originalIssues = new ArrayList<>();
            originalIssues.add(new Issue("Test issue", State.MIGRATABLE));
            MigrationResult<String> result = new MigrationResult<>("test", originalIssues);

            // When
            Collection<Issue> returnedIssues = result.issues();
            originalIssues.add(new Issue("Another issue", State.MIGRATED));

            // Then
            assertThat(returnedIssues).hasSize(1);
            assertThat(returnedIssues).extracting(Issue::message).containsExactly("Test issue");
        }
    }

    @Nested
    class StateMethodTests {

        @Test
        void should_return_impossible_when_value_is_null() {
            // Given
            MigrationResult<String> result = MigrationResult.issues(List.of());

            // When & Then
            assertThat(result.state()).isEqualTo(State.IMPOSSIBLE);
        }

        @Test
        void should_return_migratable_when_no_issues() {
            // Given
            MigrationResult<String> result = MigrationResult.value("test");

            // When & Then
            assertThat(result.state()).isEqualTo(State.MIGRATABLE);
        }

        @Test
        void should_return_highest_state_weight_from_issues() {
            // Given
            Collection<Issue> issues = List.of(
                new Issue("Issue 1", State.MIGRATABLE),
                new Issue("Issue 2", State.CAN_BE_FORCED),
                new Issue("Issue 3", State.MIGRATED)
            );
            MigrationResult<String> result = new MigrationResult<>("test", issues);

            // When & Then
            assertThat(result.state()).isEqualTo(State.CAN_BE_FORCED);
        }
    }

    @Nested
    class MapMethodTests {

        @Test
        void should_mapvalue_and_preserve_issues() {
            // Given
            String originalValue = "42";
            Collection<Issue> issues = List.of(new Issue("Test issue", State.MIGRATED));
            MigrationResult<String> originalResult = new MigrationResult<>(originalValue, issues);
            Function<String, Integer> mapper = Integer::parseInt;

            // When
            MigrationResult<Integer> mappedResult = originalResult.map(mapper);

            // Then
            assertThat(mappedResult.value()).isEqualTo(42);
            assertThat(mappedResult.issues()).containsExactlyElementsOf(issues);
            assertThat(mappedResult.state()).isEqualTo(State.MIGRATED);
        }

        @Test
        void should_handle_mapping_exceptions() {
            // Given
            String originalValue = "not a number";
            Collection<Issue> issues = List.of(new Issue("Test issue", State.MIGRATABLE));
            MigrationResult<String> originalResult = new MigrationResult<>(originalValue, issues);
            Function<String, Integer> mapper = Integer::parseInt;

            // When
            MigrationResult<Integer> mappedResult = originalResult.map(mapper);

            // Then
            assertThat(mappedResult.value()).isNull();
            assertThat(mappedResult.issues()).hasSize(2);
            assertThat(mappedResult.state()).isEqualTo(State.IMPOSSIBLE);
        }
    }

    @Test
    void should_flat_map_value_and_combine_issues() {
        // Given
        String originalValue = "test";
        var originalIssues = List.of(new Issue("Original issue", State.MIGRATABLE));
        MigrationResult<String> originalResult = new MigrationResult<>(originalValue, originalIssues);

        var mappedIssues = List.of(new Issue("Mapped issue", State.MIGRATED));
        Function<String, MigrationResult<Integer>> mapper = s -> new MigrationResult<>(s.length(), mappedIssues);

        // When
        MigrationResult<Integer> mappedResult = originalResult.flatMap(mapper);

        // Then
        assertThat(mappedResult.value()).isEqualTo(4);
        assertThat(mappedResult.issues()).extracting(Issue::message).containsExactlyInAnyOrder("Original issue", "Mapped issue");
        assertThat(mappedResult.state()).isEqualTo(State.MIGRATED);
    }

    @Test
    void should_fold_left_with_another_result_and_combine_issues() {
        // Given
        String value1 = "Hello";
        var issues1 = List.of(new Issue("Issue 1", State.MIGRATABLE));
        MigrationResult<String> result1 = new MigrationResult<>(value1, issues1);

        int value2 = 12;
        var issues2 = List.of(new Issue("Issue 2", State.CAN_BE_FORCED));
        MigrationResult<Integer> result2 = new MigrationResult<>(value2, issues2);

        // When
        MigrationResult<String> foldedResult = result1.foldLeft(result2, (s1, s2) -> s1 + " " + s2);

        // Then
        assertThat(foldedResult.value()).isEqualTo("Hello 12");
        assertThat(foldedResult.issues()).extracting(Issue::message).containsExactlyInAnyOrder("Issue 1", "Issue 2");
        assertThat(foldedResult.state()).isEqualTo(State.CAN_BE_FORCED);
    }
}
