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
package io.gravitee.gamma.rest.core.observability.dashboard.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.NullNode;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.DashboardNotFoundException;
import io.gravitee.gamma.rest.core.observability.dashboard.inmemory.InMemoryDashboardRepository;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardFilter;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRange;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRangeType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetObservabilityDashboardUseCaseTest {

    private static final String ENV = "env-1";
    private static final String OTHER_ENV = "env-2";
    private static final String DASHBOARD_ID = "dash-1";

    private final InMemoryDashboardRepository dashboardRepository = new InMemoryDashboardRepository();
    private final GetObservabilityDashboardUseCase useCase = new GetObservabilityDashboardUseCase(dashboardRepository);

    @BeforeEach
    void reset() {
        dashboardRepository.reset();
    }

    @Test
    void should_return_the_dashboard_with_filters_and_time_range_restored() {
        dashboardRepository.givenDashboard(
            new Dashboard(
                DASHBOARD_ID,
                ENV,
                "Performance overview",
                "desc",
                List.of(
                    new DashboardFilter(new FilterCondition("API_TYPE", FilterOperator.EQ, List.of("MCP")), "API Type", false),
                    new DashboardFilter(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of()), "Status Code", true)
                ),
                new TimeRange(TimeRangeType.RELATIVE, "24h", null, null),
                NullNode.getInstance(),
                3,
                "user-1",
                Instant.parse("2026-06-10T00:00:00Z"),
                Instant.parse("2026-06-11T00:00:00Z")
            )
        );

        var output = useCase.execute(new GetObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID));

        assertThat(output.dashboard().id()).isEqualTo(DASHBOARD_ID);
        assertThat(output.dashboard().filters()).hasSize(2);
        assertThat(output.dashboard().filters().get(1).condition().values()).isEmpty();
        assertThat(output.dashboard().timeRange().type()).isEqualTo(TimeRangeType.RELATIVE);
        assertThat(output.dashboard().version()).isEqualTo(3);
    }

    @Test
    void should_throw_not_found_when_dashboard_does_not_exist() {
        assertThatThrownBy(() -> useCase.execute(new GetObservabilityDashboardUseCase.Input(ENV, "unknown"))).isInstanceOf(
            DashboardNotFoundException.class
        );
    }

    @Test
    void should_throw_not_found_when_dashboard_belongs_to_another_environment() {
        dashboardRepository.givenDashboard(
            new Dashboard(
                DASHBOARD_ID,
                OTHER_ENV,
                "Performance overview",
                null,
                List.of(),
                null,
                NullNode.getInstance(),
                1,
                "user-1",
                Instant.parse("2026-06-10T00:00:00Z"),
                Instant.parse("2026-06-10T00:00:00Z")
            )
        );

        assertThatThrownBy(() -> useCase.execute(new GetObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID))).isInstanceOf(
            DashboardNotFoundException.class
        );
    }
}
