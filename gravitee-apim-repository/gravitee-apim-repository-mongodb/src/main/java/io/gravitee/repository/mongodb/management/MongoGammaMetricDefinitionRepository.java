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

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GammaMetricDefinitionRepository;
import io.gravitee.repository.management.model.GammaMetricDefinition;
import io.gravitee.repository.mongodb.management.internal.GammaMetricDefinitionMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Component
@RequiredArgsConstructor
public class MongoGammaMetricDefinitionRepository implements GammaMetricDefinitionRepository {

    private final GammaMetricDefinitionMongoRepository internalRepository;
    private final GraviteeMapper mapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public Set<GammaMetricDefinition> findAll() throws TechnicalException {
        return mapper.mapGammaMetricDefinitions(internalRepository.findAll());
    }

    @Override
    public Optional<GammaMetricDefinition> findById(String id) throws TechnicalException {
        return internalRepository.findById(id).map(mapper::map);
    }

    @Override
    public GammaMetricDefinition create(GammaMetricDefinition item) throws TechnicalException {
        log.debug("Create gamma metric definition [{}]", item.getRawKey());
        try {
            return mapper.map(internalRepository.insert(mapper.map(item)));
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new DuplicateKeyException(
                "Gamma metric definition [" + item.getRawKey() + "] already exists in environment [" + item.getEnvironmentId() + "]",
                e
            );
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when creating gamma metric definition " + item.getRawKey(), e);
        }
    }

    /**
     * A conditional replace on {@code _id} rather than {@code save()}, which would upsert: a record deleted meanwhile
     * is reported, as the JDBC implementation does through its affected-row count, instead of being re-inserted.
     */
    @Override
    public GammaMetricDefinition update(GammaMetricDefinition item) throws TechnicalException {
        if (item == null) {
            throw new IllegalStateException("Unable to update a null gamma metric definition");
        }
        GammaMetricDefinition replaced;
        try {
            replaced = Optional.ofNullable(
                mongoTemplate.findAndReplace(
                    Query.query(Criteria.where("_id").is(item.getId())),
                    mapper.map(item),
                    FindAndReplaceOptions.options().returnNew()
                )
            )
                .map(mapper::map)
                .orElse(null);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when updating gamma metric definition " + item.getId(), e);
        }
        if (replaced == null) {
            throw new IllegalStateException(String.format("No gamma metric definition found with id [%s]", item.getId()));
        }
        return replaced;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalRepository.deleteById(id);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when deleting gamma metric definition " + id, e);
        }
    }

    @Override
    public List<GammaMetricDefinition> findByEnvironmentId(String environmentId) throws TechnicalException {
        return internalRepository.findByEnvironmentIdOrderByRawKeyAsc(environmentId).stream().map(mapper::map).toList();
    }

    @Override
    public Optional<GammaMetricDefinition> findByEnvironmentIdAndRawKey(String environmentId, String rawKey) throws TechnicalException {
        return internalRepository.findByEnvironmentIdAndRawKey(environmentId, rawKey).map(mapper::map);
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        try {
            internalRepository.deleteByEnvironmentId(environmentId);
        } catch (Exception e) {
            throw new TechnicalException("An error occurred when deleting gamma metric definitions by environment id " + environmentId, e);
        }
    }
}
