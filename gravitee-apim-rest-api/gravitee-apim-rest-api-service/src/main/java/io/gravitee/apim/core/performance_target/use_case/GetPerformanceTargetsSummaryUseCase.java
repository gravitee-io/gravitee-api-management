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
package io.gravitee.apim.core.performance_target.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.apim.core.performance_target.query_service.PerformanceTargetEvaluationQueryService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GetPerformanceTargetsSummaryUseCase {

    private final PerformanceTargetEvaluationQueryService performanceTargetEvaluationQueryService;

    public Output execute(Input input) {
        return new Output(performanceTargetEvaluationQueryService.getEnvironmentSummary(input.environmentId()));
    }

    public record Input(String environmentId) {}

    public record Output(PerformanceTargetEnvironmentSummary summary) {}
}
