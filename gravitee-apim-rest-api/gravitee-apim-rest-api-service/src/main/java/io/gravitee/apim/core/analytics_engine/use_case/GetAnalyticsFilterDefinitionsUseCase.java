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
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.observability.model.FilterSignal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UseCase
public class GetAnalyticsFilterDefinitionsUseCase {

    private static final Set<String> LOGS_ENGINE_FILTER_NAMES = Arrays.stream(FilterName.values())
        .map(Enum::name)
        .collect(Collectors.toUnmodifiableSet());

    private final AnalyticsDefinitionQueryService definition;

    public GetAnalyticsFilterDefinitionsUseCase(AnalyticsDefinitionQueryService definition) {
        this.definition = definition;
    }

    public record Output(List<FilterSpec> specs) {}

    public Output execute() {
        return new Output(
            definition
                .getAllFilters()
                .stream()
                .map(spec -> spec.withSignals(signalsFor(spec.name())))
                .toList()
        );
    }

    /**
     * The filter catalog serves two observability surfaces (analytics dashboards and logs); each
     * filter advertises the ones that actually support it so clients only offer applicable filters.
     * Both sides derive from the engines' own definitions — {@link AnalyticsQueryValidator#supportsAnalytics}
     * for analytics, the logs engine {@link FilterName}s for logs (matched by name, see
     * {@link #supportsLogs} for the exceptions).
     */
    private static List<FilterSignal> signalsFor(FilterSpec.Name name) {
        var signals = new ArrayList<FilterSignal>(2);
        if (AnalyticsQueryValidator.supportsAnalytics(name)) {
            signals.add(FilterSignal.ANALYTICS);
        }
        if (supportsLogs(name)) {
            signals.add(FilterSignal.LOGS);
        }
        return List.copyOf(signals);
    }

    private static boolean supportsLogs(FilterSpec.Name name) {
        // The logs engine names the path filter URI. Its MCP_METHOD and RESPONSE_TIME have no
        // same-named catalog filter and stay unmapped until the logs page can actually apply them.
        if (name == FilterSpec.Name.HTTP_PATH) {
            return true;
        }
        return LOGS_ENGINE_FILTER_NAMES.contains(name.name());
    }
}
