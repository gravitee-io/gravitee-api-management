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
import io.gravitee.rest.api.portal.rest.model.AnalyticsDashboard;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFilter;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMetricRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeRange;
import io.gravitee.rest.api.portal.rest.model.AnalyticsWidget;
import io.gravitee.rest.api.portal.rest.model.AnalyticsWidgetLayout;
import io.gravitee.rest.api.portal.rest.model.AnalyticsWidgetRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsWidgetType;
import java.time.OffsetDateTime;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
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

    AnalyticsWidgetRequest map(DashboardWidget.Request request);

    AnalyticsTimeRange map(DashboardWidget.TimeRange timeRange);

    AnalyticsMetricRequest map(DashboardWidget.MetricRequest metric);

    AnalyticsFilter map(DashboardWidget.Filter filter);
}
