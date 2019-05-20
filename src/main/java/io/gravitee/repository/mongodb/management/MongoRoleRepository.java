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
package io.gravitee.repository.mongodb.management;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.repository.mongodb.management.internal.model.RoleMongo;
import io.gravitee.repository.mongodb.management.internal.model.RolePkMongo;
import io.gravitee.repository.mongodb.management.internal.role.RoleMongoRepository;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoRoleRepository implements RoleRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoRoleRepository.class);

    @Autowired
    private RoleMongoRepository internalRoleRepo;

    @Override
    public Optional<Role> findById(RoleScope scope, String name) throws TechnicalException {
        LOGGER.debug("Find role by ID [{}, {}]", scope, name);

        final RoleMongo role = internalRoleRepo.findById(new RolePkMongo(scope.getId(), name)).orElse(null);

        LOGGER.debug("Find role by ID [{}, {}] - Done", scope, name);
        return Optional.ofNullable(map(role));
    }

    @Override
    public Role create(Role role) throws TechnicalException {
        LOGGER.debug("Create role [{}]", role.getName());

        RoleMongo roleMongo = map(role);
        RoleMongo createdRoleMongo = internalRoleRepo.insert(roleMongo);

        Role res = map(createdRoleMongo);

        LOGGER.debug("Create role [{}] - Done", role.getName());

        return res;
    }

    @Override
    public Role update(Role role) throws TechnicalException {
        final RolePkMongo id = convert(role);
        final RoleMongo roleMongo = internalRoleRepo.findById(id).orElse(null);

        if (roleMongo == null) {
            throw new IllegalStateException(String.format("No role found with id [%s]", id));
        }

        try {
            roleMongo.setDescription(role.getDescription());
            roleMongo.setDefaultRole(role.isDefaultRole());
            roleMongo.setPermissions(role.getPermissions());

            RoleMongo roleMongoUpdated = internalRoleRepo.save(roleMongo);
            return map(roleMongoUpdated);

        } catch (Exception e) {

            LOGGER.error("An error occured when updating role", e);
            throw new TechnicalException("An error occured when updating role");
        }
    }

    @Override
    public void delete(RoleScope scope, String name) throws TechnicalException {
        try {
            internalRoleRepo.deleteById(new RolePkMongo(scope.getId(), name));
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting role [{}, {}]", scope, name, e);
            throw new TechnicalException("An error occured when deleting role");
        }
    }

    @Override
    public Set<Role> findByScope(RoleScope scope) throws TechnicalException {
        LOGGER.debug("Find role by scope [{}]", scope);

        final Set<RoleMongo> roles = internalRoleRepo.findByScope(scope.getId());

        LOGGER.debug("Find role by scope [{}] - Done", scope);
        return roles.stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public Set<Role> findAll() throws TechnicalException {
        final List<RoleMongo> roles = internalRoleRepo.findAll();
        return roles.stream()
                .map(this::map)
                .collect(Collectors.toSet());
    }

    private RolePkMongo convert(Role role) {
        if (role == null || role.getScope() == null || role.getName() == null) {
            throw new IllegalStateException("Role to update must not be null");
        }
        return new RolePkMongo(role.getScope().getId(), role.getName());
    }

    private Role map(RoleMongo roleMongo) {
        if (roleMongo == null) {
            return null;
        }

        Role role = new Role();
        role.setScope(RoleScope.valueOf(roleMongo.getId().getScope()));
        role.setName(roleMongo.getId().getName());
        role.setReferenceId(roleMongo.getReferenceId());
        role.setReferenceType(RoleReferenceType.valueOf(roleMongo.getReferenceType()));
        role.setDescription(roleMongo.getDescription());
        role.setDefaultRole(roleMongo.isDefaultRole());
        role.setSystem(roleMongo.isSystem());
        role.setPermissions(roleMongo.getPermissions());
        role.setUpdatedAt(roleMongo.getUpdatedAt());
        role.setCreatedAt(roleMongo.getCreatedAt());
        return role;
    }

    private RoleMongo map(Role role) {
        if (role == null) {
            return null;
        }

        RoleMongo roleMongo = new RoleMongo();
        roleMongo.setId(convert(role));
        roleMongo.setReferenceId(role.getReferenceId());
        roleMongo.setReferenceType(role.getReferenceType().name());
        roleMongo.setDescription(role.getDescription());
        roleMongo.setDefaultRole(role.isDefaultRole());
        roleMongo.setSystem(role.isSystem());
        roleMongo.setPermissions(role.getPermissions());
        roleMongo.setUpdatedAt(role.getUpdatedAt());
        roleMongo.setCreatedAt(role.getCreatedAt());
        return roleMongo;
    }

    @Override
    public Set<Role> findAllByReferenceIdAndReferenceType(String referenceId, RoleReferenceType referenceType)
            throws TechnicalException {
        final List<RoleMongo> roles = internalRoleRepo.findByReferenceIdAndReferenceType(referenceId, referenceType.name());
        return roles.stream()
                .map(this::map)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Role> findByScopeAndReferenceIdAndReferenceType(RoleScope scope, String referenceId,
            RoleReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("Find role by scope and ref [{}, {}, {}]", scope, referenceId, referenceType);

        final Set<RoleMongo> roles = internalRoleRepo.findByScopeAndReferenceIdAndReferenceType(scope.getId(), referenceId, referenceType.name());

        LOGGER.debug("Find role by scope [{}, {}, {}] - Done", scope, referenceId, referenceType);
        return roles.stream().map(this::map).collect(Collectors.toSet());
    }
}
