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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApplicationScopeFilterTransformerTest {

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo("DEFAULT", "DEFAULT", "user");

    private static final Filter API_FILTER = new Filter(FilterSpec.Name.API, FilterOperator.IN, Set.of("api-1"));

    private final ApplicationScopeFilterTransformer transformer = new ApplicationScopeFilterTransformer();

    private static AnalyticsQueryContext context(Optional<Set<String>> restrictedToApplicationIds) {
        return new AnalyticsQueryContext(
            AUDIT_INFO,
            new ExecutionContext("DEFAULT", "DEFAULT"),
            Set.of("api-1"),
            Map.of(),
            Map.of(),
            Map.of(),
            restrictedToApplicationIds
        );
    }

    @Test
    void should_restrict_the_query_to_the_applications_of_the_context() {
        var filters = transformer.transform(context(Optional.of(Set.of("app-1", "app-2"))), List.of(API_FILTER));

        assertThat(filters).containsExactly(
            API_FILTER,
            new Filter(FilterSpec.Name.APPLICATION, FilterOperator.IN, Set.of("app-1", "app-2"))
        );
    }

    @Test
    void should_match_no_call_when_the_context_is_restricted_to_no_application() {
        var filters = transformer.transform(context(Optional.of(Set.of())), List.of(API_FILTER));

        assertThat(filters).containsExactly(API_FILTER, new Filter(FilterSpec.Name.APPLICATION, FilterOperator.IN, Set.of()));
    }

    @Test
    void should_keep_an_application_condition_of_the_request_next_to_the_scope() {
        var requested = new Filter(FilterSpec.Name.APPLICATION, FilterOperator.EQ, "app-3");

        var filters = transformer.transform(context(Optional.of(Set.of("app-1"))), List.of(requested));

        assertThat(filters).containsExactly(requested, new Filter(FilterSpec.Name.APPLICATION, FilterOperator.IN, Set.of("app-1")));
    }

    @Test
    void should_leave_the_filters_unchanged_when_the_context_is_not_restricted_to_applications() {
        var filters = transformer.transform(context(Optional.empty()), List.of(API_FILTER));

        assertThat(filters).containsExactly(API_FILTER);
    }

    @Test
    void should_not_restrict_a_context_built_without_an_application_scope() {
        var context = AnalyticsQueryContext.forApis(AUDIT_INFO, Set.of("api-1"));

        assertThat(transformer.transform(context, List.of(API_FILTER))).containsExactly(API_FILTER);
    }
}
