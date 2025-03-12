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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.log.model.AggregatedMessageLog;
import io.gravitee.apim.core.log.model.ConnectionLog;
import io.gravitee.rest.api.model.analytics.Range;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.log.ApplicationRequest;
import io.gravitee.rest.api.model.log.ApplicationRequestItem;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.portal.rest.model.Log;
import io.gravitee.rest.api.portal.rest.model.MessageLogContent;
import io.gravitee.rest.api.portal.rest.model.Request;
import io.gravitee.rest.api.portal.rest.model.Response;
import io.gravitee.rest.api.portal.rest.resource.param.ResponseTimeRange;
import io.gravitee.rest.api.portal.rest.resource.param.SearchApplicationLogsParam;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class })
public interface LogMapper {
    LogMapper INSTANCE = Mappers.getMapper(LogMapper.class);

    Log convert(ApplicationRequest applicationRequest);
    Log convert(ApplicationRequestItem applicationRequestItem);

    @Mapping(target = "plan", source = "plan.id")
    @Mapping(target = "api", source = "api.id")
    @Mapping(target = "id", source = "requestId")
    @Mapping(target = "path", source = "uri")
    @Mapping(target = "responseTime", source = "gatewayResponseTime")
    Log convert(ConnectionLog connectionLog);

    List<Log> convertConnectionLogs(List<ConnectionLog> connectionLogs);

    Request convert(ConnectionLogDetail.Request request);

    Response convert(ConnectionLogDetail.Response response);

    @Mapping(target = "uri", source = "path")
    SearchLogsFilters convert(SearchApplicationLogsParam searchLogsParam);

    Range convert(ResponseTimeRange responseTimeRange);
    List<Range> convertResponseTimeRanges(List<ResponseTimeRange> responseTimeRanges);

    @Mapping(target = "timestamp", qualifiedByName = "mapTimestamp")
    io.gravitee.rest.api.portal.rest.model.AggregatedMessageLog convert(AggregatedMessageLog messageLog);

    List<io.gravitee.rest.api.portal.rest.model.AggregatedMessageLog> convert(List<AggregatedMessageLog> data);

    MessageLogContent convert(AggregatedMessageLog.Message messageLog);

    default SearchLogsFilters convert(String applicationId, SearchApplicationLogsParam searchLogsParam) {
        return convert(searchLogsParam).toBuilder().applicationIds(Set.of(applicationId)).build();
    }

    default Log convert(
        ConnectionLog connectionLog,
        Optional<ConnectionLogDetail> connectionLogDetail,
        Map<String, Map<String, Object>> metadata
    ) {
        var log = convert(connectionLog);
        log.setMetadata(metadata);

        if (connectionLogDetail.isPresent()) {
            var connectionLogDetailGet = connectionLogDetail.get();
            log.setRequest(convert(connectionLogDetailGet.getEntrypointRequest()));
            log.setResponse(convert(connectionLogDetailGet.getEntrypointResponse()));
        }
        return log;
    }

    default Map<String, Map<String, Object>> map(Map<String, Map<String, String>> value) {
        if (value == null) {
            return new HashMap<>();
        }
        return value.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> mapToObjectMap(e.getValue())));
    }

    default Map<String, Object> mapToObjectMap(Map<String, String> value) {
        if (value == null) {
            return new HashMap<>();
        }
        return value.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
