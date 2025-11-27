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
package io.gravitee.apim.core.log.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.log.crud_service.MessageLogsCrudService;
import io.gravitee.apim.core.log.model.MessageLog;
import io.gravitee.rest.api.model.analytics.SearchMessageLogsFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

@UseCase
@RequiredArgsConstructor
public class SearchApiMessageLogsUseCase {

    private final MessageLogsCrudService messageMetricsCrudService;

    public record Input(String apiId, SearchMessageLogsFilters searchMessageMetricsFilters, Pageable pageable) {}

    public record Output(long total, List<MessageLog> data) {}

    public Output execute(ExecutionContext executionContext, Input input) {
        var response = messageMetricsCrudService.searchApiMessageLogs(
            executionContext,
            input.apiId(),
            input.searchMessageMetricsFilters(),
            input.pageable()
        );
        return new Output(response.total(), response.logs());
    }
}
