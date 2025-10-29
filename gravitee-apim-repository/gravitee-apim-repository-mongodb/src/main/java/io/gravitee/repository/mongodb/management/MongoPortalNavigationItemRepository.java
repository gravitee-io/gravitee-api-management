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
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.mongodb.management.internal.model.PortalNavigationItemMongo;
import io.gravitee.repository.mongodb.management.internal.portalnavigationitem.PortalNavigationItemMongoRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MongoPortalNavigationItemRepository implements PortalNavigationItemRepository {

    @Autowired
    private PortalNavigationItemMongoRepository internalRepo;

    @Override
    public Optional<PortalNavigationItem> findById(String id) throws TechnicalException {
        log.debug("Find PortalNavigationItem by id [{}]", id);
        Optional<PortalNavigationItem> maybe = internalRepo.findById(id).map(this::map);
        log.debug("Find PortalNavigationItem by id [{}] - Done", id);
        return maybe;
    }

    @Override
    public List<PortalNavigationItem> findAllByOrganizationIdAndEnvironmentId(String organizationId, String environmentId)
        throws TechnicalException {
        log.debug("Find all PortalNavigationItem by organizationId [{}] and environmentId [{}]", organizationId, environmentId);
        Set<PortalNavigationItemMongo> items = internalRepo.findAllByOrganizationIdAndEnvironmentId(organizationId, environmentId);
        List<PortalNavigationItem> mapped = items.stream().map(this::map).collect(Collectors.toList());
        log.debug("Find all PortalNavigationItem - Done, found {} items", mapped.size());
        return mapped;
    }

    @Override
    public List<PortalNavigationItem> findAllByAreaAndOrganizationIdAndEnvironmentId(
        PortalNavigationItem.Area area,
        String organizationId,
        String environmentId
    ) throws TechnicalException {
        log.debug(
            "Find all PortalNavigationItem by area [{}], organizationId [{}], environmentId [{}]",
            area,
            organizationId,
            environmentId
        );
        Set<PortalNavigationItemMongo> items = internalRepo.findAllByAreaAndOrganizationIdAndEnvironmentId(
            area,
            organizationId,
            environmentId
        );
        return items.stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public PortalNavigationItem create(PortalNavigationItem item) throws TechnicalException {
        log.debug("Create PortalNavigationItem [{}]", item.getId());
        PortalNavigationItemMongo created = internalRepo.insert(map(item));
        return map(created);
    }

    @Override
    public PortalNavigationItem update(PortalNavigationItem item) throws TechnicalException {
        log.debug("Update PortalNavigationItem [{}]", item.getId());
        PortalNavigationItemMongo saved = internalRepo.save(map(item));
        return map(saved);
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
        return results.stream().map(this::map).collect(Collectors.toSet());
    }

    private PortalNavigationItem map(PortalNavigationItemMongo mongo) {
        PortalNavigationItem item = new PortalNavigationItem();
        item.setId(mongo.getId());
        item.setOrganizationId(mongo.getOrganizationId());
        item.setEnvironmentId(mongo.getEnvironmentId());
        item.setTitle(mongo.getTitle());
        item.setType(mongo.getType());
        item.setArea(mongo.getArea());
        item.setParentId(mongo.getParentId());
        item.setOrder(mongo.getOrder());
        item.setConfiguration(mongo.getConfiguration());
        return item;
    }

    private PortalNavigationItemMongo map(PortalNavigationItem item) {
        PortalNavigationItemMongo mongo = new PortalNavigationItemMongo();
        mongo.setId(item.getId());
        mongo.setOrganizationId(item.getOrganizationId());
        mongo.setEnvironmentId(item.getEnvironmentId());
        mongo.setTitle(item.getTitle());
        mongo.setType(item.getType());
        mongo.setArea(item.getArea());
        mongo.setParentId(item.getParentId());
        mongo.setOrder(item.getOrder());
        mongo.setConfiguration(item.getConfiguration());
        return mongo;
    }
}
