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

import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityFilterValidatorAuthzDecisionFiltersTest {

    private final ObservabilityFilterValidator validator = new ObservabilityFilterValidator(new SpiFilterRegistry());

    @ParameterizedTest
    @CsvSource(
        {
            "AUTHZ_SUBJECT_ID, LOGS",
            "AUTHZ_SUBJECT_ID, ANALYTICS",
            "AUTHZ_ACTION, LOGS",
            "AUTHZ_ACTION, ANALYTICS",
            "AUTHZ_RESOURCE_ID, LOGS",
            "AUTHZ_RESOURCE_ID, ANALYTICS",
        }
    )
    void should_accept_several_entity_references_in_one_condition(String filter, Signal signal) {
        var conditions = List.of(new FilterCondition(filter, FilterOperator.IN, List.of("alice", "User::\"bob\"")));

        assertThatCode(() -> validator.validate(conditions, signal)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({ "evaluation_timeout", "pdp_unavailable", "evaluation_failed" })
    void should_accept_the_error_types_the_callers_write(String errorType) {
        var conditions = List.of(new FilterCondition("AUTHZ_ERROR_TYPE", FilterOperator.IN, List.of(errorType)));

        assertThatCode(() -> validator.validate(conditions, Signal.LOGS)).doesNotThrowAnyException();
    }
}
