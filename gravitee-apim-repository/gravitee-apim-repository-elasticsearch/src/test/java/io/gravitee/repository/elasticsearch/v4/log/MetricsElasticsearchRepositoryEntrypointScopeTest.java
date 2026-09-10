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
package io.gravitee.repository.elasticsearch.v4.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.analytics.engine.api.query.ObservabilityEntrypoints;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery.Filter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * What the logs default scope, an exact condition and the legacy predicate each select on one API exposing
 * every entrypoint family (the "Entrypoint scope API" block of {@code freemarker-v4-analytics/v4-metrics.ftl},
 * shared with the analytics engine tests so both signals are proven on the same documents).
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
public class MetricsElasticsearchRepositoryEntrypointScopeTest extends AbstractElasticsearchRepositoryTest {

    private static final String SCOPE_API = "entrypoint-scope-api-001";

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private MetricsElasticsearchRepository metricsV4Repository;

    private long total(Filter.FilterBuilder filter) {
        return metricsV4Repository
            .searchMetrics(
                queryContext,
                MetricsQuery.builder().page(1).size(20).filter(filter.apiIds(Set.of(SCOPE_API)).build()).build(),
                List.of(DefinitionVersion.V4)
            )
            .total();
    }

    @Test
    void should_keep_every_request_outside_the_logs_exclusions_including_native_unknown_and_unattributed_ones() {
        // Everything but the Edge report: the six HTTP-scope entrypoints, the sse subscription the logs screen
        // lists and analytics does not, the native connection, the unknown entrypoint, and the request refused
        // before an entrypoint was selected.
        assertThat(
            total(Filter.builder().entrypointScope(Filter.EntrypointScope.excluding(ObservabilityEntrypoints.LOGS_EXCLUDED_IDS)))
        ).isEqualTo(10L);
    }

    @Test
    void should_select_exactly_the_given_entrypoint() {
        assertThat(total(Filter.builder().entrypointScope(Filter.EntrypointScope.exactly(List.of("llm-proxy"))))).isEqualTo(1L);
    }

    @Test
    void should_reach_requests_without_an_entrypoint_only_through_the_synthetic_value() {
        assertThat(total(Filter.builder().entrypointScope(Filter.EntrypointScope.exactly(List.of("(none)"))))).isEqualTo(1L);
    }

    @Test
    void should_keep_the_legacy_predicate_for_callers_that_send_ids_only() {
        // the Console runtime logs keep matching the ids plus every document without an entrypoint id
        assertThat(total(Filter.builder().entrypointIds(Set.of("llm-proxy")))).isEqualTo(2L);
    }
}
