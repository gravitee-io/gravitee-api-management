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
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Name.APPLICATION;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class PermissionBasedFilterTest extends AbstractTest {

    static final String allowedApi1 = "api-id-1";
    static final String allowedApi2 = "api-id-2";
    static final String forbiddenApi1 = "api-id-11";
    static final String forbiddenApi2 = "api-id-12";

    static Set<String> allowedApiIds = Set.of(allowedApi1, allowedApi2);

    @Test
    void usrers_without_API_access_should_not_see_any_data() {
        var filter = new Filter(API, EQ, forbiddenApi2);

        var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(List.of(filter), Collections.emptySet());

        assertThat(updatedFilters).size().isEqualTo(2);
        assertThat(updatedFilters).contains(filter);
        assertThat(updatedFilters.getLast()).satisfies(f -> {
            assertThat(f.name()).isEqualTo(API);
            assertThat(f.operator()).isEqualTo(IN);
            assertThat(f.value()).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrderElementsOf(List.of());
        });
    }

    @Test
    void should_update_empty_API_filter_to_include_all_allowed_ids() {
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
        var equalityFilter1 = new Filter(API, EQ, allowedApi1);
        var listFilter = new Filter(API, IN, List.of(allowedApi2, forbiddenApi1));
        var equalityFilter2 = new Filter(API, EQ, forbiddenApi2);
        var lteFilter = new Filter(API, LTE, "some-value");
        var gteFilter = new Filter(API, GTE, "other-value");
        var appplicationFilter = new Filter(APPLICATION, EQ, "some-app");

        var filters = List.of(equalityFilter1, listFilter, equalityFilter2, lteFilter, gteFilter, appplicationFilter);

        var updatedFilters = apiAnalyticsQueryFilterDecorator.applyPermissionBasedFilters(filters, allowedApiIds);

        assertThat(updatedFilters).size().isEqualTo(filters.size() + 1);
        assertThat(updatedFilters).containsAll(filters);
        assertThat(updatedFilters.getLast()).satisfies(f -> {
            assertThat(f.name()).isEqualTo(API);
            assertThat(f.operator()).isEqualTo(IN);
            assertThat(f.value()).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrderElementsOf(allowedApiIds);
        });
    }
}
