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
import io.gravitee.gamma.rest.core.observability.dashboard.exception.DashboardVersionConflictException;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import io.gravitee.gamma.rest.core.observability.dashboard.inmemory.InMemoryDashboardRepository;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardContent;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRange;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRangeType;
import io.gravitee.gamma.rest.core.observability.dashboard.model.VersionPrecondition;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
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

        var output = useCase.execute(
            new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, VersionPrecondition.version(3), content)
        );

        assertThat(output.dashboard().title()).isEqualTo("New title");
        assertThat(output.dashboard().description()).isEqualTo("new desc");
        assertThat(output.dashboard().timeRange().period()).isEqualTo("7d");
        assertThat(output.dashboard().version()).isEqualTo(4);
        assertThat(output.dashboard().createdBy()).isEqualTo("user-1");
        assertThat(output.dashboard().createdAt()).isEqualTo(CREATED_AT);
        assertThat(output.dashboard().updatedAt()).isEqualTo(NOW);
        assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENV)).contains(output.dashboard());
    }

    /**
     * A stored version of {@code null} is not reachable through the API — every dashboard is created with 1 — but the
     * repository model still allows it. Such a dashboard advertises no ETag, so no version can match it and only an
     * overwrite can save it; that write starts the counter, making it an ordinary versioned dashboard from then on.
     */
    @Test
    void should_refuse_a_version_match_against_a_dashboard_whose_stored_version_is_null() {
        dashboardRepository.givenDashboard(existingDashboard(null));
        var content = new DashboardContent("New title", null, List.of(), null, null);

        assertThatThrownBy(() ->
            useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, VersionPrecondition.version(1), content))
        ).isInstanceOf(DashboardVersionConflictException.class);
    }

    @Test
    void should_let_an_overwrite_save_a_dashboard_whose_stored_version_is_null_and_start_the_counter() {
        dashboardRepository.givenDashboard(existingDashboard(null));
        var content = new DashboardContent("New title", null, List.of(), null, null);

        var output = useCase.execute(
            new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, VersionPrecondition.anyVersion(), content)
        );

        assertThat(output.dashboard().version()).isEqualTo(1);
        assertThat(output.dashboard().title()).isEqualTo("New title");
    }

    @Test
    void should_throw_not_found_when_dashboard_does_not_exist() {
        var content = new DashboardContent("New title", null, List.of(), null, null);

        assertThatThrownBy(() ->
            useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, "unknown", VersionPrecondition.version(1), content))
        ).isInstanceOf(DashboardNotFoundException.class);
    }

    @Test
    void should_throw_not_found_when_dashboard_belongs_to_another_environment() {
        dashboardRepository.givenDashboard(existingDashboard(1));
        var content = new DashboardContent("New title", null, List.of(), null, null);

        assertThatThrownBy(() ->
            useCase.execute(new UpdateObservabilityDashboardUseCase.Input(OTHER_ENV, DASHBOARD_ID, VersionPrecondition.version(1), content))
        ).isInstanceOf(DashboardNotFoundException.class);
    }

    @Test
    void should_reject_invalid_content_before_touching_the_repository() {
        Dashboard existing = existingDashboard(3);
        dashboardRepository.givenDashboard(existing);
        var content = new DashboardContent(" ", null, List.of(), null, null);

        assertThatThrownBy(() ->
            useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, VersionPrecondition.version(3), content))
        ).isInstanceOf(InvalidDashboardException.class);
        assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENV)).contains(existing);
    }

    @Test
    void should_reject_a_stale_version_and_leave_the_stored_dashboard_untouched() {
        Dashboard existing = existingDashboard(3);
        dashboardRepository.givenDashboard(existing);
        var content = new DashboardContent("New title", null, List.of(), null, null);

        assertThatThrownBy(() ->
            useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, VersionPrecondition.version(2), content))
        ).isInstanceOfSatisfying(DashboardVersionConflictException.class, e -> assertThat(e.getCurrent()).isEqualTo(existing));
        assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENV)).contains(existing);
    }

    /**
     * The version the caller sends is ahead of the stored one — impossible from a well-behaved client, so it means a
     * fabricated or replayed request. Refuse it for the same reason a stale one is refused: the caller is not editing
     * the revision it claims to be.
     */
    @Test
    void should_reject_a_version_ahead_of_the_stored_one() {
        dashboardRepository.givenDashboard(existingDashboard(3));
        var content = new DashboardContent("New title", null, List.of(), null, null);

        assertThatThrownBy(() ->
            useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, VersionPrecondition.version(4), content))
        ).isInstanceOf(DashboardVersionConflictException.class);
    }

    @Test
    void should_reject_a_missing_version_rather_than_treat_it_as_a_force_overwrite() {
        Dashboard existing = existingDashboard(3);
        dashboardRepository.givenDashboard(existing);
        var content = new DashboardContent("New title", null, List.of(), null, null);

        assertThatThrownBy(() -> useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, null, content)))
            .isInstanceOf(InvalidDashboardException.class)
            .hasMessageContaining("revision this edit is based on must be stated");
        assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENV)).contains(existing);
    }

    /**
     * The race the storage-level guard exists for: the version still matched when this use case read it, and a
     * competing save landed before the write. The conditional update refuses it, and the 409 must describe the state
     * that actually won — not the stale one this use case read.
     */
    @Test
    void should_reject_and_report_the_winner_when_a_concurrent_save_lands_between_the_read_and_the_write() {
        dashboardRepository.givenDashboard(existingDashboard(3));
        Dashboard winner = new Dashboard(
            DASHBOARD_ID,
            ENV,
            "Someone else's title",
            "desc",
            List.of(),
            null,
            NullNode.getInstance(),
            4,
            "user-1",
            CREATED_AT,
            NOW
        );
        dashboardRepository.givenAConcurrentSaveBeforeTheNextVersionedWrite(winner);
        var content = new DashboardContent("New title", null, List.of(), null, null);

        assertThatThrownBy(() ->
            useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, VersionPrecondition.version(3), content))
        ).isInstanceOfSatisfying(DashboardVersionConflictException.class, e -> assertThat(e.getCurrent()).isEqualTo(winner));
    }

    @Test
    void should_apply_an_overwrite_over_whatever_revision_is_current() {
        dashboardRepository.givenDashboard(existingDashboard(3));
        var content = new DashboardContent("Overwritten", null, List.of(), null, null);

        var output = useCase.execute(
            new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, VersionPrecondition.anyVersion(), content)
        );

        assertThat(output.dashboard().title()).isEqualTo("Overwritten");
        assertThat(output.dashboard().version()).as("an overwrite still moves the counter on").isEqualTo(4);
    }

    /**
     * An overwrite is not a resurrection: the dashboard has to still exist. Deleting it inside the write reproduces
     * the only way that can happen — a delete landing after this use case's read.
     */
    @Test
    void should_refuse_an_overwrite_when_the_dashboard_was_deleted_first() {
        dashboardRepository.givenDashboard(existingDashboard(3));
        dashboardRepository.givenADeleteBeforeTheNextWrite(DASHBOARD_ID);
        var content = new DashboardContent("Overwritten", null, List.of(), null, null);

        assertThatThrownBy(() ->
            useCase.execute(new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, VersionPrecondition.anyVersion(), content))
        ).isInstanceOf(DashboardNotFoundException.class);
        assertThat(dashboardRepository.findByIdAndEnvironmentId(DASHBOARD_ID, ENV)).isEmpty();
    }

    /** If-Match accepts a list of validators; any one matching is enough. */
    @Test
    void should_accept_a_precondition_listing_several_versions_when_one_of_them_is_stored() {
        dashboardRepository.givenDashboard(existingDashboard(3));
        var content = new DashboardContent("New title", null, List.of(), null, null);

        var output = useCase.execute(
            new UpdateObservabilityDashboardUseCase.Input(ENV, DASHBOARD_ID, VersionPrecondition.oneOf(Set.of(2, 3)), content)
        );

        assertThat(output.dashboard().version()).isEqualTo(4);
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
