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
import io.gravitee.repository.management.api.CatalogItemRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.catalog.CatalogItem;
import io.gravitee.repository.management.model.catalog.Model;
import io.gravitee.repository.mongodb.management.internal.catalog.CatalogItemMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.CatalogItemMongo;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@RequiredArgsConstructor
public class MongoCatalogItemRepository implements CatalogItemRepository {

    private static final Map<Class<? extends CatalogItem>, String> KIND_BY_TYPE = Map.of(Model.class, "model");

    private final CatalogItemMongoRepository internalRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public CatalogItem create(CatalogItem item) throws TechnicalException {
        log.debug("Create catalog item [{}]", item.id());
        var created = internalRepository.insert(toMongo(item));
        log.debug("Create catalog item [{}] - Done", created.getId());
        return fromMongo(created);
    }

    @Override
    public Optional<CatalogItem> findById(String id) throws TechnicalException {
        log.debug("Find catalog item by id [{}]", id);
        var result = internalRepository.findById(id).map(this::fromMongo);
        log.debug("Find catalog item by id [{}] - Done", id);
        return result;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete catalog item [{}]", id);
        internalRepository.deleteById(id);
        log.debug("Delete catalog item [{}] - Done", id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CatalogItem> Page<T> findByType(Class<T> type, Pageable pageable) throws TechnicalException {
        log.debug("Find catalog items by type [{}]", type.getSimpleName());
        var kind = KIND_BY_TYPE.get(type);
        if (kind == null) {
            throw new TechnicalException("Unknown catalog item type: " + type.getSimpleName());
        }

        var query = new Query();
        query.addCriteria(Criteria.where("kind").is(kind));
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));

        long total = mongoTemplate.count(query, CatalogItemMongo.class);

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<T> content = mongoTemplate
            .find(query, CatalogItemMongo.class)
            .stream()
            .map(m -> (T) fromMongo(m))
            .toList();

        log.debug("Find catalog items by type [{}] - Done", type.getSimpleName());
        return new Page<>(content, pageable != null ? pageable.pageNumber() : 0, content.size(), total);
    }

    @Override
    public List<CatalogItem> findBySourceId(String sourceId) throws TechnicalException {
        log.debug("Find catalog items by sourceId [{}]", sourceId);
        var result = internalRepository.findBySourceId(sourceId).stream().map(this::fromMongo).toList();
        log.debug("Find catalog items by sourceId [{}] - Done", sourceId);
        return result;
    }

    @Override
    public Page<CatalogItem> findAll(Pageable pageable) throws TechnicalException {
        log.debug("Find all catalog items");
        var query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));

        long total = mongoTemplate.count(query, CatalogItemMongo.class);

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<CatalogItem> content = mongoTemplate.find(query, CatalogItemMongo.class).stream().map(this::fromMongo).toList();

        log.debug("Find all catalog items - Done");
        return new Page<>(content, pageable != null ? pageable.pageNumber() : 0, content.size(), total);
    }

    private CatalogItemMongo toMongo(CatalogItem item) {
        var mongo = new CatalogItemMongo();
        mongo.setId(item.id());
        mongo.setSourceId(item.sourceId());
        mongo.setCreatedAt(Date.from(item.createdAt()));
        mongo.setKind(KIND_BY_TYPE.get(item.getClass()));
        mongo.setDefinition(toDefinition(item));
        return mongo;
    }

    private Document toDefinition(CatalogItem item) {
        return switch (item) {
            case Model m -> new Document("name", m.name()).append("description", m.description());
        };
    }

    private CatalogItem fromMongo(CatalogItemMongo mongo) {
        return switch (mongo.getKind()) {
            case "model" -> new Model(
                mongo.getId(),
                mongo.getSourceId(),
                mongo.getCreatedAt().toInstant(),
                mongo.getDefinition().getString("name"),
                mongo.getDefinition().getString("description")
            );
            default -> throw new IllegalStateException("Unknown catalog item kind: " + mongo.getKind());
        };
    }
}
