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
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import io.gravitee.repository.mongodb.management.internal.model.PortalPageContextMongo;
import io.gravitee.repository.mongodb.management.internal.portalpagecontext.PortalPageContextMongoRepository;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class MongoPortalPageContextRepository implements PortalPageContextRepository {

    @Autowired
    private PortalPageContextMongoRepository internalRepo;

    @Override
    public Optional<PortalPageContext> findById(String id) throws TechnicalException {
        log.debug("Find PortalPageContext by id [{}]", id);
        Optional<PortalPageContext> portalPageContext = internalRepo.findById(id).map(this::map);
        log.debug("Find PortalPageContext by id [{}] - Done", id);
        return portalPageContext;
    }

    @Override
    public List<PortalPageContext> findAllByContextTypeAndEnvironmentId(PortalPageContextType contextType, String environmentId) {
        log.debug("Find all PortalPageContexts by contextType [{}] and environmentId [{}]", contextType, environmentId);
        Set<PortalPageContextMongo> mongoPortalPageContexts = internalRepo.findAllByContextTypeAndEnvironmentId(contextType, environmentId);
        List<PortalPageContext> portalPageContexts = mongoPortalPageContexts.stream().map(this::map).collect(Collectors.toList());
        log.debug(
            "Find all PortalPageContexts by contextType [{}] and environmentId [{}] - Done, found {} contexts",
            contextType,
            environmentId,
            portalPageContexts.size()
        );
        return portalPageContexts;
    }

    @Override
    public PortalPageContext findByPageId(String string) {
        log.debug("Find PortalPageContext by pageId [{}]", string);
        Optional<PortalPageContext> portalPageContext = internalRepo.findByPageId(string).map(this::map);
        log.debug("Find PortalPageContext by pageId [{}] - Done", string);
        return portalPageContext.orElse(null);
    }

    @Override
    public void updateByPageId(@Nonnull PortalPageContext item) throws TechnicalException {
        log.debug("Update PortalPageContext by pageId [{}]", item.getPageId());
        PortalPageContextMongo portalPageContextMongo = map(item);
        internalRepo.updateByPageId(portalPageContextMongo);
        log.debug("Update PortalPageContext by pageId [{}] - Done", item.getPageId());
    }

    @Override
    public PortalPageContext create(PortalPageContext portalPageContext) throws TechnicalException {
        log.debug("Create PortalPageContext [{}]", portalPageContext.getPageId());
        PortalPageContextMongo portalPageContextMongo = map(portalPageContext);
        PortalPageContextMongo createdPortalPageContextMongo = internalRepo.insert(portalPageContextMongo);
        PortalPageContext createdPortalPageContext = map(createdPortalPageContextMongo);
        log.debug("Create PortalPageContext [{}] - Done", createdPortalPageContext.getPageId());
        return createdPortalPageContext;
    }

    @Override
    public PortalPageContext update(PortalPageContext portalPageContext) throws TechnicalException {
        if (portalPageContext == null) {
            throw new IllegalStateException("PortalPageContext must not be null");
        }

        internalRepo
            .findById(portalPageContext.getId())
            .orElseThrow(() -> new TechnicalException("Unable to find PortalPageContext with id " + portalPageContext.getId()));

        log.debug("Update PortalPageContext [{}]", portalPageContext.getId());
        PortalPageContextMongo portalPageContextMongo = map(portalPageContext);
        PortalPageContextMongo updatedPortalPageContextMongo = internalRepo.save(portalPageContextMongo);
        PortalPageContext updatedPortalPageContext = map(updatedPortalPageContextMongo);
        log.debug("Update PortalPageContext [{}] - Done", updatedPortalPageContext.getId());
        return updatedPortalPageContext;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete PortalPageContext [{}]", id);
        internalRepo.deleteById(id);
        log.debug("Delete PortalPageContext [{}] - Done", id);
    }

    @Override
    public Set<PortalPageContext> findAll() throws TechnicalException {
        log.debug("Find all PortalPageContexts");
        Set<PortalPageContext> portalPageContexts = internalRepo.findAll().stream().map(this::map).collect(Collectors.toSet());
        log.debug("Find all PortalPageContexts - Done");
        return portalPageContexts;
    }

    private PortalPageContextMongo map(PortalPageContext portalPageContext) {
        PortalPageContextMongo portalPageContextMongo = new PortalPageContextMongo();

        portalPageContextMongo.setId(portalPageContext.getId());
        portalPageContextMongo.setPageId(portalPageContext.getPageId());
        portalPageContextMongo.setContextType(portalPageContext.getContextType());
        portalPageContextMongo.setEnvironmentId(portalPageContext.getEnvironmentId());
        portalPageContextMongo.setPublished(portalPageContext.isPublished());
        return portalPageContextMongo;
    }

    private PortalPageContext map(PortalPageContextMongo portalPageContextMongo) {
        return PortalPageContext.builder()
            .id(portalPageContextMongo.getId())
            .pageId(portalPageContextMongo.getPageId())
            .contextType(portalPageContextMongo.getContextType())
            .environmentId(portalPageContextMongo.getEnvironmentId())
            .published(portalPageContextMongo.isPublished())
            .build();
    }
}
