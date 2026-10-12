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
package io.gravitee.apim.rest.api.mcp.infra;

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.CALLER;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.logs_engine.model.ApiLog;
import io.gravitee.apim.core.logs_engine.model.ArrayFilter;
import io.gravitee.apim.core.logs_engine.model.BaseApplication;
import io.gravitee.apim.core.logs_engine.model.Filter;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.logs_engine.model.HttpMethod;
import io.gravitee.apim.core.logs_engine.model.NumericFilter;
import io.gravitee.apim.core.logs_engine.model.Operator;
import io.gravitee.apim.core.logs_engine.model.Pagination;
import io.gravitee.apim.core.logs_engine.model.SearchLogsRequest;
import io.gravitee.apim.core.logs_engine.model.SearchLogsResponse;
import io.gravitee.apim.core.logs_engine.model.StringFilter;
import io.gravitee.apim.core.logs_engine.use_case.SearchEnvironmentLogsUseCase;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.FailedRequest;
import io.gravitee.apim.rest.api.mcp.core.model.StatusGroup;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.port.RequestLogs;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LogsEngineRequestLogsTest {

    private static final TimeWindow WINDOW = TimeWindow.parse("15m", null, NOW);

    private final SearchEnvironmentLogsUseCase searchLogs = mock(SearchEnvironmentLogsUseCase.class);
    private final LogsEngineRequestLogs logs = new LogsEngineRequestLogs(searchLogs);

    @Test
    void should_search_every_error_status_of_the_window_when_nothing_else_is_asked() {
        givenLogs(0);

        logs.failures(CALLER, WINDOW, new RequestLogs.Query(null, null, null), 20);

        var request = searched();
        assertThat(request.filters()).containsExactly(new Filter(new NumericFilter(FilterName.HTTP_STATUS, Operator.GTE, 400)));
        assertThat(request.timeRange().from()).isEqualTo(WINDOW.from().atOffset(ZoneOffset.UTC));
        assertThat(request.timeRange().to()).isEqualTo(NOW.atOffset(ZoneOffset.UTC));
        assertThat(request.page()).isEqualTo(1);
        assertThat(request.perPage()).isEqualTo(20);
    }

    @Test
    void should_narrow_the_search_to_the_api_status_class_and_error_key() {
        givenLogs(0);

        logs.failures(CALLER, WINDOW, new RequestLogs.Query("orders-id", StatusGroup.CLIENT_ERROR, "API_KEY_INVALID"), 5);

        assertThat(searched().filters()).containsExactly(
            new Filter(new ArrayFilter(FilterName.HTTP_STATUS_CODE_GROUP, Operator.IN, List.of("4XX"))),
            new Filter(new StringFilter(FilterName.API, Operator.EQ, "orders-id")),
            new Filter(new StringFilter(FilterName.ERROR_KEY, Operator.EQ, "API_KEY_INVALID"))
        );
    }

    @Test
    void should_report_the_reason_the_gateway_recorded_for_each_failure() {
        givenLogs(
            1200,
            ApiLog.builder()
                .apiId("orders-id")
                .apiName("Orders")
                .timestamp(NOW.atOffset(ZoneOffset.UTC))
                .requestId("request-id")
                .method(HttpMethod.GET)
                .uri("/orders/42")
                .status(401)
                .errorKey("API_KEY_INVALID")
                .errorComponentName("api-key")
                .errorComponentType("POLICY")
                .application(new BaseApplication("1", "Unknown", null, null, null, null, null))
                .gateway("gateway-1")
                .gatewayResponseTime(3)
                .build()
        );

        var page = logs.failures(CALLER, WINDOW, new RequestLogs.Query(null, null, null), 20);

        assertThat(page.total()).isEqualTo(1200);
        assertThat(page.requests()).containsExactly(
            new FailedRequest(
                NOW,
                new ApiRef("orders-id", "Orders"),
                "GET",
                "/orders/42",
                401,
                "API_KEY_INVALID",
                "api-key",
                null,
                "Unknown",
                null,
                "gateway-1",
                3,
                "request-id"
            )
        );
    }

    private void givenLogs(long total, ApiLog... entries) {
        when(searchLogs.execute(any())).thenReturn(
            new SearchEnvironmentLogsUseCase.Output(
                new SearchLogsResponse(List.of(entries), new Pagination(1, 20, 1, entries.length, total))
            )
        );
    }

    private SearchLogsRequest searched() {
        var input = ArgumentCaptor.forClass(SearchEnvironmentLogsUseCase.Input.class);
        verify(searchLogs).execute(input.capture());
        assertThat(input.getValue().auditInfo()).isEqualTo(CALLER);
        return input.getValue().request();
    }
}
