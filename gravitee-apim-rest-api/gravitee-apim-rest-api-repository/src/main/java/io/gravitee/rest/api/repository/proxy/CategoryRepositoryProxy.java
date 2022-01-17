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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Category;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class CategoryRepositoryProxy extends AbstractProxy<CategoryRepository> implements CategoryRepository {

    @Override
    public Optional<Category> findById(String s) throws TechnicalException {
        return target.findById(s);
    }

    @Override
    public Set<Category> findByEnvironmentIdAndIdIn(String environmentId, Set<String> ids) throws TechnicalException {
        return target.findByEnvironmentIdAndIdIn(environmentId, ids);
    }

    @Override
    public Optional<Category> findByKey(String s, String environment) throws TechnicalException {
        return target.findByKey(s, environment);
    }

    @Override
    public Set<Category> findByPage(String page) throws TechnicalException {
        return target.findByPage(page);
    }

    @Override
    public Category create(Category item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public Category update(Category item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public Set<Category> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    @Override
    public Set<Category> findAllByEnvironment(String environment) throws TechnicalException {
        return target.findAllByEnvironment(environment);
    }
}
