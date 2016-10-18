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
package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Visibility;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiRepositoryProxy extends AbstractProxy<ApiRepository> implements ApiRepository {

    @Override
    public Set<Api> findAll() throws TechnicalException {
        return target.findAll();
    }

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
    public Set<Api> findByVisibility(Visibility visibility) throws TechnicalException {
        return target.findByVisibility(visibility);
    }

    @Override
    public Set<Api> findByIds(List<String> ids) throws TechnicalException {
        return target.findByIds(ids);
    }

    @Override
    public Set<Api> findByGroups(List<String> groupIds) throws TechnicalException {
        return target.findByGroups(groupIds);
    }
}
