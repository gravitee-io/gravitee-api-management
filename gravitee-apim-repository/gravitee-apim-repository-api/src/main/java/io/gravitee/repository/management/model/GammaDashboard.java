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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Dashboard owned by the Gamma observability module.
 *
 * <p>Distinct from {@link Dashboard} (legacy analytics dashboard) and from {@link CustomDashboard}, whose collection is
 * read unfiltered by the APIM Console and the Developer Portal.
 *
 * <p>{@code widgets} is deliberately stored as opaque JSON: the widget shape belongs to the frontend and would
 * otherwise force a backend release per new widget option. It also sidesteps Mongo's rejection of dotted map keys,
 * which widget colour maps use (they are keyed by host and API names).
 *
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = "id")
public class GammaDashboard {

    private String id;
    private String environmentId;
    private String title;
    private String description;
    private List<Filter> filters;
    private TimeRange timeRange;

    /** Raw, opaque widget JSON. Never parsed by the backend. */
    private String widgets;

    private String createdBy;
    private Date createdAt;
    private Date updatedAt;

    /**
     * Optimistic concurrency counter. This layer only persists it verbatim — it never initialises, increments or
     * checks it. Bumping it belongs to the update use case, and rejecting a stale value is a separate concern.
     */
    private Integer version;

    /**
     * Dashboard-definition filter, applied to every widget of the dashboard.
     *
     * <p>Field names and casing deliberately mirror the frontend's {@code FilterCondition}, which is the only producer
     * and consumer of these values — the same type also travels verbatim inside the opaque {@code widgets} payload, so
     * any divergence here would mean storing one type in two shapes and maintaining a mapping table between them.
     */
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Filter {

        private String field;

        /**
         * Display name of the field, e.g. {@code API Type}. Persisted because nothing derives it: the frontend's
         * resolve hook rehydrates {@code valueLabels} from the API, never {@code label}.
         */
        private String label;

        /** Lowercase wire operator, matching the frontend vocabulary — {@code eq}, {@code in}, {@code not_in}. */
        private String operator;

        /** An empty list is the "all values" placeholder and carries no query constraint. */
        private List<String> value;

        /**
         * {@code true} when the viewer may change the value; {@code false} locks it. The one deliberate addition to
         * the frontend shape: it replaces the optional {@code locked} tri-state with a required positive boolean.
         */
        private boolean editable;
    }

    /**
     * Time window a dashboard opens on. {@code period} is set for {@code relative}, {@code from} / {@code to} (epoch
     * millis) for {@code absolute}.
     */
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class TimeRange {

        /** {@code relative} or {@code absolute}, lowercase to match the frontend's discriminated union. */
        private String type;

        /** Relative period token owned by the frontend, e.g. {@code 7d}, {@code 1h}, {@code currentMonth}. */
        private String period;

        private Long from;
        private Long to;
    }
}
