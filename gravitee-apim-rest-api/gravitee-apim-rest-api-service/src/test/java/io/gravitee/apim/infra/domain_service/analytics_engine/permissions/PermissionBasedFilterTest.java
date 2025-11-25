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
package io.gravitee.apim.infra.domain_service.analytics_engine.permissions;

import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Name.API;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.EQ;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.GTE;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.IN;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.LTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Builder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author GraviteeSource Team
 */
class PermissionBasedFilterTest extends AbstractTest {

    static final String apiId1 = "api-id-1";
    static final String apiId2 = "api-id-2";
    static final String apiId11 = "api-id-11";
    static final String apiId12 = "api-id-12";

    static Set<String> allowedApiIds = Set.of(apiId1, apiId2);

    @Builder
    record EqTestArguments(String wantedApiId, FilterSpec.Operator expectedOperator, Object expectedApiIds) {}

    @Builder
    record InTestArguments(List<String> wantedApiIds, List<String> expectedApiIds) {}

    @Test
    void should_update_empty_filter_to_include_all_allowed_ids() {
        var emptyFilters = new ArrayList<Filter>();
        var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(emptyFilters, allowedApiIds);

        assertThat(updatedFilters)
            .singleElement()
            .satisfies(filter -> {
                assertThat(filter.name()).isEqualTo(API);
                assertThat(filter.operator()).isEqualTo(IN);
                assertThat(filter.value())
                    .asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                    .containsExactlyInAnyOrderElementsOf(allowedApiIds);
            });
    }

    @Test
    void should_update_multiple_filters() {
        var equalityFilter1 = new Filter(API, EQ, apiId1);
        var listFilter = new Filter(API, IN, List.of(apiId2, apiId11, "invalid-api-id"));
        var equalityFilter2 = new Filter(API, EQ, "invalid-api-id");

        var filters = List.of(equalityFilter1, listFilter, equalityFilter2);

        var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters, allowedApiIds);

        assertThat(updatedFilters).containsExactly(equalityFilter1, new Filter(API, IN, List.of(apiId2)), new Filter(API, IN, List.of()));
    }

    @Test
    void should_not_change_unsupported_filters() {
        var filter1 = new Filter(API, LTE, "some-value");
        var filter2 = new Filter(API, GTE, "other-value");

        var filters = List.of(filter1, filter2);

        var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters, allowedApiIds);

        assertThat(updatedFilters).isEqualTo(filters);
    }

    @ParameterizedTest
    @MethodSource("eqTestParams")
    void should_allow_access_to_single_api(EqTestArguments args) {
        var filter = new Filter(API, EQ, args.wantedApiId);

        var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(List.of(filter), allowedApiIds);

        assertThat(updatedFilters)
            .singleElement()
            .satisfies(f -> {
                assertThat(f.name()).isEqualTo(API);
                assertThat(f.operator()).isEqualTo(args.expectedOperator);
                assertThat(f.value()).isEqualTo(args.expectedApiIds);
            });
    }

    @ParameterizedTest
    @MethodSource("inTestParams")
    void should_update_filter_list_to_include_only_valid_api_ids(InTestArguments args) {
        var filter = new Filter(API, IN, args.wantedApiIds);

        var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(List.of(filter), allowedApiIds);

        assertThat(updatedFilters)
            .singleElement()
            .satisfies(f -> {
                assertThat(f.name()).isEqualTo(API);
                assertThat(f.operator()).isEqualTo(IN);
                assertThat(f.value())
                    .asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                    .containsExactlyInAnyOrderElementsOf(args.expectedApiIds);
            });
    }

    @Test
    void should_throw_exception_when_IN_filter_is_not_an_iterable() {
        var filters = List.of(new Filter(API, IN, 1));

        assertThatThrownBy(() -> apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters, allowedApiIds))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Filter value must be an Iterable");
    }

    @Test
    void should_handle_non_string_values_in_IN_filter() {
        var filters = List.of(new Filter(API, IN, List.of(apiId2, true, 6)));

        var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters, allowedApiIds);

        var expectedFilter = new Filter(API, IN, List.of(apiId2));
        assertThat(updatedFilters).containsExactly(expectedFilter);
    }

    static Stream<EqTestArguments> eqTestParams() {
        return Stream.of(
            // User has access to the ID requested
            EqTestArguments.builder().wantedApiId(apiId1).expectedOperator(EQ).expectedApiIds(apiId1).build(),
            // User doesn't have access to the API ID requested
            EqTestArguments.builder().wantedApiId(apiId11).expectedOperator(IN).expectedApiIds(List.of()).build(),
            // User requests an invalid API ID
            EqTestArguments.builder().wantedApiId("invalid-api-id").expectedOperator(IN).expectedApiIds(List.of()).build()
        );
    }

    static Stream<InTestArguments> inTestParams() {
        return Stream.of(
            // User requesting an empty list should get an empty list
            InTestArguments.builder().wantedApiIds(List.of()).expectedApiIds(List.of()).build(),
            // User has access to the API IDs requested
            InTestArguments.builder().wantedApiIds(List.of(apiId1)).expectedApiIds(List.of(apiId1)).build(),
            // User doesn't have access to the API IDs requested
            InTestArguments.builder().wantedApiIds(List.of(apiId11, apiId12)).expectedApiIds(List.of()).build(),
            // User has access to some if the API IDs requested
            InTestArguments.builder()
                .wantedApiIds(List.of(apiId1, apiId2, apiId11, "invalid-api-id"))
                .expectedApiIds(List.of(apiId1, apiId2))
                .build()
        );
    }
}
