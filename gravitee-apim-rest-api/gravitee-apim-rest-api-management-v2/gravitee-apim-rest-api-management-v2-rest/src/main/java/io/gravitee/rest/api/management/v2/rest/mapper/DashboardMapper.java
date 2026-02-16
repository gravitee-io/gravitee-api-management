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

import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.model.DashboardWidget;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.CreateDashboard;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.CustomInterval;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasureName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeRange;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.UpdateDashboard;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Widget;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.WidgetLayout;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.WidgetRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.WidgetType;
import java.time.OffsetDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class })
public interface DashboardMapper {
    DashboardMapper INSTANCE = Mappers.getMapper(DashboardMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModified", ignore = true)
    Dashboard map(CreateDashboard createDashboard);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModified", ignore = true)
    Dashboard map(UpdateDashboard updateDashboard);

    io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Dashboard map(Dashboard dashboard);

    List<io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Dashboard> mapList(List<Dashboard> dashboards);

    @Mapping(target = "type", expression = "java(mapWidgetTypeToString(widget.getType()))")
    @Mapping(target = "request", expression = "java(mapToDomainRequest(widget.getRequest()))")
    DashboardWidget mapWidget(Widget widget);

    @Mapping(target = "type", expression = "java(mapStringToWidgetType(dashboardWidget.getType()))")
    @Mapping(target = "request", expression = "java(mapToRestRequest(dashboardWidget.getRequest()))")
    Widget mapWidget(DashboardWidget dashboardWidget);

    DashboardWidget.Layout map(WidgetLayout layout);

    WidgetLayout map(DashboardWidget.Layout layout);

    default String mapWidgetTypeToString(WidgetType widgetType) {
        return widgetType == null ? null : widgetType.getValue();
    }

    default WidgetType mapStringToWidgetType(String type) {
        return type == null ? null : WidgetType.fromValue(type);
    }

    default DashboardWidget.Request mapToDomainRequest(WidgetRequest request) {
        if (request == null) {
            return null;
        }
        return DashboardWidget.Request.builder()
            .type(request.getType() != null ? request.getType().getValue() : null)
            .timeRange(mapToDomainTimeRange(request.getTimeRange()))
            .metrics(request.getMetrics() != null ? request.getMetrics().stream().map(this::mapToDomainMetricRequest).toList() : null)
            .interval(request.getInterval() != null ? request.getInterval().toMillis() : null)
            .by(request.getBy() != null ? request.getBy().stream().map(FacetName::getValue).toList() : null)
            .limit(request.getLimit())
            .build();
    }

    default DashboardWidget.MetricRequest mapToDomainMetricRequest(MetricRequest metricRequest) {
        if (metricRequest == null) {
            return null;
        }
        return DashboardWidget.MetricRequest.builder()
            .name(metricRequest.getName() != null ? metricRequest.getName().getValue() : null)
            .measures(metricRequest.getMeasures() != null ? metricRequest.getMeasures().stream().map(MeasureName::getValue).toList() : null)
            .build();
    }

    default WidgetRequest mapToRestRequest(DashboardWidget.Request request) {
        if (request == null) {
            return null;
        }
        var widgetRequest = new WidgetRequest();
        if (request.getType() != null) {
            widgetRequest.setType(WidgetRequest.TypeEnum.fromValue(request.getType()));
        }
        widgetRequest.setTimeRange(mapToRestTimeRange(request.getTimeRange()));
        if (request.getMetrics() != null) {
            widgetRequest.setMetrics(request.getMetrics().stream().map(this::mapToRestMetricRequest).toList());
        }
        if (request.getInterval() != null) {
            widgetRequest.setInterval(new CustomInterval(request.getInterval()));
        }
        if (request.getBy() != null) {
            widgetRequest.setBy(request.getBy().stream().map(FacetName::fromValue).toList());
        }
        widgetRequest.setLimit(request.getLimit());
        return widgetRequest;
    }

    default MetricRequest mapToRestMetricRequest(DashboardWidget.MetricRequest metricRequest) {
        if (metricRequest == null) {
            return null;
        }
        var result = new MetricRequest();
        if (metricRequest.getName() != null) {
            result.setName(MetricName.fromValue(metricRequest.getName()));
        }
        if (metricRequest.getMeasures() != null) {
            result.setMeasures(metricRequest.getMeasures().stream().map(MeasureName::fromValue).toList());
        }
        return result;
    }

    default DashboardWidget.TimeRange mapToDomainTimeRange(TimeRange timeRange) {
        if (timeRange == null) {
            return null;
        }
        return DashboardWidget.TimeRange.builder()
            .from(timeRange.getFrom() != null ? timeRange.getFrom().toString() : null)
            .to(timeRange.getTo() != null ? timeRange.getTo().toString() : null)
            .build();
    }

    default TimeRange mapToRestTimeRange(DashboardWidget.TimeRange timeRange) {
        if (timeRange == null) {
            return null;
        }
        return new TimeRange()
            .from(timeRange.getFrom() != null ? OffsetDateTime.parse(timeRange.getFrom()) : null)
            .to(timeRange.getTo() != null ? OffsetDateTime.parse(timeRange.getTo()) : null);
    }
}
