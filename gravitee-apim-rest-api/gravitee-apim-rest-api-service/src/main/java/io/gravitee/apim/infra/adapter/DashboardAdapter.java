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

import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.model.DashboardWidget;
import io.gravitee.repository.management.model.CustomDashboard;
import io.gravitee.repository.management.model.CustomDashboardWidget;
import java.time.ZonedDateTime;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface DashboardAdapter {
    DashboardAdapter INSTANCE = Mappers.getMapper(DashboardAdapter.class);

    @Mapping(target = "createdAt", qualifiedByName = "dateToZonedDateTime")
    @Mapping(target = "lastModified", qualifiedByName = "dateToZonedDateTime")
    Dashboard toModel(CustomDashboard repository);

    @Mapping(target = "createdAt", qualifiedByName = "zonedDateTimeToDate")
    @Mapping(target = "lastModified", qualifiedByName = "zonedDateTimeToDate")
    CustomDashboard toRepository(Dashboard domain);

    DashboardWidget toWidgetModel(CustomDashboardWidget repository);

    CustomDashboardWidget toWidgetRepository(DashboardWidget domain);

    DashboardWidget.Layout toLayoutModel(CustomDashboardWidget.Layout repository);

    CustomDashboardWidget.Layout toLayoutRepository(DashboardWidget.Layout domain);

    DashboardWidget.TimeRange toTimeRangeModel(CustomDashboardWidget.TimeRange repository);

    CustomDashboardWidget.TimeRange toTimeRangeRepository(DashboardWidget.TimeRange domain);

    DashboardWidget.Request toRequestModel(CustomDashboardWidget.Request repository);

    CustomDashboardWidget.Request toRequestRepository(DashboardWidget.Request domain);

    DashboardWidget.MetricRequest toMetricRequestModel(CustomDashboardWidget.MetricRequest repository);

    CustomDashboardWidget.MetricRequest toMetricRequestRepository(DashboardWidget.MetricRequest domain);

    @Named("dateToZonedDateTime")
    default ZonedDateTime dateToZonedDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(java.time.ZoneId.systemDefault());
    }

    @Named("zonedDateTimeToDate")
    default Date zonedDateTimeToDate(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return Date.from(zonedDateTime.toInstant());
    }
}
