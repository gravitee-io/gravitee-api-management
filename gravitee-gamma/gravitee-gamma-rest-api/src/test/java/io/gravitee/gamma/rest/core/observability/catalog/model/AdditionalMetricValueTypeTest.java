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

import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricValueTypeTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({ "KEYWORD, KEYWORD", "LONG, NUMBER", "INT, NUMBER", "DOUBLE, NUMBER", "BOOL, BOOLEAN" })
    void should_offer_a_filterable_value_type_under_its_filter_type(AdditionalMetricValueType valueType, FilterType filterType) {
        assertThat(valueType.filterType()).contains(filterType);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = AdditionalMetricValueType.class, names = { "STRING", "JSON" })
    void should_offer_no_filter_type_for_a_detail_only_value_type(AdditionalMetricValueType valueType) {
        assertThat(valueType.filterType()).isEmpty();
        assertThat(valueType.isDetailOnly()).isTrue();
    }
}
