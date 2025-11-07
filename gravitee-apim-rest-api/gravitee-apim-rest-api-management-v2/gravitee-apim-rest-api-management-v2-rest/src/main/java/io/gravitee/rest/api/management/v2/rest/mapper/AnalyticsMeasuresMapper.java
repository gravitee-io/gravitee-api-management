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

import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ArrayFilter;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Interval;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.NumberFilter;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Operator;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.StringFilter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface AnalyticsMeasuresMapper {
    AnalyticsMeasuresMapper INSTANCE = Mappers.getMapper(AnalyticsMeasuresMapper.class);

    MeasuresResponse fromResponseModel(io.gravitee.apim.core.analytics_engine.model.MeasuresResponse responseModel);

    MeasuresRequest fromRequestEntity(io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresRequest requestEntity);

    default TimeRange fromTimeRangeEntity(io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeRange timeRangeEntity) {
        return new TimeRange(toInstant(timeRangeEntity.getFrom()), toInstant(timeRangeEntity.getTo()));
    }

    default Filter fromFilterEntity(io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Filter filterEntity) {
        var instance = filterEntity.getActualInstance();
        return switch (instance) {
            case NumberFilter n -> new Filter(mapFilterName(n.getName()), mapOperator(n.getOperator()), n.getValue());
            case StringFilter s -> new Filter(mapFilterName(s.getName()), mapOperator(s.getOperator()), s.getValue());
            case ArrayFilter a -> new Filter(mapFilterName(a.getName()), mapOperator(a.getOperator()), a.getValue());
            default -> throw new ValidationDomainException("unknown filter type");
        };
    }

    default FilterSpec.Name mapFilterName(FilterName filterName) {
        return FilterSpec.Name.valueOf(filterName.name());
    }

    default FilterSpec.Operator mapOperator(Operator operator) {
        return FilterSpec.Operator.valueOf(operator.name());
    }

    default Instant toInstant(Object timeRangeBound) {
        return switch (timeRangeBound) {
            case Number n -> Instant.ofEpochMilli(n.longValue());
            case OffsetDateTime odt -> odt.toInstant();
            default -> throw new ValidationDomainException("unknown value type for timeRange bound");
        };
    }

    default BigDecimal parseInterval(Interval interval) {
        return BigDecimal.valueOf(parseIntervalDuration(interval).toMillis());
    }

    default Duration parseIntervalDuration(Interval interval) {
        return switch (interval.getActualInstance()) {
            case Number n -> Duration.ofMillis(n.longValue());
            case String s -> parseDurationString(s);
            default -> null;
        };
    }

    default Duration parseDurationString(String s) {
        if (s.endsWith("d")) {
            return Duration.parse("P" + s.toUpperCase());
        }
        return Duration.parse("PT" + s);
    }
}
