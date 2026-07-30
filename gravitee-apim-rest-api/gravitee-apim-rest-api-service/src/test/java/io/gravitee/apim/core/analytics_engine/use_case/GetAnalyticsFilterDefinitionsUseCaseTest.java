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
package io.gravitee.apim.core.analytics_engine.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.FilterSignal;
import io.gravitee.apim.core.observability.model.FilterType;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetAnalyticsFilterDefinitionsUseCaseTest {

    private final AnalyticsDefinitionQueryService definition = mock(AnalyticsDefinitionQueryService.class);
    private final GetAnalyticsFilterDefinitionsUseCase useCase = new GetAnalyticsFilterDefinitionsUseCase(definition);

    @Test
    void should_advertise_the_surfaces_supporting_each_filter() {
        when(definition.getAllFilters()).thenReturn(
            List.of(
                filter(FilterSpec.Name.API),
                filter(FilterSpec.Name.TRANSACTION_ID),
                filter(FilterSpec.Name.GEO_IP_COUNTRY),
                filter(FilterSpec.Name.HTTP_PATH)
            )
        );

        var specs = useCase.execute().specs();

        assertThat(specs)
            .extracting(FilterSpec::name, FilterSpec::signals)
            .containsExactly(
                tuple(FilterSpec.Name.API, List.of(FilterSignal.ANALYTICS, FilterSignal.LOGS)),
                // Logs-only: rejected by the analytics engine.
                tuple(FilterSpec.Name.TRANSACTION_ID, List.of(FilterSignal.LOGS)),
                tuple(FilterSpec.Name.GEO_IP_COUNTRY, List.of(FilterSignal.ANALYTICS)),
                // The logs engine names the path filter URI — still a logs-supported filter.
                tuple(FilterSpec.Name.HTTP_PATH, List.of(FilterSignal.ANALYTICS, FilterSignal.LOGS))
            );
    }

    /**
     * Guards the name-based mapping in {@code supportsLogs} against silent drift: every logs-engine
     * filter must either share a catalog name, be an explicit translation, or be a documented
     * exception. Adding or renaming a value in either enum without updating the mapping fails here.
     */
    @Test
    void should_account_for_every_logs_engine_filter_in_the_catalog_mapping() {
        var catalogNames = Arrays.stream(FilterSpec.Name.values()).map(Enum::name).collect(Collectors.toSet());
        var translatedToCatalog = Map.of("URI", "HTTP_PATH");
        // Logs-engine filters with no same-named catalog filter, intentionally not advertised (see supportsLogs).
        var intentionallyUnmapped = Set.of("MCP_METHOD", "RESPONSE_TIME");

        for (var logsFilter : FilterName.values()) {
            var name = logsFilter.name();
            var accounted = catalogNames.contains(name) || translatedToCatalog.containsKey(name) || intentionallyUnmapped.contains(name);
            assertThat(accounted)
                .withFailMessage(
                    "Logs filter %s is neither a catalog filter name, a documented translation, nor a documented exception — update GetAnalyticsFilterDefinitionsUseCase.supportsLogs and this test",
                    name
                )
                .isTrue();
        }
        assertThat(translatedToCatalog.values()).allSatisfy(catalog -> assertThat(catalogNames).contains(catalog));
    }

    /**
     * The per-metric endpoint advertises whatever the yaml metric filter lists declare, and the
     * analytics engine rejects the observability-only filters. Walking the real definition here
     * makes adding one of them to any metric's list a build failure instead of an
     * advertised-but-rejected filter in production.
     */
    @Test
    void should_not_declare_an_analytics_rejected_filter_on_any_metric() {
        var yaml = new AnalyticsDefinitionYAMLQueryService();

        for (var api : yaml.getApis()) {
            for (var metric : yaml.getMetrics(api.name())) {
                var rejected = yaml
                    .getFilters(metric.name())
                    .stream()
                    .map(FilterSpec::name)
                    .filter(name -> !AnalyticsQueryValidator.supportsAnalytics(name))
                    .toList();
                assertThat(rejected)
                    .withFailMessage("Metric %s declares filters the analytics engine rejects: %s", metric.name(), rejected)
                    .isEmpty();
            }
        }
    }

    private static FilterSpec filter(FilterSpec.Name name) {
        return new FilterSpec(name, name.name(), FilterType.KEYWORD, null, null, List.of(FilterOperator.EQ), List.of());
    }
}
