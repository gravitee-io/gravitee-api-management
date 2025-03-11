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
package io.gravitee.repository.noop.log.v4;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.ConnectionLog;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetailQuery;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery;
import io.gravitee.repository.log.v4.model.message.AggregatedMessageLog;
import io.gravitee.repository.log.v4.model.message.MessageLogQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoOpLogRepository implements LogRepository {

    @Override
    public LogResponse<ConnectionLog> searchConnectionLogs(
        QueryContext queryContext,
        ConnectionLogQuery query,
        List<DefinitionVersion> definitionVersions
    ) {
        return new LogResponse(0L, new ArrayList<>());
    }

    @Override
    public Optional<ConnectionLogDetail> searchConnectionLogDetail(QueryContext queryContext, ConnectionLogDetailQuery query)
        throws AnalyticsException {
        return Optional.empty();
    }

    @Override
    public LogResponse<ConnectionLogDetail> searchConnectionLogDetails(QueryContext queryContext, ConnectionLogDetailQuery query)
        throws AnalyticsException {
        return new LogResponse(0L, new ArrayList<>());
    }

    @Override
    public LogResponse<AggregatedMessageLog> searchAggregatedMessageLog(QueryContext queryContext, MessageLogQuery query) {
        return new LogResponse(0L, new ArrayList<>());
    }
}
