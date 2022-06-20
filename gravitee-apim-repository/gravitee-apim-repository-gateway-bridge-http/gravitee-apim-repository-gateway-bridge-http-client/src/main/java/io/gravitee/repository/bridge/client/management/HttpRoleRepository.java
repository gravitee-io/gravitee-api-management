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

import io.gravitee.repository.bridge.client.utils.ExcludeMethodFromGeneratedCoverage;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpRoleRepository extends AbstractRepository implements RoleRepository {

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Optional<Role> findById(String roleId) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Role create(Role role) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Role update(Role role) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public void delete(String roleId) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Set<Role> findAll() throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Role> findByScopeAndReferenceIdAndReferenceType(RoleScope scope, String referenceId, RoleReferenceType referenceType)
        throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    @ExcludeMethodFromGeneratedCoverage
    public Set<Role> findAllByReferenceIdAndReferenceType(String referenceId, RoleReferenceType referenceType) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Role> findByScopeAndNameAndReferenceIdAndReferenceType(
        RoleScope scope,
        String name,
        String referenceId,
        RoleReferenceType referenceType
    ) throws TechnicalException {
        throw new IllegalStateException();
    }
}
