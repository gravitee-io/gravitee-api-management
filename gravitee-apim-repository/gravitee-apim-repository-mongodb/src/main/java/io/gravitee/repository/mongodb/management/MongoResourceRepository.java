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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ResourceRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Resource;
import io.gravitee.repository.mongodb.management.internal.model.ResourceMongo;
import io.gravitee.repository.mongodb.management.internal.resource.ResourceMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@RequiredArgsConstructor
public class MongoResourceRepository implements ResourceRepository {

    private final ResourceMongoRepository internalRepository;
    private final GraviteeMapper mapper;

    @Override
    public Resource create(Resource resource) throws TechnicalException {
        log.debug("Create resource [{}]", resource.getId());
        try {
            ResourceMongo saved = internalRepository.insert(mapper.map(resource));
            log.debug("Create resource [{}] - Done", saved.getId());
            return mapper.map(saved);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create resource [%s]".formatted(resource.getId()), ex);
        }
    }

    @Override
    public Resource update(Resource resource) throws TechnicalException {
        if (resource == null || resource.getId() == null) {
            throw new IllegalStateException("Resource must not be null and must have an id");
        }
        return internalRepository
            .findById(resource.getId())
            .map(found -> {
                log.debug("Update resource [{}]", resource.getId());
                ResourceMongo saved = internalRepository.save(mapper.map(resource));
                log.debug("Update resource [{}] - Done", saved.getId());
                return mapper.map(saved);
            })
            .orElseThrow(() -> new IllegalStateException(String.format("No resource found with id [%s]", resource.getId())));
    }

    @Override
    public Optional<Resource> findById(String id) throws TechnicalException {
        log.debug("Find resource by id [{}]", id);
        try {
            return internalRepository.findById(id).map(mapper::map);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find resource [%s]".formatted(id), ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete resource [{}]", id);
        try {
            internalRepository.deleteById(id);
            log.debug("Delete resource [{}] - Done", id);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete resource [%s]".formatted(id), ex);
        }
    }

    @Override
    public Page<Resource> findByReference(Resource.ReferenceType referenceType, String referenceId, Pageable pageable, String query)
        throws TechnicalException {
        log.debug("Find resources by reference [{}/{}], query=[{}]", referenceType, referenceId, query);
        try {
            return internalRepository.search(referenceType.name(), referenceId, query, pageable).map(mapper::map);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find resources by reference [%s/%s]".formatted(referenceType, referenceId), ex);
        }
    }

    @Override
    public boolean existsByNameAndReference(String name, Resource.ReferenceType referenceType, String referenceId)
        throws TechnicalException {
        try {
            return internalRepository.existsByReferenceTypeAndReferenceIdAndName(referenceType.name(), referenceId, name);
        } catch (Exception ex) {
            throw new TechnicalException(
                "Failed to check resource existence [%s / %s / %s]".formatted(name, referenceType, referenceId),
                ex
            );
        }
    }
}
