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
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Component
public class ApiKeyRepositoryProxy extends AbstractProxy<ApiKeyRepository> implements ApiKeyRepository {

    public ApiKey create(String s, String s1, ApiKey apiKey) throws TechnicalException {
        return target.create(s, s1, apiKey);
    }

    public Set<ApiKey> findByApi(String s) throws TechnicalException {
        return target.findByApi(s);
    }

    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    public Set<ApiKey> findByApplication(String s) throws TechnicalException {
        return target.findByApplication(s);
    }

    public Set<ApiKey> findByApplicationAndApi(String s, String s1) throws TechnicalException {
        return target.findByApplicationAndApi(s, s1);
    }

    public Optional<ApiKey> retrieve(String s) throws TechnicalException {
        return target.retrieve(s);
    }

    public ApiKey update(ApiKey apiKey) throws TechnicalException {
        return target.update(apiKey);
    }
}
