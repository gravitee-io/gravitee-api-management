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
package io.gravitee.apim.core.analytics_engine.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsQueryContextTest {

    private static AnalyticsQueryContext context(Optional<Set<String>> restrictedToApplicationIds) {
        return new AnalyticsQueryContext(
            AuditInfoFixtures.anAuditInfo("org-id", "env-id", "user-id"),
            new ExecutionContext("org-id", "env-id"),
            Set.of("api-1"),
            Map.of(),
            Map.of(),
            Map.of(),
            restrictedToApplicationIds
        );
    }

    @Test
    void should_refuse_a_context_that_does_not_say_whether_it_is_restricted_to_applications() {
        assertThatThrownBy(() -> context(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_keep_the_applications_it_was_restricted_to_when_the_caller_changes_its_set() {
        var applicationIds = new HashSet<>(List.of("app-1"));

        var context = context(Optional.of(applicationIds));
        applicationIds.add("app-2");

        assertThat(context.restrictedToApplicationIds()).contains(Set.of("app-1"));
    }
}
