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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardFilter;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRange;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRangeType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GammaDashboardRepository;
import io.gravitee.repository.management.model.GammaDashboard;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The persisted {@link GammaDashboard.Filter} uses {@code field}/lowercase-{@code operator}/
 * {@code value} while this endpoint's wire format (and the domain {@code FilterCondition} it
 * composes) uses {@code name}/UPPERCASE-{@code operator}/{@code values} — this test pins the
 * anticorruption mapping between the two so a future refactor can't silently drop or mis-map a field.
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GammaDashboardRepositoryAdapterTest {

    @Mock
    private GammaDashboardRepository gammaDashboardRepository;

    @Test
    void should_map_persisted_filter_shape_to_the_domain_filter_condition_shape() throws Exception {
        GammaDashboard persisted = GammaDashboard.builder()
            .id("dash-1")
            .environmentId("env-1")
            .title("Performance overview")
            .filters(
                List.of(
                    GammaDashboard.Filter.builder()
                        .field("API_TYPE")
                        .label("API Type")
                        .operator("eq")
                        .value(List.of("MCP"))
                        .editable(false)
                        .build()
                )
            )
            .widgets("[]")
            .version(3)
            .createdAt(new Date())
            .updatedAt(new Date())
            .build();
        when(gammaDashboardRepository.findByIdAndEnvironmentId("dash-1", "env-1")).thenReturn(Optional.of(persisted));

        Dashboard dashboard = new GammaDashboardRepositoryAdapter(gammaDashboardRepository)
            .findByIdAndEnvironmentId("dash-1", "env-1")
            .orElseThrow();

        var filter = dashboard.filters().get(0);
        assertThat(filter.condition().name()).isEqualTo("API_TYPE");
        assertThat(filter.condition().operator()).isEqualTo(FilterOperator.EQ);
        assertThat(filter.condition().values()).containsExactly("MCP");
        assertThat(filter.label()).isEqualTo("API Type");
        assertThat(filter.editable()).isFalse();
    }

    @Test
    void should_wrap_unsupported_or_missing_persisted_operator_as_a_technical_exception() throws Exception {
        GammaDashboard persisted = GammaDashboard.builder()
            .id("dash-1")
            .environmentId("env-1")
            .title("Performance overview")
            .filters(List.of(GammaDashboard.Filter.builder().field("API_TYPE").operator(null).value(List.of("MCP")).build()))
            .widgets("[]")
            .version(1)
            .createdAt(new Date())
            .updatedAt(new Date())
            .build();
        when(gammaDashboardRepository.findByIdAndEnvironmentId("dash-1", "env-1")).thenReturn(Optional.of(persisted));

        assertThatThrownBy(() ->
            new GammaDashboardRepositoryAdapter(gammaDashboardRepository).findByIdAndEnvironmentId("dash-1", "env-1")
        ).isInstanceOf(TechnicalDomainException.class);
    }

    @Test
    void should_parse_widgets_json_verbatim() throws Exception {
        GammaDashboard persisted = GammaDashboard.builder()
            .id("dash-1")
            .environmentId("env-1")
            .title("Performance overview")
            .widgets("[{\"type\":\"metric\"}]")
            .version(1)
            .createdAt(new Date())
            .updatedAt(new Date())
            .build();
        when(gammaDashboardRepository.findByIdAndEnvironmentId("dash-1", "env-1")).thenReturn(Optional.of(persisted));

        Dashboard dashboard = new GammaDashboardRepositoryAdapter(gammaDashboardRepository)
            .findByIdAndEnvironmentId("dash-1", "env-1")
            .orElseThrow();

        assertThat(dashboard.widgets().isArray()).isTrue();
        assertThat(dashboard.widgets().get(0).get("type").asText()).isEqualTo("metric");
    }

    @Test
    void should_default_widgets_to_null_node_when_not_persisted() throws Exception {
        GammaDashboard persisted = GammaDashboard.builder()
            .id("dash-1")
            .environmentId("env-1")
            .title("Performance overview")
            .widgets(null)
            .version(1)
            .createdAt(new Date())
            .updatedAt(new Date())
            .build();
        when(gammaDashboardRepository.findByIdAndEnvironmentId("dash-1", "env-1")).thenReturn(Optional.of(persisted));

        Dashboard dashboard = new GammaDashboardRepositoryAdapter(gammaDashboardRepository)
            .findByIdAndEnvironmentId("dash-1", "env-1")
            .orElseThrow();

        assertThat(dashboard.widgets().isNull()).isTrue();
    }

    @Test
    void should_map_the_domain_shape_back_to_the_persisted_shape_on_create() throws Exception {
        Instant createdAt = Instant.parse("2026-08-07T10:00:00Z");
        Dashboard dashboard = new Dashboard(
            "dash-1",
            "env-1",
            "Performance overview",
            "desc",
            List.of(new DashboardFilter(new FilterCondition("API_TYPE", FilterOperator.NOT_IN, List.of("MCP")), "API Type", true)),
            new TimeRange(TimeRangeType.RELATIVE, "24h", null, null),
            new ObjectMapper().readTree("[{\"id\":\"w1\",\"type\":\"metric\"}]"),
            1,
            "user-1",
            createdAt,
            createdAt
        );
        when(gammaDashboardRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new GammaDashboardRepositoryAdapter(gammaDashboardRepository).create(dashboard);

        var captor = ArgumentCaptor.forClass(GammaDashboard.class);
        verify(gammaDashboardRepository).create(captor.capture());
        GammaDashboard persisted = captor.getValue();
        assertThat(persisted.getId()).isEqualTo("dash-1");
        assertThat(persisted.getEnvironmentId()).isEqualTo("env-1");
        assertThat(persisted.getTitle()).isEqualTo("Performance overview");
        var filter = persisted.getFilters().get(0);
        assertThat(filter.getField()).isEqualTo("API_TYPE");
        assertThat(filter.getLabel()).isEqualTo("API Type");
        assertThat(filter.getOperator()).isEqualTo("not_in");
        assertThat(filter.getValue()).containsExactly("MCP");
        assertThat(filter.isEditable()).isTrue();
        assertThat(persisted.getTimeRange().getType()).isEqualTo("relative");
        assertThat(persisted.getTimeRange().getPeriod()).isEqualTo("24h");
        assertThat(persisted.getWidgets()).isEqualTo("[{\"id\":\"w1\",\"type\":\"metric\"}]");
        assertThat(persisted.getVersion()).isEqualTo(1);
        assertThat(persisted.getCreatedBy()).isEqualTo("user-1");
        assertThat(persisted.getCreatedAt()).isEqualTo(Date.from(createdAt));
        assertThat(persisted.getUpdatedAt()).isEqualTo(Date.from(createdAt));
    }

    @Test
    void should_persist_absent_widgets_as_null_on_update() throws Exception {
        Dashboard dashboard = new Dashboard(
            "dash-1",
            "env-1",
            "Performance overview",
            null,
            List.of(),
            null,
            NullNode.getInstance(),
            2,
            "user-1",
            Instant.parse("2026-08-07T10:00:00Z"),
            Instant.parse("2026-08-07T11:00:00Z")
        );
        when(gammaDashboardRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new GammaDashboardRepositoryAdapter(gammaDashboardRepository).update(dashboard);

        var captor = ArgumentCaptor.forClass(GammaDashboard.class);
        verify(gammaDashboardRepository).update(captor.capture());
        assertThat(captor.getValue().getWidgets()).isNull();
        assertThat(captor.getValue().getTimeRange()).isNull();
        assertThat(captor.getValue().getVersion()).isEqualTo(2);
    }

    @Test
    void should_wrap_a_delete_failure_as_a_technical_exception() throws Exception {
        doThrow(new TechnicalException("boom")).when(gammaDashboardRepository).delete("dash-1");

        assertThatThrownBy(() -> new GammaDashboardRepositoryAdapter(gammaDashboardRepository).delete("dash-1")).isInstanceOf(
            TechnicalDomainException.class
        );
    }
}
