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
import io.gravitee.repository.management.api.ApiCategoryRepository;
import io.gravitee.repository.management.model.ApiCategory;
import io.gravitee.repository.mongodb.management.internal.api.ApiCategoryMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiCategoryMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MongoApiCategoryRepository implements ApiCategoryRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoApiCategoryRepository.class);

    @Autowired
    private ApiCategoryMongoRepository internalApiCategoryRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Set<ApiCategory> findAll() throws TechnicalException {
        LOGGER.debug("Find all api categories");

        var allApiCategories = this.internalApiCategoryRepo.findAll();

        LOGGER.debug("Find all api categories - Done");
        return mapper.map(allApiCategories);
    }

    @Override
    public Optional<ApiCategory> findById(ApiCategory.Id id) throws TechnicalException {
        LOGGER.debug("Find api category by ID [{}]", id);

        var foundApiCategory = this.internalApiCategoryRepo.findById(mapper.map(id)).orElse(null);

        LOGGER.debug("Find api category by ID [{}] - Done", id);
        return Optional.ofNullable(foundApiCategory).map(apiCategoryMongo -> mapper.map(apiCategoryMongo));
    }

    @Override
    public ApiCategory create(ApiCategory apiCategory) throws TechnicalException {
        LOGGER.debug("Create api category [{}]", apiCategory);

        ApiCategoryMongo apiCategoryMongo = mapper.map(apiCategory);
        ApiCategoryMongo createdApiCategoryMongo = this.internalApiCategoryRepo.insert(apiCategoryMongo);

        LOGGER.debug("Create api category [{}] - Done", apiCategory);

        return mapper.map(createdApiCategoryMongo);
    }

    @Override
    public ApiCategory update(ApiCategory apiCategory) throws TechnicalException {
        LOGGER.debug("Update api category [{}]", apiCategory);

        internalApiCategoryRepo
            .findById(mapper.map(apiCategory.getId()))
            .orElseThrow(() ->
                new IllegalStateException(String.format("No api category found with name [%s]", apiCategory.getId().toString()))
            );

        try {
            var updatedApiCategory = internalApiCategoryRepo.save(mapper.map(apiCategory));
            LOGGER.debug("Update api category [{}] - Done", apiCategory);
            return mapper.map(updatedApiCategory);
        } catch (Exception e) {
            LOGGER.error("An error occurred when updating api category", e);
            throw new TechnicalException("An error occurred when updating api category");
        }
    }

    @Override
    public void delete(ApiCategory.Id id) throws TechnicalException {
        try {
            internalApiCategoryRepo.deleteById(mapper.map(id));
        } catch (Exception e) {
            LOGGER.error("An error occurred when deleting api category [{}]", id, e);
            throw new TechnicalException("An error occurred when deleting api category");
        }
    }
}
