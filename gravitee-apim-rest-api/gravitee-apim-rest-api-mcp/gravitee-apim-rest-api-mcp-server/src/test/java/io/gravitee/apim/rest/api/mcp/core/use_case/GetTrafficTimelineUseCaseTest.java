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
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.traffic;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Finding.Kind;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TimelinePoint;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficScope;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import io.gravitee.apim.rest.api.mcp.fakes.InMemoryTrafficAnalytics;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetTrafficTimelineUseCaseTest {

    private static final TimeWindow WINDOW = TimeWindow.parse("15m", null, NOW);
    private static final ApiRef ORDERS = new ApiRef("orders-id", "Orders");

    private final InMemoryTrafficAnalytics analytics = new InMemoryTrafficAnalytics();
    private final GetTrafficTimelineUseCase useCase = new GetTrafficTimelineUseCase(
        Fakes.environments(PROD),
        Fakes.apis(ORDERS),
        analytics
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
    void should_date_the_onset_at_the_first_interval_that_departs_from_the_baseline() {
        analytics.total(WINDOW.baseline(), traffic(3000, 0, 0));
        analytics.timeline(
            point("2026-01-31T13:45:00Z", 100, 0),
            point("2026-01-31T13:45:30Z", 100, 1),
            point("2026-01-31T13:46:00Z", 100, 70),
            point("2026-01-31T13:46:30Z", 100, 100)
        );

        var timeline = execute("15m");

        assertThat(timeline.onset().at()).isEqualTo(Instant.parse("2026-01-31T13:46:00Z"));
        assertThat(timeline.onset().findings()).containsExactly(Kind.CLIENT_ERROR_RATE_UP);
        assertThat(timeline.points()).hasSize(4);
        assertThat(timeline.summary()).contains("2026-01-31T13:46:00Z");
    }

    @Test
    void should_report_no_onset_when_every_interval_matches_the_baseline() {
        analytics.total(WINDOW.baseline(), traffic(3000, 0, 0));
        analytics.timeline(point("2026-01-31T13:45:00Z", 100, 0), point("2026-01-31T13:45:30Z", 100, 1));

        assertThat(execute("15m").onset()).isNull();
    }

    @Test
    void should_compare_each_interval_with_the_share_of_the_baseline_it_represents() {
        analytics.total(WINDOW.baseline(), traffic(3000, 30, 0));

        var timeline = execute("15m");

        assertThat(timeline.intervalSeconds()).isEqualTo(30);
        assertThat(timeline.baselinePerInterval().requests()).isEqualTo(100);
        assertThat(timeline.baselinePerInterval().clientErrors()).isEqualTo(1);
    }

    @Test
    void should_split_the_window_in_thirty_intervals_of_at_least_ten_seconds() {
        execute("5h");
        assertThat(analytics.interval).isEqualTo(Duration.ofMinutes(10));

        execute("2m");
        assertThat(analytics.interval).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void should_scope_the_timeline_to_the_api_asked_for() {
        useCase.execute(new GetTrafficTimelineUseCase.Input(MCP_CALLER, null, "Orders", null, null));

        assertThat(analytics.scopes).containsOnly(new TrafficScope("orders-id", null));
    }

    private GetTrafficTimelineUseCase.Output execute(String window) {
        return useCase.execute(new GetTrafficTimelineUseCase.Input(MCP_CALLER, null, null, window, null));
    }

    private static TimelinePoint point(String at, long requests, long clientErrors) {
        return new TimelinePoint(Instant.parse(at), traffic(requests, clientErrors, 0));
    }
}
