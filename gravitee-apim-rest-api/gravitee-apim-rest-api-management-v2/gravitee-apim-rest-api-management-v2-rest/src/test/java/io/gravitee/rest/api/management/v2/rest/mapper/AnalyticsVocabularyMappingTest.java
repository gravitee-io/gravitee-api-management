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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

// A missing constant fails the build; a logs-only filter reaching the analytics catalog does not.
class AnalyticsVocabularyMappingTest {

    private static final Set<String> LOGS_ONLY = Set.of("PAYLOAD", "ERROR_KEY", "REQUEST_ID", "TRANSACTION_ID");

    @Test
    void should_refuse_exactly_the_logs_only_filters_on_the_analytics_catalog() {
        var refused = Arrays.stream(FilterSpec.Name.values())
            .filter(name -> {
                try {
                    AnalyticsDefinitionMapper.INSTANCE.mapAnalyticsFilterName(name);
                    return false;
                } catch (IllegalArgumentException e) {
                    return true;
                }
            })
            .map(Enum::name)
            .collect(java.util.stream.Collectors.toSet());

        assertThat(refused)
            .as("AnalyticsQueryValidator rejects these at query time; the catalog must not advertise them")
            .isEqualTo(LOGS_ONLY);
    }

    @Test
    void should_map_every_analytics_filter_onto_the_same_generated_name() {
        var renamed = Arrays.stream(FilterSpec.Name.values())
            .filter(name -> !LOGS_ONLY.contains(name.name()))
            .filter(name -> !AnalyticsDefinitionMapper.INSTANCE.mapAnalyticsFilterName(name).name().equals(name.name()))
            .toList();

        assertThat(renamed).as("the REST contract name must equal the core name, or clients receive an unknown filter").isEmpty();
    }

    @Test
    void should_map_every_filter_onto_the_observability_catalog() {
        var unmapped = Arrays.stream(FilterSpec.Name.values())
            .filter(name -> {
                var mapped = AnalyticsDefinitionMapper.INSTANCE.mapObservabilityFilterName(name);
                return mapped == null || !mapped.name().equals(name.name());
            })
            .toList();

        assertThat(unmapped).as("the observability catalog carries every filter, logs-only ones included").isEmpty();
    }
}
