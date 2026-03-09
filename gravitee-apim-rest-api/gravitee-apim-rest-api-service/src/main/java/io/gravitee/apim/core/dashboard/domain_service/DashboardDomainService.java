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
package io.gravitee.apim.core.dashboard.domain_service;

import static java.util.Optional.*;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.DashboardAuditLogEntity;
import io.gravitee.apim.core.audit.model.event.ApiProductAuditEvent;
import io.gravitee.apim.core.dashboard.crud_service.DashboardCrudService;
import io.gravitee.apim.core.dashboard.exception.DashboardNotFoundException;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.model.DashboardWidget;
import io.gravitee.apim.core.dashboard.model.DashboardWidgetRequestMapper;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.common.utils.TimeProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class DashboardDomainService {

    private final DashboardCrudService dashboardCrudService;
    private final AnalyticsQueryValidator analyticsQueryValidator;

    public Optional<Dashboard> findById(String dashboardId) {
        return dashboardCrudService.findById(dashboardId);
    }

    public List<Dashboard> findByOrganizationId(String organizationId) {
        return dashboardCrudService.findByOrganizationId(organizationId);
    }

    public Dashboard create(Dashboard dashboard) {
        validate(dashboard);
        return dashboardCrudService.create(dashboard);
    }

    public Dashboard update(Dashboard dashboard) {
        validate(dashboard);
        return dashboardCrudService.update(dashboard);
    }

    public void delete(String dashboardId) {
        dashboardCrudService.delete(dashboardId);
    }

    private void validate(Dashboard dashboard) {
        if (dashboard.getName().isBlank()) {
            throw new ValidationDomainException("Dashboard name cannot be blank");
        }

        ofNullable(dashboard.getWidgets()).ifPresent(this::validateWidgets);
        ofNullable(dashboard.getLabels()).ifPresent(this::validateLabels);
    }

    private void validateWidgets(List<DashboardWidget> widgets) {
        for (var widget : widgets) {
            if (widget.getRequest() == null) {
                continue;
            }
            if (widget.getTitle().isBlank()) {
                throw new ValidationDomainException("Widget title cannot be blank");
            }
            var mapped = DashboardWidgetRequestMapper.toAnalyticsRequest(widget.getRequest());
            switch (mapped) {
                case MeasuresRequest r -> analyticsQueryValidator.validateMeasuresRequest(r);
                case FacetsRequest r -> analyticsQueryValidator.validateFacetsRequest(r);
                case TimeSeriesRequest r -> analyticsQueryValidator.validateTimeSeriesRequest(r);
                default -> throw new ValidationDomainException("Unexpected request type: " + mapped.getClass());
            }
        }
    }

    private void validateLabels(Map<String, String> labels) {
        for (var entry : labels.entrySet()) {
            validateLabelEntry(entry);
        }
    }

    private void validateLabelEntry(Map.Entry<String, String> label) {
        if (StringUtils.isEmpty(label.getKey()) || label.getKey().isBlank()) {
            throw new ValidationDomainException("label key must not be empty");
        }
        if (StringUtils.isEmpty(label.getValue()) || label.getValue().isBlank()) {
            throw new ValidationDomainException("label key value must not be empty");
        }
        if (label.getKey().contains(".")) {
            throw new ValidationDomainException("label keys cannot contain dots");
        }
    }
}
