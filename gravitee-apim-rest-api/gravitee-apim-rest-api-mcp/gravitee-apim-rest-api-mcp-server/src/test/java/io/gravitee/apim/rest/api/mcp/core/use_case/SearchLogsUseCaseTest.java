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
package io.gravitee.apim.rest.api.mcp.core.use_case;

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.MCP_CALLER;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.NOW;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.PROD;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.logs_engine.model.ApiLog;
import io.gravitee.apim.core.logs_engine.model.ArrayFilter;
import io.gravitee.apim.core.logs_engine.model.BaseApplication;
import io.gravitee.apim.core.logs_engine.model.Filter;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.logs_engine.model.HttpMethod;
import io.gravitee.apim.core.logs_engine.model.NumericFilter;
import io.gravitee.apim.core.logs_engine.model.Operator;
import io.gravitee.apim.core.logs_engine.model.Pagination;
import io.gravitee.apim.core.logs_engine.model.SearchLogsResponse;
import io.gravitee.apim.core.logs_engine.model.StringFilter;
import io.gravitee.apim.rest.api.mcp.core.model.AnalyticsQuery;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchLogsUseCaseTest {

    private final Fakes.StubLogSearch logs = new Fakes.StubLogSearch();
    private final SearchLogsUseCase useCase = new SearchLogsUseCase(
        Fakes.environments(PROD),
        Fakes.apis(new ApiRef("orders-id", "Orders")),
        logs
    );

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void should_pass_the_filters_to_the_logs_engine_in_its_own_shapes_with_api_names_resolved() {
        var filters = List.of(
            new AnalyticsQuery.Filter("api", "EQ", "Orders"),
            new AnalyticsQuery.Filter("HTTP_STATUS", "GTE", 400),
            new AnalyticsQuery.Filter("HTTP_STATUS_CODE_GROUP", "IN", List.of("4XX", "5XX"))
        );

        var output = useCase.execute(new SearchLogsUseCase.Input(MCP_CALLER, null, filters, 2, 5, "30m", null));

        var window = TimeWindow.parse("30m", null, NOW);
        assertThat(logs.caller.environmentId()).isEqualTo("e-prod");
        assertThat(logs.request.page()).isEqualTo(2);
        assertThat(logs.request.perPage()).isEqualTo(5);
        assertThat(logs.request.timeRange().from().toInstant()).isEqualTo(window.from());
        assertThat(logs.request.filters()).containsExactly(
            new Filter(new StringFilter(FilterName.API, Operator.EQ, "orders-id")),
            new Filter(new NumericFilter(FilterName.HTTP_STATUS, Operator.GTE, 400)),
            new Filter(new ArrayFilter(FilterName.HTTP_STATUS_CODE_GROUP, Operator.IN, List.of("4XX", "5XX")))
        );
        assertThat(output.page()).isEqualTo(2);
    }

    @Test
    void should_return_what_the_gateway_recorded_for_each_request() {
        logs.response = new SearchLogsResponse(
            List.of(
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
                    .application(new BaseApplication("1", "Unknown", null, null, null, null, null))
                    .gateway("gateway-1")
                    .gatewayResponseTime(3)
                    .build()
            ),
            new Pagination(1, 20, 1, 1, 1200L)
        );

        var output = useCase.execute(new SearchLogsUseCase.Input(MCP_CALLER, null, null, null, null, null, null));

        assertThat(output.total()).isEqualTo(1200);
        var entry = output.logs().getFirst();
        assertThat(entry.at()).isEqualTo(NOW);
        assertThat(entry.api()).isEqualTo(new ApiRef("orders-id", "Orders"));
        assertThat(entry.status()).isEqualTo(401);
        assertThat(entry.errorKey()).isEqualTo("API_KEY_INVALID");
        assertThat(entry.errorComponent()).isEqualTo("api-key");
        assertThat(entry.application()).isEqualTo(new SearchLogsUseCase.Named("1", "Unknown"));
        assertThat(entry.gatewayResponseTimeMs()).isEqualTo(3);
        assertThat(output.summary()).isEqualTo("1200 request(s) in the window, 1 returned, newest first.");
    }
}
