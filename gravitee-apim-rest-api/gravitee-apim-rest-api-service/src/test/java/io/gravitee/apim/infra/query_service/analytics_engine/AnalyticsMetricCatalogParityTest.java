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
package io.gravitee.apim.infra.query_service.analytics_engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The catalog says a metric exists; a query service says it can answer it. Nothing connects the two,
 * so a metric declared in {@code analytics-definition.yaml} and registered nowhere reaches the UI as
 * an offered metric and fails only when a widget asks for it — a 400 "not supported at the moment"
 * against a dashboard that looks correctly configured.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsMetricCatalogParityTest {

    private final AnalyticsDefinitionYAMLQueryService catalog = new AnalyticsDefinitionYAMLQueryService();

    private final AnalyticsQueryContextProvider provider = new AnalyticsQueryContextProvider(services());

    @Test
    void should_have_a_query_service_for_every_metric_the_catalog_offers() {
        assertThat(declaredMetrics()).allSatisfy(metric -> assertThat(provider.resolve(metric)).isNotNull());
    }

    private List<MetricSpec.Name> declaredMetrics() {
        return catalog
            .getApis()
            .stream()
            .flatMap(api -> catalog.getMetrics(api.name()).stream())
            .map(MetricSpec::name)
            .distinct()
            .toList();
    }

    private static List<AnalyticsEngineQueryService> services() {
        var repository = mock(AnalyticsRepository.class);
        return List.of(
            new HTTPDataPlaneAnalyticsQueryService(repository),
            new MessageDataPlaneQueryService(repository),
            new NativeApiAnalyticsQueryService(repository),
            new EventMetricsAnalyticsQueryService(repository),
            new AuthzAnalyticsQueryService(repository)
        );
    }
}
