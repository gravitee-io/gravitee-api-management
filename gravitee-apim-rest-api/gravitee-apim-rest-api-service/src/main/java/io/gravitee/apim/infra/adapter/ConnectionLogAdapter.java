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

import io.gravitee.repository.log.v4.model.connection.ConnectionDiagnostic;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import io.gravitee.rest.api.model.analytics.Range;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionDiagnosticModel;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface ConnectionLogAdapter {
    ConnectionLogAdapter INSTANCE = Mappers.getMapper(ConnectionLogAdapter.class);

    BaseConnectionLog toEntity(Metrics connectionLog);

    List<BaseConnectionLog> toEntitiesList(List<Metrics> connectionLogs);

    ConnectionLogDetail toEntity(io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail connectionLogDetail);

    MetricsQuery.Filter.ResponseTimeRange convert(Range range);
    List<MetricsQuery.Filter.ResponseTimeRange> convert(List<Range> range);

    ConnectionDiagnosticModel convert(ConnectionDiagnostic connectionDiagnostic);
}
