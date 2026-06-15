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
package io.gravitee.gamma.rest.core.observability.filter.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Validates incoming {@link FilterCondition}s against the unified filter catalog for a given
 * {@link Signal}. Centralising this here keeps the catalog the single source of truth and guarantees
 * that the logs and analytics surfaces enforce the <em>same</em> name / signal / operator rules for a
 * given filter — a filter and its operators behave identically whichever signal queries it.
 *
 * <p>Each condition is rejected (HTTP 400 via {@link UnsupportedObservabilityFilterException}) when:
 * <ul>
 *   <li>its {@code name} is not in the catalog,</li>
 *   <li>the catalog entry does not apply to the requested signal, or</li>
 *   <li>its {@code operator} is not among the operators the catalog advertises for that filter.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ObservabilityFilterValidator {

    private final FilterRegistry filterRegistry;

    public void validate(List<FilterCondition> conditions, Signal signal) {
        if (conditions == null) {
            return;
        }
        Map<String, FilterSpec> specsByName = filterRegistry
            .getFilters(Set.of(), Set.of())
            .stream()
            .collect(Collectors.toMap(FilterSpec::name, spec -> spec, (a, b) -> b));

        for (FilterCondition condition : conditions) {
            FilterSpec spec = specsByName.get(condition.name());
            if (spec == null) {
                throw UnsupportedObservabilityFilterException.unknownName(condition.name());
            }
            if (!spec.signals().contains(signal)) {
                throw UnsupportedObservabilityFilterException.signalMismatch(condition.name(), signal);
            }
            if (!spec.operators().contains(condition.operator())) {
                throw UnsupportedObservabilityFilterException.unsupportedOperator(condition.name(), condition.operator().name());
            }
        }
    }
}
