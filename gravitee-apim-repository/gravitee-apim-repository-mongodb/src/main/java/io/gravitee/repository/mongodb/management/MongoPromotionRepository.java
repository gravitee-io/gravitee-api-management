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
import io.gravitee.repository.management.api.PromotionRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.PromotionCriteria;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Promotion;
import io.gravitee.repository.mongodb.management.internal.model.PromotionMongo;
import io.gravitee.repository.mongodb.management.internal.promotion.PromotionMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MongoPromotionRepository implements PromotionRepository {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private PromotionMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Promotion> findById(String id) throws TechnicalException {
        logger.debug("Find promotion by ID [{}]", id);

        return internalRepository
            .findById(id)
            .map(promotionMongo -> {
                logger.debug("Find promotion by ID [{}] - Done", id);
                return map(promotionMongo);
            });
    }

    @Override
    public Promotion create(Promotion promotion) throws TechnicalException {
        logger.debug("Create promotion [{}]", promotion.getId());
        Promotion createdPromotion = map(internalRepository.insert(map(promotion)));
        logger.debug("Create promotion [{}] - Done", createdPromotion.getId());
        return createdPromotion;
    }

    @Override
    public Promotion update(Promotion promotion) throws TechnicalException {
        if (promotion == null) {
            throw new IllegalStateException("Promotion must not be null");
        }

        return internalRepository
            .findById(promotion.getId())
            .map(existingPromotion -> {
                logger.debug("Update promotion [{}]", promotion.getId());
                return internalRepository.save(map(promotion));
            })
            .map(this::map)
            .orElseThrow(() -> new IllegalStateException(String.format("No promotion found with id [%s]", promotion.getId())));
    }

    @Override
    public void delete(String id) throws TechnicalException {
        logger.debug("Delete promotion [{}]", id);
        internalRepository.deleteById(id);
        logger.debug("Delete promotion [{}] - Done", id);
    }

    private PromotionMongo map(Promotion promotion) {
        return mapper.map(promotion);
    }

    private Promotion map(PromotionMongo promotion) {
        return mapper.map(promotion);
    }

    @Override
    public Page<Promotion> search(PromotionCriteria criteria, Sortable sortable, Pageable pageable) {
        logger.debug("Searching promotions");

        var promotions = internalRepository.search(criteria, sortable, pageable).map(mapper::map);

        logger.debug("Searching promotions - Done");
        return promotions;
    }

    @Override
    public Set<Promotion> findAll() throws TechnicalException {
        return internalRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
    }
}
