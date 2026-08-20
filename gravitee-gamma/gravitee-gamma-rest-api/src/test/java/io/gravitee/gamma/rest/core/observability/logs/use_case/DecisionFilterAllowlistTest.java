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
package io.gravitee.gamma.rest.core.observability.logs.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.ExtensibleFilters;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.model.StaticFilters;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The decisions screen shows whatever the catalog scopes to {@code AUTHZ_DECISION}, while the search
 * refuses whatever is missing from its allowlist. Nothing links the two lists, so they drift silently:
 * a filter added to one and not the other is offered in the picker and then rejected with a 400.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DecisionFilterAllowlistTest {

    @Test
    void should_support_exactly_the_filters_the_decisions_screen_offers() {
        // Scope filters are stripped by removeScopeConditions before the allowlist is consulted, so
        // they are offered without ever being checked against it.
        Set<String> scopeFilters = Set.of("API", "API_TYPE", ExtensibleFilters.RECORD_TYPE.filterName());
        Set<String> offered = Arrays.stream(StaticFilters.values())
            .map(StaticFilters::toSpec)
            .filter(spec -> spec.signals().contains(Signal.LOGS))
            .filter(spec -> spec.apiTypes().contains(ApiType.AUTHZ_DECISION))
            .map(FilterSpec::name)
            .filter(name -> !scopeFilters.contains(name))
            .collect(Collectors.toSet());

        assertThat(SearchObservabilityLogsUseCase.DECISION_SUPPORTED_FILTERS)
            .as("every filter the decisions screen offers must be applicable by the decision search")
            .containsExactlyInAnyOrderElementsOf(offered);
    }
}
