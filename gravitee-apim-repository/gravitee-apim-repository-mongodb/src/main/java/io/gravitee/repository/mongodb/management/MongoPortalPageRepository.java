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
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.ExpandsViewContext;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.mongodb.management.internal.model.PortalPageMongo;
import io.gravitee.repository.mongodb.management.internal.portalpage.PortalPageMongoRepository;
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
public class MongoPortalPageRepository implements PortalPageRepository {

    @Autowired
    private PortalPageMongoRepository internalRepo;

    @Override
    public Optional<PortalPage> findById(String id) throws TechnicalException {
        log.debug("Find PortalPage by id [{}]", id);
        Optional<PortalPage> mappedPortalPage = internalRepo.findById(id).map(this::map);
        log.debug("Find PortalPage by id [{}] - Done", id);
        return mappedPortalPage;
    }

    @Override
    public PortalPage create(PortalPage portalPage) throws TechnicalException {
        log.debug("Create PortalPage [{}]", portalPage.getId());
        PortalPageMongo portalPageMongo = map(portalPage);
        PortalPageMongo createdPortalPageMongo = internalRepo.insert(portalPageMongo);
        PortalPage createdPortalPage = map(createdPortalPageMongo);
        log.debug("Create PortalPage [{}] - Done", createdPortalPage.getId());
        return createdPortalPage;
    }

    @Override
    public PortalPage update(PortalPage portalPage) throws TechnicalException {
        if (portalPage == null) {
            throw new IllegalStateException("PortalPage must not be null");
        }

        internalRepo
            .findById(portalPage.getId())
            .orElseThrow(() -> new TechnicalException("Unable to find PortalPage with id " + portalPage.getId()));

        log.debug("Update PortalPage [{}]", portalPage.getId());
        PortalPageMongo updatedPortalPageMongo = internalRepo.save(map(portalPage));
        PortalPage updatedPortalPage = map(updatedPortalPageMongo);
        log.debug("Update PortalPage [{}] - Done", updatedPortalPage.getId());
        return updatedPortalPage;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete PortalPage [{}]", id);
        internalRepo.deleteById(id);
        log.debug("Delete PortalPage [{}] - Done", id);
    }

    @Override
    public Set<PortalPage> findAll() throws TechnicalException {
        log.debug("Find all PortalPages");
        Set<PortalPage> portalPages = internalRepo.findAll().stream().map(this::map).collect(Collectors.toSet());
        log.debug("Find all PortalPages - Done");
        return portalPages;
    }

    private PortalPageMongo map(PortalPage portalPage) {
        PortalPageMongo portalPageMongo = new PortalPageMongo();
        portalPageMongo.setId(portalPage.getId());
        portalPageMongo.setEnvironmentId(portalPage.getEnvironmentId());
        portalPageMongo.setName(portalPage.getName());
        portalPageMongo.setContent(portalPage.getContent());
        portalPageMongo.setCreatedAt(portalPage.getCreatedAt());
        portalPageMongo.setUpdatedAt(portalPage.getUpdatedAt());
        return portalPageMongo;
    }

    private PortalPage map(PortalPageMongo portalPageMongo) {
        return PortalPage.builder()
            .id(portalPageMongo.getId())
            .environmentId(portalPageMongo.getEnvironmentId())
            .name(portalPageMongo.getName())
            .content(portalPageMongo.getContent())
            .createdAt(portalPageMongo.getCreatedAt())
            .updatedAt(portalPageMongo.getUpdatedAt())
            .build();
    }

    @Override
    public List<PortalPage> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<PortalPageMongo> portalPages = internalRepo.findAllById(ids);
        return portalPages.stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public List<PortalPage> findByIdsWithExpand(List<String> ids, List<ExpandsViewContext> expands) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        var projections = internalRepo.findPortalPagesByIdWithExpand(ids, expands);
        return projections
            .stream()
            .map(p -> {
                var builder = PortalPage.builder()
                    .id(p.getId())
                    .environmentId(p.getEnvironmentId())
                    .name(p.getName())
                    .createdAt(p.getCreatedAt())
                    .updatedAt(p.getUpdatedAt())
                    .content(p.getContent());
                return builder.build();
            })
            .collect(Collectors.toList());
    }
}
