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
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiKeyRepositoryProxy extends AbstractProxy<ApiKeyRepository> implements ApiKeyRepository {

    public Optional<ApiKey> findById(String apiKey) throws TechnicalException {
        return target.findById(apiKey);
    }

    public ApiKey create(ApiKey apiKey) throws TechnicalException {
        return target.create(apiKey);
    }

    public ApiKey update(ApiKey key) throws TechnicalException {
        return target.update(key);
    }

    public Set<ApiKey> findBySubscription(String subscription) throws TechnicalException {
        return target.findBySubscription(subscription);
    }

    @Override
    public Set<ApiKey> findByPlan(String plan) throws TechnicalException {
        return target.findByPlan(plan);
    }

    @Override
    public List<ApiKey> findByCriteria(ApiKeyCriteria filter) throws TechnicalException {
        return target.findByCriteria(filter);
    }
}
