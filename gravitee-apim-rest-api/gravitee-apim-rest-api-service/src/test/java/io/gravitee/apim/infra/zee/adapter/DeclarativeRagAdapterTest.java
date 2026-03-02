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
package io.gravitee.apim.infra.zee.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.zee.domain_service.RagSection;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeclarativeRagAdapterTest {

    @Test
    void resource_type_returns_configured_type() {
        var adapter = new DeclarativeRagAdapter(ZeeResourceType.FLOW, List.of());
        assertThat(adapter.resourceType()).isEqualTo(ZeeResourceType.FLOW);
    }

    @Nested
    class RetrieveContext {

        @Test
        void returns_empty_when_no_sections() {
            var adapter = new DeclarativeRagAdapter(ZeeResourceType.FLOW, List.of());
            var result = adapter.retrieveContext("env-1", "org-1", Map.of());
            assertThat(result).isEmpty();
        }

        @Test
        void renders_single_section_with_items() {
            var section = new RagSection<>(
                "Existing Flows",
                ctx -> List.of("Rate Limit Flow: 2 step(s)", "Auth Flow: 1 step(s)"),
                item -> item,
                10
            );

            var adapter = new DeclarativeRagAdapter(ZeeResourceType.FLOW, List.of(section));
            var result = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(result).contains("### Existing Flows");
            assertThat(result).contains("- Rate Limit Flow: 2 step(s)");
            assertThat(result).contains("- Auth Flow: 1 step(s)");
        }

        @Test
        void renders_multiple_sections_with_separator() {
            var flowSection = new RagSection<>("Existing Flows", ctx -> List.of("Rate Limit Flow"), item -> item, 10);
            var policySection = new RagSection<>("Available Policies", ctx -> List.of("rate-limit: Rate limiting"), item -> item, 10);

            var adapter = new DeclarativeRagAdapter(ZeeResourceType.FLOW, List.of(flowSection, policySection));
            var result = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(result).contains("### Existing Flows");
            assertThat(result).contains("### Available Policies");
            assertThat(result).contains("- Rate Limit Flow");
            assertThat(result).contains("- rate-limit: Rate limiting");
        }

        @Test
        void limits_items_to_max() {
            var section = new RagSection<>("Items", ctx -> List.of("a", "b", "c", "d", "e"), item -> item, 3);

            var adapter = new DeclarativeRagAdapter(ZeeResourceType.FLOW, List.of(section));
            var result = adapter.retrieveContext("env-1", "org-1", Map.of());

            long itemLines = result
                .lines()
                .filter(line -> line.startsWith("- "))
                .count();
            assertThat(itemLines).isEqualTo(3);
        }

        @Test
        void skips_section_when_fetcher_returns_empty() {
            var emptySection = new RagSection<String>("Empty Section", ctx -> List.of(), item -> item, 10);
            var populatedSection = new RagSection<>("Populated Section", ctx -> List.of("item1"), item -> item, 10);

            var adapter = new DeclarativeRagAdapter(ZeeResourceType.FLOW, List.of(emptySection, populatedSection));
            var result = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(result).doesNotContain("### Empty Section");
            assertThat(result).contains("### Populated Section");
        }

        @Test
        void skips_section_when_fetcher_returns_null() {
            var nullSection = new RagSection<String>("Null Section", ctx -> null, item -> item, 10);

            var adapter = new DeclarativeRagAdapter(ZeeResourceType.FLOW, List.of(nullSection));
            var result = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(result).isEmpty();
        }

        @Test
        void degrades_gracefully_when_fetcher_throws() {
            var failingSection = new RagSection<String>(
                "Failing Section",
                ctx -> {
                    throw new RuntimeException("DB unavailable");
                },
                item -> item,
                10
            );
            var okSection = new RagSection<>("OK Section", ctx -> List.of("item1"), item -> item, 10);

            var adapter = new DeclarativeRagAdapter(ZeeResourceType.FLOW, List.of(failingSection, okSection));
            var result = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(result).doesNotContain("### Failing Section");
            assertThat(result).contains("### OK Section");
            assertThat(result).contains("- item1");
        }

        @Test
        void passes_context_data_to_fetcher() {
            var section = new RagSection<>(
                "Contextual Section",
                ctx -> {
                    var apiId = (String) ctx.get("apiId");
                    return apiId != null ? List.of("Flow for " + apiId) : List.of();
                },
                item -> item,
                10
            );

            var adapter = new DeclarativeRagAdapter(ZeeResourceType.FLOW, List.of(section));

            var withContext = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));
            assertThat(withContext).contains("- Flow for api-123");

            var withoutContext = adapter.retrieveContext("env-1", "org-1", Map.of());
            assertThat(withoutContext).isEmpty();
        }

        @Test
        void applies_formatter_to_each_item() {
            record NamedItem(String id, String desc) {}
            var section = new RagSection<>(
                "Formatted Items",
                ctx -> List.of(new NamedItem("jwt", "JWT validation"), new NamedItem("rate-limit", "Rate limiting")),
                item -> item.id() + ": " + item.desc(),
                10
            );

            var adapter = new DeclarativeRagAdapter(ZeeResourceType.ENDPOINT, List.of(section));
            var result = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(result).contains("- jwt: JWT validation");
            assertThat(result).contains("- rate-limit: Rate limiting");
        }
    }
}
