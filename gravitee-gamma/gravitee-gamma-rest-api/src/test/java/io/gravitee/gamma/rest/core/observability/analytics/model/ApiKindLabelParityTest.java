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
package io.gravitee.gamma.rest.core.observability.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * One vocabulary for the API kinds on the Gamma surface (OBS-184): the label a family of one kind is served with on
 * {@code GET /analytics/definition} is the label the {@code API_TYPE} filter serves for that kind's value on
 * {@code GET /filters/definition}, read from the registry shipped with the host.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiKindLabelParityTest {

    private final FilterSpec apiTypeFilter = new SpiFilterRegistry()
        .getFilters(Set.of(), Set.of())
        .stream()
        .filter(spec -> spec.name().equals("API_TYPE"))
        .findFirst()
        .orElseThrow();

    @ParameterizedTest
    @EnumSource(value = MetricFamily.class, names = { "GATEWAY", "ADDITIONAL" }, mode = EnumSource.Mode.EXCLUDE)
    void should_label_the_family_of_a_kind_as_the_api_type_filter_labels_its_value(MetricFamily family) {
        var kind = family.ownKind().orElseThrow();

        assertThat(apiTypeFilter.enumValues())
            .filteredOn(value -> value.value().equals(kind.name()))
            .singleElement()
            .extracting(FilterSpec.EnumValue::label)
            .isEqualTo(family.label());
    }
}
