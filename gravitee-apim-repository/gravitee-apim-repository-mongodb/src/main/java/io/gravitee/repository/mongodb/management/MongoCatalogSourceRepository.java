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
import io.gravitee.repository.management.api.CatalogSourceRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.catalog.CatalogSource;
import io.gravitee.repository.management.model.catalog.LlmProvider;
import io.gravitee.repository.mongodb.management.internal.catalog.CatalogSourceMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.CatalogSourceMongo;
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
public class MongoCatalogSourceRepository implements CatalogSourceRepository {

    private static final Map<Class<? extends CatalogSource>, String> KIND_BY_TYPE = Map.of(LlmProvider.class, "llm-provider");

    private static final Map<String, Class<? extends CatalogSource>> TYPE_BY_KIND = Map.of("llm-provider", LlmProvider.class);

    private final CatalogSourceMongoRepository internalRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public CatalogSource create(CatalogSource source) throws TechnicalException {
        log.debug("Create catalog source [{}]", source.id());
        var created = internalRepository.insert(toMongo(source));
        log.debug("Create catalog source [{}] - Done", created.getId());
        return fromMongo(created);
    }

    @Override
    public Optional<CatalogSource> findById(String id) throws TechnicalException {
        log.debug("Find catalog source by id [{}]", id);
        var result = internalRepository.findById(id).map(this::fromMongo);
        log.debug("Find catalog source by id [{}] - Done", id);
        return result;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete catalog source [{}]", id);
        internalRepository.deleteById(id);
        log.debug("Delete catalog source [{}] - Done", id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CatalogSource> Page<T> findByType(Class<T> type, Pageable pageable) throws TechnicalException {
        log.debug("Find catalog sources by type [{}]", type.getSimpleName());
        var kind = KIND_BY_TYPE.get(type);
        if (kind == null) {
            throw new TechnicalException("Unknown catalog source type: " + type.getSimpleName());
        }

        var query = new Query();
        query.addCriteria(Criteria.where("kind").is(kind));
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));

        long total = mongoTemplate.count(query, CatalogSourceMongo.class);

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<T> content = mongoTemplate
            .find(query, CatalogSourceMongo.class)
            .stream()
            .map(m -> (T) fromMongo(m))
            .toList();

        log.debug("Find catalog sources by type [{}] - Done", type.getSimpleName());
        return new Page<>(content, pageable != null ? pageable.pageNumber() : 0, content.size(), total);
    }

    @Override
    public Page<CatalogSource> findAll(Pageable pageable) throws TechnicalException {
        log.debug("Find all catalog sources");
        var query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));

        long total = mongoTemplate.count(query, CatalogSourceMongo.class);

        if (pageable != null) {
            query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize()));
        }

        List<CatalogSource> content = mongoTemplate.find(query, CatalogSourceMongo.class).stream().map(this::fromMongo).toList();

        log.debug("Find all catalog sources - Done");
        return new Page<>(content, pageable != null ? pageable.pageNumber() : 0, content.size(), total);
    }

    private CatalogSourceMongo toMongo(CatalogSource source) {
        var mongo = new CatalogSourceMongo();
        mongo.setId(source.id());
        mongo.setCreatedAt(Date.from(source.createdAt()));
        mongo.setKind(KIND_BY_TYPE.get(source.getClass()));
        mongo.setDefinition(toDefinition(source));
        return mongo;
    }

    private Document toDefinition(CatalogSource source) {
        return switch (source) {
            case LlmProvider p -> new Document("name", p.name()).append("apiKey", p.apiKey());
        };
    }

    private CatalogSource fromMongo(CatalogSourceMongo mongo) {
        return switch (mongo.getKind()) {
            case "llm-provider" -> new LlmProvider(
                mongo.getId(),
                mongo.getCreatedAt().toInstant(),
                mongo.getDefinition().getString("name"),
                mongo.getDefinition().getString("apiKey")
            );
            default -> throw new IllegalStateException("Unknown catalog source kind: " + mongo.getKind());
        };
    }
}
