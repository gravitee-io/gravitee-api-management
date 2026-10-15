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
import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;

@UseCase
public class SearchEnvironmentConnectionLogErrorKeysUseCase {

    private final ConnectionLogsCrudService connectionLogsCrudService;
    private final EnvironmentAnalyticsApisDomainService environmentAnalyticsApisDomainService;

    public SearchEnvironmentConnectionLogErrorKeysUseCase(
        ConnectionLogsCrudService connectionLogsCrudService,
        EnvironmentAnalyticsApisDomainService environmentAnalyticsApisDomainService
    ) {
        this.connectionLogsCrudService = connectionLogsCrudService;
        this.environmentAnalyticsApisDomainService = environmentAnalyticsApisDomainService;
    }

    public Output execute(ExecutionContext executionContext, Input input) {
        var apiIds = environmentAnalyticsApisDomainService
            .findReadableApis(executionContext, input.userId())
            .stream()
            .map(Api::getId)
            .toList();
        if (apiIds.isEmpty()) {
            return new Output(List.of());
        }
        return new Output(
            connectionLogsCrudService.searchEnvironmentConnectionLogErrorKeys(executionContext, apiIds, input.from(), input.to())
        );
    }

    public record Input(Long from, Long to, String userId) {}

    public record Output(List<String> errorKeys) {}
}
