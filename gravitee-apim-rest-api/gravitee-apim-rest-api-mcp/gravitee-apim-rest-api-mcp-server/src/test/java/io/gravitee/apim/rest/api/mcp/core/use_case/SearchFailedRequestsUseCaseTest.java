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

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.CALLER;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.FailedRequest;
import io.gravitee.apim.rest.api.mcp.core.model.StatusGroup;
import io.gravitee.apim.rest.api.mcp.core.port.RequestLogs;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchFailedRequestsUseCaseTest {

    private static final ApiRef ORDERS = new ApiRef("orders-id", "Orders");

    private final Fakes.InMemoryRequestLogs logs = new Fakes.InMemoryRequestLogs();
    private final SearchFailedRequestsUseCase useCase = new SearchFailedRequestsUseCase(new Fakes.Access(), Fakes.apis(ORDERS), logs);

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void should_count_the_returned_requests_per_error_key() {
        logs.page = new RequestLogs.Page(
            1200,
            List.of(failure(401, "API_KEY_INVALID"), failure(401, "API_KEY_INVALID"), failure(502, null))
        );

        var failures = execute(null, null, null, null);

        assertThat(failures.total()).isEqualTo(1200);
        assertThat(failures.requests()).hasSize(3);
        assertThat(failures.errorKeys()).containsExactly(entry("API_KEY_INVALID", 2L), entry("NONE (answered by the backend)", 1L));
        assertThat(failures.summary()).isEqualTo("1200 failed request(s) in the window, 3 returned, newest first.");
    }

    @Test
    void should_query_the_api_status_group_and_error_key_asked_for() {
        execute("Orders", "4XX", " API_KEY_INVALID ", 5);

        assertThat(logs.query).isEqualTo(new RequestLogs.Query("orders-id", StatusGroup.CLIENT_ERROR, "API_KEY_INVALID"));
        assertThat(logs.limit).isEqualTo(5);
    }

    @Test
    void should_return_twenty_requests_by_default_and_fifty_at_most() {
        execute(null, null, null, null);
        assertThat(logs.limit).isEqualTo(20);

        execute(null, null, null, 500);
        assertThat(logs.limit).isEqualTo(50);
    }

    private SearchFailedRequestsUseCase.Output execute(String api, String statusGroup, String errorKey, Integer limit) {
        return useCase.execute(new SearchFailedRequestsUseCase.Input(CALLER, api, statusGroup, errorKey, null, null, limit));
    }

    private static FailedRequest failure(int status, String errorKey) {
        return new FailedRequest(NOW, ORDERS, "GET", "/orders", status, errorKey, null, null, "Unknown", null, "gw-1", 3, "request-id");
    }
}
