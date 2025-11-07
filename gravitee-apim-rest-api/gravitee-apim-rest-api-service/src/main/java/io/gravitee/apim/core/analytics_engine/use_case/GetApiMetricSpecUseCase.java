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
package io.gravitee.apim.core.analytics_engine.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import java.util.List;

@UseCase
public class GetApiMetricSpecUseCase {

    public record Input(String api) {}

    public record Output(List<MetricSpec> specs) {}

    private final AnalyticsDefinitionQueryService definition;

    public GetApiMetricSpecUseCase(AnalyticsDefinitionQueryService definition) {
        this.definition = definition;
    }

    public Output execute(Input input) {
        var api = definition.validateApiName(input.api());
        return new Output(definition.getMetrics(api));
    }
}
