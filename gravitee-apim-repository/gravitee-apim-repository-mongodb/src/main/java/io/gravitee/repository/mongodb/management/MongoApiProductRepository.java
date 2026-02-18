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

import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.repository.mongodb.management.internal.apiproducts.ApiProductsMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiProductMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class MongoApiProductRepository implements ApiProductsRepository {

    @Autowired
    private ApiProductsMongoRepository internalApiProductRepo;

    @Autowired
    private GraviteeMapper mapper;

    public MongoApiProductRepository() {
        log.info("MongoApiProductRepository constructor called - bean created!");
    }

    @Override
    public Optional<ApiProduct> findByEnvironmentIdAndName(String environmentId, String name) throws TechnicalException {
        log.debug("MongoApiProductRepository.findByEnvironmentIdAndName({} , {})", environmentId, name);

        try {
            Optional<ApiProductMongo> apiProductMongo = internalApiProductRepo.findByEnvironmentIdAndName(environmentId, name);
            return apiProductMongo.map(mapper::map);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find api product by environment and name", ex);
        }
    }

    @Override
    public Set<ApiProduct> findByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("MongoApiProductRepository.findByEnvironmentId({})", environmentId);
        try {
            return internalApiProductRepo.findByEnvironmentId(environmentId).stream().map(mapper::map).collect(Collectors.toSet());
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find api products by environment", ex);
        }
    }

    @Override
    public Optional<ApiProduct> findById(String id) throws TechnicalException {
        log.debug("MongoApiProductRepository.findById({})", id);
        try {
            Optional<ApiProductMongo> apiProductMongo = internalApiProductRepo.findById(id);
            return apiProductMongo.map(mapper::map);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find api product by id", ex);
        }
    }

    @Override
    public ApiProduct create(ApiProduct apiProduct) throws TechnicalException {
        ApiProductMongo apiProductMongo = mapper.map(apiProduct);
        ApiProductMongo createdMongo = internalApiProductRepo.insert(apiProductMongo);
        return mapper.map(createdMongo);
    }

    @Override
    public ApiProduct update(ApiProduct apiProduct) throws TechnicalException {
        log.debug("MongoApiProductRepository.update({})", apiProduct.getId());
        try {
            if (apiProduct.getId() == null) {
                throw new IllegalArgumentException("ApiProduct id is null");
            }

            internalApiProductRepo.findById(apiProduct.getId()).orElseThrow(() -> new TechnicalException("ApiProduct not found"));

            ApiProductMongo apiProductMongo = mapper.map(apiProduct);
            ApiProductMongo updatedMongo = internalApiProductRepo.save(apiProductMongo);
            return mapper.map(updatedMongo);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to update api product", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalApiProductRepo.deleteById(id);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete api product", ex);
        }
    }

    @Override
    public Set<ApiProduct> findAll() throws TechnicalException {
        log.debug("MongoApiProductRepository.findAll()");
        try {
            return internalApiProductRepo.findAll().stream().map(mapper::map).collect(Collectors.toSet());
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find all api products", ex);
        }
    }

    @Override
    public Set<ApiProduct> findByApiId(String apiId) throws TechnicalException {
        log.debug("MongoApiProductRepository.findByApiId({})", apiId);
        try {
            Set<ApiProductMongo> apiProductMongos = internalApiProductRepo.findByApiId(apiId);
            return apiProductMongos.stream().map(mapper::map).collect(Collectors.toSet());
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find api products by api id", ex);
        }
    }

    @Override
    public Set<ApiProduct> findByIds(Collection<String> ids) throws TechnicalException {
        if (isEmpty(ids)) {
            return Set.of();
        }
        log.debug("MongoApiProductRepository.findByIds({})", ids);
        try {
            return internalApiProductRepo.findAllById(ids).stream().map(mapper::map).collect(Collectors.toSet());
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find api products by ids", ex);
        }
    }

    @Override
    public Set<ApiProduct> findApiProductsByApiIds(Collection<String> apiIds) throws TechnicalException {
        if (isEmpty(apiIds)) {
            return Set.of();
        }
        log.debug("MongoApiProductRepository.findApiProductsByApiIds({})", apiIds);
        try {
            Set<ApiProductMongo> apiProductMongos = internalApiProductRepo.findApiProductsByApiIds(apiIds);
            return apiProductMongos.stream().map(mapper::map).collect(Collectors.toSet());
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find api products by api ids", ex);
        }
    }
}
