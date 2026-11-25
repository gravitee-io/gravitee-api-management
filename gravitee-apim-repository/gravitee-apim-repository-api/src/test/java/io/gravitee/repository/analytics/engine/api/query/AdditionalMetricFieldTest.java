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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField.Root;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField.ValueType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricFieldTest {

    @Test
    void should_name_a_field_by_its_root_and_key_the_path_of_the_field_on_a_request_document() {
        assertThat(new AdditionalMetricField(ValueType.LONG, "long_failover_count").name()).isEqualTo(
            "additional-metrics.long_failover_count"
        );
        assertThat(new AdditionalMetricField(Root.CUSTOM, ValueType.KEYWORD, "order.channel").name()).isEqualTo("custom.order.channel");
    }

    @ParameterizedTest
    @EnumSource(value = ValueType.class, names = "KEYWORD", mode = EnumSource.Mode.EXCLUDE)
    void should_refuse_a_custom_field_holding_anything_but_keywords(ValueType valueType) {
        assertThatThrownBy(() -> new AdditionalMetricField(Root.CUSTOM, valueType, "fraud-tier")).isInstanceOf(
            IllegalArgumentException.class
        );
    }
}
