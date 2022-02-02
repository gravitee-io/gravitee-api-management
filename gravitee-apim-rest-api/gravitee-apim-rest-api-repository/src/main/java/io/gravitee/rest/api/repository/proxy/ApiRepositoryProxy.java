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
import io.gravitee.repository.management.api.ApiFieldInclusionFilter;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Api;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    public Page<Api> search(
        ApiCriteria apiCriteria,
        Sortable sortable,
        Pageable pageable,
        ApiFieldExclusionFilter apiFieldExclusionFilter
    ) {
        return target.search(apiCriteria, sortable, pageable, apiFieldExclusionFilter);
    }

    @Override
    public Page<Api> search(ApiCriteria apiCriteria, Pageable pageable) {
        return target.search(apiCriteria, pageable);
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria) {
        return target.search(apiCriteria);
    }

    @Override
    public List<Api> search(ApiCriteria apiCriteria, ApiFieldExclusionFilter apiFieldExclusionFilter) {
        return target.search(apiCriteria, apiFieldExclusionFilter);
    }

    @Override
    public List<String> searchIds(ApiCriteria... apiCriteria) {
        return target.searchIds(apiCriteria);
    }

    @Override
    public List<String> searchIds(Sortable sortable, ApiCriteria... apiCriteria) {
        return target.searchIds(sortable, apiCriteria);
    }

    @Override
    public Set<Api> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Set<Api> search(ApiCriteria apiCriteria, ApiFieldInclusionFilter apiFieldInclusionFilter) {
        return target.search(apiCriteria, apiFieldInclusionFilter);
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
