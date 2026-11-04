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

import io.gravitee.apim.core.observability.model.AdditionalMetricField;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.TrafficScope;
import io.gravitee.repository.log.v4.model.connection.ConnectionDiagnostic;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import io.gravitee.rest.api.model.analytics.Range;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionDiagnosticModel;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import java.util.List;
import java.util.Map;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface ConnectionLogAdapter {
    String MCP_PROXY_METHOD_KEY = "keyword_mcp-proxy_method";
    ConnectionLogAdapter INSTANCE = Mappers.getMapper(ConnectionLogAdapter.class);

    BaseConnectionLog toEntity(Metrics connectionLog);

    List<BaseConnectionLog> toEntitiesList(List<Metrics> connectionLogs);

    ConnectionLogDetail toEntity(io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail connectionLogDetail);

    MetricsQuery.Filter.ResponseTimeRange convert(Range range);
    List<MetricsQuery.Filter.ResponseTimeRange> convert(List<Range> range);

    MetricsQuery.Filter.StatusRange convertStatusRange(SearchLogsFilters.StatusRange statusRange);
    List<MetricsQuery.Filter.StatusRange> convertStatusRanges(List<SearchLogsFilters.StatusRange> statusRanges);

    MetricsQuery.Filter.EntrypointScope convertEntrypointScope(SearchLogsFilters.EntrypointScope entrypointScope);

    TrafficScope convertTrafficScope(SearchLogsFilters.TrafficScope trafficScope);

    default List<MetricsQuery.Filter.AdditionalMetricCondition> convertAdditionalMetrics(
        List<SearchLogsFilters.AdditionalMetricCondition> conditions
    ) {
        return conditions == null ? null : conditions.stream().map(this::toAdditionalMetricCondition).toList();
    }

    /** A name the grammar does not read is refused: dropping it would widen the search under an active filter. */
    default MetricsQuery.Filter.AdditionalMetricCondition toAdditionalMetricCondition(
        SearchLogsFilters.AdditionalMetricCondition condition
    ) {
        var field = AdditionalMetricField.parse(condition.name()).orElseThrow(() ->
            new IllegalArgumentException("Not an additional metric field name: " + condition.name())
        );
        return new MetricsQuery.Filter.AdditionalMetricCondition(
            AnalyticsMeasuresAdapter.INSTANCE.toAdditionalMetricField(field),
            toOperator(condition.operator()),
            condition.values()
        );
    }

    Filter.Operator toOperator(SearchLogsFilters.AdditionalMetricCondition.Operator operator);

    ConnectionDiagnosticModel convert(ConnectionDiagnostic connectionDiagnostic);

    @AfterMapping
    default void extractMcpMethod(@MappingTarget BaseConnectionLog target) {
        Map<String, Object> metrics = target.getAdditionalMetrics();
        if (metrics == null) {
            return;
        }

        Object mcpMethodObj = metrics.get(MCP_PROXY_METHOD_KEY);
        if (mcpMethodObj instanceof String mcpMethod) {
            target.setMcpMethod(mcpMethod);
        }
    }
}
