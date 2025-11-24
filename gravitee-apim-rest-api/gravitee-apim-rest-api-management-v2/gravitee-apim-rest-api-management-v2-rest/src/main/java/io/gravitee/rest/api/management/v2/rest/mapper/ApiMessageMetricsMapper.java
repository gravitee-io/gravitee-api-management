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

import io.gravitee.apim.core.metrics.model.MessageMetrics;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageMetrics;
import io.gravitee.rest.api.management.v2.rest.model.ConnectorType;
import io.gravitee.rest.api.management.v2.rest.model.MessageOperation;
import io.gravitee.rest.api.management.v2.rest.resource.api.metrics.param.SearchMessageMetricsParam;
import io.gravitee.rest.api.model.analytics.SearchMessageMetricsFilters;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class })
public interface ApiMessageMetricsMapper {
    ApiMessageMetricsMapper INSTANCE = Mappers.getMapper(ApiMessageMetricsMapper.class);

    List<ApiMessageMetrics> map(List<MessageMetrics> messageMetricsList);

    @Mapping(source = "timestamp", target = "timestamp", qualifiedByName = "mapTimestamp")
    @Mapping(target = "operation", expression = "java(mapOperation(messageMetrics.getOperation()))")
    @Mapping(target = "connectorType", expression = "java(mapConnectorType(messageMetrics.getConnectorType()))")
    ApiMessageMetrics map(MessageMetrics messageMetrics);

    SearchMessageMetricsFilters map(SearchMessageMetricsParam searchMessageMetricsParam);

    default MessageOperation mapOperation(String operation) {
        return Optional.ofNullable(operation).map(MessageOperation::fromValue).orElse(null);
    }

    default ConnectorType mapConnectorType(String connectorType) {
        return Optional.ofNullable(connectorType).map(ConnectorType::fromValue).orElse(null);
    }
}
