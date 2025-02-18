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
package io.gravitee.apim.core.log.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.log.model.ConnectionLog;
import io.gravitee.apim.core.plan.model.Plan;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConnectionLogMetadataDomainServiceTest {

    ConnectionLogMetadataDomainService cut = new ConnectionLogMetadataDomainService();

    @Nested
    class GetMetadataForApplicationConnectionLog {

        @Test
        public void should_return_empty_metadata_map_if_empty_log_list() {
            List<ConnectionLog> data = List.of();
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).isEmpty();
        }

        @Test
        public void should_include_published_api_data_when_present() {
            var apiId = "api-id";
            var apiName = "api-name";
            var apiVersion = "1";
            List<ConnectionLog> data = List.of(
                ConnectionLog
                    .builder()
                    .api(
                        Api.builder().id(apiId).name(apiName).version(apiVersion).apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build()
                    )
                    .apiId(apiId)
                    .build()
            );
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(apiId).isEqualTo(Map.of("name", apiName, "version", apiVersion));
        }

        @Test
        public void should_include_archived_api_data_when_present() {
            var apiId = "api-id";
            var apiName = "api-name";
            var apiVersion = "1";
            List<ConnectionLog> data = List.of(
                ConnectionLog
                    .builder()
                    .api(
                        Api.builder().id(apiId).name(apiName).version(apiVersion).apiLifecycleState(Api.ApiLifecycleState.ARCHIVED).build()
                    )
                    .apiId(apiId)
                    .build()
            );
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result)
                .hasSize(1)
                .extractingByKey(apiId)
                .isEqualTo(Map.of("name", apiName, "version", apiVersion, "deleted", "true"));
        }

        @Test
        public void should_include_api_data_once_if_multiple_logs_use_same_api() {
            var apiId = "api-id";
            var apiName = "api-name";
            var apiVersion = "1";
            var api = Api.builder().id(apiId).name(apiName).version(apiVersion).apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build();

            List<ConnectionLog> data = List.of(
                ConnectionLog.builder().requestId("req-1").api(api).apiId(apiId).build(),
                ConnectionLog.builder().requestId("req-1").api(api).apiId(apiId).build()
            );
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(apiId).isEqualTo(Map.of("name", apiName, "version", apiVersion));
        }

        @Test
        public void should_include_api_data_if_unknown_service_as_api_id() {
            var apiId = "1";
            List<ConnectionLog> data = List.of(ConnectionLog.builder().apiId(apiId).build());
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(apiId).isEqualTo(Map.of("name", "Unknown API (not found)", "unknown", "true"));
        }

        @Test
        public void should_include_api_data_if_unknown_service_mapped_as_api_id() {
            var apiId = "?";
            List<ConnectionLog> data = List.of(ConnectionLog.builder().apiId(apiId).build());
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(apiId).isEqualTo(Map.of("name", "Unknown API (not found)", "unknown", "true"));
        }

        @Test
        public void should_include_api_data_if_api_ull() {
            var apiId = "api-not-found";
            List<ConnectionLog> data = List.of(ConnectionLog.builder().apiId(apiId).build());
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(apiId).isEqualTo(Map.of("name", "Deleted API", "deleted", "true"));
        }

        @Test
        public void should_include_api_data_if_unknown_service_api() {
            var apiId = "api-id";
            List<ConnectionLog> data = List.of(ConnectionLog.builder().apiId(apiId).api(Api.builder().id("1").build()).build());
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(apiId).isEqualTo(Map.of("name", "Unknown API (not found)", "unknown", "true"));
        }

        @Test
        public void should_include_api_data_if_unknown_service_mapped_api() {
            var apiId = "api-id";
            List<ConnectionLog> data = List.of(ConnectionLog.builder().apiId(apiId).api(Api.builder().id("?").build()).build());
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(apiId).isEqualTo(Map.of("name", "Unknown API (not found)", "unknown", "true"));
        }

        @Test
        public void should_include_plan_data_when_present() {
            var planId = "plan-id";
            var planName = "plan-name";
            List<ConnectionLog> data = List.of(
                ConnectionLog.builder().plan(Plan.builder().id(planId).name(planName).build()).planId(planId).build()
            );
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(planId).isEqualTo(Map.of("name", planName));
        }

        @Test
        public void should_include_plan_data_once_if_multiple_logs_use_same_plan() {
            var planId = "plan-id";
            var planName = "plan-name";
            var plan = Plan.builder().id(planId).name(planName).build();

            List<ConnectionLog> data = List.of(
                ConnectionLog.builder().requestId("req-1").plan(plan).planId(planId).build(),
                ConnectionLog.builder().requestId("req-1").plan(plan).planId(planId).build()
            );
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(planId).isEqualTo(Map.of("name", planName));
        }

        @Test
        public void should_include_plan_data_if_unknown_service_as_plan_id() {
            var planId = "1";
            List<ConnectionLog> data = List.of(ConnectionLog.builder().planId(planId).build());
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(planId).isEqualTo(Map.of("name", "Unknown plan", "unknown", "true"));
        }

        @Test
        public void should_include_plan_data_if_unknown_service_mapped_as_plan_id() {
            var planId = "?";
            List<ConnectionLog> data = List.of(ConnectionLog.builder().planId(planId).build());
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(planId).isEqualTo(Map.of("name", "Unknown plan", "unknown", "true"));
        }

        @Test
        public void should_include_plan_data_if_plan_is_null() {
            var planId = "plan-not-found";
            List<ConnectionLog> data = List.of(ConnectionLog.builder().planId(planId).build());
            var result = cut.getMetadataForApplicationConnectionLog(data);
            assertThat(result).hasSize(1).extractingByKey(planId).isEqualTo(Map.of("name", "Deleted plan", "deleted", "true"));
        }
    }
}
