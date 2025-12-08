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
import static org.springframework.util.StringUtils.hasText;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.api.search.PortalNavigationItemCriteria;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.mongodb.management.internal.model.PortalNavigationItemMongo;
import io.gravitee.repository.mongodb.management.internal.portalnavigationitem.PortalNavigationItemMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MongoPortalNavigationItemRepository implements PortalNavigationItemRepository {

    @Autowired
    private PortalNavigationItemMongoRepository internalRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Optional<PortalNavigationItem> findById(String id) throws TechnicalException {
        log.debug("Find PortalNavigationItem by id [{}]", id);
        Optional<PortalNavigationItem> maybe = internalRepo.findById(id).map(mapper::map);
        log.debug("Find PortalNavigationItem by id [{}] - Done", id);
        return maybe;
    }

    @Override
    public List<PortalNavigationItem> findAllByOrganizationIdAndEnvironmentId(String organizationId, String environmentId) {
        log.debug("Find all PortalNavigationItem by organizationId [{}] and environmentId [{}]", organizationId, environmentId);
        Set<PortalNavigationItemMongo> items = internalRepo.findAllByOrganizationIdAndEnvironmentId(organizationId, environmentId);
        List<PortalNavigationItem> mapped = items.stream().map(mapper::map).collect(Collectors.toList());
        log.debug("Find all PortalNavigationItem - Done, found {} items", mapped.size());
        return mapped;
    }

    @Override
    public List<PortalNavigationItem> findAllByParentIdAndEnvironmentId(String parentId, String environmentId) {
        log.debug("Find all PortalNavigationItem by parentId [{}] and environmentId [{}]", parentId, environmentId);
        Set<PortalNavigationItemMongo> items = internalRepo.findAllByParentIdAndEnvironmentId(parentId, environmentId);
        List<PortalNavigationItem> mapped = items.stream().map(mapper::map).collect(Collectors.toList());
        log.debug("Find all PortalNavigationItem - Done, found {} items", mapped.size());
        return mapped;
    }

    @Override
    public List<PortalNavigationItem> findAllByAreaAndEnvironmentId(PortalNavigationItem.Area area, String environmentId) {
        log.debug("Find all PortalNavigationItem by area [{}], environmentId [{}]", area, environmentId);
        Set<PortalNavigationItemMongo> items = internalRepo.findAllByAreaAndEnvironmentId(area, environmentId);
        return items.stream().map(mapper::map).collect(Collectors.toList());
    }

    @Override
    public List<PortalNavigationItem> searchByCriteria(PortalNavigationItemCriteria criteria) throws TechnicalException {
        log.debug("Search PortalNavigationItem by criteria [{}]", criteria);
        try {
            Query query = buildQuery(criteria);
            List<PortalNavigationItemMongo> items = mongoTemplate.find(query, PortalNavigationItemMongo.class);
            List<PortalNavigationItem> mapped = items.stream().map(mapper::map).collect(Collectors.toList());
            log.debug("Search PortalNavigationItem by criteria - Done, found {} items", mapped.size());
            return mapped;
        } catch (Exception ex) {
            log.error("Failed to search portal navigation items by criteria", ex);
            throw new TechnicalException("Failed to search portal navigation items by criteria", ex);
        }
    }

    private Query buildQuery(PortalNavigationItemCriteria criteria) {
        Query query = new Query();
        if (criteria != null) {
            if (hasText(criteria.getEnvironmentId())) {
                query.addCriteria(where("environmentId").is(criteria.getEnvironmentId()));
            }

            if (hasText(criteria.getParentId())) {
                query.addCriteria(where("parentId").is(criteria.getParentId()));
            } else if (Boolean.TRUE.equals(criteria.getRoot())) {
                query.addCriteria(where("parentId").isNull());
            }

            if (hasText(criteria.getPortalArea())) {
                try {
                    PortalNavigationItem.Area area = PortalNavigationItem.Area.valueOf(criteria.getPortalArea());
                    query.addCriteria(where("area").is(area));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid portal area value: {}", criteria.getPortalArea());
                }
            }

            if (criteria.getPublished() != null) {
                query.addCriteria(where("published").is(criteria.getPublished()));
            }

            if (criteria.getVisibility() != null) {
                query.addCriteria(where("visibility").is(criteria.getVisibility()));
            }
        }
        return query;
    }

    @Override
    public List<PortalNavigationItem> findAllByAreaAndEnvironmentIdAndParentIdIsNull(PortalNavigationItem.Area area, String environmentId) {
        log.debug("Find all PortalNavigationItem by area [{}], environmentId [{}] and parentId is null", area, environmentId);
        Set<PortalNavigationItemMongo> items = internalRepo.findAllByAreaAndEnvironmentIdAndParentIdIsNull(area, environmentId);
        return items.stream().map(mapper::map).collect(Collectors.toList());
    }

    @Override
    public PortalNavigationItem create(PortalNavigationItem item) throws TechnicalException {
        log.debug("Create PortalNavigationItem [{}]", item.getId());
        PortalNavigationItemMongo created = internalRepo.insert(mapper.map(item));
        return mapper.map(created);
    }

    @Override
    public PortalNavigationItem update(PortalNavigationItem item) throws TechnicalException {
        log.debug("Update PortalNavigationItem [{}]", item.getId());
        PortalNavigationItemMongo saved = internalRepo.save(mapper.map(item));
        return mapper.map(saved);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete PortalNavigationItem [{}]", id);
        internalRepo.deleteById(id);
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("Delete PortalNavigationItem by organizationId [{}]", organizationId);
        internalRepo.deleteByOrganizationId(organizationId);
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Delete PortalNavigationItem by environmentId [{}]", environmentId);
        internalRepo.deleteByEnvironmentId(environmentId);
    }

    @Override
    public Set<PortalNavigationItem> findAll() throws TechnicalException {
        java.util.List<PortalNavigationItemMongo> results = internalRepo.findAll();
        return results.stream().map(mapper::map).collect(Collectors.toSet());
    }
}
