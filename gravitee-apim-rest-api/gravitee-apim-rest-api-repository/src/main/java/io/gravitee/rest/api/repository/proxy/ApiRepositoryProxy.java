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
package io.gravitee.rest.api.repository.proxy;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Api;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiRepositoryProxy extends AbstractProxy<ApiRepository> implements ApiRepository {

    @Override
    public Api create(Api api) throws TechnicalException {
        return target.create(api);
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    @Override
    public Optional<Api> findById(String s) throws TechnicalException {
        return target.findById(s);
    }

    @Override
    public Api update(Api api) throws TechnicalException {
        return target.update(api);
    }

    @Override
    public Page<Api> search(ApiCriteria apiCriteria, Sortable sortable, Pageable pageable, ApiFieldFilter apiFieldFilter) {
        return target.search(apiCriteria, sortable, pageable, apiFieldFilter);
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria, ApiFieldFilter apiFieldFilter) {
        return target.search(apiCriteria, apiFieldFilter);
    }

    @Override
    public Stream<Api> search(ApiCriteria apiCriteria, Sortable sortable, ApiFieldFilter apiFieldFilter, int batchSize) {
        return target.search(apiCriteria, sortable, apiFieldFilter, batchSize);
    }

    @Override
    public Page<String> searchIds(List<ApiCriteria> apiCriteria, Pageable pageable, Sortable sortable) {
        return target.searchIds(apiCriteria, pageable, sortable);
    }

    @Override
    public Set<Api> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Set<String> listCategories(ApiCriteria apiCriteria) throws TechnicalException {
        return target.listCategories(apiCriteria);
    }

    @Override
    public Optional<Api> findByEnvironmentIdAndCrossId(String environmentId, String crossId) throws TechnicalException {
        return target.findByEnvironmentIdAndCrossId(environmentId, crossId);
    }
}
