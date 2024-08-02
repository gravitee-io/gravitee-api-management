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
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.model.ApiCategoryOrder;
import io.gravitee.repository.mongodb.management.internal.api.ApiCategoryOrderMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiCategoryOrderPkMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoApiCategoryOrderRepository implements ApiCategoryOrderRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoApiCategoryOrderRepository.class);

    @Autowired
    private ApiCategoryOrderMongoRepository internalApiCategoryOrderRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public ApiCategoryOrder create(ApiCategoryOrder apiCategoryOrder) throws TechnicalException {
        LOGGER.debug("Create ApiCategoryOrder [{}]", apiCategoryOrder);

        var apiCategoryMongo = mapper.map(apiCategoryOrder);
        var createdApiCategoryMongo = internalApiCategoryOrderRepo.insert(apiCategoryMongo);

        LOGGER.debug("Create ApiCategoryOrder [{}] - Done", apiCategoryOrder);
        return mapper.map(createdApiCategoryMongo);
    }

    @Override
    public ApiCategoryOrder update(ApiCategoryOrder apiCategoryOrder) throws TechnicalException {
        LOGGER.debug("Update ApiCategoryOrder [{}]", apiCategoryOrder);

        if (Objects.isNull(apiCategoryOrder)) {
            throw new IllegalStateException("ApiCategoryOrder must not be null for update");
        }

        this.findById(apiCategoryOrder.getApiId(), apiCategoryOrder.getCategoryId())
            .orElseThrow(() ->
                new IllegalStateException(
                    String.format(
                        "No ApiCategoryOrder found with apiId [%s] and categoryId [%s]",
                        apiCategoryOrder.getApiId(),
                        apiCategoryOrder.getCategoryId()
                    )
                )
            );

        var apiCategoryMongo = mapper.map(apiCategoryOrder);
        var updatedApiCategoryMongo = internalApiCategoryOrderRepo.save(apiCategoryMongo);

        LOGGER.debug("Update ApiCategoryOrder [{}] - Done", apiCategoryOrder);
        return mapper.map(updatedApiCategoryMongo);
    }

    @Override
    public void delete(String apiId, Collection<String> categoriesIds) throws TechnicalException {
        try {
            var toDelete = categoriesIds
                .stream()
                .map(categoryId -> ApiCategoryOrderPkMongo.builder().categoryId(categoryId).apiId(apiId).build())
                .toList();
            internalApiCategoryOrderRepo.deleteAllById(toDelete);
        } catch (Exception e) {
            LOGGER.error("An error occurred when deleting ApiCategoryOrder [{}, {}]", apiId, categoriesIds, e);
            throw new TechnicalException("An error occurred when deleting ApiCategoryOrder", e);
        }
    }

    @Override
    public Optional<ApiCategoryOrder> findById(String apiId, String categoryId) {
        LOGGER.debug("Finding ApiCategoryOrder by ID [{}, {}]", apiId, categoryId);

        return internalApiCategoryOrderRepo
            .findById(ApiCategoryOrderPkMongo.builder().categoryId(categoryId).apiId(apiId).build())
            .map(apiCategoryOrderMongo -> mapper.map(apiCategoryOrderMongo));
    }

    @Override
    public Set<ApiCategoryOrder> findAllByCategoryId(String categoryId) {
        LOGGER.debug("Find all ApiCategoryOrder by category id [{}]", categoryId);

        if (Objects.isNull(categoryId)) {
            return Set.of();
        }

        var allByCategoryId = internalApiCategoryOrderRepo.findAllByCategoryId(categoryId);

        LOGGER.debug("Find all ApiCategoryOrder by category id [{}] -- Done", categoryId);

        return mapper.map(allByCategoryId);
    }

    @Override
    public Set<ApiCategoryOrder> findAllByApiId(String apiId) {
        LOGGER.debug("Find all ApiCategoryOrder by api id [{}]", apiId);

        if (Objects.isNull(apiId)) {
            return Set.of();
        }

        var allByApiId = internalApiCategoryOrderRepo.findAllByApiId(apiId);

        LOGGER.debug("Find all ApiCategoryOrder by api id [{}] -- Done", apiId);

        return mapper.map(allByApiId);
    }

    @Override
    public List<String> deleteByApiId(String apiId) throws TechnicalException {
        LOGGER.debug("Delete all by api id [{}]", apiId);
        try {
            final var allByApiId = internalApiCategoryOrderRepo
                .deleteByApiId(apiId)
                .stream()
                .map(apiCategoryOrderMongo -> apiCategoryOrderMongo.getId().getCategoryId())
                .toList();
            LOGGER.debug("Delete all by api id [{}] -- Done", apiId);
            return allByApiId;
        } catch (Exception ex) {
            LOGGER.error("Failed to delete api category order by apiId: {}", apiId, ex);
            throw new TechnicalException("Failed to delete api category order by apiId");
        }
    }

    @Override
    public Set<ApiCategoryOrder> findAll() throws TechnicalException {
        LOGGER.debug("Find all ApiCategoryOrder");

        var allEntries = internalApiCategoryOrderRepo.findAll();

        LOGGER.debug("Find all ApiCategoryOrder -- Done");

        return mapper.map(allEntries);
    }
}
