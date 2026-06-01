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
import io.gravitee.repository.management.api.PortalRepository;
import io.gravitee.repository.management.model.Portal;
import io.gravitee.repository.mongodb.management.internal.model.PortalMongo;
import io.gravitee.repository.mongodb.management.internal.portal.PortalMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class MongoPortalRepository implements PortalRepository {

    @Autowired
    private PortalMongoRepository internalPortalRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Portal> findByIdAndEnvironmentId(String portalId, String environmentId) throws TechnicalException {
        log.debug("Find portal by ID [{}] and environment [{}]", portalId, environmentId);
        PortalMongo portal = internalPortalRepo.findByIdAndEnvironmentId(portalId, environmentId).orElse(null);
        return Optional.ofNullable(mapper.map(portal));
    }

    @Override
    public List<Portal> findByEnvironmentId(String environmentId) throws TechnicalException {
        return mapper.mapPortals(internalPortalRepo.findByEnvironmentId(environmentId));
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        try {
            internalPortalRepo.deleteByEnvironmentId(environmentId);
        } catch (Exception e) {
            log.error("An error occurred when deleting portals by environment [{}]", environmentId, e);
            throw new TechnicalException("An error occurred when deleting portals by environment");
        }
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        try {
            internalPortalRepo.deleteByOrganizationId(organizationId);
        } catch (Exception e) {
            log.error("An error occurred when deleting portals by organization [{}]", organizationId, e);
            throw new TechnicalException("An error occurred when deleting portals by organization");
        }
    }

    @Override
    public Optional<Portal> findById(String portalId) throws TechnicalException {
        log.debug("Find portal by ID [{}]", portalId);
        PortalMongo portal = internalPortalRepo.findById(portalId).orElse(null);
        return Optional.ofNullable(mapper.map(portal));
    }

    @Override
    public Portal create(Portal portal) throws TechnicalException {
        log.debug("Create portal [{}]", portal.getName());
        PortalMongo portalMongo = mapper.map(portal);
        PortalMongo created = internalPortalRepo.insert(portalMongo);
        return mapper.map(created);
    }

    @Override
    public Portal update(Portal portal) throws TechnicalException {
        if (portal == null) {
            throw new IllegalStateException("Portal must not be null");
        }

        PortalMongo existing = internalPortalRepo.findById(portal.getId()).orElse(null);
        if (existing == null) {
            throw new IllegalStateException(String.format("No portal found with id [%s]", portal.getId()));
        }

        try {
            PortalMongo updated = internalPortalRepo.save(mapper.map(portal));
            return mapper.map(updated);
        } catch (Exception e) {
            log.error("An error occurred when updating portal [{}]", portal.getId(), e);
            throw new TechnicalException("An error occurred when updating portal");
        }
    }

    @Override
    public void delete(String portalId) throws TechnicalException {
        try {
            internalPortalRepo.deleteById(portalId);
        } catch (Exception e) {
            log.error("An error occurred when deleting portal [{}]", portalId, e);
            throw new TechnicalException("An error occurred when deleting portal");
        }
    }

    @Override
    public Set<Portal> findAll() throws TechnicalException {
        return internalPortalRepo.findAll().stream().map(mapper::map).collect(Collectors.toSet());
    }
}
