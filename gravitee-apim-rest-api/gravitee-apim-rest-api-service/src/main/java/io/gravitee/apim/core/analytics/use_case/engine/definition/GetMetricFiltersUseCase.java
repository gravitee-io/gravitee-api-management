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
package io.gravitee.apim.core.analytics.use_case.engine.definition;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.domain_service.engine.definition.AnalyticsDefinition;
import io.gravitee.apim.core.analytics.model.engine.definition.FilterSpec;
import java.util.List;

@UseCase
public class GetMetricFiltersUseCase {

    private final AnalyticsDefinition definition;

    public GetMetricFiltersUseCase(AnalyticsDefinition definition) {
        this.definition = definition;
    }

    public record Input(String metric) {}

    public record Output(List<FilterSpec> specs) {}

    public Output execute(Input input) {
        var metric = definition.validateMetricName(input.metric());
        return new Output(definition.getFilters(metric));
    }
}
