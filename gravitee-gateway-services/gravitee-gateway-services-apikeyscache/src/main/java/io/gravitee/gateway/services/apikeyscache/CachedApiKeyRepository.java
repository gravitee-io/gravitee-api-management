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
package io.gravitee.gateway.services.apikeyscache;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class CachedApiKeyRepository implements ApiKeyRepository {

    private final Cache cache;

    public CachedApiKeyRepository(Cache cache) {
        this.cache = cache;
    }

    @Override
    public ApiKey create(String s, String s1, ApiKey apiKey) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<ApiKey> findByApi(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<ApiKey> findByApplication(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<ApiKey> findByApplicationAndApi(String s, String s1) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Optional<ApiKey> retrieve(String apiKey) throws TechnicalException {
        Element elt = cache.get(apiKey);
        if (elt != null) {
            return Optional.of((ApiKey) elt.getObjectValue());
        }

        return Optional.empty();
    }

    @Override
    public ApiKey update(ApiKey apiKey) throws TechnicalException {
        throw new IllegalStateException();
    }
}
