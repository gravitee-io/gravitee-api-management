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
package io.gravitee.repository.bridge.client.management;

import io.gravitee.repository.bridge.client.utils.BodyCodecs;
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
public class HttpApiKeyRepository extends AbstractRepository implements ApiKeyRepository {

    @Override
    public Optional<ApiKey> findById(String id) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public List<ApiKey> findByKey(String apiKey) {
        throw new IllegalStateException();
    }

    @Override
    public Optional<ApiKey> findByKeyAndApi(String key, String api) throws TechnicalException {
        return blockingGet(get(String.format("/apis/%s/keys/%s", api, key), BodyCodecs.optional(ApiKey.class)).send()).payload();
    }

    @Override
    public ApiKey create(ApiKey apiKey) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public ApiKey update(ApiKey key) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<ApiKey> findBySubscription(String subscription) {
        throw new IllegalStateException();
    }

    @Override
    public Set<ApiKey> findByPlan(String plan) {
        throw new IllegalStateException();
    }

    @Override
    public List<ApiKey> findByCriteria(ApiKeyCriteria filter) throws TechnicalException {
        return blockingGet(post("/keys/_search", BodyCodecs.list(ApiKey.class)).send(filter)).payload();
    }

    @Override
    public Set<ApiKey> findAll() throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public List<ApiKey> findByApplication(String applicationId) throws TechnicalException {
        throw new IllegalStateException();
    }
}
