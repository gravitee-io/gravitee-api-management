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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CustomDashboardRepository;
import io.gravitee.repository.management.model.CustomDashboard;
import io.gravitee.repository.mongodb.management.internal.CustomDashboardMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
@RequiredArgsConstructor
public class MongoCustomDashboardRepository implements CustomDashboardRepository {

    private final CustomDashboardMongoRepository internalCustomDashboardRepo;
    private final GraviteeMapper mapper;

    @Override
    public Set<CustomDashboard> findAll() throws TechnicalException {
        log.debug("Find all custom dashboards");
        var dashboards = internalCustomDashboardRepo.findAll();
        var res = mapper.mapCustomDashboards(dashboards);
        log.debug("Find all custom dashboards - Done");
        return res;
    }

    @Override
    public Optional<CustomDashboard> findById(String id) throws TechnicalException {
        log.debug("Find custom dashboard by ID [{}]", id);
        var dashboard = internalCustomDashboardRepo.findById(id).orElse(null);
        log.debug("Find custom dashboard by ID [{}] - Done", id);
        return Optional.ofNullable(mapper.map(dashboard));
    }

    @Override
    public CustomDashboard create(CustomDashboard customDashboard) throws TechnicalException {
        log.debug("Create custom dashboard [{}]", customDashboard.getName());
        var dashboardMongo = mapper.map(customDashboard);
        var createdDashboardMongo = internalCustomDashboardRepo.insert(dashboardMongo);
        var res = mapper.map(createdDashboardMongo);
        log.debug("Create custom dashboard [{}] - Done", customDashboard.getName());
        return res;
    }

    @Override
    public CustomDashboard update(CustomDashboard customDashboard) throws TechnicalException {
        if (customDashboard == null || customDashboard.getName() == null) {
            throw new IllegalStateException("Custom dashboard to update must have a name");
        }

        var existingDashboard = internalCustomDashboardRepo.findById(customDashboard.getId()).orElse(null);

        if (existingDashboard == null) {
            throw new IllegalStateException(String.format("No custom dashboard found with id [%s]", customDashboard.getId()));
        }

        try {
            var dashboardMongo = mapper.map(customDashboard);
            var updatedDashboardMongo = internalCustomDashboardRepo.save(dashboardMongo);
            return mapper.map(updatedDashboardMongo);
        } catch (Exception e) {
            var error = "An error occurred when updating custom dashboard";
            log.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalCustomDashboardRepo.deleteById(id);
        } catch (Exception e) {
            var error = "An error occurred when deleting custom dashboard " + id;
            log.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public List<CustomDashboard> findByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("Find custom dashboards by organization ID [{}]", organizationId);
        var dashboards = internalCustomDashboardRepo.findByOrganizationId(organizationId);
        log.debug("Find custom dashboards by organization ID [{}] - Done", organizationId);
        return dashboards.stream().map(mapper::map).toList();
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("Delete custom dashboards by organization ID [{}]", organizationId);
        try {
            internalCustomDashboardRepo.deleteByOrganizationId(organizationId);
            log.debug("Delete custom dashboards by organization ID [{}] - Done", organizationId);
        } catch (Exception e) {
            var error = "An error occurred when deleting custom dashboards by organization id " + organizationId;
            log.error(error, e);
            throw new TechnicalException(error, e);
        }
    }
}
