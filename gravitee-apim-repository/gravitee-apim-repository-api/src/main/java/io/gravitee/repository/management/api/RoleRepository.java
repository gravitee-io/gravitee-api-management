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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import java.util.Optional;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface RoleRepository extends FindAllRepository<Role> {
    Optional<Role> findById(String roleId) throws TechnicalException;

    Set<Role> findAllById(Set<String> ids) throws TechnicalException;

    Role create(Role role) throws TechnicalException;

    Role update(Role role) throws TechnicalException;

    void delete(String roleId) throws TechnicalException;

    /**
     * find all roles by scope id and scope name for a given reference
     * @param scope the scope id (Application, API, Group, Environment, Organization, Platform)
     * @param name the scope name
     * @param referenceId the id of the reference of the role
     * @param referenceType the type of the reference of the role. Can be ORGANIZATION or ENVIRONMENT.
     * @return an Optional of Role
     * @throws TechnicalException if something wrong happen
     */
    Optional<Role> findByScopeAndNameAndReferenceIdAndReferenceType(
        RoleScope scope,
        String name,
        String referenceId,
        RoleReferenceType referenceType
    ) throws TechnicalException;

    /**
     * find all roles by scope id for a given reference
     * @param scope the scope id (Application, API, Portal or Management)
     * @param referenceId the id of the reference of the role
     * @param referenceType the type of the reference of the role. Can be ORGANIZATION or ENVIRONMENT.
     * @return list of roles
     * @throws TechnicalException if something wrong happen
     */
    Set<Role> findByScopeAndReferenceIdAndReferenceType(RoleScope scope, String referenceId, RoleReferenceType referenceType)
        throws TechnicalException;

    Set<Role> findAllByReferenceIdAndReferenceType(String referenceId, RoleReferenceType referenceType) throws TechnicalException;

    Optional<Role> findByIdAndReferenceIdAndReferenceType(String roleId, String referenceId, RoleReferenceType referenceType)
        throws TechnicalException;
}
