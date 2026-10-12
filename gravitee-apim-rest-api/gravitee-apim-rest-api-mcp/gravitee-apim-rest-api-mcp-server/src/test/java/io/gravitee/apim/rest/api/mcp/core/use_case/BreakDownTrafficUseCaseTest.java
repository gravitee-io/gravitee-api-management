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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Dimension;
import io.gravitee.apim.rest.api.mcp.core.model.StatusGroup;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficSample;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficScope;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import io.gravitee.apim.rest.api.mcp.core.use_case.BreakDownTrafficUseCase.Bucket;
import io.gravitee.apim.rest.api.mcp.core.use_case.BreakDownTrafficUseCase.Presence;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import io.gravitee.apim.rest.api.mcp.fakes.InMemoryTrafficAnalytics;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BreakDownTrafficUseCaseTest {

    private static final TimeWindow WINDOW = TimeWindow.parse("15m", null, NOW);
    private static final TimeWindow BASELINE = WINDOW.baseline();
    private static final ApiRef ORDERS = new ApiRef("orders-id", "Orders");

    private final Fakes.Access access = new Fakes.Access();
    private final InMemoryTrafficAnalytics analytics = new InMemoryTrafficAnalytics();
    private final BreakDownTrafficUseCase useCase = new BreakDownTrafficUseCase(
        access,
        Fakes.apis(ORDERS, new ApiRef("stock-1", "Stock"), new ApiRef("stock-2", "Stock")),
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
    void should_show_an_application_that_vanished_and_the_unresolved_one_that_appeared() {
        analytics
            .samples(WINDOW, new TrafficSample("1", "Unknown", traffic(900, 900, 0)))
            .samples(BASELINE, new TrafficSample("app-id", "Mobile app", traffic(880, 0, 0)));

        var breakdown = execute("application", "Orders", null);

        assertThat(breakdown.buckets())
            .extracting(Bucket::key, Bucket::name, Bucket::presence)
            .containsExactly(
                tuple("1", "Unknown (credential not resolved)", Presence.APPEARED),
                tuple("app-id", "Mobile app", Presence.VANISHED)
            );
        assertThat(breakdown.buckets().getLast().current()).isEqualTo(TrafficStats.NONE);
        assertThat(breakdown.summary()).isEqualTo("2 value(s): 1 appeared and 1 vanished since the baseline.");
    }

    @Test
    void should_pair_a_value_present_in_both_ranges() {
        analytics
            .samples(WINDOW, new TrafficSample("401", "401 Unauthorized", traffic(600, 600, 0)))
            .samples(BASELINE, new TrafficSample("401", "401 Unauthorized", traffic(3, 3, 0)));

        var bucket = execute("status", null, null).buckets().getFirst();

        assertThat(bucket.presence()).isEqualTo(Presence.BOTH);
        assertThat(bucket.current().requests()).isEqualTo(600);
        assertThat(bucket.baseline().requests()).isEqualTo(3);
    }

    @Test
    void should_scope_the_query_to_the_api_and_status_group_asked_for() {
        var breakdown = execute("consumer_ip", "orders-id", "4xx");

        assertThat(breakdown.api()).isEqualTo(ORDERS);
        assertThat(analytics.dimensions).containsOnly(Dimension.CONSUMER_IP);
        assertThat(analytics.scopes).containsOnly(new TrafficScope("orders-id", StatusGroup.CLIENT_ERROR));
    }

    @Test
    void should_query_the_whole_environment_when_no_api_is_given() {
        execute("gateway", null, null);

        assertThat(analytics.scopes).containsOnly(TrafficScope.ENVIRONMENT);
    }

    @Test
    void should_reject_an_unknown_dimension_and_list_the_accepted_ones() {
        assertThatThrownBy(() -> execute("country", null, null))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("api, application, plan, gateway, status, path, method, host, consumer_ip");
    }

    @Test
    void should_reject_an_api_the_caller_cannot_see() {
        assertThatThrownBy(() -> execute("status", "Billing", null))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("No API 'Billing' is visible to you");
    }

    @Test
    void should_ask_for_an_id_when_several_apis_share_the_name() {
        assertThatThrownBy(() -> execute("status", "stock", null))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("stock-1, stock-2");
    }

    @Test
    void should_refuse_a_caller_who_cannot_read_analytics() {
        access.traffic = false;

        assertThatThrownBy(() -> execute("status", null, null))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("not allowed");
    }

    private BreakDownTrafficUseCase.Output execute(String by, String api, String statusGroup) {
        return useCase.execute(new BreakDownTrafficUseCase.Input(CALLER, by, api, statusGroup, null, null));
    }
}
