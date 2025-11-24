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
package io.gravitee.apim.core.metrics.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.metrics.crud_service.MessageMetricsCrudService;
import io.gravitee.apim.core.metrics.model.MessageMetrics;
import io.gravitee.rest.api.model.analytics.SearchMessageMetricsFilters;
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
public class SearchApiMessageMetricsUseCase {

    private final MessageMetricsCrudService messageMetricsCrudService;

    public record Input(String apiId, SearchMessageMetricsFilters searchMessageMetricsFilters, Pageable pageable) {}

    public record Output(long total, List<MessageMetrics> data) {}

    public Output execute(ExecutionContext executionContext, Input input) {
        var response = messageMetricsCrudService.searchApiMessageMetrics(
            executionContext,
            input.apiId(),
            input.searchMessageMetricsFilters(),
            input.pageable()
        );
        return new Output(response.total(), response.logs());
    }
}
