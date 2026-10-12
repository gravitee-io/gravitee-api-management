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
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.traffic;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.trafficAnsweredIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Finding;
import io.gravitee.apim.rest.api.mcp.core.model.Finding.Kind;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficSample;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import io.gravitee.apim.rest.api.mcp.core.use_case.GetHealthOverviewUseCase.Status;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import io.gravitee.apim.rest.api.mcp.fakes.InMemoryTrafficAnalytics;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetHealthOverviewUseCaseTest {

    private static final TimeWindow WINDOW = TimeWindow.parse("15m", null, NOW);
    private static final TimeWindow BASELINE = WINDOW.baseline();
    private static final ApiRef ORDERS = new ApiRef("orders-id", "Orders");
    private static final ApiRef STOCK = new ApiRef("stock-id", "Stock");

    private final Fakes.Access access = new Fakes.Access();
    private final InMemoryTrafficAnalytics analytics = new InMemoryTrafficAnalytics();
    private final GetHealthOverviewUseCase useCase = new GetHealthOverviewUseCase(access, analytics);

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void should_report_healthy_when_traffic_matches_the_baseline() {
        analytics.total(WINDOW, traffic(1000, 10, 0)).total(BASELINE, traffic(980, 12, 0));
        analytics.samples(WINDOW, sample(ORDERS, traffic(1000, 10, 0))).samples(BASELINE, sample(ORDERS, traffic(980, 12, 0)));

        var overview = overview();

        assertThat(overview.status()).isEqualTo(Status.HEALTHY);
        assertThat(overview.findings()).isEmpty();
        assertThat(overview.window()).isEqualTo(WINDOW);
        assertThat(overview.baseline()).isEqualTo(BASELINE);
        assertThat(overview.environment().current().requests()).isEqualTo(1000);
    }

    @Test
    void should_report_no_traffic_when_neither_range_has_requests() {
        assertThat(overview().status()).isEqualTo(Status.NO_TRAFFIC);
    }

    @Test
    void should_find_a_server_error_surge_and_rank_its_api_first() {
        analytics.total(WINDOW, traffic(2000, 0, 300)).total(BASELINE, traffic(2000, 0, 0));
        analytics
            .samples(WINDOW, sample(STOCK, traffic(1500, 0, 0)), sample(ORDERS, traffic(500, 0, 300)))
            .samples(BASELINE, sample(STOCK, traffic(1500, 0, 0)), sample(ORDERS, traffic(500, 0, 0)));

        var overview = overview();

        assertThat(overview.status()).isEqualTo(Status.DEGRADED);
        assertThat(overview.findings())
            .extracting(Finding::kind, Finding::api)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(Kind.SERVER_ERROR_RATE_UP, null),
                org.assertj.core.groups.Tuple.tuple(Kind.SERVER_ERROR_RATE_UP, ORDERS)
            );
        assertThat(overview.apis().getFirst().api()).isEqualTo(ORDERS);
        assertThat(overview.apis().getFirst().findings()).containsExactly(Kind.SERVER_ERROR_RATE_UP);
        assertThat(overview.apis().getLast().findings()).isEmpty();
    }

    @Test
    void should_find_a_client_error_surge_without_calling_it_a_server_error() {
        analytics.total(WINDOW, traffic(1000, 600, 0)).total(BASELINE, traffic(1000, 5, 0));
        analytics.samples(WINDOW, sample(ORDERS, traffic(1000, 600, 0))).samples(BASELINE, sample(ORDERS, traffic(1000, 5, 0)));

        var overview = overview();

        assertThat(overview.findings()).extracting(Finding::kind).containsOnly(Kind.CLIENT_ERROR_RATE_UP);
        assertThat(overview.findings().getLast().summary()).isEqualTo("4xx rate 60.00% of 1000 requests, up from 0.50%");
    }

    @Test
    void should_find_a_response_time_regression() {
        analytics.total(WINDOW, trafficAnsweredIn(1000, 900)).total(BASELINE, trafficAnsweredIn(1000, 120));

        assertThat(overview().findings()).extracting(Finding::kind).containsExactly(Kind.LATENCY_UP);
    }

    @Test
    void should_not_find_a_response_time_regression_under_a_hundred_milliseconds() {
        analytics.total(WINDOW, trafficAnsweredIn(1000, 90)).total(BASELINE, trafficAnsweredIn(1000, 20));

        assertThat(overview().findings()).isEmpty();
    }

    @Test
    void should_find_a_traffic_drop_on_an_api_that_stopped_receiving_requests() {
        analytics.total(WINDOW, traffic(1000, 0, 0)).total(BASELINE, traffic(1600, 0, 0));
        analytics
            .samples(WINDOW, sample(STOCK, traffic(1000, 0, 0)))
            .samples(BASELINE, sample(STOCK, traffic(1000, 0, 0)), sample(ORDERS, traffic(600, 0, 0)));

        var overview = overview();

        assertThat(overview.findings())
            .extracting(Finding::kind, Finding::api)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(Kind.TRAFFIC_DROP, ORDERS));
        assertThat(overview.apis().getFirst().current()).isEqualTo(TrafficStats.NONE);
    }

    @Test
    void should_ignore_error_rates_on_a_handful_of_requests() {
        analytics.total(WINDOW, traffic(1000, 0, 0)).total(BASELINE, traffic(1000, 0, 0));
        analytics.samples(WINDOW, sample(ORDERS, traffic(10, 0, 10))).samples(BASELINE, sample(ORDERS, traffic(10, 0, 0)));

        assertThat(overview().status()).isEqualTo(Status.HEALTHY);
    }

    @Test
    void should_keep_the_ten_most_affected_apis() {
        var samples = new TrafficSample[15];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = sample(new ApiRef("api-" + i, "Api " + i), traffic(100 + i, 0, 0));
        }
        analytics.total(WINDOW, traffic(1600, 0, 0)).total(BASELINE, traffic(1600, 0, 0));
        analytics.samples(WINDOW, samples).samples(BASELINE, samples);

        var overview = overview();

        assertThat(overview.apis()).hasSize(10);
        assertThat(overview.apis().getFirst().api().id()).isEqualTo("api-14");
    }

    @Test
    void should_refuse_a_caller_who_cannot_read_analytics() {
        access.traffic = false;

        assertThatThrownBy(this::overview).isInstanceOf(TriageException.class).hasMessageContaining("not allowed");
    }

    private GetHealthOverviewUseCase.Output overview() {
        return useCase.execute(new GetHealthOverviewUseCase.Input(CALLER, null, null));
    }

    private static TrafficSample sample(ApiRef api, TrafficStats stats) {
        return new TrafficSample(api.id(), api.name(), stats);
    }
}
