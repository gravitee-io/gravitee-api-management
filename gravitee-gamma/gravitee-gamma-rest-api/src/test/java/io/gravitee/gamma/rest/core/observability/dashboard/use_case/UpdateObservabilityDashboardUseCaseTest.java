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
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.DashboardNotFoundException;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import io.gravitee.gamma.rest.core.observability.dashboard.inmemory.InMemoryDashboardRepository;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardContent;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRange;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRangeType;
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
class UpdateObservabilityDashboardUseCaseTest {

    private static final String ENV = "env-1";
    private static final String OTHER_ENV = "env-2";
    private static final String DASHBOARD_ID = "dash-1";
    private static final Instant CREATED_AT = Instant.parse("2026-06-10T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-08-07T10:00:00Z");

    private final InMemoryDashboardRepository dashboardRepository = new InMemoryDashboardRepository();
    private final UpdateObservabilityDashboardUseCase useCase = new UpdateObservabilityDashboardUseCase(dashboardRepository);

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
    void should_replace_content_preserve_creation_fields_bump_updated_at_and_increment_version() {
        dashboardRepository.givenDashboard(existingDashboard(3));
        var content = new DashboardContent(
            "New title",
            "new desc",
            List.of(),
            new TimeRange(TimeRangeType.RELATIVE, "7d", null, null),
            null
        );

        var output = useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, content));

        assertThat(output.dashboard().title()).isEqualTo("New title");
        assertThat(output.dashboard().description()).isEqualTo("new desc");
        assertThat(output.dashboard().timeRange().period()).isEqualTo("7d");
        assertThat(output.dashboard().version()).isEqualTo(4);
        assertThat(output.dashboard().createdBy()).isEqualTo("user-1");
        assertThat(output.dashboard().createdAt()).isEqualTo(CREATED_AT);
        assertThat(output.dashboard().updatedAt()).isEqualTo(NOW);
        assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENV)).contains(output.dashboard());
    }

    @Test
    void should_restart_the_version_counter_when_the_existing_row_predates_versioning() {
        dashboardRepository.givenDashboard(existingDashboard(null));
        var content = new DashboardContent("New title", null, List.of(), null, null);

        var output = useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, content));

        assertThat(output.dashboard().version()).isEqualTo(1);
    }

    @Test
    void should_throw_not_found_when_dashboard_does_not_exist() {
        var content = new DashboardContent("New title", null, List.of(), null, null);

        assertThatThrownBy(() -> useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, "unknown", content))).isInstanceOf(
            DashboardNotFoundException.class
        );
    }

    @Test
    void should_throw_not_found_when_dashboard_belongs_to_another_environment() {
        dashboardRepository.givenDashboard(existingDashboard(1));
        var content = new DashboardContent("New title", null, List.of(), null, null);

        assertThatThrownBy(() ->
            useCase.execute(new UpdateObservabilityDashboardUseCase.Input(OTHER_ENV, DASHBOARD_ID, content))
        ).isInstanceOf(DashboardNotFoundException.class);
    }

    @Test
    void should_reject_invalid_content_before_touching_the_repository() {
        Dashboard existing = existingDashboard(3);
        dashboardRepository.givenDashboard(existing);
        var content = new DashboardContent(" ", null, List.of(), null, null);

        assertThatThrownBy(() -> useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, content))).isInstanceOf(
            InvalidDashboardException.class
        );
        assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENV)).contains(existing);
    }

    private static Dashboard existingDashboard(Integer version) {
        return new Dashboard(
            DASHBOARD_ID,
            ENV,
            "Performance overview",
            "desc",
            List.of(),
            new TimeRange(TimeRangeType.RELATIVE, "24h", null, null),
            NullNode.getInstance(),
            version,
            "user-1",
            CREATED_AT,
            CREATED_AT
        );
    }
}
