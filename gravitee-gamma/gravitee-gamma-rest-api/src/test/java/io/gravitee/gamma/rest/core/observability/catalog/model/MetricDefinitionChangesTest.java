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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricDefinitionChangesTest {

    @Test
    void should_default_exposed_to_true_and_read_blank_text_as_absent() {
        var changes = MetricDefinitionChanges.of("  ", "", null, null);

        assertThat(changes).isEqualTo(new MetricDefinitionChanges(null, null, null, true));
    }

    @Test
    void should_keep_a_named_unit_and_a_short_suffix() {
        assertThat(MetricDefinitionChanges.of(null, null, "MILLISECONDS", false).unit()).isEqualTo("MILLISECONDS");
        assertThat(MetricDefinitionChanges.of(null, null, " € ", false).unit()).isEqualTo("€");
        assertThat(MetricDefinitionChanges.of(null, null, "x".repeat(16), false).unit()).hasSize(16);
    }

    @Test
    void should_refuse_a_suffix_longer_than_16_characters_or_with_control_characters() {
        assertThatThrownBy(() -> MetricDefinitionChanges.of(null, null, "x".repeat(17), true))
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.invalid_unit");
        assertThatThrownBy(() -> MetricDefinitionChanges.of(null, null, "m\ns", true))
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.invalid_unit");
    }

    @Test
    void should_bound_the_label_to_64_and_the_description_to_512_characters() {
        assertThat(MetricDefinitionChanges.of("l".repeat(64), "d".repeat(512), null, true).label()).hasSize(64);
        assertThatThrownBy(() -> MetricDefinitionChanges.of("l".repeat(65), null, null, true))
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.label_too_long");
        assertThatThrownBy(() -> MetricDefinitionChanges.of(null, "d".repeat(513), null, true))
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.description_too_long");
    }
}
