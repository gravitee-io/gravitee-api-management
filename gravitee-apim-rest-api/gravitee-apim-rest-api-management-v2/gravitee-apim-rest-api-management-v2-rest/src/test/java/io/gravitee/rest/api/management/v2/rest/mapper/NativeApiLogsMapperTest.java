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

import static fixtures.core.model.NativeApiLogFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.log.use_case.NativeApiLogSummaryUseCase;
import io.gravitee.rest.api.management.v2.rest.model.NativeApiLog;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeApiLogsMapperTest {

    private final NativeApiLogsMapper mapper = NativeApiLogsMapper.INSTANCE;

    @Test
    void mapSummary_maps_count_by_connection_status() {
        var output = new NativeApiLogSummaryUseCase.Output(CONNECTION_STATUS_COUNTS);

        var summary = mapper.mapSummary(output);

        assertThat(summary.getCountByConnectionStatus()).containsExactlyInAnyOrderEntriesOf(CONNECTION_STATUS_COUNTS);
    }

    @Test
    void mapSummary_maps_empty_counts() {
        var summary = mapper.mapSummary(new NativeApiLogSummaryUseCase.Output(Map.of()));

        assertThat(summary.getCountByConnectionStatus()).isEmpty();
    }

    @Test
    void map_translates_all_fields_including_timestamp() {
        var detail = mapper.map(buildNativeApiErrorLog(API_ID, REQUEST_ID));

        assertThat(detail.getApiId()).isEqualTo(API_ID);
        assertThat(detail.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(detail.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(detail.getTimestamp()).isEqualTo(TIMESTAMP_UTC);
        assertThat(detail.getApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(detail.getPlanId()).isEqualTo(PLAN_ID);
        assertThat(detail.getClientIdentifier()).isEqualTo(CLIENT_IDENTIFIER);
        assertThat(detail.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(detail.getEntrypointId()).isEqualTo(ENTRYPOINT_ID);
        assertThat(detail.getGateway()).isEqualTo(GATEWAY);
        assertThat(detail.getRemoteAddress()).isEqualTo(REMOTE_ADDRESS);
        assertThat(detail.getLocalAddress()).isEqualTo(LOCAL_ADDRESS);
        assertThat(detail.getHost()).isEqualTo(HOST);
        assertThat(detail.getErrorKey()).isEqualTo(ERROR_KEY);
        assertThat(detail.getErrorMessage()).isEqualTo(MESSAGE);
        assertThat(detail.getConnectionStatus()).isEqualTo(NativeApiLog.ConnectionStatusEnum.CONNECTION_ERROR);
        assertThat(detail.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(detail.getBrokerId()).isEqualTo(BROKER_ID);
        assertThat(detail.getConnectionDurationMs()).isEqualTo(CONNECTION_DURATION_MS);
    }

    @Test
    void mapList_preserves_every_field_for_each_log_in_order() {
        var logs = List.of(buildNativeApiLog(API_ID, "r1"), buildNativeApiLog(API_ID, "r2"));

        var mapped = mapper.mapList(logs);

        assertThat(mapped).hasSize(2).extracting(NativeApiLog::getRequestId).containsExactly("r1", "r2");

        var first = mapped.get(0);
        assertThat(first.getApiId()).isEqualTo(API_ID);
        assertThat(first.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(first.getTimestamp()).isEqualTo(TIMESTAMP_UTC);
        assertThat(first.getApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(first.getPlanId()).isEqualTo(PLAN_ID);
        assertThat(first.getClientIdentifier()).isEqualTo(CLIENT_IDENTIFIER);
        assertThat(first.getEntrypointId()).isEqualTo(ENTRYPOINT_ID);
        assertThat(first.getConnectionStatus()).isEqualTo(NativeApiLog.ConnectionStatusEnum.CONNECTION_ERROR);
        assertThat(first.getConnectionDurationMs()).isEqualTo(CONNECTION_DURATION_MS);
        assertThat(first.getClientId()).isNull();
        assertThat(first.getBrokerId()).isNull();
    }
}
