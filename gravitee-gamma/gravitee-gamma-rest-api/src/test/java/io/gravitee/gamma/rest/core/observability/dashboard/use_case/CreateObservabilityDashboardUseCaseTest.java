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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import io.gravitee.gamma.rest.core.observability.dashboard.inmemory.InMemoryDashboardRepository;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardContent;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardFilter;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRange;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRangeType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateObservabilityDashboardUseCaseTest {

    private static final String ENV = "env-1";
    private static final String USER = "user-1";
    private static final String DASHBOARD_ID = "dash-1";
    private static final Instant NOW = Instant.parse("2026-08-07T10:00:00Z");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InMemoryDashboardRepository dashboardRepository = new InMemoryDashboardRepository();
    private final CreateObservabilityDashboardUseCase useCase = new CreateObservabilityDashboardUseCase(dashboardRepository);

    @BeforeAll
    static void freezeClock() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void restoreClock() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void reset() {
        dashboardRepository.reset();
    }

    @Test
    void should_create_the_dashboard_with_server_owned_fields_derived() throws Exception {
        var content = new DashboardContent(
            "Performance overview",
            "desc",
            List.of(new DashboardFilter(new FilterCondition("API_TYPE", FilterOperator.EQ, List.of("MCP")), "API Type", false)),
            new TimeRange(TimeRangeType.RELATIVE, "24h", null, null),
            MAPPER.readTree("[{\"id\":\"w1\",\"type\":\"metric\"}]")
        );

        var output = useCase.execute(new CreateObservabilityDashboardUseCase.Input(ENV, USER, DASHBOARD_ID, content));

        assertThat(output.dashboard().id()).isEqualTo(DASHBOARD_ID);
        assertThat(output.dashboard().environmentId()).isEqualTo(ENV);
        assertThat(output.dashboard().version()).isEqualTo(1);
        assertThat(output.dashboard().createdBy()).isEqualTo(USER);
        assertThat(output.dashboard().createdAt()).isEqualTo(NOW);
        assertThat(output.dashboard().updatedAt()).isEqualTo(NOW);
        assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENV)).contains(output.dashboard());
    }

    @Test
    void should_reject_invalid_content_before_touching_the_repository() {
        var content = new DashboardContent(" ", null, List.of(), null, null);

        assertThatThrownBy(() ->
            useCase.execute(new CreateObservabilityDashboardUseCase.Input(ENV, USER, DASHBOARD_ID, content))
        ).isInstanceOf(InvalidDashboardException.class);
        assertThat(dashboardRepository.findByEnvironmentId(ENV)).isEmpty();
    }

    @Test
    void should_reject_a_blank_dashboard_id() {
        var content = new DashboardContent("Performance overview", null, List.of(), null, null);

        assertThatThrownBy(() -> useCase.execute(new CreateObservabilityDashboardUseCase.Input(ENV, USER, " ", content))).isInstanceOf(
            InvalidDashboardException.class
        );
    }
}
