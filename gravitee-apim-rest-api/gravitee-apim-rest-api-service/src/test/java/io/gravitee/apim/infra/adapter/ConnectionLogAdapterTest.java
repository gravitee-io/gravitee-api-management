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
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters.AdditionalMetricCondition;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConnectionLogAdapterTest {

    @Test
    void should_convert_connection_log_to_connection_log_entity() {
        final Metrics toConvert = Metrics.builder()
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
    void should_extract_mcp_method_from_additional_metrics() {
        final Metrics toConvert = Metrics.builder()
            .requestId("request-id")
            .additionalMetrics(Map.of(ConnectionLogAdapter.MCP_PROXY_METHOD_KEY, "tools/call"))
            .build();

        final BaseConnectionLog result = ConnectionLogAdapter.INSTANCE.toEntity(toConvert);

        assertThat(result.getMcpMethod()).isEqualTo("tools/call");
    }

    @Test
    void should_leave_mcp_method_null_when_additional_metrics_is_null() {
        final Metrics toConvert = Metrics.builder().requestId("request-id").build();

        final BaseConnectionLog result = ConnectionLogAdapter.INSTANCE.toEntity(toConvert);

        assertThat(result.getMcpMethod()).isNull();
    }

    @Test
    void should_convert_connection_logs_to_connection_log_entities() {
        final Metrics toConvert = Metrics.builder()
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
                    .requestContentLength(0L)
                    .responseContentLength(0L)
                    .build()
            );
    }

    @ParameterizedTest
    @EnumSource(AdditionalMetricCondition.Operator.class)
    void should_map_every_additional_metric_operator_to_the_repository_one_of_its_name(AdditionalMetricCondition.Operator operator) {
        assertThat(ConnectionLogAdapter.INSTANCE.toOperator(operator).name()).isEqualTo(operator.name());
    }

    @Test
    void should_give_every_repository_operator_an_additional_metric_counterpart() {
        assertThat(Arrays.stream(AdditionalMetricCondition.Operator.values()).map(Enum::name)).containsExactlyInAnyOrderElementsOf(
            Arrays.stream(Filter.Operator.values()).map(Enum::name).toList()
        );
    }
}
