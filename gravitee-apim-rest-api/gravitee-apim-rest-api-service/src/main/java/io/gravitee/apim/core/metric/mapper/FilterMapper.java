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
package io.gravitee.apim.core.metric.mapper;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.metric.model.ArrayFilter;
import io.gravitee.apim.core.metric.model.Filter;
import io.gravitee.apim.core.metric.model.FilterName;
import io.gravitee.apim.core.metric.model.NumberFilter;
import io.gravitee.apim.core.metric.model.Operator;
import io.gravitee.apim.core.metric.model.StringFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FilterMapper {
    FilterMapper INSTANCE = Mappers.getMapper(FilterMapper.class);

    default Filter toFilter(io.gravitee.apim.core.analytics_engine.model.Filter filter) {
        if (filter == null) {
            return null;
        }
        var operator = filter.operator();
        if (operator == null) {
            return null;
        }
        return switch (operator) {
            case EQ -> new Filter(
                new StringFilter(mapFilterName(filter.name()), mapOperator(filter.operator()), filter.value().toString())
            );
            case IN -> {
                List<String> values = filter.value() instanceof Collection<?> rawValues
                    ? rawValues.stream().map(String::valueOf).toList()
                    : List.of(String.valueOf(filter.value()));

                yield new Filter(new ArrayFilter(mapFilterName(filter.name()), mapOperator(filter.operator()), values));
            }
            case GTE, LTE -> new Filter(
                new NumberFilter(mapFilterName(filter.name()), mapOperator(filter.operator()), (Integer) filter.value())
            );
        };
    }

    default io.gravitee.apim.core.analytics_engine.model.Filter toAnalyticsFilter(Filter filter) {
        if (filter == null) {
            return null;
        }
        var instance = filter.actualInstance();
        if (instance == null) {
            return null;
        }

        return switch (instance) {
            case StringFilter s -> new io.gravitee.apim.core.analytics_engine.model.Filter(
                mapAnalyticsFilterName(s.name()),
                mapAnalyticsOperator(s.operator()),
                s.value()
            );
            case ArrayFilter a -> new io.gravitee.apim.core.analytics_engine.model.Filter(
                mapAnalyticsFilterName(a.name()),
                mapAnalyticsOperator(a.operator()),
                new LinkedHashSet<>(a.value())
            );
            case NumberFilter n -> new io.gravitee.apim.core.analytics_engine.model.Filter(
                mapAnalyticsFilterName(n.name()),
                mapAnalyticsOperator(n.operator()),
                n.value()
            );
            default -> throw new ValidationDomainException("unknown filter type");
        };
    }

    default List<Filter> toFilters(Collection<io.gravitee.apim.core.analytics_engine.model.Filter> filters) {
        if (filters == null) {
            return null;
        }
        return filters.stream().map(this::toFilter).collect(Collectors.toList());
    }

    default List<io.gravitee.apim.core.analytics_engine.model.Filter> toAnalyticsFilters(Collection<Filter> filters) {
        if (filters == null) {
            return null;
        }
        return filters.stream().map(this::toAnalyticsFilter).collect(Collectors.toList());
    }

    private List<Filter> filterListToFilterList(List<io.gravitee.apim.core.analytics_engine.model.Filter> list) {
        if (list == null) {
            return null;
        }

        var list1 = new ArrayList<Filter>(list.size());
        for (io.gravitee.apim.core.analytics_engine.model.Filter filter : list) {
            list1.add(toFilter(filter));
        }

        return list1;
    }

    FilterName mapFilterName(io.gravitee.apim.core.analytics_engine.model.FilterSpec.Name filterName);

    io.gravitee.apim.core.analytics_engine.model.FilterSpec.Name mapAnalyticsFilterName(FilterName filterName);

    Operator mapOperator(io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator operator);

    io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator mapAnalyticsOperator(Operator operator);
}
