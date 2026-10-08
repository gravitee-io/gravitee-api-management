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
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import io.gravitee.gamma.rest.core.observability.dashboard.inmemory.InMemoryDashboardRepository;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListObservabilityDashboardUseCaseTest {

    private static final String ENV = "env-1";
    private static final String OTHER_ENV = "env-2";

    private final InMemoryDashboardRepository dashboardRepository = new InMemoryDashboardRepository();
    private final ListObservabilityDashboardUseCase useCase = new ListObservabilityDashboardUseCase(dashboardRepository);

    @BeforeEach
    void reset() {
        dashboardRepository.reset();
    }

    @Test
    void should_return_every_dashboard_of_the_context_environment_when_it_fits_on_one_page() {
        dashboardRepository.givenDashboard(dashboard("dash-1", ENV));
        dashboardRepository.givenDashboard(dashboard("dash-2", ENV));
        dashboardRepository.givenDashboard(dashboard("dash-3", OTHER_ENV));

        var output = useCase.execute(new ListObservabilityDashboardUseCase.Input(ENV, null, null, null));

        assertThat(output.dashboards()).extracting(Dashboard::id).containsExactlyInAnyOrder("dash-1", "dash-2");
        assertThat(output.totalCount()).isEqualTo(2);
        assertThat(output.page()).isEqualTo(1);
        assertThat(output.perPage()).isEqualTo(20);
    }

    @Test
    void should_return_only_the_dashboards_of_the_requested_module() {
        dashboardRepository.givenDashboard(dashboard("dash-aim", ENV, "aim"));
        dashboardRepository.givenDashboard(dashboard("dash-apim", ENV, "apim"));
        dashboardRepository.givenDashboard(dashboard("dash-none", ENV, null));
        dashboardRepository.givenDashboard(dashboard("dash-aim-other-env", OTHER_ENV, "aim"));

        var output = useCase.execute(new ListObservabilityDashboardUseCase.Input(ENV, Set.of("aim"), null, null));

        assertThat(output.dashboards()).extracting(Dashboard::id).containsExactly("dash-aim");
        assertThat(output.totalCount()).isEqualTo(1);
    }

    @Test
    void should_return_the_dashboards_of_every_requested_module_in_one_list() {
        dashboardRepository.givenDashboard(dashboard("dash-aim", ENV, "aim"));
        dashboardRepository.givenDashboard(dashboard("dash-apim", ENV, "apim"));
        dashboardRepository.givenDashboard(dashboard("dash-portals", ENV, "portals"));
        dashboardRepository.givenDashboard(dashboard("dash-none", ENV, null));

        var output = useCase.execute(new ListObservabilityDashboardUseCase.Input(ENV, Set.of("aim", "apim"), null, null));

        assertThat(output.dashboards()).extracting(Dashboard::id).containsExactly("dash-aim", "dash-apim");
        assertThat(output.totalCount()).isEqualTo(2);
    }

    /** An empty selection is no selection, the same as omitting {@code module}. */
    @Test
    void should_return_every_dashboard_when_the_module_selection_is_empty() {
        dashboardRepository.givenDashboard(dashboard("dash-aim", ENV, "aim"));
        dashboardRepository.givenDashboard(dashboard("dash-none", ENV, null));

        var output = useCase.execute(new ListObservabilityDashboardUseCase.Input(ENV, Set.of(), null, null));

        assertThat(output.dashboards()).extracting(Dashboard::id).containsExactly("dash-aim", "dash-none");
    }

    /** No module is the cross-module view, and what clients that do not send one yet still get. */
    @Test
    void should_return_every_module_including_dashboards_without_one_when_no_module_is_requested() {
        dashboardRepository.givenDashboard(dashboard("dash-aim", ENV, "aim"));
        dashboardRepository.givenDashboard(dashboard("dash-apim", ENV, "apim"));
        dashboardRepository.givenDashboard(dashboard("dash-none", ENV, null));

        var output = useCase.execute(new ListObservabilityDashboardUseCase.Input(ENV, null, null, null));

        assertThat(output.dashboards()).extracting(Dashboard::id).containsExactly("dash-aim", "dash-apim", "dash-none");
    }

    @Test
    void should_reject_an_invalid_module() {
        assertThatThrownBy(() ->
            useCase.execute(new ListObservabilityDashboardUseCase.Input(ENV, Set.of("aim", "AIM"), null, null))
        ).isInstanceOf(InvalidDashboardException.class);
    }

    @Test
    void should_return_empty_list_when_no_dashboard_exists_for_the_environment() {
        var output = useCase.execute(new ListObservabilityDashboardUseCase.Input(ENV, null, null, null));

        assertThat(output.dashboards()).isEmpty();
        assertThat(output.totalCount()).isEqualTo(0);
    }

    @Test
    void should_slice_by_page_and_perPage() {
        for (int i = 1; i <= 5; i++) {
            dashboardRepository.givenDashboard(dashboard("dash-" + i, ENV));
        }

        var output = useCase.execute(new ListObservabilityDashboardUseCase.Input(ENV, null, 2, 2));

        assertThat(output.dashboards()).hasSize(2);
        assertThat(output.totalCount()).isEqualTo(5);
        assertThat(output.page()).isEqualTo(2);
        assertThat(output.perPage()).isEqualTo(2);
    }

    @Test
    void should_return_empty_page_when_page_is_beyond_the_last_one() {
        dashboardRepository.givenDashboard(dashboard("dash-1", ENV));

        var output = useCase.execute(new ListObservabilityDashboardUseCase.Input(ENV, null, 5, 10));

        assertThat(output.dashboards()).isEmpty();
        assertThat(output.totalCount()).isEqualTo(1);
    }

    @Test
    void should_default_invalid_page_and_perPage_to_defaults() {
        dashboardRepository.givenDashboard(dashboard("dash-1", ENV));

        var output = useCase.execute(new ListObservabilityDashboardUseCase.Input(ENV, null, 0, -5));

        assertThat(output.page()).isEqualTo(1);
        assertThat(output.perPage()).isEqualTo(20);
    }

    @Test
    void should_cap_perPage_at_the_maximum() {
        var output = useCase.execute(new ListObservabilityDashboardUseCase.Input(ENV, null, 1, 1000));

        assertThat(output.perPage()).isEqualTo(100);
    }

    private static Dashboard dashboard(String id, String environmentId) {
        return dashboard(id, environmentId, null);
    }

    private static Dashboard dashboard(String id, String environmentId, String module) {
        return new Dashboard(
            id,
            environmentId,
            module,
            "Performance overview",
            null,
            List.of(),
            null,
            NullNode.getInstance(),
            1,
            "user-1",
            Instant.parse("2026-06-10T00:00:00Z"),
            Instant.parse("2026-06-10T00:00:00Z")
        );
    }
}
