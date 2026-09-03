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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.performance_target.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PerformanceTargetEvaluationAdapter {
    PerformanceTargetEvaluationAdapter INSTANCE = Mappers.getMapper(PerformanceTargetEvaluationAdapter.class);

    PerformanceTargetEvaluation toEntity(io.gravitee.repository.management.model.PerformanceTargetEvaluation source);
    io.gravitee.repository.management.model.PerformanceTargetEvaluation toRepository(PerformanceTargetEvaluation source);

    PerformanceTargetEnvironmentSummary toEntity(io.gravitee.repository.management.model.PerformanceTargetEnvironmentSummary source);

    @Mapping(target = "deviation", source = "source")
    PerformanceTargetEvaluation.RuleResult toEntity(io.gravitee.repository.management.model.PerformanceTargetEvaluation.RuleResult source);

    @Mapping(target = "deviation", source = "deviation.absolute")
    @Mapping(target = "deviationRatio", source = "deviation.ratio")
    io.gravitee.repository.management.model.PerformanceTargetEvaluation.RuleResult toRepository(
        PerformanceTargetEvaluation.RuleResult source
    );

    @Nullable
    default PerformanceTargetEvaluation.Deviation toDeviation(
        io.gravitee.repository.management.model.PerformanceTargetEvaluation.RuleResult source
    ) {
        if (source.deviation() == null || source.deviationRatio() == null) {
            return null;
        }
        return new PerformanceTargetEvaluation.Deviation(source.deviation(), source.deviationRatio());
    }

    /**
     * This method is explicit to show at compile time the link between
     * <ul>
     *     <li>{@link io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status}</li>
     *     <li>{@link io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status}</li>
     * </ul>
     */
    @Nullable
    default PerformanceTargetEvaluation.Status map(io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status source) {
        return switch (source) {
            case PASS -> PerformanceTargetEvaluation.Status.PASS;
            case BREACH -> PerformanceTargetEvaluation.Status.BREACH;
            case NOT_EVALUABLE -> PerformanceTargetEvaluation.Status.NOT_EVALUABLE;
            case null -> null;
        };
    }

    /**
     * This method is explicit to show at compile time the link between
     * <ul>
     *     <li>{@link io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status}</li>
     *     <li>{@link io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status}</li>
     * </ul>
     */
    @Nullable
    default io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status map(PerformanceTargetEvaluation.Status source) {
        return switch (source) {
            case PASS -> io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status.PASS;
            case BREACH -> io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status.BREACH;
            case NOT_EVALUABLE -> io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status.NOT_EVALUABLE;
            case null -> null;
        };
    }

    @Nullable
    default Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    @Nullable
    default Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }
}
