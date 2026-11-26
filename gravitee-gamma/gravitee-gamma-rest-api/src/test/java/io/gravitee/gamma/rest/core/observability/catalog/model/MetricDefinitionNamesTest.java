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
package io.gravitee.gamma.rest.core.observability.catalog.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricDefinitionNamesTest {

    @ParameterizedTest
    @ValueSource(
        strings = {
            "custom.a\rb", "custom.a\nb", "custom.a\u0000b", "custom.a\u007Fb", "custom.a\u0085b", "custom.a\uD83Db", "custom.a\uDE80b",
        }
    )
    void should_tell_a_control_character_or_an_unpaired_surrogate(String name) {
        assertThat(MetricDefinitionNames.holdsUnprintable(name)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "custom.fraud-tier", "custom.a b", "custom.niveau-é", "custom.\uD83D\uDE80", "additional-metrics.long_a/b" })
    void should_accept_printable_names_paired_surrogates_included(String name) {
        assertThat(MetricDefinitionNames.holdsUnprintable(name)).isFalse();
        assertThat(MetricDefinitionNames.printable(name)).isSameAs(name);
    }

    @Test
    void should_escape_each_unprintable_character_and_keep_the_rest() {
        assertThat(MetricDefinitionNames.printable("custom.a\r\nb\u0000\uD83Dc\uD83D\uDE80")).isEqualTo(
            "custom.a\\u000D\\u000Ab\\u0000\\uD83Dc\uD83D\uDE80"
        );
    }
}
