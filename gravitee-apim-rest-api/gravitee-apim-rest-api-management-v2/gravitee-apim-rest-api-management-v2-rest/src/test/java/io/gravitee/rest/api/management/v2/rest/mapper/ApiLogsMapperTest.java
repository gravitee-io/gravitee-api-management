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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.management.v2.rest.resource.api.log.param.SearchLogsParam;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiLogsMapperTest {

    private final ApiLogsMapper mapper = Mappers.getMapper(ApiLogsMapper.class);

    @Nested
    class MapSearchLogsParam {

        @Test
        void should_map_native_kafka_client_ids() {
            SearchLogsParam param = new SearchLogsParam();
            param.setNativeKafkaClientIds(Set.of("consumer-A", "consumer-B"));

            SearchLogsFilters filters = mapper.toSearchLogsFilters(param);

            assertThat(filters.nativeKafkaClientIds()).containsExactlyInAnyOrder("consumer-A", "consumer-B");
        }

        @Test
        void should_map_native_kafka_consumer_group_ids() {
            SearchLogsParam param = new SearchLogsParam();
            param.setNativeKafkaConsumerGroupIds(Set.of("group-alpha"));

            SearchLogsFilters filters = mapper.toSearchLogsFilters(param);

            assertThat(filters.nativeKafkaConsumerGroupIds()).containsExactly("group-alpha");
        }

        @Test
        void should_map_all_fields() {
            SearchLogsParam param = new SearchLogsParam();
            param.setFrom(1000L);
            param.setTo(2000L);
            param.setApplicationIds(Set.of("app-1"));
            param.setPlanIds(Set.of("plan-1"));
            param.setMethods(Set.of(HttpMethod.GET));
            param.setStatuses(Set.of(200));
            param.setEntrypointIds(Set.of("native-kafka"));
            param.setErrorKeys(Set.of("CONNECTION_ERROR"));
            param.setNativeKafkaClientIds(Set.of("consumer-A"));
            param.setNativeKafkaConsumerGroupIds(Set.of("group-alpha"));

            SearchLogsFilters filters = mapper.toSearchLogsFilters(param);

            assertThat(filters.from()).isEqualTo(1000L);
            assertThat(filters.to()).isEqualTo(2000L);
            assertThat(filters.applicationIds()).containsExactly("app-1");
            assertThat(filters.planIds()).containsExactly("plan-1");
            assertThat(filters.methods()).containsExactly(HttpMethod.GET);
            assertThat(filters.statuses()).containsExactly(200);
            assertThat(filters.entrypointIds()).containsExactly("native-kafka");
            assertThat(filters.errorKeys()).containsExactly("CONNECTION_ERROR");
            assertThat(filters.nativeKafkaClientIds()).containsExactly("consumer-A");
            assertThat(filters.nativeKafkaConsumerGroupIds()).containsExactly("group-alpha");
        }

        @Test
        void should_not_set_kafka_filters_when_params_are_absent() {
            SearchLogsParam param = new SearchLogsParam();
            param.setFrom(1000L);
            param.setTo(2000L);

            SearchLogsFilters filters = mapper.toSearchLogsFilters(param);

            assertThat(filters.from()).isEqualTo(1000L);
            assertThat(filters.to()).isEqualTo(2000L);
            assertThat(filters.nativeKafkaClientIds()).isNull();
            assertThat(filters.nativeKafkaConsumerGroupIds()).isNull();
        }
    }
}
