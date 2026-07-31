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

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.observability.model.Signal;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetAnalyticsFilterDefinitionsUseCaseTest {

    private final AnalyticsDefinitionYAMLQueryService catalog = new AnalyticsDefinitionYAMLQueryService();

    private final GetAnalyticsFilterDefinitionsUseCase useCase = new GetAnalyticsFilterDefinitionsUseCase(catalog);

    @Test
    void should_return_the_whole_catalog_when_no_signal_is_requested() {
        var specs = useCase.execute(GetAnalyticsFilterDefinitionsUseCase.Input.ALL).specs();

        assertThat(specs).isEqualTo(catalog.getAllFilters());
    }

    @Test
    void should_narrow_to_the_requested_signal() {
        var specs = useCase.execute(new GetAnalyticsFilterDefinitionsUseCase.Input(Set.of(Signal.LOGS))).specs();

        assertThat(specs)
            .isNotEmpty()
            .allSatisfy(spec -> assertThat(spec.appliesTo(Signal.LOGS)).isTrue());
        assertThat(specs).hasSizeLessThan(catalog.getAllFilters().size());
    }

    /**
     * The per-metric endpoint advertises whatever the {@code metrics[].filters} lists declare, and the analytics
     * engine rejects filters the catalog withholds from that signal. Walking the real definition here turns
     * adding one of them to a metric's list into a build failure rather than an advertised-but-rejected filter
     * in production.
     *
     * <p>It crosses two independent parts of the definition file — the per-metric lists and the per-filter
     * {@code signals} — so it restates neither.
     */
    @Test
    void should_not_declare_a_filter_on_a_metric_that_the_catalog_withholds_from_analytics() {
        for (var api : catalog.getApis()) {
            for (var metric : catalog.getMetrics(api.name())) {
                var withheld = catalog
                    .getFilters(metric.name())
                    .stream()
                    .filter(spec -> !spec.appliesTo(Signal.ANALYTICS))
                    .map(FilterSpec::name)
                    .toList();

                assertThat(withheld)
                    .withFailMessage(
                        "Metric %s declares filters the analytics engine will reject: %s. Either drop them from its " +
                            "filters list in analytics-definition.yaml, or give them the ANALYTICS signal.",
                        metric.name(),
                        withheld
                    )
                    .isEmpty();
            }
        }
    }
}
