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
package io.gravitee.rest.api.service.impl;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.rest.api.model.NewRoleEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.*;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.exceptions.*;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

import static io.gravitee.repository.management.model.Audit.AuditProperties.ROLE;
import static io.gravitee.repository.management.model.Role.AuditEvent.*;
import static io.gravitee.rest.api.model.permissions.ApiPermission.REVIEWS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.*;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RoleServiceImpl extends AbstractService implements RoleService {

    private final Logger LOGGER = LoggerFactory.getLogger(RoleServiceImpl.class);

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private AuditService auditService;

    @Override
    public RoleEntity findById(final String roleId) {
        try {
            LOGGER.debug("Find Role by id");

            Optional<Role> role = roleRepository.findById(roleId);
            if (!role.isPresent()) {
                throw new RoleNotFoundException(roleId);
            }
            return convert(role.get());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find a role : {}", roleId,  ex);
            throw new TechnicalManagementException("An error occurs while trying to find a role : " + roleId, ex);
        }
    }

    @Override
    public List<RoleEntity> findAll() {
        return this.findAllByOrganization(GraviteeContext.getCurrentOrganization());
    }
    
    private List<RoleEntity> findAllByOrganization(String organizationId) {
        try {
            LOGGER.debug("Find all Roles");
            return roleRepository.findAllByReferenceIdAndReferenceType(organizationId, RoleReferenceType.ORGANIZATION)
                    .stream()
                    .map(this::convert).collect(toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all roles", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all roles", ex);
        }
    }

    @Override
    public RoleEntity create(final NewRoleEntity roleEntity) {
        return this.create(roleEntity, GraviteeContext.getCurrentOrganization());
    }
    
    private RoleEntity create(final NewRoleEntity roleEntity, String organizationId) {
        try {
            Role role = convert(roleEntity);
            if (roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(role.getScope(), role.getName(), organizationId, RoleReferenceType.ORGANIZATION).isPresent()) {
                throw new RoleAlreadyExistsException(role.getScope(), role.getName());
            }
            role.setId(RandomString.generate());
            role.setCreatedAt(new Date());
            role.setUpdatedAt(role.getCreatedAt());
            role.setReferenceId(organizationId);
            role.setReferenceType(RoleReferenceType.ORGANIZATION);
            
            RoleEntity entity = convert(roleRepository.create(role));
            auditService.createPortalAuditLog(
                    Collections.singletonMap(ROLE, role.getScope() + ":" + role.getName()),
                    ROLE_CREATED,
                    role.getCreatedAt(),
                    null,
                    role);
            if (entity.isDefaultRole()) {
                toggleDefaultRole(roleEntity.getScope(), entity.getName());
            }
            return entity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create role {}", roleEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to create role " + roleEntity.getName(), ex);
        }
    }

    @Override
    public RoleEntity update(final UpdateRoleEntity roleEntity) {
        if (isReserved(roleEntity.getName())) {
            throw new RoleReservedNameException(roleEntity.getName());
        }
        RoleScope scope = roleEntity.getScope();
        try {
            Optional<Role> optRole = roleRepository.findById(roleEntity.getId());
            if (!optRole.isPresent()) {
                throw new RoleNotFoundException(roleEntity.getId());
            }
            Role role = optRole.get();
            Role updatedRole = convert(roleEntity);
            updatedRole.setCreatedAt(role.getCreatedAt());
            RoleEntity entity = convert(roleRepository.update(updatedRole));
            auditService.createPortalAuditLog(
                    Collections.singletonMap(ROLE, role.getScope()+":"+role.getName()),
                    ROLE_UPDATED,
                    updatedRole.getUpdatedAt(),
                    role,
                    updatedRole);
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
    public void delete(final String roleId) {
        try {
            Optional<Role> optRole = roleRepository.findById(roleId);
            if (!optRole.isPresent()) {
                throw new RoleNotFoundException(roleId);
            }
            Role role = optRole.get();
            RoleScope scope = convert(role.getScope());
            if (role.isDefaultRole() || role.isSystem()) {
                throw new RoleDeletionForbiddenException(scope, role.getName());
            }

            List<RoleEntity> defaultRoleByScopes = findDefaultRoleByScopes(scope);
            if (defaultRoleByScopes.isEmpty()) {
                throw new DefaultRoleNotFoundException();
            }
            membershipService.removeRoleUsage(roleId, defaultRoleByScopes.get(0).getId());

            roleRepository.delete(roleId);

            auditService.createPortalAuditLog(
                    Collections.singletonMap(ROLE, scope+":"+role.getName()),
                    ROLE_DELETED,
                    role.getUpdatedAt(),
                    role,
                    null);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete role {}", roleId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete role " + roleId, ex);
        }
    }

    @Override
    public List<RoleEntity> findByScope(RoleScope scope) {
        try {
            LOGGER.debug("Find Roles by scope");
            return roleRepository.findByScopeAndReferenceIdAndReferenceType(convert(scope), GraviteeContext.getCurrentOrganization(), RoleReferenceType.ORGANIZATION).stream()
                    .map(this::convert)
                    .sorted(comparing(RoleEntity::getName))
                    .collect(toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find roles by scope", ex);
            throw new TechnicalManagementException("An error occurs while trying to find roles by scope", ex);
        }
    }

    @Override
    public Optional<RoleEntity> findByScopeAndName(RoleScope scope, String name) {
        try {
            LOGGER.debug("Find Roles by scope and name");
            
            Optional<Role> optRole = roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(convert(scope), name, GraviteeContext.getCurrentOrganization(), RoleReferenceType.ORGANIZATION);
            if (optRole.isPresent()) {
                return Optional.of(this.convert(optRole.get()));
            } else {
                return Optional.empty();
            }
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
                        roleRepository.findByScopeAndReferenceIdAndReferenceType(convert(scope), GraviteeContext.getCurrentOrganization(), RoleReferenceType.ORGANIZATION).
                                stream().
                                filter(Role::isDefaultRole).
                                map(this::convert).
                                collect(toList())
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
        List<Role> roles = roleRepository.findByScopeAndReferenceIdAndReferenceType(convert(scope), GraviteeContext.getCurrentOrganization(), RoleReferenceType.ORGANIZATION).
                stream().
                filter(Role::isDefaultRole).
                collect(toList());
        for (Role role : roles) {
            if(!role.getName().equals(newDefaultRoleName)) {
                Role previousRole = new Role(role);
                role.setDefaultRole(false);
                role.setUpdatedAt(new Date());
                roleRepository.update(role);
                auditService.createPortalAuditLog(
                        Collections.singletonMap(ROLE, role.getScope()+":"+role.getName()),
                        ROLE_UPDATED,
                        role.getUpdatedAt(),
                        previousRole,
                        role);
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
        role.setId(roleEntity.getId());
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
        roleEntity.setId(role.getId());
        roleEntity.setName(role.getName());
        roleEntity.setDescription(role.getDescription());
        roleEntity.setScope(convert(role.getScope()));
        roleEntity.setDefaultRole(role.isDefaultRole());
        roleEntity.setSystem(role.isSystem());
        roleEntity.setPermissions(convertPermissions(roleEntity.getScope(), role.getPermissions()));
        return roleEntity;
    }

    private int[] convertPermissions(io.gravitee.rest.api.model.permissions.RoleScope scope, Map<String, char[]> perms) {
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

    private Map<String, char[]> convertPermissions(io.gravitee.rest.api.model.permissions.RoleScope scope, int[] perms) {
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

    private io.gravitee.repository.management.model.RoleScope convert(RoleScope scope) {
        if (scope== null) {
            return null;
        }
        return io.gravitee.repository.management.model.RoleScope.valueOf(scope.name());
    }
    private RoleScope convert(io.gravitee.repository.management.model.RoleScope scope) {
        if (scope== null) {
            return null;
        }
        return RoleScope.valueOf(scope.name());
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

    @Override
    public void initialize(String organizationId) {
        LOGGER.info("     - <ORGANIZATION> USER (default)");
        this.create(DEFAULT_ROLE_ORGANIZATION_USER, organizationId);

        LOGGER.info("     - <ENVIRONMENT> API_PUBLISHER");
        this.create(ROLE_ENVIRONMENT_API_PUBLISHER, organizationId);

        LOGGER.info("     - <ENVIRONMENT> USER (default)");
        this.create(DEFAULT_ROLE_ENVIRONMENT_USER, organizationId);

        LOGGER.info("     - <API> USER (default)");
        this.create(DEFAULT_ROLE_API_USER, organizationId);

        LOGGER.info("     - <API> OWNER");
        this.create(ROLE_API_OWNER, organizationId);

        LOGGER.info("     - <API> REVIEWER");
        this.create(ROLE_API_REVIEWER, organizationId);

        LOGGER.info("     - <APPLICATION> USER (default)");
        this.create(DEFAULT_ROLE_APPLICATION_USER, organizationId);

        LOGGER.info("     - <APPLICATION> OWNER");
        this.create(ROLE_APPLICATION_OWNER, organizationId);
    }

    @Override
    public void createOrUpdateSystemRoles(String organizationId) {
        try {
            //ORGANIZATION - ADMIN
            createOrUpdateSystemRole(SystemRole.ADMIN, RoleScope.ORGANIZATION, OrganizationPermission.values(), organizationId);
            //ENVIRONMENT - ADMIN
            createOrUpdateSystemRole(SystemRole.ADMIN, RoleScope.ENVIRONMENT, EnvironmentPermission.values(), organizationId);
            //API - PRIMARY_OWNER
            createOrUpdateSystemRole(SystemRole.PRIMARY_OWNER, RoleScope.API, stream(ApiPermission.values()).filter(permission -> !REVIEWS.equals(permission)).toArray(Permission[]::new), organizationId);
            //APPLICATION - PRIMARY_OWNER
            createOrUpdateSystemRole(SystemRole.PRIMARY_OWNER, RoleScope.APPLICATION, ApplicationPermission.values(), organizationId);
            //GROUP - ADMINISTRATOR
            createOrUpdateSystemRole(SystemRole.ADMIN, RoleScope.GROUP, GroupPermission.values(), organizationId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create admin roles", ex);
            throw new TechnicalManagementException("An error occurs while trying to create admin roles ", ex);
        }
    }

    private void createOrUpdateSystemRole(SystemRole roleName, RoleScope roleScope, Permission[] permissions, String organizationId) throws TechnicalException {
        Role systemRole = createSystemRoleWithoutPermissions(roleName.name(), roleScope, new Date());
        Map<String, char[]> perms = new HashMap<>();
        for (Permission perm : permissions) {
            perms.put(perm.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()});
        }
        systemRole.setPermissions(convertPermissions(roleScope , perms));

        systemRole.setReferenceId(organizationId);
        systemRole.setReferenceType(RoleReferenceType.ORGANIZATION);
        
        Optional<Role> existingRole = roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(systemRole.getScope(), systemRole.getName(), GraviteeContext.getCurrentOrganization(), RoleReferenceType.ORGANIZATION);
        if (existingRole.isPresent() && permissionsAreDifferent(existingRole.get(), systemRole)) {
            systemRole.setId(existingRole.get().getId());
            systemRole.setUpdatedAt(new Date());
            roleRepository.update(systemRole);
            auditService.createPortalAuditLog(
                    Collections.singletonMap(ROLE, systemRole.getScope() + ":" + systemRole.getName()),
                    ROLE_UPDATED,
                    systemRole.getCreatedAt(),
                    existingRole,
                    systemRole);
        } else if (!existingRole.isPresent()) {
            roleRepository.create(systemRole);
            auditService.createPortalAuditLog(
                    Collections.singletonMap(ROLE, systemRole.getScope() + ":" + systemRole.getName()),
                    ROLE_CREATED,
                    systemRole.getCreatedAt(),
                    null,
                    systemRole);
        }
    }

    private Role createSystemRoleWithoutPermissions(String name, RoleScope scope, Date date) {
        LOGGER.info("      - <" + scope + "> " + name + " (system)");
        Role systemRole = new Role();
        systemRole.setId(RandomString.generate());
        systemRole.setName(name);
        systemRole.setDescription("System Role. Created by Gravitee.io");
        systemRole.setDefaultRole(false);
        systemRole.setSystem(true);
        systemRole.setScope(convert(scope));
        systemRole.setCreatedAt(date);
        systemRole.setUpdatedAt(date);
        return systemRole;
    }

    private boolean permissionsAreDifferent(Role role1, Role role2) {
        return stream(role1.getPermissions()).reduce(Math::addExact).orElse(0) !=
                stream(role2.getPermissions()).reduce(Math::addExact).orElse(0);
    }
}
