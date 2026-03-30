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
package io.gravitee.apim.core.analytics.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsDateHistoAggregate;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsDateHistoQuery;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsField;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchApiAnalyticsDateHistoUseCase {

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        final Api api = ApiAnalyticsV4ApiValidation.validateAndGetApi(apiCrudService, input.apiId(), input.environmentId());
        if (!ApiAnalyticsV4ApiValidation.isAnalyticsEnabled(api)) {
            return new Output();
        }
        return analyticsQueryService
            .searchApiAnalyticsDateHisto(
                executionContext,
                new ApiAnalyticsDateHistoQuery(
                    input.apiId(),
                    input.from().orElse(null),
                    input.to().orElse(null),
                    input.field().orElse(null),
                    input.interval().orElse(null)
                )
            )
            .map(Output::new)
            .orElse(new Output());
    }

    /**
     * @param field field to split in the histogram (optional)
     * @param interval bucket width (optional)
     */
    public record Input(
        String apiId,
        String environmentId,
        Optional<Instant> from,
        Optional<Instant> to,
        Optional<ApiAnalyticsField> field,
        Optional<Duration> interval
    ) {}

    /**
     * @param aggregate time series buckets per field, or empty when disabled / no data
     */
    public record Output(Optional<ApiAnalyticsDateHistoAggregate> aggregate) {
        public Output(ApiAnalyticsDateHistoAggregate aggregate) {
            this(Optional.of(aggregate));
        }

        public Output() {
            this(Optional.empty());
        }
    }
}
