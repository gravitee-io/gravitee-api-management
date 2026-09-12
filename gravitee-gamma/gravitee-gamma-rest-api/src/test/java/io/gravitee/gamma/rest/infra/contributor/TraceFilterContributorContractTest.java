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
package io.gravitee.gamma.rest.infra.contributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.gamma.rest.core.tracing.model.FilterCondition;
import io.gravitee.gamma.rest.core.tracing.model.FilterOperator;
import io.gravitee.gamma.rest.core.tracing.model.FilterType;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterSpec;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterContributor;
import io.gravitee.gamma.rest.core.tracing.use_case.SearchTraceFilterTranslator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Contract between what the trace explorer <b>advertises</b> and what it can <b>execute</b>.
 *
 * <p>The two surfaces are written in different files and drift silently: a filter offered by
 * {@code GET /filters/definition} that {@link SearchTraceFilterTranslator} cannot route answers 400
 * the moment the user picks it — a dead chip in the palette, discovered in production. The class
 * javadocs call the sync "a code-review check"; this test makes it a build check, across every
 * contributor actually registered in {@code META-INF/services}.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TraceFilterContributorContractTest {

    private static List<TraceFilterContributor> registeredContributors() {
        return ServiceLoader.load(TraceFilterContributor.class).stream().map(ServiceLoader.Provider::get).toList();
    }

    static Stream<TraceFilterSpec> allAdvertisedFilters() {
        return registeredContributors()
            .stream()
            .flatMap(contributor -> contributor.getFilters().stream());
    }

    @ParameterizedTest
    @MethodSource("allAdvertisedFilters")
    void should_translate_every_advertised_filter_to_a_search(TraceFilterSpec spec) {
        FilterCondition condition = new FilterCondition(spec.name(), FilterOperator.EQ, List.of(sampleValue(spec)));

        assertThatCode(() -> SearchTraceFilterTranslator.toAttributeFilters(List.of(condition))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("allAdvertisedFilters")
    void should_declare_the_only_operator_the_translator_supports(TraceFilterSpec spec) {
        // The translator handles EQ only today; a spec offering just IN would render a chip whose
        // every use is a 400.
        assertThat(spec.operators()).contains(FilterOperator.EQ);
    }

    @ParameterizedTest
    @MethodSource("allAdvertisedFilters")
    void should_not_advertise_a_filter_whose_values_the_backend_cannot_list(TraceFilterSpec spec) {
        // KEYWORD makes the lib render an async combobox backed by `/filters/{name}/values`, which the
        // MVP answers with `tracing.filter.value_listing_not_supported`. ENUM carries its own values,
        // STRING / NUMBER render free text — those degrade gracefully, KEYWORD does not.
        assertThat(spec.type()).isNotEqualTo(FilterType.KEYWORD);
    }

    @ParameterizedTest
    @MethodSource("allAdvertisedFilters")
    void should_ship_the_values_of_every_enum_filter(TraceFilterSpec spec) {
        if (spec.type() == FilterType.ENUM) {
            assertThat(spec.enumValues()).isNotNull().isNotEmpty();
        }
    }

    @Test
    void should_register_the_esm_contributor_scoped_to_the_esm_module() {
        // The ESM console sends `module=esm`; the registry matches on an exact id, so a typo here
        // silently falls back to the cross-module palette.
        assertThat(registeredContributors())
            .filteredOn(contributor -> "esm".equals(contributor.moduleId()))
            .singleElement()
            .isInstanceOf(EsmTraceFilterContributor.class);
    }

    /** A value the spec would accept — enums are picky, the rest take anything. */
    private static String sampleValue(TraceFilterSpec spec) {
        return spec.type() == FilterType.ENUM ? spec.enumValues().get(0) : "sample";
    }
}
