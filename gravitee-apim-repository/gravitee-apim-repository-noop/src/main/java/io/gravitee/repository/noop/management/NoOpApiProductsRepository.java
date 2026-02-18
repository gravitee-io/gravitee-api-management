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
package io.gravitee.repository.noop.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.repository.management.model.ApiProduct;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * @author GraviteeSource Team
 */
public class NoOpApiProductsRepository implements ApiProductsRepository {

    @Override
    public Optional<ApiProduct> findById(String id) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public ApiProduct create(ApiProduct item) throws TechnicalException {
        return null;
    }

    @Override
    public ApiProduct update(ApiProduct item) throws TechnicalException {
        return null;
    }

    @Override
    public void delete(String id) throws TechnicalException {}

    @Override
    public Set<ApiProduct> findAll() throws TechnicalException {
        return Set.of();
    }

    @Override
    public Optional<ApiProduct> findByEnvironmentIdAndName(String environmentId, String name) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public Set<ApiProduct> findByEnvironmentId(String environmentId) throws TechnicalException {
        return Set.of();
    }

    @Override
    public Set<ApiProduct> findByApiId(String apiId) throws TechnicalException {
        return Set.of();
    }

    @Override
    public Set<ApiProduct> findByIds(Collection<String> ids) throws TechnicalException {
        return Set.of();
    }

    @Override
    public Set<ApiProduct> findApiProductsByApiIds(Collection<String> apiIds) throws TechnicalException {
        return Set.of();
    }
}
