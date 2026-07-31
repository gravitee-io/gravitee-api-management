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
import io.gravitee.repository.management.api.AiWorkspaceComponentRepository;
import io.gravitee.repository.management.model.AiWorkspaceComponent;
import io.gravitee.repository.management.model.AiWorkspaceComponentType;
import io.gravitee.repository.mongodb.management.internal.aiworkspacecomponent.AiWorkspaceComponentMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.AiWorkspaceComponentMongo;
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
public class MongoAiWorkspaceComponentRepository implements AiWorkspaceComponentRepository {

    @Autowired
    private AiWorkspaceComponentMongoRepository internalRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public List<AiWorkspaceComponent> findByApiProductId(String apiProductId) throws TechnicalException {
        log.debug("MongoAiWorkspaceComponentRepository.findByApiProductId({})", apiProductId);
        try {
            return internalRepo.findByApiProductId(apiProductId).stream().map(mapper::map).toList();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find ai workspace components by api product id", ex);
        }
    }

    @Override
    public List<AiWorkspaceComponent> findByApiProductIdAndComponentType(String apiProductId, AiWorkspaceComponentType componentType)
        throws TechnicalException {
        log.debug("MongoAiWorkspaceComponentRepository.findByApiProductIdAndComponentType({}, {})", apiProductId, componentType);
        try {
            return internalRepo.findByApiProductIdAndComponentType(apiProductId, componentType).stream().map(mapper::map).toList();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find ai workspace components by api product id and type", ex);
        }
    }

    @Override
    public List<AiWorkspaceComponent> findByRefId(String refId) throws TechnicalException {
        log.debug("MongoAiWorkspaceComponentRepository.findByRefId({})", refId);
        try {
            return internalRepo.findByRefId(refId).stream().map(mapper::map).toList();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find ai workspace components by ref id", ex);
        }
    }

    @Override
    public void deleteByApiProductId(String apiProductId) throws TechnicalException {
        log.debug("MongoAiWorkspaceComponentRepository.deleteByApiProductId({})", apiProductId);
        try {
            internalRepo.deleteByApiProductId(apiProductId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete ai workspace components by api product id", ex);
        }
    }

    @Override
    public Optional<AiWorkspaceComponent> findById(String id) throws TechnicalException {
        log.debug("MongoAiWorkspaceComponentRepository.findById({})", id);
        try {
            return internalRepo.findById(id).map(mapper::map);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find ai workspace component by id", ex);
        }
    }

    @Override
    public AiWorkspaceComponent create(AiWorkspaceComponent component) throws TechnicalException {
        log.debug("MongoAiWorkspaceComponentRepository.create({})", component.getId());
        try {
            AiWorkspaceComponentMongo created = internalRepo.insert(mapper.map(component));
            return mapper.map(created);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create ai workspace component", ex);
        }
    }

    @Override
    public AiWorkspaceComponent update(AiWorkspaceComponent component) throws TechnicalException {
        log.debug("MongoAiWorkspaceComponentRepository.update({})", component.getId());
        try {
            if (component.getId() == null) {
                throw new IllegalArgumentException("AiWorkspaceComponent id is null");
            }
            internalRepo.findById(component.getId()).orElseThrow(() -> new TechnicalException("AiWorkspaceComponent not found"));
            AiWorkspaceComponentMongo updated = internalRepo.save(mapper.map(component));
            return mapper.map(updated);
        } catch (TechnicalException te) {
            throw te;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to update ai workspace component", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("MongoAiWorkspaceComponentRepository.delete({})", id);
        try {
            internalRepo.deleteById(id);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete ai workspace component", ex);
        }
    }

    @Override
    public Set<AiWorkspaceComponent> findAll() throws TechnicalException {
        log.debug("MongoAiWorkspaceComponentRepository.findAll()");
        try {
            return internalRepo.findAll().stream().map(mapper::map).collect(Collectors.toSet());
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find all ai workspace components", ex);
        }
    }
}
