/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.repository.management.api.ApiFieldInclusionFilter;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.mongodb.management.internal.api.ApiMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoApiRepository implements ApiRepository {

    @Autowired
    private ApiMongoRepository internalApiRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Api> findById(String apiId) throws TechnicalException {
        ApiMongo apiMongo = internalApiRepo.findById(apiId).orElse(null);
        return Optional.ofNullable(mapApi(apiMongo));
    }

    @Override
    public Api create(Api api) throws TechnicalException {
        ApiMongo apiMongo = mapApi(api);
        ApiMongo apiMongoCreated = internalApiRepo.insert(apiMongo);
        return mapApi(apiMongoCreated);
    }

    @Override
    public Api update(Api api) throws TechnicalException {
        if (api == null || api.getId() == null) {
            throw new IllegalStateException("Api to update must have an id");
        }

        final ApiMongo apiMongo = internalApiRepo.findById(api.getId()).orElse(null);
        if (apiMongo == null) {
            throw new IllegalStateException(String.format("No api found with id [%s]", api.getId()));
        }

        final ApiMongo apiMongoUpdated = internalApiRepo.save(mapApi(api));
        return mapApi(apiMongoUpdated);
    }

    @Override
    public void delete(String apiId) throws TechnicalException {
        internalApiRepo.deleteById(apiId);
    }

    @Override
    public Page<Api> search(
        ApiCriteria apiCriteria,
        Sortable sortable,
        Pageable pageable,
        ApiFieldExclusionFilter apiFieldExclusionFilter
    ) {
        final Page<ApiMongo> apisMongo = internalApiRepo.search(apiCriteria, sortable, pageable, apiFieldExclusionFilter);
        final List<Api> content = mapper.collection2list(apisMongo.getContent(), ApiMongo.class, Api.class);
        return new Page<>(content, apisMongo.getPageNumber(), (int) apisMongo.getPageElements(), apisMongo.getTotalElements());
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria) {
        return findByCriteria(apiCriteria, null);
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria, ApiFieldExclusionFilter apiFieldExclusionFilter) {
        return findByCriteria(apiCriteria, apiFieldExclusionFilter);
    }

    @Override
    public List<String> searchIds(ApiCriteria... apiCriteria) {
        return internalApiRepo.searchIds(null, apiCriteria);
    }

    @Override
    public List<String> searchIds(Sortable sortable, ApiCriteria... apiCriteria) {
        return internalApiRepo.searchIds(sortable, apiCriteria);
    }

    @Override
    public Set<String> listCategories(ApiCriteria apiCriteria) {
        return internalApiRepo.listCategories(apiCriteria);
    }

    private List<Api> findByCriteria(ApiCriteria apiCriteria, ApiFieldExclusionFilter apiFieldExclusionFilter) {
        final Page<ApiMongo> apisMongo = internalApiRepo.search(apiCriteria, null, null, apiFieldExclusionFilter);
        return mapper.collection2list(apisMongo.getContent(), ApiMongo.class, Api.class);
    }

    private ApiMongo mapApi(Api api) {
        return (api == null) ? null : mapper.map(api, ApiMongo.class);
    }

    private Api mapApi(ApiMongo apiMongo) {
        return (apiMongo == null) ? null : mapper.map(apiMongo, Api.class);
    }

    @Override
    public Set<Api> findAll() throws TechnicalException {
        return internalApiRepo.findAll().stream().map(this::mapApi).collect(Collectors.toSet());
    }

    @Override
    public Set<Api> search(ApiCriteria criteria, ApiFieldInclusionFilter apiFieldInclusionFilter) {
        return internalApiRepo.search(criteria, apiFieldInclusionFilter).stream().map(this::mapApi).collect(Collectors.toSet());
    }

    @Override
    public Optional<Api> findByEnvironmentIdAndCrossId(String environmentId, String crossId) throws TechnicalException {
        return internalApiRepo.findByEnvironmentIdAndCrossId(environmentId, crossId).map(this::mapApi);
    }
}
