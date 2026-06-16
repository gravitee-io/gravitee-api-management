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
package io.gravitee.repository.elasticsearch.v4.shared;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StatusCodeGroupsTest {

    private static final String FIELD = "status";

    @Nested
    class RangeForGroup {

        @Test
        void should_build_range_for_2xx_group() {
            var result = StatusCodeGroups.rangeForGroup(FIELD, "2XX");

            assertThatJson(result.encode()).isEqualTo(
                """
                    { "range": { "status": { "gte": 200, "lte": 299 } } }
                """
            );
        }

        @Test
        void should_be_case_insensitive() {
            var result = StatusCodeGroups.rangeForGroup(FIELD, "4xx");

            assertThatJson(result.encode()).isEqualTo(
                """
                    { "range": { "status": { "gte": 400, "lte": 499 } } }
                """
            );
        }

        @Test
        void should_throw_for_null_group() {
            assertThatThrownBy(() -> StatusCodeGroups.rangeForGroup(FIELD, null)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_for_unknown_group() {
            assertThatThrownBy(() -> StatusCodeGroups.rangeForGroup(FIELD, "9XX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9XX");
        }
    }

    @Nested
    class ShouldForGroups {

        @Test
        void should_return_single_range_for_one_group() {
            var result = StatusCodeGroups.shouldForGroups(FIELD, List.of("5XX"));

            assertThatJson(result.encode()).isEqualTo(
                """
                    { "range": { "status": { "gte": 500, "lte": 599 } } }
                """
            );
        }

        @Test
        void should_return_bool_should_for_multiple_groups() {
            var result = StatusCodeGroups.shouldForGroups(FIELD, Set.of("2XX", "4XX"));

            assertThatJson(result.encode()).inPath("$.bool.should").isArray().hasSize(2);
            assertThatJson(result.encode()).inPath("$.bool.minimum_should_match").isEqualTo(1);
        }

        @Test
        void should_return_match_none_for_empty_groups() {
            var result = StatusCodeGroups.shouldForGroups(FIELD, List.of());

            assertThatJson(result.encode()).isEqualTo(
                """
                    { "match_none": {} }
                """
            );
        }

        @Test
        void should_return_match_none_for_null_groups() {
            var result = StatusCodeGroups.shouldForGroups(FIELD, null);

            assertThatJson(result.encode()).isEqualTo(
                """
                    { "match_none": {} }
                """
            );
        }
    }

    @Nested
    class RangeForBounds {

        @Test
        void should_build_range_with_both_bounds() {
            var result = StatusCodeGroups.rangeForBounds(FIELD, 200, 299);

            assertThatJson(result.encode()).isEqualTo(
                """
                    { "range": { "status": { "gte": 200, "lte": 299 } } }
                """
            );
        }

        @Test
        void should_build_range_with_only_gte() {
            var result = StatusCodeGroups.rangeForBounds(FIELD, 500, null);

            assertThatJson(result.encode()).isEqualTo(
                """
                    { "range": { "status": { "gte": 500 } } }
                """
            );
        }

        @Test
        void should_build_range_with_only_lte() {
            var result = StatusCodeGroups.rangeForBounds(FIELD, null, 299);

            assertThatJson(result.encode()).isEqualTo(
                """
                    { "range": { "status": { "lte": 299 } } }
                """
            );
        }

        @Test
        void should_throw_when_both_bounds_are_null() {
            assertThatThrownBy(() -> StatusCodeGroups.rangeForBounds(FIELD, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one bound");
        }
    }
}
