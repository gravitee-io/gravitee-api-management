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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.repository.mongodb.management.internal.model.RoleMongo;
import io.gravitee.repository.mongodb.management.internal.role.RoleMongoRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    public Optional<Role> findById(String roleId) throws TechnicalException {
        LOGGER.debug("Find role by ID [{}]", roleId);

        final RoleMongo role = internalRoleRepo.findById(roleId).orElse(null);

        LOGGER.debug("Find role by ID [{}] - Done", roleId);
        return Optional.ofNullable(map(role));
    }

    @Override
    public Set<Role> findAllById(Set<String> ids) throws TechnicalException {
        LOGGER.debug("Find roles by IDs [{}]", ids);

        final Iterable<RoleMongo> rolesIterable = internalRoleRepo.findAllById(ids);

        LOGGER.debug("Find roles by IDs [{}] - Done", ids);

        return StreamSupport.stream(rolesIterable.spliterator(), false).map(this::map).collect(Collectors.toSet());
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
        if (role == null || role.getId() == null || role.getScope() == null || role.getName() == null) {
            throw new IllegalStateException("Role to update must not be null");
        }
        final String id = role.getId();
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
    public void delete(String roleId) throws TechnicalException {
        try {
            internalRoleRepo.deleteById(roleId);
        } catch (Exception e) {
            LOGGER.error("An error occurred when deleting role [{}]", roleId, e);
            throw new TechnicalException("An error occurred when deleting role");
        }
    }

    @Override
    public Optional<Role> findByScopeAndNameAndReferenceIdAndReferenceType(
        RoleScope scope,
        String name,
        String referenceId,
        RoleReferenceType referenceType
    ) throws TechnicalException {
        LOGGER.debug("Find role by scope and name [{}, {}, {}, {}]", scope, name, referenceId, referenceType);

        final Optional<Role> role = internalRoleRepo
            .findByScopeAndNameAndReferenceIdAndReferenceType(scope.name(), name, referenceId, referenceType.name())
            .stream()
            .map(this::map)
            .findFirst();

        LOGGER.debug("Find role by scope and name [{}, {}, {}, {}] - Done", scope, name, referenceId, referenceType);
        return role;
    }

    @Override
    public Set<Role> findAll() throws TechnicalException {
        final List<RoleMongo> roles = internalRoleRepo.findAll();
        return roles.stream().map(this::map).collect(Collectors.toSet());
    }

    private Role map(RoleMongo roleMongo) {
        if (roleMongo == null) {
            return null;
        }

        Role role = new Role();
        role.setId(roleMongo.getId());
        role.setScope(RoleScope.valueOf(roleMongo.getScope()));
        role.setName(roleMongo.getName());
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
        roleMongo.setId(role.getId());
        roleMongo.setScope(role.getScope().name());
        roleMongo.setName(role.getName());
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
    public Set<Role> findAllByReferenceIdAndReferenceType(String referenceId, RoleReferenceType referenceType) {
        final List<RoleMongo> roles = internalRoleRepo.findByReferenceIdAndReferenceType(referenceId, referenceType.name());
        return roles.stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public Optional<Role> findByIdAndReferenceIdAndReferenceType(String roleId, String referenceId, RoleReferenceType referenceType) {
        return Optional.ofNullable(map(internalRoleRepo.findByIdAndReferenceIdAndReferenceType(roleId, referenceId, referenceType.name())));
    }

    @Override
    public Set<Role> findByScopeAndReferenceIdAndReferenceType(RoleScope scope, String referenceId, RoleReferenceType referenceType) {
        LOGGER.debug("Find role by scope and ref [{}, {}, {}]", scope, referenceId, referenceType);

        final Set<RoleMongo> roles = internalRoleRepo.findByScopeAndReferenceIdAndReferenceType(
            scope.name(),
            referenceId,
            referenceType.name()
        );

        LOGGER.debug("Find role by scope [{}, {}, {}] - Done", scope, referenceId, referenceType);
        return roles.stream().map(this::map).collect(Collectors.toSet());
    }
}
