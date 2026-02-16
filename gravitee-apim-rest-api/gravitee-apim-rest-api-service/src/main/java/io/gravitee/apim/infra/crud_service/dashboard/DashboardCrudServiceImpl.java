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
package io.gravitee.apim.infra.crud_service.dashboard;

import io.gravitee.apim.core.dashboard.crud_service.DashboardCrudService;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.DashboardAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CustomDashboardRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class DashboardCrudServiceImpl implements DashboardCrudService {

    private final CustomDashboardRepository customDashboardRepository;

    public DashboardCrudServiceImpl(@Lazy CustomDashboardRepository customDashboardRepository) {
        this.customDashboardRepository = customDashboardRepository;
    }

    @Override
    public Dashboard create(Dashboard dashboard) {
        try {
            var repositoryModel = DashboardAdapter.INSTANCE.toRepository(dashboard);
            var created = customDashboardRepository.create(repositoryModel);
            return DashboardAdapter.INSTANCE.toModel(created);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to create a dashboard", e);
        }
    }

    @Override
    public Optional<Dashboard> findById(String dashboardId) {
        try {
            return customDashboardRepository.findById(dashboardId).map(DashboardAdapter.INSTANCE::toModel);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to find dashboard: " + dashboardId, e);
        }
    }

    @Override
    public List<Dashboard> findByOrganizationId(String organizationId) {
        try {
            return customDashboardRepository.findByOrganizationId(organizationId).stream().map(DashboardAdapter.INSTANCE::toModel).toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to find dashboards for organization: " + organizationId, e);
        }
    }

    @Override
    public Dashboard update(Dashboard dashboard) {
        try {
            var repositoryModel = DashboardAdapter.INSTANCE.toRepository(dashboard);
            var updated = customDashboardRepository.update(repositoryModel);
            return DashboardAdapter.INSTANCE.toModel(updated);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to update dashboard: " + dashboard.getId(), e);
        }
    }

    @Override
    public void delete(String dashboardId) {
        try {
            customDashboardRepository.delete(dashboardId);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to delete dashboard: " + dashboardId, e);
        }
    }
}
