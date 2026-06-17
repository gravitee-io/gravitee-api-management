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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsRequestPipelineTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";

    @Mock
    private ObservabilityAnalyticsDataPort analyticsDataPort;

    @Mock
    private FilterRegistry filterRegistry;

    private AnalyticsRequestPipeline pipeline;

    @BeforeEach
    void setUp() {
        var accessibleApiScope = new AccessibleApiScopeDomainService();
        var filterValidator = new ObservabilityFilterValidator(filterRegistry);
        pipeline = new AnalyticsRequestPipeline(filterValidator, accessibleApiScope);

        when(filterRegistry.getFilters(any(), any())).thenReturn(
            List.of(
                new FilterSpec(
                    "API",
                    "API",
                    FilterType.KEYWORD,
                    List.of(FilterOperator.EQ, FilterOperator.IN),
                    null,
                    null,
                    Set.of(Signal.LOGS, Signal.ANALYTICS),
                    ApiType.ALL
                ),
                new FilterSpec(
                    "HTTP_STATUS",
                    "Status Code",
                    FilterType.NUMBER,
                    List.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE),
                    null,
                    new FilterSpec.Range(100, 599),
                    Set.of(Signal.LOGS, Signal.ANALYTICS),
                    Set.of(ApiType.HTTP_PROXY)
                ),
                new FilterSpec(
                    "ENTRYPOINT",
                    "Entrypoint",
                    FilterType.KEYWORD,
                    List.of(FilterOperator.IN),
                    null,
                    null,
                    Set.of(Signal.LOGS, Signal.ANALYTICS),
                    ApiType.ALL
                )
            )
        );
    }

    @Nested
    class Scoping {

        @Test
        void should_compute_scope_from_accessible_apis() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(
                    new AccessibleApi("api-proxy", "Proxy API", ApiType.HTTP_PROXY),
                    new AccessibleApi("api-llm", "LLM API", ApiType.LLM)
                )
            );

            var scope = pipeline.prepare(ORG_ID, ENV_ID, List.of(), null, null, analyticsDataPort);

            assertThat(scope.apiIds()).containsExactlyInAnyOrder("api-proxy", "api-llm");
        }

        @Test
        void should_intersect_user_api_filter_with_accessible_apis() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY), new AccessibleApi("api-2", "API 2", ApiType.HTTP_PROXY))
            );

            var filters = List.of(new FilterCondition("API", FilterOperator.EQ, List.of("api-1")));
            var scope = pipeline.prepare(ORG_ID, ENV_ID, filters, null, null, analyticsDataPort);

            assertThat(scope.apiIds()).containsExactly("api-1");
        }

        @Test
        void should_return_empty_scope_when_no_accessible_apis() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(List.of());

            var scope = pipeline.prepare(ORG_ID, ENV_ID, List.of(), null, null, analyticsDataPort);

            assertThat(scope.apiIds()).isEmpty();
        }

        @Test
        void should_return_empty_scope_when_user_filters_unknown_api() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );

            var filters = List.of(new FilterCondition("API", FilterOperator.IN, List.of("non-existent-api")));
            var scope = pipeline.prepare(ORG_ID, ENV_ID, filters, null, null, analyticsDataPort);

            assertThat(scope.isEmpty()).isTrue();
            assertThat(scope.apiIds()).isEmpty();
        }
    }

    @Nested
    class FilterValidation {

        @Test
        void should_reject_unknown_filter_name() {
            var filters = List.of(new FilterCondition("UNKNOWN", FilterOperator.EQ, List.of("val")));

            assertThatThrownBy(() -> pipeline.prepare(ORG_ID, ENV_ID, filters, null, null, analyticsDataPort)).isInstanceOf(
                UnsupportedObservabilityFilterException.class
            );
        }

        @Test
        void should_reject_unsupported_operator() {
            var filters = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.CONTAINS, List.of("200")));

            assertThatThrownBy(() -> pipeline.prepare(ORG_ID, ENV_ID, filters, null, null, analyticsDataPort)).isInstanceOf(
                UnsupportedObservabilityFilterException.class
            );
        }
    }

    @Nested
    class TimeRange {

        @Test
        void should_reject_inverted_time_range() {
            var from = Instant.parse("2026-06-11T12:00:00Z");
            var to = Instant.parse("2026-06-10T12:00:00Z");

            assertThatThrownBy(() -> pipeline.prepare(ORG_ID, ENV_ID, List.of(), from, to, analyticsDataPort))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("from");
        }

        @Test
        void should_preserve_valid_time_range() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );

            var from = Instant.parse("2026-06-10T00:00:00Z");
            var to = Instant.parse("2026-06-11T00:00:00Z");
            var scope = pipeline.prepare(ORG_ID, ENV_ID, List.of(), from, to, analyticsDataPort);

            assertThat(scope.from()).isEqualTo(from);
            assertThat(scope.to()).isEqualTo(to);
        }
    }

    @Nested
    class DefaultEntrypointScoping {

        @Test
        void should_inject_default_entrypoint_filter_when_none_provided() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );

            var scope = pipeline.prepare(ORG_ID, ENV_ID, List.of(), null, null, analyticsDataPort);

            var entrypoint = scope
                .filters()
                .stream()
                .filter(c -> "ENTRYPOINT".equals(c.name()))
                .findFirst();
            assertThat(entrypoint).isPresent();
            assertThat(entrypoint.get().values()).containsExactlyInAnyOrder(
                "http-get",
                "http-post",
                "http-proxy",
                "llm-proxy",
                "mcp-proxy"
            );
        }

        @Test
        void should_preserve_user_entrypoint_filter_when_provided() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );

            var filters = List.of(new FilterCondition("ENTRYPOINT", FilterOperator.IN, List.of("http-proxy")));
            var scope = pipeline.prepare(ORG_ID, ENV_ID, filters, null, null, analyticsDataPort);

            var entrypoints = scope
                .filters()
                .stream()
                .filter(c -> "ENTRYPOINT".equals(c.name()))
                .toList();
            assertThat(entrypoints).hasSize(1);
            assertThat(entrypoints.getFirst().values()).containsExactly("http-proxy");
        }
    }
}
