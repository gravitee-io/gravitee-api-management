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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.log.v4.model.connection.ConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConnectionLogAdapterTest {

    @Test
    public void should_convert_connection_log_to_connection_log_entity() {
        final ConnectionLog toConvert = ConnectionLog.builder()
            .apiId("api-id")
            .requestId("request-id")
            .timestamp("timestamp")
            .applicationId("app-id")
            .clientIdentifier("client-id")
            .method(HttpMethod.GET)
            .planId("plan-id")
            .requestEnded(true)
            .transactionId("transaction-id")
            .status(200)
            .build();

        final BaseConnectionLog result = ConnectionLogAdapter.INSTANCE.toEntity(toConvert);

        assertThat(result.getApiId()).isEqualTo(toConvert.getApiId());
        assertThat(result.getRequestId()).isEqualTo(toConvert.getRequestId());
        assertThat(result.getTimestamp()).isEqualTo(toConvert.getTimestamp());
        assertThat(result.getApplicationId()).isEqualTo(toConvert.getApplicationId());
        assertThat(result.getClientIdentifier()).isEqualTo(toConvert.getClientIdentifier());
        assertThat(result.getMethod()).isEqualTo(toConvert.getMethod());
        assertThat(result.getPlanId()).isEqualTo(toConvert.getPlanId());
        assertThat(result.isRequestEnded()).isEqualTo(toConvert.isRequestEnded());
        assertThat(result.getTransactionId()).isEqualTo(toConvert.getTransactionId());
        assertThat(result.getStatus()).isEqualTo(toConvert.getStatus());
    }

    @Test
    public void should_convert_connection_logs_to_connection_log_entities() {
        final ConnectionLog toConvert = ConnectionLog.builder()
            .apiId("api-id")
            .requestId("request-id")
            .timestamp("timestamp")
            .applicationId("app-id")
            .clientIdentifier("client-id")
            .method(HttpMethod.GET)
            .planId("plan-id")
            .requestEnded(true)
            .transactionId("transaction-id")
            .status(200)
            .build();

        final List<BaseConnectionLog> result = ConnectionLogAdapter.INSTANCE.toEntitiesList(List.of(toConvert));

        assertThat(result)
            .hasSize(1)
            .containsExactly(
                BaseConnectionLog.builder()
                    .apiId("api-id")
                    .requestId("request-id")
                    .timestamp("timestamp")
                    .applicationId("app-id")
                    .clientIdentifier("client-id")
                    .method(HttpMethod.GET)
                    .planId("plan-id")
                    .requestEnded(true)
                    .transactionId("transaction-id")
                    .status(200)
                    .build()
            );
    }
}
