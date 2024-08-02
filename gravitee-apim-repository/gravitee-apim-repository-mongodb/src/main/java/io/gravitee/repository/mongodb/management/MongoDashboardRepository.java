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
import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.model.Dashboard;
import io.gravitee.repository.management.model.DashboardReferenceType;
import io.gravitee.repository.mongodb.management.internal.DashboardMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.DashboardMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoDashboardRepository implements DashboardRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoDashboardRepository.class);

    @Autowired
    private DashboardMongoRepository internalDashboardRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Set<Dashboard> findAll() {
        LOGGER.debug("Find all dashboards");
        final List<DashboardMongo> dictionaries = internalDashboardRepo.findAll();
        final Set<Dashboard> res = mapper.mapDashboards(dictionaries);
        LOGGER.debug("Find all dashboards - Done");
        return res;
    }

    @Override
    public Optional<Dashboard> findById(String dashboardId) throws TechnicalException {
        LOGGER.debug("Find dashboard by ID [{}]", dashboardId);

        final DashboardMongo dashboard = internalDashboardRepo.findById(dashboardId).orElse(null);

        LOGGER.debug("Find dashboard by ID [{}] - Done", dashboardId);
        return Optional.ofNullable(mapper.map(dashboard));
    }

    @Override
    public Dashboard create(Dashboard dashboard) throws TechnicalException {
        LOGGER.debug("Create dashboard [{}]", dashboard.getName());

        DashboardMongo dashboardMongo = mapper.map(dashboard);
        DashboardMongo createdDashboardMongo = internalDashboardRepo.insert(dashboardMongo);

        Dashboard res = mapper.map(createdDashboardMongo);

        LOGGER.debug("Create dashboard [{}] - Done", dashboard.getName());

        return res;
    }

    @Override
    public Dashboard update(Dashboard dashboard) throws TechnicalException {
        if (dashboard == null || dashboard.getName() == null) {
            throw new IllegalStateException("Dashboard to update must have a name");
        }

        final DashboardMongo dashboardMongo = internalDashboardRepo.findById(dashboard.getId()).orElse(null);

        if (dashboardMongo == null) {
            throw new IllegalStateException(String.format("No dashboard found with name [%s]", dashboard.getId()));
        }
        try {
            //Update
            dashboardMongo.setReferenceType(dashboard.getReferenceType());
            dashboardMongo.setReferenceId(dashboard.getReferenceId());
            dashboardMongo.setName(dashboard.getName());
            dashboardMongo.setQueryFilter(dashboard.getQueryFilter());
            dashboardMongo.setOrder(dashboard.getOrder());
            dashboardMongo.setEnabled(dashboard.isEnabled());
            dashboardMongo.setDefinition(dashboard.getDefinition());
            dashboardMongo.setCreatedAt(dashboard.getCreatedAt());
            dashboardMongo.setUpdatedAt(dashboard.getUpdatedAt());

            DashboardMongo dashboardMongoUpdated = internalDashboardRepo.save(dashboardMongo);
            return mapper.map(dashboardMongoUpdated);
        } catch (Exception e) {
            final String error = "An error occurred when updating dashboard";
            LOGGER.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public void delete(String dashboardId) throws TechnicalException {
        try {
            internalDashboardRepo.deleteById(dashboardId);
        } catch (Exception e) {
            final String error = "An error occurred when deleting dashboard " + dashboardId;
            LOGGER.error(error, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public List<Dashboard> findByReference(String referenceType, String referenceId) throws TechnicalException {
        final List<DashboardMongo> dashboards = internalDashboardRepo.findByReferenceTypeAndReferenceId(referenceType, referenceId);
        return dashboards.stream().map(dashboardMongo -> mapper.map(dashboardMongo)).toList();
    }

    @Override
    public List<Dashboard> findByReferenceAndType(String referenceType, String referenceId, String type) throws TechnicalException {
        final List<DashboardMongo> dashboards = internalDashboardRepo.findByReferenceTypeAndReferenceIdAndTypeOrderByOrder(
            referenceType,
            referenceId,
            type
        );
        return dashboards.stream().map(dashboardMongo -> mapper.map(dashboardMongo)).toList();
    }

    @Override
    public Optional<Dashboard> findByReferenceAndId(String referenceType, String referenceId, String id) throws TechnicalException {
        LOGGER.debug("Find dashboard by ID [{}-{}-{}]", referenceType, referenceId, id);

        final DashboardMongo dashboard = internalDashboardRepo
            .findByReferenceTypeAndReferenceIdAndId(referenceType, referenceId, id)
            .orElse(null);

        LOGGER.debug("Find dashboard by ID [{}-{}-{}] - Done", referenceType, referenceId, id);
        return Optional.ofNullable(mapper.map(dashboard));
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, DashboardReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("Delete dashboard by ref [{}/{}]", referenceId, referenceType);

        try {
            final var dashboardIds = internalDashboardRepo
                .deleteByReferenceIdAndReferenceType(referenceId, referenceType.name())
                .stream()
                .map(DashboardMongo::getId)
                .toList();

            LOGGER.debug("Delete dashboard by ref [{}/{}] - Done", referenceId, referenceType);
            return dashboardIds;
        } catch (Exception ex) {
            LOGGER.error("Failed to delete dashboards by ref: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete dashboards by reference");
        }
    }
}
