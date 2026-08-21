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
import org.junit.jupiter.api.Test;

// Real registry, not mocked: this asserts RECORD_TYPE actually carries the ANALYTICS signal.
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityFilterValidatorRecordTypeAnalyticsScopeTest {

    private final ObservabilityFilterValidator validator = new ObservabilityFilterValidator(new SpiFilterRegistry());

    @Test
    void should_accept_record_type_authz_decision_as_an_analytics_scope_condition() {
        var conditions = List.of(new FilterCondition("RECORD_TYPE", FilterOperator.EQ, List.of("AUTHZ_DECISION")));

        assertThatCode(() -> validator.validate(conditions, Signal.ANALYTICS)).doesNotThrowAnyException();
    }
}
