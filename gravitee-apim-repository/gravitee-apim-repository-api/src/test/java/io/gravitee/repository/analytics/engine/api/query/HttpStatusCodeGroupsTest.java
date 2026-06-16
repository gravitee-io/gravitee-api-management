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
package io.gravitee.repository.analytics.engine.api.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpStatusCodeGroupsTest {

    @Nested
    class Resolve {

        @ParameterizedTest
        @ValueSource(strings = { "1XX", "2XX", "3XX", "4XX", "5XX" })
        void should_resolve_known_groups(String group) {
            var bounds = HttpStatusCodeGroups.resolve(group);

            assertThat(bounds).isPresent();
            assertThat(bounds.get().min()).isGreaterThanOrEqualTo(100);
            assertThat(bounds.get().max()).isLessThanOrEqualTo(599);
            assertThat(bounds.get().max() - bounds.get().min()).isEqualTo(99);
        }

        @Test
        void should_be_case_insensitive() {
            assertThat(HttpStatusCodeGroups.resolve("2xx")).isPresent().hasValue(new HttpStatusCodeGroups.Bounds(200, 299));
        }

        @Test
        void should_return_empty_for_unknown_group() {
            assertThat(HttpStatusCodeGroups.resolve("9XX")).isEmpty();
        }

        @Test
        void should_return_empty_for_null() {
            assertThat(HttpStatusCodeGroups.resolve(null)).isEmpty();
        }
    }

    @Nested
    class AsNumberRanges {

        @Test
        void should_return_all_five_groups() {
            var ranges = HttpStatusCodeGroups.asNumberRanges();

            assertThat(ranges).hasSize(5);
        }

        @Test
        void should_contain_2xx_range() {
            var ranges = HttpStatusCodeGroups.asNumberRanges();

            assertThat(ranges).contains(new NumberRange(200, 299));
        }
    }

    @Nested
    class EsBucketKeyToGroupLabel {

        @Test
        void should_return_all_five_groups() {
            var map = HttpStatusCodeGroups.esBucketKeyToGroupLabel();

            assertThat(map).hasSize(5);
        }

        @Test
        void should_map_es_key_to_group_label() {
            var map = HttpStatusCodeGroups.esBucketKeyToGroupLabel();

            assertThat(map)
                .containsEntry("100-199", "1XX")
                .containsEntry("200-299", "2XX")
                .containsEntry("300-399", "3XX")
                .containsEntry("400-499", "4XX")
                .containsEntry("500-599", "5XX");
        }
    }
}
