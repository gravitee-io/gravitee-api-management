/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.model.Dashboard;
import io.gravitee.repository.redis.management.internal.DashboardRedisRepository;
import io.gravitee.repository.redis.management.model.RedisDashboard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisDashboardRepository implements DashboardRepository {

    @Autowired
    private DashboardRedisRepository dashboardRedisRepository;

    @Override
    public Set<Dashboard> findAll() {
        final Set<RedisDashboard> dashboards = dashboardRedisRepository.findAll();
        return dashboards.stream().map(this::convert).collect(toSet());
    }

    @Override
    public Optional<Dashboard> findById(final String dashboardId) throws TechnicalException {
        final RedisDashboard redisDashboard = dashboardRedisRepository.findById(dashboardId);
        return Optional.ofNullable(convert(redisDashboard));
    }

    @Override
    public Dashboard create(final Dashboard dashboard) throws TechnicalException {
        final RedisDashboard redisDashboard = dashboardRedisRepository.create(convert(dashboard));
        return convert(redisDashboard);
    }

    @Override
    public Dashboard update(final Dashboard dashboard) throws TechnicalException {
        if (dashboard == null || dashboard.getName() == null) {
            throw new IllegalStateException("Dashboard to update must have a name");
        }

        final RedisDashboard redisDashboard = dashboardRedisRepository.findById(dashboard.getId());

        if (redisDashboard == null) {
            throw new IllegalStateException(String.format("No dashboard found with name [%s]", dashboard.getId()));
        }

        final RedisDashboard redisDashboardUpdated = dashboardRedisRepository.update(convert(dashboard));
        return convert(redisDashboardUpdated);
    }

    @Override
    public List<Dashboard> findByReferenceType(String referenceType) throws TechnicalException {
        final List<RedisDashboard> dashboards = dashboardRedisRepository.findByReferenceType(referenceType);

        return dashboards.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(final String dashboardId) throws TechnicalException {
        dashboardRedisRepository.delete(dashboardId);
    }

    private Dashboard convert(final RedisDashboard redisDashboard) {
        final Dashboard dashboard = new Dashboard();
        dashboard.setId(redisDashboard.getId());
        dashboard.setReferenceId(redisDashboard.getReferenceId());
        dashboard.setReferenceType(redisDashboard.getReferenceType());
        dashboard.setName(redisDashboard.getName());
        dashboard.setQueryFilter(redisDashboard.getQueryFilter());
        dashboard.setOrder(redisDashboard.getOrder());
        dashboard.setEnabled(redisDashboard.isEnabled());
        dashboard.setDefinition(redisDashboard.getDefinition());
        dashboard.setCreatedAt(redisDashboard.getCreatedAt());
        dashboard.setUpdatedAt(redisDashboard.getUpdatedAt());
        return dashboard;
    }

    private RedisDashboard convert(final Dashboard dashboard) {
        final RedisDashboard redisDashboard = new RedisDashboard();
        redisDashboard.setId(dashboard.getId());
        redisDashboard.setReferenceId(dashboard.getReferenceId());
        redisDashboard.setReferenceType(dashboard.getReferenceType());
        redisDashboard.setName(dashboard.getName());
        redisDashboard.setQueryFilter(dashboard.getQueryFilter());
        redisDashboard.setOrder(dashboard.getOrder());
        redisDashboard.setEnabled(dashboard.isEnabled());
        redisDashboard.setDefinition(dashboard.getDefinition());
        redisDashboard.setCreatedAt(dashboard.getCreatedAt());
        redisDashboard.setUpdatedAt(dashboard.getUpdatedAt());
        return redisDashboard;
    }
}
