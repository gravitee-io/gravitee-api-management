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
import io.gravitee.repository.management.api.CrudRepository;

import java.util.Optional;
import java.util.Set;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractNoOpManagementRepository<U, V> implements CrudRepository<U, V> {

    @Override
    public Optional<U> findById(V v) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public U create(U item) throws TechnicalException {
        return null;
    }

    @Override
    public U update(U item) throws TechnicalException {
        return null;
    }

    @Override
    public void delete(V v) throws TechnicalException {

    }

    @Override
    public Set<U> findAll() throws TechnicalException {
        return Set.of();
    }
}
