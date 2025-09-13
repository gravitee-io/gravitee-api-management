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

import io.gravitee.rest.api.management.v2.rest.model.ApiLog;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogDiagnostic;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogResponse;
import io.gravitee.rest.api.management.v2.rest.resource.api.log.param.SearchLogsParam;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionDiagnosticModel;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogModel;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { ApplicationMapper.class, DateMapper.class, PlanMapper.class })
public interface ApiLogsMapper {
    Logger logger = LoggerFactory.getLogger(ApiLogsMapper.class);
    ApiLogsMapper INSTANCE = Mappers.getMapper(ApiLogsMapper.class);

    @Mapping(source = "timestamp", target = "timestamp", qualifiedByName = "mapTimestamp")
    ApiLog map(ConnectionLogModel connectionLog);

    List<ApiLog> mapToList(List<ConnectionLogModel> logs);

    SearchLogsFilters toSearchLogsFilters(SearchLogsParam searchLogsParam);

    @Mapping(source = "timestamp", target = "timestamp", qualifiedByName = "mapTimestamp")
    ApiLogResponse map(ConnectionLogDetail connectionLogDetail);

    ApiLogDiagnostic map(ConnectionDiagnosticModel connectionDiagnosticModel);
}
