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
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.PortalMenuLinkRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.AccessControl;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageMedia;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.PageSource;
import io.gravitee.repository.management.model.PortalMenuLink;
import io.gravitee.repository.mongodb.management.internal.model.AccessControlMongo;
import io.gravitee.repository.mongodb.management.internal.model.PageMediaMongo;
import io.gravitee.repository.mongodb.management.internal.model.PageMongo;
import io.gravitee.repository.mongodb.management.internal.model.PageSourceMongo;
import io.gravitee.repository.mongodb.management.internal.model.PortalMenuLinkMongo;
import io.gravitee.repository.mongodb.management.internal.page.PageMongoRepository;
import io.gravitee.repository.mongodb.management.internal.portalMenuLink.PortalMenuLinkMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class MongoPortalMenuLinkRepository implements PortalMenuLinkRepository {

    private static final Logger logger = LoggerFactory.getLogger(MongoPortalMenuLinkRepository.class);

    @Autowired
    private PortalMenuLinkMongoRepository internalPortalMenuLinkRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        try {
            internalPortalMenuLinkRepo.deleteByEnvironmentId(environmentId);
        } catch (Exception e) {
            logger.error("An error occurred when deleting portal menu links by environment[{}]", environmentId, e);
            throw new TechnicalException("An error occurred when deleting portal menu links");
        }
    }

    @Override
    public List<PortalMenuLink> findByEnvironmentIdSortByOrder(String environmentId) throws TechnicalException {
        List<PortalMenuLinkMongo> results = internalPortalMenuLinkRepo.findByEnvironmentIdSortByOrder(environmentId);
        return mapper.mapPortalMenuLinks(results);
    }

    @Override
    public Optional<PortalMenuLink> findByIdAndEnvironmentId(String portalMenuLinkId, String environmentId) throws TechnicalException {
        logger.debug("Find portal menu link by ID [{}]", portalMenuLinkId);

        PortalMenuLinkMongo portalMenuLink = internalPortalMenuLinkRepo
            .findByIdAndEnvironmentId(portalMenuLinkId, environmentId)
            .orElse(null);
        PortalMenuLink res = mapper.map(portalMenuLink);

        logger.debug("Find portal menu link by ID [{}] - Done", portalMenuLinkId);
        return Optional.ofNullable(res);
    }

    @Override
    public Optional<PortalMenuLink> findById(String portalMenuLinkId) throws TechnicalException {
        logger.debug("Find portal menu link by ID [{}]", portalMenuLinkId);

        PortalMenuLinkMongo portalMenuLink = internalPortalMenuLinkRepo.findById(portalMenuLinkId).orElse(null);
        PortalMenuLink res = mapper.map(portalMenuLink);

        logger.debug("Find portal menu link by ID [{}] - Done", portalMenuLinkId);
        return Optional.ofNullable(res);
    }

    @Override
    public PortalMenuLink create(PortalMenuLink portalMenuLink) throws TechnicalException {
        logger.debug("Create portal menu link [{}]", portalMenuLink.getName());

        PortalMenuLinkMongo portalMenuLinkMongo = mapper.map(portalMenuLink);
        PortalMenuLinkMongo createdPortalMenuLink = internalPortalMenuLinkRepo.insert(portalMenuLinkMongo);

        PortalMenuLink res = mapper.map(createdPortalMenuLink);

        logger.debug("Create portal menu link [{}] - Done", portalMenuLink.getName());

        return res;
    }

    @Override
    public PortalMenuLink update(PortalMenuLink portalMenuLink) throws TechnicalException {
        if (portalMenuLink == null) {
            throw new IllegalStateException("Portal menu link must not be null");
        }

        PortalMenuLinkMongo portalMenuLinkMongo = internalPortalMenuLinkRepo.findById(portalMenuLink.getId()).orElse(null);
        if (portalMenuLinkMongo == null) {
            throw new IllegalStateException(String.format("No portal menu link found with id [%s]", portalMenuLink.getId()));
        }

        try {
            portalMenuLinkMongo.setEnvironmentId(portalMenuLink.getEnvironmentId());
            portalMenuLinkMongo.setName(portalMenuLink.getName());
            portalMenuLinkMongo.setTarget(portalMenuLink.getTarget());
            portalMenuLinkMongo.setType(portalMenuLinkMongo.getType());
            portalMenuLinkMongo.setOrder(portalMenuLink.getOrder());

            PortalMenuLinkMongo portalMenuLinkMongoUpdated = internalPortalMenuLinkRepo.save(portalMenuLinkMongo);
            return mapper.map(portalMenuLinkMongoUpdated);
        } catch (Exception e) {
            logger.error("An error occurred when updating portal menu page", e);
            throw new TechnicalException("An error occurred when updating portal menu page");
        }
    }

    @Override
    public void delete(String portalMenuLinkId) throws TechnicalException {
        try {
            internalPortalMenuLinkRepo.deleteById(portalMenuLinkId);
        } catch (Exception e) {
            logger.error("An error occurred when deleting portal menu link [{}]", portalMenuLinkId, e);
            throw new TechnicalException("An error occurred when deleting portal menu link");
        }
    }

    @Override
    public Set<PortalMenuLink> findAll() throws TechnicalException {
        return internalPortalMenuLinkRepo
            .findAll()
            .stream()
            .map(portalMenuLinkMongo -> mapper.map(portalMenuLinkMongo))
            .collect(Collectors.toSet());
    }
}
