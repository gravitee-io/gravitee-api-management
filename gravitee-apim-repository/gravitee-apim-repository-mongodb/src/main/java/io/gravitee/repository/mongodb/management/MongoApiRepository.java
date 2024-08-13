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
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.mongodb.management.internal.api.ApiMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    public Set<Api> findAll() throws TechnicalException {
        return internalApiRepo.findAll().stream().map(this::mapApi).collect(Collectors.toSet());
    }

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
    public Page<Api> search(ApiCriteria apiCriteria, Sortable sortable, Pageable pageable, ApiFieldFilter apiFieldFilter) {
        final Page<ApiMongo> apisMongo = internalApiRepo.search(apiCriteria, sortable, pageable, apiFieldFilter);
        final List<Api> content = mapper.mapApis(apisMongo.getContent());
        return new Page<>(content, apisMongo.getPageNumber(), (int) apisMongo.getPageElements(), apisMongo.getTotalElements());
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria, ApiFieldFilter apiFieldFilter) {
        final Page<ApiMongo> apisMongo = internalApiRepo.search(apiCriteria, null, null, apiFieldFilter);
        return mapper.mapApis(apisMongo.getContent());
    }

    @Override
    public Stream<Api> search(ApiCriteria apiCriteria, Sortable sortable, ApiFieldFilter apiFieldFilter, int batchSize) {
        var pageable = new PageableBuilder().pageSize(batchSize).pageNumber(0).build();
        var page = search(apiCriteria, sortable, pageable, apiFieldFilter);
        if (page == null || page.getContent() == null) {
            return Stream.empty();
        }
        return Stream
            .iterate(
                page,
                p -> !isEmpty(p),
                p -> hasNext(p) ? search(apiCriteria, sortable, nextPageable(p, pageable), apiFieldFilter) : null
            )
            .flatMap(p -> {
                if (p != null && p.getContent() != null) {
                    return p.getContent().stream();
                }
                return Stream.empty();
            });
    }

    @Override
    public Page<String> searchIds(List<ApiCriteria> apiCriteria, Pageable pageable, Sortable sortable) {
        return internalApiRepo.searchIds(apiCriteria, pageable, sortable);
    }

    @Override
    public Map<String, Integer> listCategories(ApiCriteria apiCriteria) {
        return internalApiRepo.listCategories(apiCriteria);
    }

    @Override
    public Optional<Api> findByEnvironmentIdAndCrossId(String environmentId, String crossId) throws TechnicalException {
        return internalApiRepo.findByEnvironmentIdAndCrossId(environmentId, crossId).map(this::mapApi);
    }

    private ApiMongo mapApi(Api api) {
        return (api == null) ? null : mapper.map(api);
    }

    private Api mapApi(ApiMongo apiMongo) {
        return (apiMongo == null) ? null : mapper.map(apiMongo);
    }

    private boolean isEmpty(Page page) {
        return page == null || page.getContent() == null || page.getContent().isEmpty();
    }

    private boolean hasNext(Page page) {
        return (page.getPageNumber() + 1) * page.getPageElements() < page.getTotalElements();
    }

    private Pageable nextPageable(Page currentPage, Pageable pageable) {
        return new PageableBuilder().pageSize(pageable.pageSize()).pageNumber(currentPage.getPageNumber() + 1).build();
    }

    @Override
    public Optional<String> findIdByEnvironmentIdAndCrossId(final String environmentId, final String crossId) throws TechnicalException {
        return internalApiRepo.findIdByEnvironmentIdAndCrossId(environmentId, crossId).map(ApiMongo::getId);
    }

    @Override
    public boolean existById(final String appId) throws TechnicalException {
        return internalApiRepo.existsById(appId);
    }
}
