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
package io.gravitee.apim.rest.api.mcp.infra;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.logs_engine.model.SearchLogsRequest;
import io.gravitee.apim.core.logs_engine.model.SearchLogsResponse;
import io.gravitee.apim.core.logs_engine.use_case.SearchEnvironmentLogsUseCase;
import io.gravitee.apim.rest.api.mcp.core.port.LogSearch;

/**
 * The logs engine through its environment use case, which restricts every search to the APIs the caller can see.
 */
public class LogsEngineLogSearch implements LogSearch {

    private final SearchEnvironmentLogsUseCase searchLogs;

    public LogsEngineLogSearch(SearchEnvironmentLogsUseCase searchLogs) {
        this.searchLogs = searchLogs;
    }

    @Override
    public SearchLogsResponse search(AuditInfo caller, SearchLogsRequest request) {
        return ApimContext.run(caller, () -> searchLogs.execute(new SearchEnvironmentLogsUseCase.Input(caller, request))).response();
    }
}
