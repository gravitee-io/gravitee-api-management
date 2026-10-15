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
import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.rest.api.model.v4.analytics.RequestResponseTime;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.CustomLog;

@CustomLog
@UseCase
@AllArgsConstructor
public class SearchEnvironmentRequestResponseTimeUseCase {

    private final EnvironmentAnalyticsApisDomainService environmentAnalyticsApisDomainService;
    private final AnalyticsQueryService analyticsQueryService;

    public Output execute(Input input) {
        var envId = input.executionContext().getEnvironmentId();
        var v4Apis = environmentAnalyticsApisDomainService.findReadableApiIdsByDefinitionVersion(input.executionContext(), input.userId());
        var apisId = v4Apis.values().stream().flatMap(Collection::stream).toList();

        log.debug("Searching Request Response Time, found: {} v4 APIs for env: {}", v4Apis.size(), envId);
        if (apisId.isEmpty()) {
            return new Output(noRequest());
        }

        var requestResponseTime = analyticsQueryService.searchRequestResponseTime(
            input.executionContext(),
            input.parameters().withApiIds(apisId).withDefinitionVersions(v4Apis.keySet())
        );

        return new Output(requestResponseTime);
    }

    private static RequestResponseTime noRequest() {
        return RequestResponseTime.builder()
            .requestsPerSecond(0d)
            .requestsTotal(0L)
            .responseMinTime(0d)
            .responseMaxTime(0d)
            .responseAvgTime(0d)
            .build();
    }

    @Builder
    public record Input(ExecutionContext executionContext, AnalyticsQueryParameters parameters, String userId) {}

    public record Output(Optional<RequestResponseTime> requestResponseTime) {
        Output(RequestResponseTime requestResponseTime) {
            this(Optional.of(requestResponseTime));
        }
    }
}
