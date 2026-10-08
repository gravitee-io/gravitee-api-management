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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.NullNode;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DashboardTest {

    @Test
    void should_reject_blank_id() {
        assertThatThrownBy(() ->
            new Dashboard("", "env-1", null, "title", null, List.of(), null, NullNode.getInstance(), 1, "u", Instant.now(), Instant.now())
        ).isInstanceOf(InvalidDashboardException.class);
    }

    @Test
    void should_reject_blank_environment_id() {
        assertThatThrownBy(() ->
            new Dashboard("d-1", " ", null, "title", null, List.of(), null, NullNode.getInstance(), 1, "u", Instant.now(), Instant.now())
        ).isInstanceOf(InvalidDashboardException.class);
    }

    @Test
    void should_reject_blank_title() {
        assertThatThrownBy(() ->
            new Dashboard("d-1", "env-1", null, "", null, List.of(), null, NullNode.getInstance(), 1, "u", Instant.now(), Instant.now())
        ).isInstanceOf(InvalidDashboardException.class);
    }

    @Test
    void should_default_null_filters_to_empty_list() {
        var dashboard = new Dashboard(
            "d-1",
            "env-1",
            null,
            "title",
            null,
            null,
            null,
            NullNode.getInstance(),
            1,
            "u",
            Instant.now(),
            Instant.now()
        );

        assertThat(dashboard.filters()).isEmpty();
    }

    @Test
    void should_belong_to_a_selection_holding_its_module() {
        assertThat(dashboardOfModule("aim").belongsToOneOf(Set.of("aim", "apim"))).isTrue();
        assertThat(dashboardOfModule("portals").belongsToOneOf(Set.of("aim", "apim"))).isFalse();
    }

    /** {@code Set.of} rejects {@code contains(null)}: a module-less dashboard must answer without asking it. */
    @Test
    void should_not_belong_to_any_selection_without_a_module() {
        assertThat(dashboardOfModule(null).belongsToOneOf(Set.of("aim"))).isFalse();
    }

    private static Dashboard dashboardOfModule(String module) {
        return new Dashboard(
            "d-1",
            "env-1",
            module,
            "title",
            null,
            List.of(),
            null,
            NullNode.getInstance(),
            1,
            "u",
            Instant.now(),
            Instant.now()
        );
    }
}
