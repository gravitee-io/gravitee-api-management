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
package io.gravitee.gamma.rest.core.observability.dashboard.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Pins the structural write guardrails of OBS-16 — and only those: widgets stay opaque beyond the
 * caps and id checks, semantic validation belongs to the analytics read path.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DashboardContentTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void should_accept_a_minimal_content_with_only_a_title() {
        assertThatCode(() -> new DashboardContent("Perf", null, null, null, null).validate()).doesNotThrowAnyException();
    }

    @Test
    void should_reject_a_blank_title() {
        assertThatThrownBy(() -> new DashboardContent("  ", null, List.of(), null, null).validate())
            .isInstanceOf(InvalidDashboardException.class)
            .hasMessageContaining("title");
    }

    @Test
    void should_accept_fifty_widgets_and_reject_fifty_one() throws Exception {
        assertThatCode(() -> content(widgetArray(50)).validate()).doesNotThrowAnyException();

        assertThatThrownBy(() -> content(widgetArray(51)).validate())
            .isInstanceOf(InvalidDashboardException.class)
            .hasMessageContaining("50");
    }

    @Test
    void should_reject_widgets_that_are_not_an_array() throws Exception {
        assertThatThrownBy(() -> content(MAPPER.readTree("{\"type\":\"metric\"}")).validate())
            .isInstanceOf(InvalidDashboardException.class)
            .hasMessageContaining("array");
    }

    @Test
    void should_reject_a_widget_without_an_id() throws Exception {
        assertThatThrownBy(() -> content(MAPPER.readTree("[{\"type\":\"metric\"}]")).validate())
            .isInstanceOf(InvalidDashboardException.class)
            .hasMessageContaining("id");
    }

    @Test
    void should_reject_duplicate_widget_ids() throws Exception {
        assertThatThrownBy(() -> content(MAPPER.readTree("[{\"id\":\"w1\"},{\"id\":\"w1\"}]")).validate())
            .isInstanceOf(InvalidDashboardException.class)
            .hasMessageContaining("w1");
    }

    @Test
    void should_reject_an_oversized_widgets_payload() throws Exception {
        String bigValue = "x".repeat(1_100_000);
        JsonNode widgets = MAPPER.readTree("[{\"id\":\"w1\",\"blob\":\"" + bigValue + "\"}]");

        assertThatThrownBy(() -> content(widgets).validate())
            .isInstanceOf(InvalidDashboardException.class)
            .hasMessageContaining("size");
    }

    @Test
    void should_reject_a_relative_time_range_without_period() {
        TimeRange timeRange = new TimeRange(TimeRangeType.RELATIVE, null, null, null);

        assertThatThrownBy(() -> new DashboardContent("Perf", null, List.of(), timeRange, null).validate())
            .isInstanceOf(InvalidDashboardException.class)
            .hasMessageContaining("period");
    }

    @Test
    void should_reject_an_absolute_time_range_with_from_not_before_to() {
        TimeRange timeRange = new TimeRange(TimeRangeType.ABSOLUTE, null, 2000L, 1000L);

        assertThatThrownBy(() -> new DashboardContent("Perf", null, List.of(), timeRange, null).validate())
            .isInstanceOf(InvalidDashboardException.class)
            .hasMessageContaining("before");
    }

    @Test
    void should_reject_an_absolute_time_range_missing_a_bound() {
        TimeRange timeRange = new TimeRange(TimeRangeType.ABSOLUTE, null, 1000L, null);

        assertThatThrownBy(() -> new DashboardContent("Perf", null, List.of(), timeRange, null).validate())
            .isInstanceOf(InvalidDashboardException.class)
            .hasMessageContaining("from and to");
    }

    private static DashboardContent content(JsonNode widgets) {
        return new DashboardContent("Perf", null, List.of(), null, widgets);
    }

    private static JsonNode widgetArray(int count) throws Exception {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"id\":\"w").append(i).append("\"}");
        }
        return MAPPER.readTree(json.append(']').toString());
    }
}
