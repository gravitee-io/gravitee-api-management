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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsMetricQuery;
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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ComputeObservabilityMeasuresUseCaseTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";

    @Mock
    private ObservabilityAnalyticsDataPort analyticsDataPort;

    @Mock
    private FilterRegistry filterRegistry;

    private ComputeObservabilityMeasuresUseCase useCase;

    @BeforeEach
    void setUp() {
        var accessibleApiScope = new AccessibleApiScopeDomainService();
        var filterValidator = new ObservabilityFilterValidator(filterRegistry);
        var pipeline = new AnalyticsRequestPipeline(filterValidator, accessibleApiScope);
        useCase = new ComputeObservabilityMeasuresUseCase(analyticsDataPort, pipeline);

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
                )
            )
        );
    }

    @Nested
    class HappyPath {

        @Test
        void should_delegate_to_port_with_prepared_scope_and_metrics() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );
            JsonNode fakeResponse = JsonNodeFactory.instance.objectNode().put("metrics", "ok");
            when(analyticsDataPort.computeMeasures(any())).thenReturn(fakeResponse);

            var metrics = List.of(new AnalyticsMetricQuery("HTTP_REQUESTS", List.of("COUNT")));
            var output = useCase.execute(new ComputeObservabilityMeasuresUseCase.Input(ORG_ID, ENV_ID, List.of(), null, null, metrics));

            assertThat(output.response()).isEqualTo(fakeResponse);

            var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.MeasuresQuery.class);
            verify(analyticsDataPort).computeMeasures(captor.capture());
            assertThat(captor.getValue().metrics()).hasSize(1);
            assertThat(captor.getValue().metrics().getFirst().metricName()).isEqualTo("HTTP_REQUESTS");
            assertThat(captor.getValue().scope().apiIds()).containsExactly("api-1");
        }
    }

    @Nested
    class FilterValidation {

        @Test
        void should_reject_unknown_filter() {
            var filters = List.of(new FilterCondition("UNKNOWN", FilterOperator.EQ, List.of("val")));
            var metrics = List.of(new AnalyticsMetricQuery("HTTP_REQUESTS", List.of("COUNT")));

            assertThatThrownBy(() ->
                useCase.execute(new ComputeObservabilityMeasuresUseCase.Input(ORG_ID, ENV_ID, filters, null, null, metrics))
            ).isInstanceOf(UnsupportedObservabilityFilterException.class);
        }
    }

    @Nested
    class RBACScoping {

        @Test
        void should_intersect_user_api_filter_with_accessible_scope() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY), new AccessibleApi("api-2", "API 2", ApiType.HTTP_PROXY))
            );
            JsonNode fakeResponse = JsonNodeFactory.instance.objectNode();
            when(analyticsDataPort.computeMeasures(any())).thenReturn(fakeResponse);

            var filters = List.of(new FilterCondition("API", FilterOperator.IN, List.of("api-1")));
            var metrics = List.of(new AnalyticsMetricQuery("HTTP_REQUESTS", List.of("COUNT")));
            useCase.execute(new ComputeObservabilityMeasuresUseCase.Input(ORG_ID, ENV_ID, filters, null, null, metrics));

            var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.MeasuresQuery.class);
            verify(analyticsDataPort).computeMeasures(captor.capture());
            assertThat(captor.getValue().scope().apiIds()).containsExactly("api-1");
        }

        @Test
        void should_return_empty_response_when_user_filters_unknown_api() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );
            JsonNode emptyResponse = JsonNodeFactory.instance.objectNode().putArray("metrics");
            when(analyticsDataPort.emptyMeasuresResponse()).thenReturn(emptyResponse);

            var filters = List.of(new FilterCondition("API", FilterOperator.IN, List.of("non-existent-api")));
            var metrics = List.of(new AnalyticsMetricQuery("HTTP_REQUESTS", List.of("COUNT")));
            var output = useCase.execute(new ComputeObservabilityMeasuresUseCase.Input(ORG_ID, ENV_ID, filters, null, null, metrics));

            assertThat(output.response()).isEqualTo(emptyResponse);
            verify(analyticsDataPort).emptyMeasuresResponse();
        }
    }
}
