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
import io.gravitee.repository.management.api.TenantRepository;
import io.gravitee.repository.management.model.Tenant;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TenantRepositoryProxy extends AbstractProxy<TenantRepository> implements TenantRepository {

    @Override
    public Optional<Tenant> findById(String s) throws TechnicalException {
        return target.findById(s);
    }

    @Override
    public Tenant create(Tenant item) throws TechnicalException {
        return target.create(item);
    }

    @Override
    public Tenant update(Tenant item) throws TechnicalException {
        return target.update(item);
    }

    @Override
    public Set<Tenant> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }
}
