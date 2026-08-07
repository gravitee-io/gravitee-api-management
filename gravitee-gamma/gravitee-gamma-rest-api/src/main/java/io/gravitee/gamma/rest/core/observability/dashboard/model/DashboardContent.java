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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The author-editable part of a {@link Dashboard} — what POST and PUT both carry (OBS-16). Server-owned
 * fields ({@code version}, {@code createdBy}, timestamps, {@code environmentId}) are deliberately
 * absent: the write use cases derive them, so a request can never smuggle them in.
 *
 * <p>{@link #validate()} holds the structural write guardrails and nothing semantic: {@code widgets}
 * stays opaque apart from the caps and per-widget {@code id} checks below — the dangerous surface
 * (the analytics query) is validated server-side at read time, so a malformed widget yields a 400 at
 * query time rather than an arbitrary query. It is separate from {@link Dashboard}'s compact
 * constructor on purpose: the constructor also runs when re-hydrating persisted rows, where a
 * pre-guardrail row must keep loading.
 *
 * @author GraviteeSource Team
 */
public record DashboardContent(String title, String description, List<DashboardFilter> filters, TimeRange timeRange, JsonNode widgets) {
    /** React keys and grid identities on the frontend — a duplicate breaks rendering, so reject early. */
    private static final int MAX_WIDGETS = 50;

    /** Order-of-magnitude cap on the serialized widgets payload — the column is otherwise unbounded. */
    private static final int MAX_WIDGETS_PAYLOAD_LENGTH = 1_048_576;

    public DashboardContent {
        filters = filters == null ? List.of() : List.copyOf(filters);
    }

    public void validate() {
        if (title == null || title.isBlank()) {
            throw new InvalidDashboardException("Dashboard title is required");
        }
        validateWidgets();
        validateTimeRange();
    }

    private void validateWidgets() {
        if (widgets == null || widgets.isNull()) {
            return;
        }
        if (!widgets.isArray()) {
            throw new InvalidDashboardException("Dashboard widgets must be an array");
        }
        if (widgets.size() > MAX_WIDGETS) {
            throw new InvalidDashboardException("Dashboard cannot hold more than " + MAX_WIDGETS + " widgets");
        }
        if (widgets.toString().length() > MAX_WIDGETS_PAYLOAD_LENGTH) {
            throw new InvalidDashboardException("Dashboard widgets payload exceeds the maximum allowed size");
        }
        Set<String> seenIds = new HashSet<>();
        for (JsonNode widget : widgets) {
            String id = widget.path("id").asText("");
            if (id.isBlank()) {
                throw new InvalidDashboardException("Every dashboard widget requires a non-blank id");
            }
            if (!seenIds.add(id)) {
                throw new InvalidDashboardException("Duplicate widget id '" + id + "'");
            }
        }
    }

    private void validateTimeRange() {
        if (timeRange == null) {
            return;
        }
        if (timeRange.type() == TimeRangeType.RELATIVE && (timeRange.period() == null || timeRange.period().isBlank())) {
            throw new InvalidDashboardException("A relative time range requires a period");
        }
        if (timeRange.type() == TimeRangeType.ABSOLUTE) {
            if (timeRange.from() == null || timeRange.to() == null) {
                throw new InvalidDashboardException("An absolute time range requires both from and to");
            }
            if (timeRange.from() >= timeRange.to()) {
                throw new InvalidDashboardException("An absolute time range requires from to be before to");
            }
        }
    }
}
