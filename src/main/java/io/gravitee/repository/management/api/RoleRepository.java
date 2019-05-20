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
package io.gravitee.repository.management.api;

import java.util.Optional;
import java.util.Set;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface RoleRepository {

    Optional<Role> findById(RoleScope scope, String name) throws TechnicalException;

    Role create(Role role) throws TechnicalException;

    Role update(Role role) throws TechnicalException;

    void delete(RoleScope scope, String name) throws TechnicalException;
    /**
     * find all roles by scope id
     * @param scope the scope id (Application, API, Portal or Management)
     * @return list of roles
     * @throws TechnicalException if something wrong happen
     */
    Set<Role> findByScope(RoleScope scope) throws TechnicalException;

    /**
     * find all roles by scope id
     * @param scope the scope id (Application, API, Portal or Management)
     * @param referenceId
     * @param referenceType
     * @return list of roles
     * @throws TechnicalException if something wrong happen
     */
    Set<Role> findByScopeAndReferenceIdAndReferenceType(RoleScope scope, String referenceId, RoleReferenceType referenceType) throws TechnicalException;
    
    /**
     * @return get all roles
     * @throws TechnicalException if something wrong happen
     */
    Set<Role> findAll() throws TechnicalException;
    
    Set<Role> findAllByReferenceIdAndReferenceType(String referenceId, RoleReferenceType referenceType) throws TechnicalException;
}
