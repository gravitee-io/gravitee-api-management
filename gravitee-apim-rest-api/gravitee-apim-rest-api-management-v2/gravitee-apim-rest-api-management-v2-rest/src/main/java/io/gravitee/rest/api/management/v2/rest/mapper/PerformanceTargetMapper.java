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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.performance_target.exception.InvalidPerformanceTargetException;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.rest.api.management.v2.rest.model.CreatePerformanceTarget;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetDeviation;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetRule;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetRuleResult;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetSubject;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetsSummary;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePerformanceTarget;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class })
public interface PerformanceTargetMapper {
    PerformanceTargetMapper INSTANCE = Mappers.getMapper(PerformanceTargetMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "window", source = "windowSeconds")
    @Mapping(target = "interval", source = "intervalSeconds")
    PerformanceTarget map(CreatePerformanceTarget source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "window", source = "windowSeconds")
    @Mapping(target = "interval", source = "intervalSeconds")
    PerformanceTarget map(UpdatePerformanceTarget source);

    @Mapping(target = "windowSeconds", source = "window")
    @Mapping(target = "intervalSeconds", source = "interval")
    io.gravitee.rest.api.management.v2.rest.model.PerformanceTarget map(PerformanceTarget source);

    PerformanceTarget.Subject map(PerformanceTargetSubject source);

    PerformanceTargetSubject map(PerformanceTarget.Subject source);

    PerformanceTarget.Rule map(PerformanceTargetRule source);

    PerformanceTargetRule map(PerformanceTarget.Rule source);

    io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetEvaluation map(PerformanceTargetEvaluation source);

    PerformanceTargetRuleResult map(PerformanceTargetEvaluation.RuleResult source);

    PerformanceTargetDeviation map(PerformanceTargetEvaluation.Deviation source);

    PerformanceTargetsSummary map(PerformanceTargetEnvironmentSummary source);

    /**
     * Maps rule by rule so that a vocabulary error names the failing rule, as the validation does.
     */
    default List<PerformanceTarget.Rule> mapRules(List<PerformanceTargetRule> rules) {
        var mapped = new ArrayList<PerformanceTarget.Rule>(rules.size());
        for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
            try {
                mapped.add(map(rules.get(ruleIndex)));
            } catch (InvalidPerformanceTargetException e) {
                throw new InvalidPerformanceTargetException(e.getMessage(), ruleIndex);
            }
        }
        return mapped;
    }

    default MetricSpec.Name toMetricName(String metric) {
        return toName(metric, MetricSpec.Name::valueOf, "metric");
    }

    default MetricSpec.Measure toMeasure(String measure) {
        return toName(measure, MetricSpec.Measure::valueOf, "measure");
    }

    default FilterSpec.Name toFilterName(String filter) {
        return toName(filter, FilterSpec.Name::valueOf, "filter");
    }

    private static <T extends Enum<T>> T toName(String value, Function<String, T> valueOf, String kind) {
        try {
            return valueOf.apply(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidPerformanceTargetException("Unknown %s %s".formatted(kind, value));
        }
    }

    default Duration toDuration(Long seconds) {
        return seconds == null ? null : Duration.ofSeconds(seconds);
    }

    default Long toSeconds(Duration duration) {
        return duration == null ? null : duration.getSeconds();
    }
}
