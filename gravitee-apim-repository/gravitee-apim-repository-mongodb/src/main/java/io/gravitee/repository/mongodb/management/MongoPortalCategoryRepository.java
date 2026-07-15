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
import io.gravitee.repository.management.api.PortalCategoryRepository;
import io.gravitee.repository.management.model.PortalCategory;
import io.gravitee.repository.mongodb.management.internal.PortalCategoryMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Component
@RequiredArgsConstructor
public class MongoPortalCategoryRepository implements PortalCategoryRepository {

    private final PortalCategoryMongoRepository internalPortalCategoryRepo;
    private final GraviteeMapper mapper;

    @Override
    public Set<PortalCategory> findAll() throws TechnicalException {
        log.debug("Find all portal categories");
        var portalCategories = internalPortalCategoryRepo.findAll();
        var res = mapper.mapPortalCategories(portalCategories);
        log.debug("Find all portal categories - Done");
        return res;
    }

    @Override
    public Optional<PortalCategory> findById(String id) throws TechnicalException {
        log.debug("Find portal category by ID [{}]", id);
        var portalCategory = internalPortalCategoryRepo.findById(id).orElse(null);
        log.debug("Find portal category by ID [{}] - Done", id);
        return Optional.ofNullable(mapper.map(portalCategory));
    }

    @Override
    public PortalCategory create(PortalCategory portalCategory) throws TechnicalException {
        log.debug("Create portal category [{}]", portalCategory.getTitle());
        var portalCategoryMongo = mapper.map(portalCategory);
        var createdPortalCategoryMongo = internalPortalCategoryRepo.insert(portalCategoryMongo);
        var res = mapper.map(createdPortalCategoryMongo);
        log.debug("Create portal category [{}] - Done", portalCategory.getTitle());
        return res;
    }

    @Override
    public PortalCategory update(PortalCategory portalCategory) throws TechnicalException {
        if (portalCategory == null || portalCategory.getTitle() == null) {
            throw new IllegalStateException("Portal category to update must have a title");
        }

        var existing = internalPortalCategoryRepo.findById(portalCategory.getId()).orElse(null);

        if (existing == null) {
            throw new IllegalStateException(String.format("No portal category found with id [%s]", portalCategory.getId()));
        }

        try {
            var portalCategoryMongo = mapper.map(portalCategory);
            var updatedPortalCategoryMongo = internalPortalCategoryRepo.save(portalCategoryMongo);
            return mapper.map(updatedPortalCategoryMongo);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when updating portal category", e);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalPortalCategoryRepo.deleteById(id);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when deleting portal category " + id, e);
        }
    }

    @Override
    public List<PortalCategory> findAllByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Find portal categories by environment ID [{}]", environmentId);
        var portalCategories = internalPortalCategoryRepo.findByEnvironmentId(environmentId, Sort.by("title"));
        log.debug("Find portal categories by environment ID [{}] - Done", environmentId);
        return portalCategories.stream().map(mapper::map).toList();
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        try {
            internalPortalCategoryRepo.deleteByEnvironmentId(environmentId);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when deleting portal categories by environment: " + environmentId, e);
        }
    }
}
