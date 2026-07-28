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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import io.gravitee.repository.management.model.GammaDashboard;
import java.util.*;
import org.junit.jupiter.api.Test;

public class GammaDashboardRepositoryTest extends AbstractManagementRepositoryTest {

    /** Widget payload with a dotted map key — Mongo rejects those as document keys, hence the opaque String column. */
    private static final String DOTTED_KEY_WIDGETS =
        "{\"widgets\":[{\"id\":\"w-1\",\"type\":\"line\",\"colorByGroup\":{\"api.example.com\":\"#fff\",\"other.host\":\"#000\"}}]}";

    @Override
    protected String getTestCasesPath() {
        return "/data/gamma-dashboard-tests/";
    }

    @Test
    public void shouldFindByEnvironmentId() throws Exception {
        var dashboards = gammaDashboardRepository.findByEnvironmentId("DEFAULT");
        assertThat(dashboards).hasSize(3);
    }

    @Test
    public void shouldFindByEnvironmentId_otherEnv() throws Exception {
        var dashboards = gammaDashboardRepository.findByEnvironmentId("OTHER_ENV");
        assertThat(dashboards).hasSize(1);
        assertThat(dashboards.get(0).getTitle()).isEqualTo("Other Env Dashboard");
    }

    /**
     * The three DEFAULT fixtures share a {@code createdAt}, so this also pins the id tie-break — without it the order
     * would still be backend-dependent.
     */
    @Test
    public void shouldReturnEnvironmentDashboardsInStableOrder() throws Exception {
        assertThat(gammaDashboardRepository.findByEnvironmentId("DEFAULT"))
            .extracting(GammaDashboard::getId)
            .containsExactly("gd-1", "gd-2", "gd-3");
    }

    @Test
    public void shouldReturnEmptyListForUnknownEnvironmentId() throws Exception {
        var dashboards = gammaDashboardRepository.findByEnvironmentId("UNKNOWN");
        assertThat(dashboards).isEmpty();
    }

    @Test
    public void shouldFindById() throws Exception {
        var dashboard = gammaDashboardRepository.findById("gd-1");
        assertThat(dashboard).hasValueSatisfying(result -> {
            assertThat(result.getTitle()).isEqualTo("Performance Overview");
            assertThat(result.getDescription()).isEqualTo("Latency and throughput");
            assertThat(result.getEnvironmentId()).isEqualTo("DEFAULT");
            assertThat(result.getCreatedBy()).isEqualTo("user-1");
            assertThat(compareDate(result.getCreatedAt(), new Date(1000000000000L))).isTrue();
            assertThat(compareDate(result.getUpdatedAt(), new Date(1111111111111L))).isTrue();
            assertThat(result.getVersion()).isEqualTo(3);
        });
    }

    @Test
    public void shouldRoundTripAbsentVersionAsNull() throws Exception {
        var dashboard = gammaDashboardRepository.findById("gd-3");
        assertThat(dashboard).hasValueSatisfying(result -> assertThat(result.getVersion()).isNull());
    }

    @Test
    public void shouldReturnEmptyForUnknownId() throws Exception {
        var dashboard = gammaDashboardRepository.findById("unknown");
        assertThat(dashboard).isEmpty();
    }

    @Test
    public void shouldFindByIdAndEnvironmentId() throws Exception {
        var dashboard = gammaDashboardRepository.findByIdAndEnvironmentId("gd-1", "DEFAULT");
        assertThat(dashboard).hasValueSatisfying(result -> assertThat(result.getTitle()).isEqualTo("Performance Overview"));
    }

    /** Cross-environment isolation is a property of the query, not of a caller-side check. */
    @Test
    public void shouldNotFindByIdWhenEnvironmentDoesNotMatch() throws Exception {
        assertThat(gammaDashboardRepository.findById("gd-4")).isPresent();
        assertThat(gammaDashboardRepository.findByIdAndEnvironmentId("gd-4", "DEFAULT")).isEmpty();
        assertThat(gammaDashboardRepository.findByIdAndEnvironmentId("gd-1", "OTHER_ENV")).isEmpty();
    }

    @Test
    public void shouldNotFindByIdAndEnvironmentIdForUnknownId() throws Exception {
        assertThat(gammaDashboardRepository.findByIdAndEnvironmentId("unknown", "DEFAULT")).isEmpty();
    }

    /**
     * The payload mirrors what the frontend actually emits — see the in-repo literal in
     * {@code gravitee-gamma-module-apim/.../templates/shared.ts}: {@code field}, {@code label}, a lowercase operator
     * and {@code value}. Asserting on an invented shape would prove serialization, not compatibility.
     */
    @Test
    public void shouldRoundTripFilters() throws Exception {
        var dashboard = gammaDashboardRepository.findById("gd-1");
        assertThat(dashboard).hasValueSatisfying(result -> {
            assertThat(result.getFilters()).hasSize(2);

            var editable = result
                .getFilters()
                .stream()
                .filter(f -> "API_TYPE".equals(f.getField()))
                .findFirst()
                .orElseThrow();
            assertThat(editable.getLabel()).isEqualTo("API Type");
            assertThat(editable.getOperator()).isEqualTo("in");
            assertThat(editable.getValue()).containsExactly("MCP", "A2A");
            assertThat(editable.isEditable()).isTrue();

            // The "all values" placeholder: locked filter carrying an empty value list.
            var locked = result
                .getFilters()
                .stream()
                .filter(f -> "HTTP_STATUS".equals(f.getField()))
                .findFirst()
                .orElseThrow();
            assertThat(locked.getLabel()).isEqualTo("Status");
            assertThat(locked.getOperator()).isEqualTo("eq");
            assertThat(locked.getValue()).isEmpty();
            assertThat(locked.isEditable()).isFalse();
        });
    }

    @Test
    public void shouldRoundTripRelativeTimeRange() throws Exception {
        var dashboard = gammaDashboardRepository.findById("gd-1");
        assertThat(dashboard).hasValueSatisfying(result -> {
            assertThat(result.getTimeRange()).isNotNull();
            assertThat(result.getTimeRange().getType()).isEqualTo("relative");
            assertThat(result.getTimeRange().getPeriod()).isEqualTo("7d");
            assertThat(result.getTimeRange().getFrom()).isNull();
            assertThat(result.getTimeRange().getTo()).isNull();
        });
    }

    @Test
    public void shouldRoundTripAbsoluteTimeRange() throws Exception {
        var dashboard = gammaDashboardRepository.findById("gd-2");
        assertThat(dashboard).hasValueSatisfying(result -> {
            assertThat(result.getTimeRange()).isNotNull();
            assertThat(result.getTimeRange().getType()).isEqualTo("absolute");
            assertThat(result.getTimeRange().getPeriod()).isNull();
            assertThat(result.getTimeRange().getFrom()).isEqualTo(1735689600000L);
            assertThat(result.getTimeRange().getTo()).isEqualTo(1738368000000L);
        });
    }

    @Test
    public void shouldRoundTripWidgetsWithDottedMapKey() throws Exception {
        var dashboard = gammaDashboardRepository.findById("gd-1");
        assertThat(dashboard).hasValueSatisfying(result -> assertThat(result.getWidgets()).isEqualTo(DOTTED_KEY_WIDGETS));
    }

    @Test
    public void shouldCreate() throws Exception {
        var dashboard = GammaDashboard.builder()
            .id("new-gd")
            .environmentId("DEFAULT")
            .title("New Dashboard")
            .description("Created by the TCK")
            .createdBy("user-new")
            .createdAt(new Date(1000000000000L))
            .updatedAt(new Date(1111111111111L))
            .version(1)
            .filters(
                List.of(
                    GammaDashboard.Filter.builder()
                        .field("API_TYPE")
                        .label("API Type")
                        .operator("eq")
                        .value(List.of("MCP"))
                        .editable(true)
                        .build()
                )
            )
            .timeRange(GammaDashboard.TimeRange.builder().type("relative").period("1h").build())
            .widgets(DOTTED_KEY_WIDGETS)
            .build();

        var nbBefore = gammaDashboardRepository.findByEnvironmentId("DEFAULT").size();
        gammaDashboardRepository.create(dashboard);
        var nbAfter = gammaDashboardRepository.findByEnvironmentId("DEFAULT").size();

        assertThat(nbAfter).isEqualTo(nbBefore + 1);

        var saved = gammaDashboardRepository.findById("new-gd");
        assertThat(saved).hasValueSatisfying(result -> {
            assertThat(result.getId()).isEqualTo("new-gd");
            assertThat(result.getEnvironmentId()).isEqualTo("DEFAULT");
            assertThat(result.getTitle()).isEqualTo("New Dashboard");
            assertThat(result.getDescription()).isEqualTo("Created by the TCK");
            assertThat(result.getCreatedBy()).isEqualTo("user-new");
            assertThat(compareDate(result.getCreatedAt(), new Date(1000000000000L))).isTrue();
            assertThat(compareDate(result.getUpdatedAt(), new Date(1111111111111L))).isTrue();
            assertThat(result.getFilters()).hasSize(1);
            assertThat(result.getFilters().get(0).getField()).isEqualTo("API_TYPE");
            assertThat(result.getFilters().get(0).isEditable()).isTrue();
            assertThat(result.getTimeRange().getPeriod()).isEqualTo("1h");
            assertThat(result.getWidgets()).isEqualTo(DOTTED_KEY_WIDGETS);
            assertThat(result.getVersion()).isEqualTo(1);
        });
    }

    /**
     * The version counter is carried, never managed here: this layer must not initialise or bump it. Bumping belongs
     * to the update use case, so an update that leaves it alone must store it unchanged, and one that sets it must
     * store exactly what it was given.
     */
    @Test
    public void shouldPersistVersionVerbatimOnUpdate() throws Exception {
        var untouched = gammaDashboardRepository.findById("gd-2").orElseThrow();
        assertThat(untouched.getVersion()).isEqualTo(7);

        untouched.setTitle("Same version");
        gammaDashboardRepository.update(untouched);
        assertThat(gammaDashboardRepository.findById("gd-2")).hasValueSatisfying(result -> assertThat(result.getVersion()).isEqualTo(7));

        var bumped = gammaDashboardRepository.findById("gd-2").orElseThrow();
        bumped.setVersion(8);
        gammaDashboardRepository.update(bumped);
        assertThat(gammaDashboardRepository.findById("gd-2")).hasValueSatisfying(result -> assertThat(result.getVersion()).isEqualTo(8));
    }

    @Test
    public void shouldUpdate() throws Exception {
        var optional = gammaDashboardRepository.findById("gd-2");
        assertThat(optional).as("Gamma dashboard to update not found").isPresent();
        assertThat(optional.get().getTitle()).isEqualTo("API Metrics");

        var dashboard = optional.get();
        dashboard.setTitle("Updated Dashboard");
        dashboard.setDescription("Updated description");
        dashboard.setFilters(
            List.of(GammaDashboard.Filter.builder().field("PLAN").label("Plan").operator("in").value(List.of()).editable(true).build())
        );
        dashboard.setTimeRange(GammaDashboard.TimeRange.builder().type("relative").period("30d").build());
        dashboard.setWidgets(DOTTED_KEY_WIDGETS);
        dashboard.setUpdatedAt(new Date(1222222222222L));

        gammaDashboardRepository.update(dashboard);

        var updated = gammaDashboardRepository.findById("gd-2");
        assertThat(updated).hasValueSatisfying(result -> {
            assertThat(result.getId()).isEqualTo("gd-2");
            assertThat(result.getEnvironmentId()).isEqualTo("DEFAULT");
            assertThat(result.getTitle()).isEqualTo("Updated Dashboard");
            assertThat(result.getDescription()).isEqualTo("Updated description");
            assertThat(result.getCreatedBy()).isEqualTo("user-2");
            assertThat(compareDate(result.getCreatedAt(), new Date(1000000000000L))).isTrue();
            assertThat(compareDate(result.getUpdatedAt(), new Date(1222222222222L))).isTrue();
            assertThat(result.getFilters()).hasSize(1);
            assertThat(result.getFilters().get(0).getValue()).isEmpty();
            assertThat(result.getFilters().get(0).isEditable()).isTrue();
            assertThat(result.getTimeRange().getType()).isEqualTo("relative");
            assertThat(result.getTimeRange().getPeriod()).isEqualTo("30d");
            assertThat(result.getTimeRange().getFrom()).isNull();
            assertThat(result.getWidgets()).isEqualTo(DOTTED_KEY_WIDGETS);
            assertThat(result.getVersion()).isEqualTo(7);
        });
    }

    @Test
    public void shouldDelete() throws Exception {
        var nbBefore = gammaDashboardRepository.findByEnvironmentId("DEFAULT").size();
        gammaDashboardRepository.delete("gd-3");
        var nbAfter = gammaDashboardRepository.findByEnvironmentId("DEFAULT").size();

        assertThat(nbAfter).isEqualTo(nbBefore - 1);
        assertThat(gammaDashboardRepository.findById("gd-3")).isEmpty();
    }

    @Test
    public void shouldDeleteByEnvironmentId() throws Exception {
        assertThat(gammaDashboardRepository.findByEnvironmentId("OTHER_ENV")).hasSize(1);
        assertThat(gammaDashboardRepository.findByEnvironmentId("DEFAULT")).hasSize(3);

        gammaDashboardRepository.deleteByEnvironmentId("OTHER_ENV");

        assertThat(gammaDashboardRepository.findByEnvironmentId("OTHER_ENV")).isEmpty();
        assertThat(gammaDashboardRepository.findByEnvironmentId("DEFAULT")).hasSize(3);
        assertThat(gammaDashboardRepository.findById("gd-4")).isEmpty();
    }

    @Test
    public void shouldNotUpdateUnknownDashboard() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            var unknown = GammaDashboard.builder().id("unknown").title("Unknown").build();
            gammaDashboardRepository.update(unknown);
            fail("An unknown gamma dashboard should not be updated");
        });
    }

    /**
     * Mongo's {@code save()} is an upsert, so a read-then-write update would silently re-insert a dashboard deleted in
     * between — while JDBC, guarded by its affected-row count, raises. This pins the two backends to the same
     * behaviour: the delete wins, and nothing comes back.
     */
    @Test
    public void shouldNotResurrectADashboardDeletedBeforeTheUpdate() throws Exception {
        var dashboard = gammaDashboardRepository.findById("gd-3").orElseThrow();
        gammaDashboardRepository.delete("gd-3");

        dashboard.setTitle("Resurrected");
        assertThrows(IllegalStateException.class, () -> gammaDashboardRepository.update(dashboard));

        assertThat(gammaDashboardRepository.findById("gd-3")).isEmpty();
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            gammaDashboardRepository.update(null);
            fail("A null gamma dashboard should not be updated");
        });
    }
}
