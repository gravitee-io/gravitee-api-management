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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.NewRoleEntity;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.UpdateRoleEntity;
import io.gravitee.management.model.permissions.*;
import io.gravitee.management.service.RoleService;
import io.gravitee.management.service.exceptions.RoleAlreadyExistsException;
import io.gravitee.management.service.exceptions.RoleNotFoundException;
import io.gravitee.management.service.exceptions.RoleReservedNameException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleScope;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.gravitee.management.model.permissions.RolePermissionAction.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RoleServiceImpl extends TransactionalService implements RoleService {

    private final Logger LOGGER = LoggerFactory.getLogger(RoleServiceImpl.class);

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public RoleEntity findById(final RoleScope scope, final String name) {
        try {
            LOGGER.debug("Find Role by id");

            Optional<Role> role = roleRepository.findById(scope, name);
            if (!role.isPresent()) {
                throw new RoleNotFoundException(scope, name);
            }
            return convert(role.get());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find a role : {} {}", scope, name,  ex);
            throw new TechnicalManagementException("An error occurs while trying to find a role : " + scope + " " + name, ex);
        }
    }

    @Override
    public List<RoleEntity> findAll() {
        try {
            LOGGER.debug("Find all Roles");
            return roleRepository.findAll()
                    .stream()
                    .map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all roles", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all roles", ex);
        }
    }

    @Override
    public RoleEntity create(final NewRoleEntity roleEntity) {
        try {
            Role role = convert(roleEntity);
            if (roleRepository.findById(role.getScope(), role.getName()).isPresent()) {
                throw new RoleAlreadyExistsException(role.getScope(), role.getName());
            }
            RoleEntity entity = convert(roleRepository.create(role));
            if (entity.isDefaultRole()) {
                toggleDefaultRole(convert(roleEntity.getScope()), entity.getName());
            }
            return entity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create role {}", roleEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to create role " + roleEntity.getName(), ex);
        }
    }

    @Override
    public void createSystemRoles() {
        try {
            Date now = new Date();
            Role systemRole;

            //MANAGEMENT - ADMIN
            systemRole = createSystemRoleWithoutPermissions(SystemRole.ADMIN.name(), RoleScope.MANAGEMENT, now);
            Map<String, char[]> perms = new HashMap<>();
            for (ManagementPermission managementPermission : ManagementPermission.values()) {
                perms.put(managementPermission.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            }
            systemRole.setPermissions(convertPermissions(io.gravitee.management.model.permissions.RoleScope.MANAGEMENT , perms));
            roleRepository.create(systemRole);

            //PORTAL - ADMIN
            systemRole = createSystemRoleWithoutPermissions(SystemRole.ADMIN.name(), RoleScope.PORTAL, now);
            perms.clear();
            for (PortalPermission portalPermission : PortalPermission.values()) {
                perms.put(portalPermission.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            }
            systemRole.setPermissions(convertPermissions(io.gravitee.management.model.permissions.RoleScope.PORTAL , perms));
            roleRepository.create(systemRole);

            //API - PRIMARY_OWNER
            systemRole = createSystemRoleWithoutPermissions(SystemRole.PRIMARY_OWNER.name(), RoleScope.API, now);
            perms.clear();
            for (ApiPermission apiPermission : ApiPermission.values()) {
                perms.put(apiPermission.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            }
            systemRole.setPermissions(convertPermissions(io.gravitee.management.model.permissions.RoleScope.API , perms));
            roleRepository.create(systemRole);

            //APPLICATION - PRIMARY_OWNER
            systemRole = createSystemRoleWithoutPermissions(SystemRole.PRIMARY_OWNER.name(), RoleScope.APPLICATION, now);
            perms.clear();
            for (ApplicationPermission applicationPermission : ApplicationPermission.values()) {
                perms.put(applicationPermission.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
            }
            systemRole.setPermissions(convertPermissions(io.gravitee.management.model.permissions.RoleScope.APPLICATION , perms));
            roleRepository.create(systemRole);

        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create admin roles", ex);
            throw new TechnicalManagementException("An error occurs while trying to create admin roles ", ex);
        }
    }

    @Override
    public RoleEntity update(final UpdateRoleEntity roleEntity) {
        if (isReserved(roleEntity.getName())) {
            throw new RoleReservedNameException(SystemRole.ADMIN.name());
        }
        RoleScope scope = convert(roleEntity.getScope());
        try {
            Optional<Role> role = roleRepository.findById(scope, roleEntity.getName());
            if (!role.isPresent()) {
                throw new RoleNotFoundException(scope, roleEntity.getName());
            }
            RoleEntity entity = convert(roleRepository.update(convert(roleEntity)));
            if (entity.isDefaultRole()) {
                toggleDefaultRole(scope, entity.getName());
            }
            return entity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update role {}", roleEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to update role " + roleEntity.getName(), ex);
        }
    }

    @Override
    public void delete(final RoleScope scope, final String name) {
        if (isReserved(name)) {
            throw new RoleReservedNameException(SystemRole.ADMIN.name());
        }
        try {
            Optional<Role> role = roleRepository.findById(scope, name);
            if (!role.isPresent()) {
                throw new RoleNotFoundException(scope, name);
            }
            roleRepository.delete(scope, name);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete role {}/{}", scope, name, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete role " + scope + "/" + name, ex);
        }
    }

    @Override
    public List<RoleEntity> findByScope(RoleScope scope) {
        try {
            LOGGER.debug("Find Roles by scope");
            return roleRepository.findByScope(scope)
                    .stream()
                    .map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find roles by scope", ex);
            throw new TechnicalManagementException("An error occurs while trying to find roles by scope", ex);
        }
    }

    @Override
    public List<RoleEntity> findDefaultRoleByScopes(RoleScope... scopes) {
        try {
            LOGGER.debug("Find default Roles by scope");
            List<RoleEntity> roles = new ArrayList<>();
            for (RoleScope scope : scopes) {
                roles.addAll(
                        roleRepository.findByScope(scope).
                                stream().
                                filter(Role::isDefaultRole).
                                map(this::convert).
                                collect(Collectors.toList())
                );
            }
            return roles;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find default roles by scope", ex);
            throw new TechnicalManagementException("An error occurs while trying to find default roles by scope", ex);
        }
    }

    @Override
    public boolean hasPermission(Map<String, char[]> userPermissions, Permission permission, RolePermissionAction[] acls) {
        boolean hasPermission = false;
        if (userPermissions != null) {
            Iterator<Map.Entry<String, char[]>> it = userPermissions.entrySet().iterator();
            while (it.hasNext() && !hasPermission) {
                Map.Entry<String, char[]> entry = it.next();
                if (permission.getName().equals(entry.getKey())) {
                    String crud = Arrays.toString(entry.getValue());
                    for (RolePermissionAction perm : acls) {
                        if (crud.indexOf(perm.getId()) != -1) {
                            hasPermission = true;
                        }
                    }
                }
            }
        }
        return hasPermission;
    }

    private void toggleDefaultRole(RoleScope scope, String newDefaultRoleName) throws TechnicalException {
        List<Role> roles = roleRepository.findByScope(scope).
                stream().
                filter(Role::isDefaultRole).
                collect(Collectors.toList());
        for (Role role : roles) {
            if(!role.getName().equals(newDefaultRoleName)) {
                role.setDefaultRole(false);
                role.setUpdatedAt(new Date());
                roleRepository.update(role);
            }
        }
    }

    private Role convert(final NewRoleEntity roleEntity) {
        final Role role = new Role();
        role.setName(generateId(roleEntity.getName()));
        role.setDescription(roleEntity.getDescription());
        role.setScope(convert(roleEntity.getScope()));
        role.setDefaultRole(roleEntity.isDefaultRole());
        role.setPermissions(convertPermissions(roleEntity.getScope(), roleEntity.getPermissions()));
        role.setCreatedAt(new Date());
        role.setUpdatedAt(role.getCreatedAt());
        return role;
    }

    private Role convert(final UpdateRoleEntity roleEntity) {
        if (roleEntity == null) {
            return null;
        }
        final Role role = new Role();
        role.setName(generateId(roleEntity.getName()));
        role.setDescription(roleEntity.getDescription());
        role.setScope(convert(roleEntity.getScope()));
        role.setDefaultRole(roleEntity.isDefaultRole());
        role.setPermissions(convertPermissions(roleEntity.getScope(), roleEntity.getPermissions()));
        role.setUpdatedAt(new Date());
        return role;
    }

    private RoleEntity convert(final Role role) {
        if (role == null) {
            return null;
        }
        final RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName(role.getName());
        roleEntity.setDescription(role.getDescription());
        roleEntity.setScope(convert(role.getScope()));
        roleEntity.setDefaultRole(role.isDefaultRole());
        roleEntity.setSystem(role.isSystem());
        roleEntity.setPermissions(convertPermissions(roleEntity.getScope(), role.getPermissions()));
        return roleEntity;
    }

    private int[] convertPermissions(io.gravitee.management.model.permissions.RoleScope scope, Map<String, char[]> perms) {
        if (perms == null || perms.isEmpty()) {
            return new int[0];
        }
        int[] result = new int[perms.size()];
        int idx=0;
        for (Map.Entry<String, char[]> entry : perms.entrySet()) {

            int perm = 0;
            for (char c : entry.getValue()) {
                perm += RolePermissionAction.findById(c).getMask();
            }
            result[idx++] = Permission.findByScopeAndName(scope, entry.getKey()).getMask() + perm;
        }
        return result;
    }

    private Map<String, char[]> convertPermissions(io.gravitee.management.model.permissions.RoleScope scope, int[] perms) {
        if (perms == null) {
            return Collections.emptyMap();
        }
        Map<String, char[]> result = new HashMap<>();
        Stream.of(Permission.findByScope(scope)).forEach(perm -> {
            for (int action : perms) {
                if (action / 100 == perm.getMask() / 100) {
                    List<Character> crud = new ArrayList<>();
                    for (RolePermissionAction rolePermissionAction : RolePermissionAction.values()) {
                        if (((action - perm.getMask()) & rolePermissionAction.getMask()) != 0) {
                            crud.add(rolePermissionAction.getId());
                        }
                    }
                    result.put(perm.getName(), ArrayUtils.toPrimitive(crud.toArray(new Character[crud.size()])));
                }
            }
        });
        return result;
    }

    private RoleScope convert(io.gravitee.management.model.permissions.RoleScope scope) {
        if (scope== null) {
            return null;
        }
        return RoleScope.valueOf(scope.name());
    }
    private io.gravitee.management.model.permissions.RoleScope convert(RoleScope scope) {
        if (scope== null) {
            return null;
        }
        return io.gravitee.management.model.permissions.RoleScope.valueOf(scope.name());
    }

    private String generateId(String name) {
        String id = name
                .trim()
                .toUpperCase()
                .replaceAll(" +", " ")
                .replaceAll(" ", "_")
                .replaceAll("[^\\w\\s]","_")
                .replaceAll("-+", "_");
        if (isReserved(id)) {
            throw new RoleReservedNameException(id);
        }
        return id;
    }

    private boolean isReserved(String name) {
        for (SystemRole systemRole : SystemRole.values()) {
            if (systemRole.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private Role createSystemRoleWithoutPermissions(String name, RoleScope scope, Date date) {
        LOGGER.info("      - <" + scope + "> " + name + " (system)");
        Role systemRole = new Role();
        systemRole.setName(name);
        systemRole.setDescription("System Role. Created by Gravitee.io");
        systemRole.setDefaultRole(false);
        systemRole.setSystem(true);
        systemRole.setScope(scope);
        systemRole.setCreatedAt(date);
        systemRole.setUpdatedAt(date);
        return systemRole;
    }
}
