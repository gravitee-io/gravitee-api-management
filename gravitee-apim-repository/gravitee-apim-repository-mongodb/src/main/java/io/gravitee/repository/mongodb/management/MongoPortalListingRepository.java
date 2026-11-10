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

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalListingRepository;
import io.gravitee.repository.management.model.PortalListing;
import io.gravitee.repository.mongodb.management.internal.model.PortalListingMongo;
import io.gravitee.repository.mongodb.management.internal.portallisting.PortalListingMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class MongoPortalListingRepository implements PortalListingRepository {

    @Autowired
    private PortalListingMongoRepository internalPortalListingRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Optional<PortalListing> findByIdAndEnvironmentId(String portalListingId, String environmentId) throws TechnicalException {
        log.debug("Find portal listing by ID [{}] and environment [{}]", portalListingId, environmentId);
        PortalListingMongo listing = internalPortalListingRepo.findByIdAndEnvironmentId(portalListingId, environmentId).orElse(null);
        return Optional.ofNullable(mapper.map(listing));
    }

    @Override
    public List<PortalListing> findAllByPortalIdAndEnvironmentId(String portalId, String environmentId) throws TechnicalException {
        log.debug("Find portal listings by portal [{}] and environment [{}]", portalId, environmentId);
        return internalPortalListingRepo.findAllByPortalIdAndEnvironmentId(portalId, environmentId).stream().map(mapper::map).toList();
    }

    @Override
    public Set<String> findPortalIdsByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Find portal ids of the listings of environment [{}]", environmentId);
        try {
            var query = new Query(where("environmentId").is(environmentId));
            return Set.copyOf(mongoTemplate.findDistinct(query, "portalId", PortalListingMongo.class, String.class));
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when finding the portal ids of environment [" + environmentId + "]", e);
        }
    }

    @Override
    public long updatePortalIds(String environmentId, Collection<String> fromPortalIds, String toPortalId) throws TechnicalException {
        log.debug("Update portal ids {} of environment [{}] to portal [{}]", fromPortalIds, environmentId, toPortalId);
        if (fromPortalIds.isEmpty()) {
            return 0;
        }
        try {
            var query = new Query(where("environmentId").is(environmentId).and("portalId").in(fromPortalIds));
            return mongoTemplate.updateMulti(query, Update.update("portalId", toPortalId), PortalListingMongo.class).getModifiedCount();
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when updating the portal ids of environment [" + environmentId + "]", e);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        try {
            internalPortalListingRepo.deleteByEnvironmentId(environmentId);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when deleting portal listings by environment [" + environmentId + "]", e);
        }
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        try {
            internalPortalListingRepo.deleteByOrganizationId(organizationId);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when deleting portal listings by organization [" + organizationId + "]", e);
        }
    }

    @Override
    public Optional<PortalListing> findById(String portalListingId) throws TechnicalException {
        log.debug("Find portal listing by ID [{}]", portalListingId);
        PortalListingMongo listing = internalPortalListingRepo.findById(portalListingId).orElse(null);
        return Optional.ofNullable(mapper.map(listing));
    }

    @Override
    public PortalListing create(PortalListing portalListing) throws TechnicalException {
        log.debug("Create portal listing [{}]", portalListing.getId());
        PortalListingMongo listingMongo = mapper.map(portalListing);
        PortalListingMongo created = internalPortalListingRepo.insert(listingMongo);
        return mapper.map(created);
    }

    @Override
    public PortalListing update(PortalListing portalListing) throws TechnicalException {
        if (portalListing == null) {
            throw new IllegalStateException("Portal listing must not be null");
        }

        PortalListingMongo existing = internalPortalListingRepo.findById(portalListing.getId()).orElse(null);
        if (existing == null) {
            throw new IllegalStateException(String.format("No portal listing found with id [%s]", portalListing.getId()));
        }

        try {
            PortalListingMongo updated = internalPortalListingRepo.save(mapper.map(portalListing));
            return mapper.map(updated);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when updating portal listing [" + portalListing.getId() + "]", e);
        }
    }

    @Override
    public void delete(String portalListingId) throws TechnicalException {
        try {
            internalPortalListingRepo.deleteById(portalListingId);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when deleting portal listing [" + portalListingId + "]", e);
        }
    }

    @Override
    public Set<PortalListing> findAll() throws TechnicalException {
        return internalPortalListingRepo.findAll().stream().map(mapper::map).collect(Collectors.toSet());
    }
}
