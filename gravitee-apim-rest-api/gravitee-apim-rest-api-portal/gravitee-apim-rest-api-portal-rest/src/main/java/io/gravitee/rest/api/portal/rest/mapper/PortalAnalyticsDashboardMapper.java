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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.model.DashboardWidget;
import io.gravitee.rest.api.portal.rest.model.AnalyticsArrayFilter;
import io.gravitee.rest.api.portal.rest.model.AnalyticsDashboard;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFilter;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFilterName;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFilterOperator;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMetricRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsNumberFilter;
import io.gravitee.rest.api.portal.rest.model.AnalyticsStringFilter;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeRange;
import io.gravitee.rest.api.portal.rest.model.AnalyticsWidget;
import io.gravitee.rest.api.portal.rest.model.AnalyticsWidgetLayout;
import io.gravitee.rest.api.portal.rest.model.AnalyticsWidgetRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsWidgetType;
import io.gravitee.rest.api.portal.rest.model.CustomInterval;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * @author GraviteeSource Team
 */
@Mapper(uses = DateMapper.class)
public interface PortalAnalyticsDashboardMapper {
    PortalAnalyticsDashboardMapper INSTANCE = Mappers.getMapper(PortalAnalyticsDashboardMapper.class);

    @BeanMapping(ignoreUnmappedSourceProperties = "environmentId")
    AnalyticsDashboard toDto(Dashboard dashboard);

    List<AnalyticsDashboard> toDto(List<Dashboard> dashboards);

    default OffsetDateTime map(java.time.ZonedDateTime value) {
        return value == null ? null : value.toOffsetDateTime();
    }

    default AnalyticsWidgetType mapWidgetType(String type) {
        return type == null ? null : AnalyticsWidgetType.fromValue(type);
    }

    AnalyticsWidget map(DashboardWidget widget);

    AnalyticsWidgetLayout map(DashboardWidget.Layout layout);

    @Mapping(target = "interval", source = "interval", qualifiedByName = "millisecondsToInterval")
    AnalyticsWidgetRequest map(DashboardWidget.Request request);

    @Named("millisecondsToInterval")
    default CustomInterval millisecondsToInterval(Long intervalMillis) {
        return intervalMillis == null ? null : new CustomInterval(intervalMillis);
    }

    AnalyticsTimeRange map(DashboardWidget.TimeRange timeRange);

    AnalyticsMetricRequest map(DashboardWidget.MetricRequest metric);

    default AnalyticsFilter map(DashboardWidget.Filter filter) {
        if (filter == null || filter.getOperator() == null) {
            return null;
        }
        var name = AnalyticsFilterName.fromValue(filter.getName());
        var wrapper = new AnalyticsFilter();
        switch (filter.getOperator()) {
            case "EQ" -> wrapper.setActualInstance(
                new AnalyticsStringFilter().name(name).operator(AnalyticsFilterOperator.EQ).value(Objects.toString(filter.getValue(), null))
            );
            case "IN" -> {
                if (!(filter.getValue() instanceof List<?> raw) || raw.stream().anyMatch(v -> !(v instanceof String))) {
                    throw new IllegalArgumentException("IN filter value must be a list of strings");
                }
                @SuppressWarnings("unchecked")
                var values = (List<String>) filter.getValue();
                wrapper.setActualInstance(new AnalyticsArrayFilter().name(name).operator(AnalyticsFilterOperator.IN).value(values));
            }
            case "LTE", "GTE" -> {
                Number num = filter.getValue() instanceof Number n ? n : Double.parseDouble(String.valueOf(filter.getValue()));
                wrapper.setActualInstance(
                    new AnalyticsNumberFilter().name(name).operator(AnalyticsFilterOperator.fromValue(filter.getOperator())).value(num)
                );
            }
            default -> throw new IllegalArgumentException("Unsupported filter operator: " + filter.getOperator());
        }
        return wrapper;
    }
}
