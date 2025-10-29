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
import io.gravitee.repository.management.api.PortalPageContentRepository;
import io.gravitee.repository.management.model.PortalPageContent;
import io.gravitee.repository.mongodb.management.internal.model.PortalPageContentMongo;
import io.gravitee.repository.mongodb.management.internal.portalpagecontent.PortalPageContentMongoRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MongoPortalPageContentRepository implements PortalPageContentRepository {

    private final Logger logger = LoggerFactory.getLogger(MongoPortalPageContentRepository.class);

    @Autowired
    private PortalPageContentMongoRepository internalRepo;

    @Override
    public List<PortalPageContent> findAllByType(PortalPageContent.Type type) throws TechnicalException {
        try {
            Set<PortalPageContentMongo> results = internalRepo.findAllByType(type);
            return results.stream().map(this::map).collect(Collectors.toList());
        } catch (Exception ex) {
            logger.error("Failed to find portal page contents by type", ex);
            throw new TechnicalException("Failed to find portal page contents by type", ex);
        }
    }

    @Override
    public PortalPageContent findByPageId(String pageId) throws TechnicalException {
        try {
            Optional<PortalPageContentMongo> maybe = internalRepo.findByPageId(pageId);
            return maybe.map(this::map).orElse(null);
        } catch (Exception ex) {
            logger.error("Failed to find portal page content by pageId", ex);
            throw new TechnicalException("Failed to find portal page content by pageId", ex);
        }
    }

    @Override
    public java.util.Optional<PortalPageContent> findById(String id) throws TechnicalException {
        try {
            return internalRepo.findById(id).map(this::map);
        } catch (Exception ex) {
            logger.error("Failed to find portal page content by id", ex);
            throw new TechnicalException("Failed to find portal page content by id", ex);
        }
    }

    @Override
    public PortalPageContent create(PortalPageContent item) throws TechnicalException {
        try {
            PortalPageContentMongo created = internalRepo.insert(map(item));
            return map(created);
        } catch (Exception ex) {
            logger.error("Failed to create portal page content", ex);
            throw new TechnicalException("Failed to create portal page content", ex);
        }
    }

    @Override
    public PortalPageContent update(PortalPageContent item) throws TechnicalException {
        try {
            PortalPageContentMongo saved = internalRepo.save(map(item));
            return map(saved);
        } catch (Exception ex) {
            logger.error("Failed to update portal page content", ex);
            throw new TechnicalException("Failed to update portal page content", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalRepo.deleteById(id);
        } catch (Exception ex) {
            logger.error("Failed to delete portal page content [{}]", id, ex);
            throw new TechnicalException("Failed to delete portal page content", ex);
        }
    }

    @Override
    public void deleteByType(PortalPageContent.Type type) throws TechnicalException {
        try {
            internalRepo.deleteByType(type);
        } catch (Exception ex) {
            logger.error("Failed to delete portal page contents by type", ex);
            throw new TechnicalException("Failed to delete portal page contents by type", ex);
        }
    }

    @Override
    public java.util.Set<PortalPageContent> findAll() throws TechnicalException {
        try {
            java.util.List<PortalPageContentMongo> results = internalRepo.findAll();
            return results.stream().map(this::map).collect(Collectors.toSet());
        } catch (Exception ex) {
            logger.error("Failed to find all portal page contents", ex);
            throw new TechnicalException("Failed to find all portal page contents", ex);
        }
    }

    private PortalPageContent map(PortalPageContentMongo mongo) {
        PortalPageContent p = new PortalPageContent();
        p.setId(mongo.getId());
        p.setType(mongo.getType());
        p.setConfiguration(mongo.getConfiguration());
        p.setContent(mongo.getContent());
        return p;
    }

    private PortalPageContentMongo map(PortalPageContent item) {
        PortalPageContentMongo mongo = new PortalPageContentMongo();
        mongo.setId(item.getId());
        mongo.setPageId(null);
        mongo.setType(item.getType());
        mongo.setConfiguration(item.getConfiguration());
        mongo.setContent(item.getContent());
        return mongo;
    }
}
