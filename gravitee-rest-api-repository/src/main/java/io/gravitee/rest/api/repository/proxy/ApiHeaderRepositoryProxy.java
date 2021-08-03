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
import io.gravitee.repository.management.api.ApiHeaderRepository;
import io.gravitee.repository.management.model.ApiHeader;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiHeaderRepositoryProxy extends AbstractProxy<ApiHeaderRepository> implements ApiHeaderRepository {

    @Override
    public ApiHeader create(ApiHeader apiHeader) throws TechnicalException {
        return target.create(apiHeader);
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    @Override
    public Optional<ApiHeader> findById(String s) throws TechnicalException {
        return target.findById(s);
    }

    @Override
    public ApiHeader update(ApiHeader apiHeader) throws TechnicalException {
        return target.update(apiHeader);
    }

    @Override
    public Set<ApiHeader> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Set<ApiHeader> findAllByEnvironment(String environment) throws TechnicalException {
        return target.findAllByEnvironment(environment);
    }
}
