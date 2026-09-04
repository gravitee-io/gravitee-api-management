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
package io.gravitee.gamma.rest.core.observability.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Holds the observability catalog to the platform analytics catalog it delegates to. A filter Gamma
 * advertises for the analytics signal is served by the platform engine, so the platform must
 * declare it and declare it for analytics; otherwise value listing and queries fail at runtime with
 * an error the caller cannot act on. Gamma-only names are legitimate only on the logs side, where
 * Gamma owns the translation.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FilterCatalogParityTest {

    private static final String RECORD_TYPE = "RECORD_TYPE";

    private final FilterRegistry gamma = new SpiFilterRegistry();
    private final AnalyticsDefinitionYAMLQueryService platform = new AnalyticsDefinitionYAMLQueryService();

    @Test
    void every_gamma_analytics_filter_should_be_a_platform_filter_that_applies_to_analytics() {
        List<String> offenders = gamma
            .getFilters(Set.of(Signal.ANALYTICS), null)
            .stream()
            .map(spec -> spec.name())
            .filter(name -> !RECORD_TYPE.equals(name))
            .filter(name ->
                platformFilter(name)
                    .map(spec -> !appliesToAnalytics(spec))
                    .orElse(true)
            )
            .toList();

        assertThat(offenders).as("Gamma analytics filters the platform catalog does not declare for analytics").isEmpty();
    }

    @Test
    void gamma_only_filter_names_should_never_apply_to_analytics() {
        List<String> gammaOnlyAnalytics = gamma
            .getFilters(null, null)
            .stream()
            .filter(spec -> !RECORD_TYPE.equals(spec.name()))
            .filter(spec -> platformFilter(spec.name()).isEmpty())
            .filter(spec -> spec.signals().contains(Signal.ANALYTICS))
            .map(spec -> spec.name())
            .toList();

        assertThat(gammaOnlyAnalytics).as("filters the platform catalog does not know yet advertised by Gamma for analytics").isEmpty();
    }

    /** Two catalogs, two {@code Signal} enums: the platform one is spelled out here, once. */
    private static boolean appliesToAnalytics(FilterSpec platformSpec) {
        return platformSpec.appliesTo(io.gravitee.apim.core.observability.model.Signal.ANALYTICS);
    }

    private Optional<FilterSpec> platformFilter(String gammaName) {
        try {
            return platform.findFilter(FilterSpec.Name.valueOf(gammaName));
        } catch (IllegalArgumentException unknownToThePlatform) {
            return Optional.empty();
        }
    }
}
