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
import io.gravitee.repository.management.api.GammaDashboardRepository;
import io.gravitee.repository.management.model.GammaDashboard;
import io.gravitee.repository.mongodb.management.internal.GammaDashboardMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Component
@RequiredArgsConstructor
public class MongoGammaDashboardRepository implements GammaDashboardRepository {

    private final GammaDashboardMongoRepository internalGammaDashboardRepo;
    private final GraviteeMapper mapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public Set<GammaDashboard> findAll() throws TechnicalException {
        log.debug("Find all gamma dashboards");
        var dashboards = internalGammaDashboardRepo.findAll();
        var res = mapper.mapGammaDashboards(dashboards);
        log.debug("Find all gamma dashboards - Done");
        return res;
    }

    @Override
    public Optional<GammaDashboard> findById(String id) throws TechnicalException {
        log.debug("Find gamma dashboard by ID [{}]", id);
        var dashboard = internalGammaDashboardRepo.findById(id).map(mapper::map);
        log.debug("Find gamma dashboard by ID [{}] - Done", id);
        return dashboard;
    }

    @Override
    public GammaDashboard create(GammaDashboard gammaDashboard) throws TechnicalException {
        log.debug("Create gamma dashboard [{}]", gammaDashboard.getTitle());
        var dashboardMongo = mapper.map(gammaDashboard);
        var createdDashboardMongo = internalGammaDashboardRepo.insert(dashboardMongo);
        var res = mapper.map(createdDashboardMongo);
        log.debug("Create gamma dashboard [{}] - Done", gammaDashboard.getTitle());
        return res;
    }

    /**
     * A single conditional replace rather than a read followed by {@code save()}.
     *
     * <p>{@code save()} is an upsert, so with a read-then-write pair a dashboard deleted between the two calls would be
     * silently re-inserted — while the JDBC implementation, guarded by its affected-row count, raises. Matching on
     * {@code _id} folds the existence check into the same round trip and keeps the two backends behaving alike.
     */
    @Override
    public GammaDashboard update(GammaDashboard gammaDashboard) throws TechnicalException {
        if (gammaDashboard == null) {
            throw new IllegalStateException("Unable to update a null gamma dashboard");
        }

        return updateIfPresent(gammaDashboard).orElseThrow(() ->
            new IllegalStateException(String.format("No gamma dashboard found with id [%s]", gammaDashboard.getId()))
        );
    }

    @Override
    public Optional<GammaDashboard> updateIfPresent(GammaDashboard gammaDashboard) throws TechnicalException {
        if (gammaDashboard == null) {
            throw new IllegalStateException("Unable to update a null gamma dashboard");
        }

        return replaceMatching(Criteria.where("_id").is(gammaDashboard.getId()), gammaDashboard);
    }

    /**
     * The version guard is one more criterion on the same {@code findAndReplace} the unguarded update already used, so
     * comparing and writing stay a single atomic operation.
     */
    @Override
    public Optional<GammaDashboard> updateIfVersionMatches(GammaDashboard gammaDashboard, int expectedVersion) throws TechnicalException {
        if (gammaDashboard == null) {
            throw new IllegalStateException("Unable to update a null gamma dashboard");
        }

        return replaceMatching(Criteria.where("_id").is(gammaDashboard.getId()).and("version").is(expectedVersion), gammaDashboard);
    }

    private Optional<GammaDashboard> replaceMatching(Criteria criteria, GammaDashboard gammaDashboard) throws TechnicalException {
        try {
            var replaced = mongoTemplate.findAndReplace(
                Query.query(criteria),
                mapper.map(gammaDashboard),
                FindAndReplaceOptions.options().returnNew()
            );
            return Optional.ofNullable(replaced).map(mapper::map);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when updating gamma dashboard " + gammaDashboard.getId(), e);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalGammaDashboardRepo.deleteById(id);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when deleting gamma dashboard " + id, e);
        }
    }

    @Override
    public List<GammaDashboard> findByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Find gamma dashboards by environment ID [{}]", environmentId);
        var dashboards = internalGammaDashboardRepo.findByEnvironmentIdOrderByCreatedAtAscIdAsc(environmentId);
        log.debug("Find gamma dashboards by environment ID [{}] - Done", environmentId);
        return dashboards.stream().map(mapper::map).toList();
    }

    @Override
    public Optional<GammaDashboard> findByIdAndEnvironmentId(String id, String environmentId) throws TechnicalException {
        log.debug("Find gamma dashboard by ID [{}] and environment ID [{}]", id, environmentId);
        var dashboard = internalGammaDashboardRepo.findByIdAndEnvironmentId(id, environmentId).map(mapper::map);
        log.debug("Find gamma dashboard by ID [{}] and environment ID [{}] - Done", id, environmentId);
        return dashboard;
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Delete gamma dashboards by environment ID [{}]", environmentId);
        try {
            internalGammaDashboardRepo.deleteByEnvironmentId(environmentId);
            log.debug("Delete gamma dashboards by environment ID [{}] - Done", environmentId);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when deleting gamma dashboards by environment id " + environmentId, e);
        }
    }
}
