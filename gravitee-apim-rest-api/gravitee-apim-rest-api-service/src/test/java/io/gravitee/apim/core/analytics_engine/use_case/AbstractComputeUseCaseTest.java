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
package io.gravitee.apim.core.analytics_engine.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterPreProcessor;
import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class AbstractComputeUseCaseTest {

    private final AnalyticsQueryContextProvider analyticsQueryContextProvider = mock(AnalyticsQueryContextProvider.class);
    private final AnalyticsQueryValidator analyticsQueryValidator = mock(AnalyticsQueryValidator.class);
    private final FilterPreProcessor filterPreprocessor = mock(FilterPreProcessor.class);
    private final SearchEngineService searchEngineService = mock(SearchEngineService.class);

    private final ComputeMeasuresUseCase computeMeasuresUseCase = new ComputeMeasuresUseCase(
        analyticsQueryContextProvider,
        analyticsQueryValidator,
        filterPreprocessor,
        searchEngineService
    );

    private static final List<String> allowedApiIds = List.of(UUID.randomUUID().toString());

    private record GetFilterByApiNameTestCasesTestCase(List<Filter> filter, Optional<Filter> expected) {}

    private record GetEffectiveFilterByApiTypeFilterTestCase(List<Filter> filter, Optional<List<String>> expected) {}

    @BeforeEach
    void setUp() {
        var result = new SearchResult(allowedApiIds);
        when(searchEngineService.search(any(), any())).thenReturn(result);
    }

    static Stream<GetFilterByApiNameTestCasesTestCase> getFilterByApiNameTestCases() {
        Filter filterById = new Filter(FilterSpec.Name.API, FilterSpec.Operator.EQ, UUID.randomUUID().toString());
        Filter filterByName1 = new Filter(FilterSpec.Name.API_NAME, FilterSpec.Operator.EQ, ApiSpec.Name.HTTP_PROXY);
        Filter filterByName2 = new Filter(FilterSpec.Name.API_NAME, FilterSpec.Operator.EQ, ApiSpec.Name.MESSAGE);

        return Stream.of(
            // Filter is not by name
            new GetFilterByApiNameTestCasesTestCase(List.of(filterById), Optional.empty()),
            // EQ filter with a name should return the value
            new GetFilterByApiNameTestCasesTestCase(
                List.of(filterByName1),
                Optional.of(new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, allowedApiIds))
            ),
            // Multiple EQ filters should return no result
            new GetFilterByApiNameTestCasesTestCase(
                List.of(filterByName1, filterByName2),
                Optional.of(new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, Collections.emptyList()))
            )
        );
    }

    @ParameterizedTest
    @MethodSource("getFilterByApiNameTestCases")
    void should_return_filter_by_api_name(GetFilterByApiNameTestCasesTestCase testCase) {
        var effectiveFilters = computeMeasuresUseCase.getFilterByApiName(testCase.filter);

        assertThat(effectiveFilters).isEqualTo(testCase.expected);
    }

    static Stream<GetEffectiveFilterByApiTypeFilterTestCase> getEffectiveFilterByApiTypeTestCases() {
        Filter filterById = new Filter(FilterSpec.Name.API, FilterSpec.Operator.EQ, UUID.randomUUID().toString());
        Filter emptyFilterByName = new Filter(FilterSpec.Name.API_NAME, FilterSpec.Operator.IN, List.of());
        Filter filterByName1 = new Filter(FilterSpec.Name.API_NAME, FilterSpec.Operator.EQ, ApiSpec.Name.HTTP_PROXY);
        Filter filterByName2 = new Filter(FilterSpec.Name.API_NAME, FilterSpec.Operator.EQ, ApiSpec.Name.MESSAGE);
        Filter filterByListOfNames1 = new Filter(
            FilterSpec.Name.API_NAME,
            FilterSpec.Operator.IN,
            List.of(ApiSpec.Name.HTTP_PROXY, ApiSpec.Name.MESSAGE)
        );
        Filter filterByListOfNames2 = new Filter(FilterSpec.Name.API_NAME, FilterSpec.Operator.IN, List.of(ApiSpec.Name.HTTP_PROXY));

        return Stream.of(
            // Filter is not by name
            new GetEffectiveFilterByApiTypeFilterTestCase(List.of(filterById), Optional.empty()),
            // EQ filter with a name should return the value
            new GetEffectiveFilterByApiTypeFilterTestCase(List.of(filterByName1), Optional.of(List.of("V4_HTTP_PROXY"))),
            // IN filter without values should return no result
            new GetEffectiveFilterByApiTypeFilterTestCase(List.of(emptyFilterByName), Optional.of(Collections.emptyList())),
            // IN filter with values should return the list of values
            new GetEffectiveFilterByApiTypeFilterTestCase(
                List.of(filterByListOfNames1),
                Optional.of(List.of("V4_HTTP_PROXY", "V4_MESSAGE"))
            ),
            // Multiple EQ filters should return no result
            new GetEffectiveFilterByApiTypeFilterTestCase(List.of(filterByName1, filterByName2), Optional.of(Collections.emptyList())),
            // Multiple IN filters should return no result
            new GetEffectiveFilterByApiTypeFilterTestCase(
                List.of(filterByListOfNames1, filterByListOfNames2),
                Optional.of(Collections.emptyList())
            ),
            // Combine a filter by name and another filter
            new GetEffectiveFilterByApiTypeFilterTestCase(List.of(filterById, filterByName1), Optional.of(List.of("V4_HTTP_PROXY")))
        );
    }

    @ParameterizedTest
    @MethodSource("getEffectiveFilterByApiTypeTestCases")
    void should_return_correct_filters(GetEffectiveFilterByApiTypeFilterTestCase testCase) {
        var effectiveFilters = computeMeasuresUseCase.getEffectiveFilterByApiType(testCase.filter);

        assertThat(effectiveFilters).isEqualTo(testCase.expected);
    }
}
