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
package io.gravitee.gamma.rest.core.observability.logs.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ValidationDomainException;
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
import io.gravitee.gamma.rest.core.observability.logs.model.LogEntry;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsPage;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsSearchQuery;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchObservabilityLogsUseCaseTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";

    @Mock
    private ObservabilityLogsDataPort logsDataPort;

    @Mock
    private FilterRegistry filterRegistry;

    private AccessibleApiScopeDomainService accessibleApiScope;

    private SearchObservabilityLogsUseCase useCase;

    @BeforeEach
    void setUp() {
        accessibleApiScope = new AccessibleApiScopeDomainService();
        var filterValidator = new ObservabilityFilterValidator(filterRegistry);
        useCase = new SearchObservabilityLogsUseCase(logsDataPort, filterValidator, accessibleApiScope);

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
                    io.gravitee.gamma.rest.core.observability.filter.model.ApiType.ALL
                ),
                new FilterSpec(
                    "HTTP_STATUS",
                    "Status Code",
                    FilterType.NUMBER,
                    List.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE),
                    null,
                    new FilterSpec.Range(100, 599),
                    Set.of(Signal.LOGS, Signal.ANALYTICS),
                    Set.of(io.gravitee.gamma.rest.core.observability.filter.model.ApiType.HTTP_PROXY)
                ),
                new FilterSpec(
                    "APPLICATION",
                    "Application",
                    FilterType.KEYWORD,
                    List.of(FilterOperator.EQ, FilterOperator.IN),
                    null,
                    null,
                    Set.of(Signal.LOGS, Signal.ANALYTICS),
                    io.gravitee.gamma.rest.core.observability.filter.model.ApiType.ALL
                ),
                new FilterSpec(
                    "GATEWAY",
                    "Gateway",
                    FilterType.KEYWORD,
                    List.of(FilterOperator.EQ, FilterOperator.IN),
                    null,
                    null,
                    Set.of(Signal.ANALYTICS),
                    io.gravitee.gamma.rest.core.observability.filter.model.ApiType.ALL
                )
            )
        );
    }

    @Nested
    class Scoping {

        @Test
        void should_return_empty_page_when_no_accessible_apis() {
            when(logsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(List.of());

            var output = useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, List.of(), null, null, 1, 20));

            assertThat(output.data()).isEqualTo(LogsPage.EMPTY);
            assertThat(output.page()).isEqualTo(1);
            assertThat(output.perPage()).isEqualTo(20);
            verify(logsDataPort).loadAccessibleApis(ORG_ID, ENV_ID);
        }

        @Test
        void should_filter_accessible_apis_by_supported_api_types() {
            when(logsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(
                    new AccessibleApi("api-proxy", "Proxy API", ApiType.HTTP_PROXY),
                    new AccessibleApi("api-message", "Message API", ApiType.MESSAGE),
                    new AccessibleApi("api-llm", "LLM API", ApiType.LLM)
                )
            );
            when(logsDataPort.searchLogs(eq(ORG_ID), eq(ENV_ID), any())).thenReturn(LogsPage.EMPTY);

            useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, List.of(), null, null, 1, 20));

            var captor = ArgumentCaptor.forClass(LogsSearchQuery.class);
            verify(logsDataPort).searchLogs(eq(ORG_ID), eq(ENV_ID), captor.capture());
            assertThat(captor.getValue().apiIds()).containsExactlyInAnyOrder("api-proxy", "api-llm");
        }

        @Test
        void should_intersect_user_api_filter_with_accessible_apis() {
            when(logsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY), new AccessibleApi("api-2", "API 2", ApiType.HTTP_PROXY))
            );
            when(logsDataPort.searchLogs(eq(ORG_ID), eq(ENV_ID), any())).thenReturn(LogsPage.EMPTY);

            var filters = List.of(new FilterCondition("API", FilterOperator.EQ, List.of("api-1")));
            useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, filters, null, null, 1, 20));

            var captor = ArgumentCaptor.forClass(LogsSearchQuery.class);
            verify(logsDataPort).searchLogs(eq(ORG_ID), eq(ENV_ID), captor.capture());
            assertThat(captor.getValue().apiIds()).containsExactly("api-1");
        }

        @Test
        void should_return_empty_page_when_user_api_filter_excludes_all_accessible_apis() {
            when(logsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );

            var filters = List.of(new FilterCondition("API", FilterOperator.EQ, List.of("api-unknown")));
            var output = useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, filters, null, null, 1, 20));

            assertThat(output.data()).isEqualTo(LogsPage.EMPTY);
        }
    }

    @Nested
    class FilterValidation {

        @Test
        void should_reject_unknown_filter_name() {
            var filters = List.of(new FilterCondition("UNKNOWN_FILTER", FilterOperator.EQ, List.of("val")));

            assertThatThrownBy(() ->
                useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, filters, null, null, 1, 20))
            ).isInstanceOf(UnsupportedObservabilityFilterException.class);
            verifyNoInteractions(logsDataPort);
        }

        @Test
        void should_reject_filter_whose_signals_exclude_logs() {
            var filters = List.of(new FilterCondition("GATEWAY", FilterOperator.EQ, List.of("gw-1")));

            assertThatThrownBy(() ->
                useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, filters, null, null, 1, 20))
            ).isInstanceOf(UnsupportedObservabilityFilterException.class);
            verifyNoInteractions(logsDataPort);
        }

        @Test
        void should_reject_operator_not_advertised_by_the_catalog() {
            var filters = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.CONTAINS, List.of("200")));

            assertThatThrownBy(() ->
                useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, filters, null, null, 1, 20))
            ).isInstanceOf(UnsupportedObservabilityFilterException.class);
            verifyNoInteractions(logsDataPort);
        }

        @Test
        void should_accept_valid_logs_filter() {
            when(logsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );
            when(logsDataPort.searchLogs(eq(ORG_ID), eq(ENV_ID), any())).thenReturn(LogsPage.EMPTY);

            var filters = List.of(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("200")));
            var output = useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, filters, null, null, 1, 20));

            assertThat(output.data()).isNotNull();
        }
    }

    @Nested
    class TimeRange {

        @Test
        void should_reject_invalid_time_range() {
            var from = Instant.parse("2026-06-11T12:00:00Z");
            var to = Instant.parse("2026-06-10T12:00:00Z");

            assertThatThrownBy(() ->
                useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, List.of(), from, to, 1, 20))
            ).isInstanceOf(ValidationDomainException.class);

            verifyNoInteractions(logsDataPort);
        }

        @Test
        void should_pass_time_range_to_port() {
            when(logsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );
            when(logsDataPort.searchLogs(eq(ORG_ID), eq(ENV_ID), any())).thenReturn(LogsPage.EMPTY);

            var from = Instant.parse("2026-06-10T00:00:00Z");
            var to = Instant.parse("2026-06-11T00:00:00Z");
            useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, List.of(), from, to, 1, 20));

            var captor = ArgumentCaptor.forClass(LogsSearchQuery.class);
            verify(logsDataPort).searchLogs(eq(ORG_ID), eq(ENV_ID), captor.capture());
            assertThat(captor.getValue().from()).isEqualTo(from.toEpochMilli());
            assertThat(captor.getValue().to()).isEqualTo(to.toEpochMilli());
        }
    }

    @Nested
    class DefaultEntrypointScoping {

        @Test
        void should_inject_default_entrypoint_filter_when_none_provided() {
            when(logsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );
            when(logsDataPort.searchLogs(eq(ORG_ID), eq(ENV_ID), any())).thenReturn(LogsPage.EMPTY);

            useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, List.of(), null, null, 1, 20));

            var captor = ArgumentCaptor.forClass(LogsSearchQuery.class);
            verify(logsDataPort).searchLogs(eq(ORG_ID), eq(ENV_ID), captor.capture());
            var entrypointCondition = captor
                .getValue()
                .conditions()
                .stream()
                .filter(c -> "ENTRYPOINT".equals(c.name()))
                .findFirst();
            assertThat(entrypointCondition).isPresent();
            assertThat(entrypointCondition.get().values()).containsExactlyInAnyOrder(
                "http-get",
                "http-post",
                "http-proxy",
                "llm-proxy",
                "mcp-proxy"
            );
        }
    }

    @Nested
    class PaginationDefaults {

        @Test
        void should_use_default_page_and_perPage_when_zero_or_negative() {
            when(logsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );
            when(logsDataPort.searchLogs(eq(ORG_ID), eq(ENV_ID), any())).thenReturn(LogsPage.EMPTY);

            var output = useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, List.of(), null, null, 0, -1));

            var captor = ArgumentCaptor.forClass(LogsSearchQuery.class);
            verify(logsDataPort).searchLogs(eq(ORG_ID), eq(ENV_ID), captor.capture());
            assertThat(captor.getValue().page()).isEqualTo(1);
            assertThat(captor.getValue().perPage()).isEqualTo(20);
            assertThat(output.page()).isEqualTo(1);
            assertThat(output.perPage()).isEqualTo(20);
        }

        @Test
        void should_use_default_page_and_perPage_when_null() {
            when(logsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );
            when(logsDataPort.searchLogs(eq(ORG_ID), eq(ENV_ID), any())).thenReturn(LogsPage.EMPTY);

            var output = useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, List.of(), null, null, null, null));

            assertThat(output.page()).isEqualTo(1);
            assertThat(output.perPage()).isEqualTo(20);
        }

        @Test
        void should_clamp_perPage_to_max() {
            when(logsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );
            when(logsDataPort.searchLogs(eq(ORG_ID), eq(ENV_ID), any())).thenReturn(LogsPage.EMPTY);

            var output = useCase.execute(new SearchObservabilityLogsUseCase.Input(ORG_ID, ENV_ID, List.of(), null, null, 1, 500));

            var captor = ArgumentCaptor.forClass(LogsSearchQuery.class);
            verify(logsDataPort).searchLogs(eq(ORG_ID), eq(ENV_ID), captor.capture());
            assertThat(captor.getValue().perPage()).isEqualTo(100);
            assertThat(output.perPage()).isEqualTo(100);
        }
    }
}
