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
package io.gravitee.apim.core.dashboard.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.DashboardAuditLogEntity;
import io.gravitee.apim.core.audit.model.event.DashboardAuditEvent;
import io.gravitee.apim.core.dashboard.domain_service.DashboardDomainService;
import io.gravitee.apim.core.dashboard.exception.DashboardNotFoundException;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.common.utils.TimeProvider;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class UpdateDashboardUseCase {

    private final DashboardDomainService dashboardDomainService;

    private final AuditDomainService auditService;

    public record Input(String dashboardId, Dashboard dashboard, AuditInfo auditInfo) {}

    public record Output(Dashboard dashboard) {}

    public Output execute(Input input) {
        var existing = dashboardDomainService
            .findById(input.dashboardId())
            .orElseThrow(() -> new DashboardNotFoundException(input.dashboardId()));

        var updated = existing
            .toBuilder()
            .name(input.dashboard().getName())
            .labels(input.dashboard().getLabels())
            .widgets(input.dashboard().getWidgets())
            .lastModified(ZonedDateTime.now(TimeProvider.clock()))
            .build();

        var saved = dashboardDomainService.update(updated);

        createAuditLog(existing, saved, input.auditInfo());

        return new Output(saved);
    }

    private void createAuditLog(Dashboard oldDashboard, Dashboard newDashboard, AuditInfo auditInfo) {
        auditService.createDashboardAuditLog(
            DashboardAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .event(DashboardAuditEvent.DASHBOARD_CREATED)
                .actor(auditInfo.actor())
                .dashboardId(newDashboard.getId())
                .oldValue(oldDashboard)
                .newValue(newDashboard)
                .createdAt(newDashboard.getLastModified())
                .properties(Map.of(AuditProperties.DASHBOARD, newDashboard.getId()))
                .build()
        );
    }
}
