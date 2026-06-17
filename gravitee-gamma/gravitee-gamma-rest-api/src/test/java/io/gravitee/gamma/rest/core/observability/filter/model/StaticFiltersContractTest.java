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
package io.gravitee.gamma.rest.core.observability.filter.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.engine.api.query.HttpStatusCodeGroups;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Contract tests ensuring the filter catalog stays consistent with canonical definitions.
 * If a value is added to the catalog but not to the canonical source, these tests fail
 * at compile time (or test time), preventing silent drift.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StaticFiltersContractTest {

    @Test
    void status_code_group_enum_values_should_all_be_valid_canonical_groups() {
        var spec = StaticFilters.HTTP_STATUS_CODE_GROUP.toSpec();
        var catalogKeys = spec.enumValues().stream().map(FilterSpec.EnumValue::value).toList();

        assertThat(catalogKeys)
            .isNotEmpty()
            .allSatisfy(key ->
                assertThat(HttpStatusCodeGroups.GROUP_BOUNDS)
                    .as("Catalog key '%s' must exist in HttpStatusCodeGroups.GROUP_BOUNDS", key)
                    .containsKey(key)
            );
    }

    @Test
    void canonical_groups_should_all_be_in_status_code_group_catalog() {
        var spec = StaticFilters.HTTP_STATUS_CODE_GROUP.toSpec();
        var catalogKeys = spec.enumValues().stream().map(FilterSpec.EnumValue::value).toList();

        assertThat(HttpStatusCodeGroups.GROUP_BOUNDS.keySet())
            .as("All canonical groups must be present in the StaticFilters catalog")
            .allSatisfy(canonicalKey -> assertThat(catalogKeys).contains(canonicalKey));
    }

    @Test
    void status_code_group_labels_should_match_friendly_labels() {
        var spec = StaticFilters.HTTP_STATUS_CODE_GROUP.toSpec();

        assertThat(spec.enumValues()).allSatisfy(enumValue ->
            assertThat(HttpStatusCodeGroups.FRIENDLY_LABELS.get(enumValue.value()))
                .as("Catalog label for '%s' must match HttpStatusCodeGroups.FRIENDLY_LABELS", enumValue.value())
                .isEqualTo(enumValue.label())
        );
    }
}
