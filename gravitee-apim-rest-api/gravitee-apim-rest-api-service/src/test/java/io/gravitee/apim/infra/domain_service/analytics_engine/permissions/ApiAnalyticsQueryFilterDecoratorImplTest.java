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
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.EQ;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.IN;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import java.util.*;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiAnalyticsQueryFilterDecoratorImplTest {

    @Nested
    class GetIdsFromFiltersTest {

        @Test
        void should_get_nothing_when_list_is_empty() {
            List<Filter> emptyFilters = List.of();

            var ids = ApiAnalyticsQueryFilterDecoratorImpl.getIdsFromFilters(emptyFilters, API);

            assertThat(ids).isEmpty();
        }

        @Test
        void should_get_ids_from_EQ_filter() {
            var apiId = UUID.randomUUID().toString();

            List<Filter> filters = List.of(new Filter(API, EQ, apiId), new Filter(APPLICATION, EQ, UUID.randomUUID().toString()));

            var ids = ApiAnalyticsQueryFilterDecoratorImpl.getIdsFromFilters(filters, API);

            assertThat(ids).containsExactly(apiId);
        }

        @Test
        void should_get_ids_from_IN_filter() {
            var apiId1 = UUID.randomUUID().toString();
            var apiId2 = UUID.randomUUID().toString();

            List<Filter> filters = List.of(
                new Filter(API, IN, List.of(apiId1, apiId2)),
                new Filter(APPLICATION, EQ, UUID.randomUUID().toString())
            );

            var ids = ApiAnalyticsQueryFilterDecoratorImpl.getIdsFromFilters(filters, API);

            assertThat(ids).containsExactlyInAnyOrder(apiId1, apiId2);
        }

        @Test
        void should_get_ids_from_several_filters() {
            var apiId1 = UUID.randomUUID().toString();
            var apiId2 = UUID.randomUUID().toString();
            var apiId3 = UUID.randomUUID().toString();

            List<Filter> filters = List.of(
                new Filter(API, IN, List.of(apiId1, apiId2)),
                new Filter(API, EQ, apiId1),
                new Filter(API, EQ, apiId3),
                new Filter(APPLICATION, EQ, UUID.randomUUID().toString())
            );

            var ids = ApiAnalyticsQueryFilterDecoratorImpl.getIdsFromFilters(filters, API);

            assertThat(ids).containsExactlyInAnyOrder(apiId1, apiId2, apiId3);
        }
    }

    @Nested
    class UpdateContextFiltersTest {

        static final String allowedApi1 = "api-id-01";
        static final String allowedApi2 = "api-id-02";
        static final String forbiddenApi1 = "api-id-11";
        static final String forbiddenApi2 = "api-id-12";

        @Test
        void should_update_filter_with_empty_filter_when_user_has_no_api_access() {
            var filters = List.of(new Filter(API, EQ, forbiddenApi1));
            var allowedApis = new HashMap<String, String>();

            var updatedFilters = ApiAnalyticsQueryFilterDecoratorImpl.updateContextFilters(filters, allowedApis);

            var expectedFilters = new ArrayList<>(filters);
            expectedFilters.add(new Filter(API, IN, allowedApis.keySet().stream().toList()));

            assertThat(updatedFilters).containsExactlyInAnyOrderElementsOf(expectedFilters);
        }

        @Test
        void should_update_filter_with_apis_user_has_access_to() {
            var filters = List.of(new Filter(API, EQ, forbiddenApi1), new Filter(API, EQ, forbiddenApi2));

            var allowedApis = Map.of(allowedApi1, "api1");

            var updatedFilters = ApiAnalyticsQueryFilterDecoratorImpl.updateContextFilters(filters, allowedApis);

            var expectedFilters = new ArrayList<>(filters);
            expectedFilters.add(new Filter(API, IN, allowedApis.keySet().stream().toList()));

            assertThat(updatedFilters).containsExactlyInAnyOrderElementsOf(expectedFilters);
        }

        @Test
        void should_update_empty_filter_with_apis_user_has_access_to() {
            List<Filter> filters = List.of();
            var allowedApis = Map.of(allowedApi1, "api1", allowedApi2, "api2");

            var updatedFilters = ApiAnalyticsQueryFilterDecoratorImpl.updateContextFilters(filters, allowedApis);

            var expectedFilters = List.of(new Filter(API, IN, allowedApis.keySet().stream().toList()));

            assertThat(updatedFilters).containsExactlyInAnyOrderElementsOf(expectedFilters);
        }
    }
}
