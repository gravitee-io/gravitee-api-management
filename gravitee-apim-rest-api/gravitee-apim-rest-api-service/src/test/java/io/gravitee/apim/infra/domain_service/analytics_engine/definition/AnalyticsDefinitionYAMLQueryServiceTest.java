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
package io.gravitee.apim.infra.domain_service.analytics_engine.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.FilterType;
import io.gravitee.apim.core.observability.model.Signal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsDefinitionYAMLQueryServiceTest {

    @Nested
    class NameIndex {

        @Test
        void should_resolve_a_filter_by_name() {
            var service = new AnalyticsDefinitionYAMLQueryService();

            assertThat(service.findFilter(FilterSpec.Name.HTTP_METHOD)).hasValueSatisfying(spec ->
                assertThat(spec.name()).isEqualTo(FilterSpec.Name.HTTP_METHOD)
            );
        }

        @Test
        void should_index_every_filter_the_catalog_declares() {
            var service = new AnalyticsDefinitionYAMLQueryService();

            assertThat(service.getAllFilters()).allSatisfy(spec -> assertThat(service.findFilter(spec.name())).contains(spec));
        }

        @Test
        void should_reject_a_catalog_declaring_the_same_filter_twice() {
            // A copy-pasted block would otherwise resolve to whichever entry loaded first, and the second one
            // would never be read — a silent catalog bug is exactly what the signal axis exists to prevent.
            var duplicated = List.of(filterNamed(FilterSpec.Name.HTTP_METHOD), filterNamed(FilterSpec.Name.HTTP_METHOD));

            assertThatThrownBy(() -> AnalyticsDefinitionYAMLQueryService.indexByName(duplicated))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP_METHOD")
                .hasMessageContaining("declared more than once");
        }

        private FilterSpec filterNamed(FilterSpec.Name name) {
            return new FilterSpec(
                name,
                name.name(),
                FilterType.STRING,
                List.of(),
                null,
                List.of(FilterOperator.EQ),
                List.of(),
                Set.of(Signal.ANALYTICS)
            );
        }
    }

    @Nested
    class AuthzFacets {

        // A search document carries no decision, so faceting searches on it yields one empty bucket.
        @Test
        void should_not_offer_decision_as_a_facet_of_searches() {
            var service = new AnalyticsDefinitionYAMLQueryService();

            var facetNames = service.getFacets(MetricSpec.Name.AUTHZ_SEARCHES).stream().map(FacetSpec::name).toList();

            assertThat(facetNames).doesNotContain(FacetSpec.Name.AUTHZ_DECISION);
        }
    }
}
