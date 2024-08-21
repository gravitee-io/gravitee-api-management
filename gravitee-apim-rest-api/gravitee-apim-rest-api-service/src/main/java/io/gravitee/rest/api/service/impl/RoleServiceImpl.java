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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.ROLE;
import static io.gravitee.repository.management.model.Role.AuditEvent.*;
import static io.gravitee.rest.api.model.permissions.ApiPermission.REVIEWS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.*;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.NewRoleEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.*;
import io.gravitee.rest.api.model.settings.ConsoleConfigEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RoleServiceImpl extends AbstractService implements RoleService {

    private final List<Map.Entry<String, NewRoleEntity>> initializeRoles = List.of(
        Map.entry("<ORGANIZATION> USER (default)", DEFAULT_ROLE_ORGANIZATION_USER),
        Map.entry("<ENVIRONMENT> API_PUBLISHER", ROLE_ENVIRONMENT_API_PUBLISHER),
        Map.entry("<ENVIRONMENT> USER (default)", DEFAULT_ROLE_ENVIRONMENT_USER),
        Map.entry("<API> USER (default)", DEFAULT_ROLE_API_USER),
        Map.entry("<API> OWNER", ROLE_API_OWNER),
        Map.entry("<API> REVIEWER", ROLE_API_REVIEWER),
        Map.entry("<APPLICATION> USER (default)", DEFAULT_ROLE_APPLICATION_USER),
        Map.entry("<APPLICATION> OWNER", ROLE_APPLICATION_OWNER),
        Map.entry("<INTEGRATION> OWNER", ROLE_INTEGRATION_OWNER),
        Map.entry("<INTEGRATION> USER", ROLE_INTEGRATION_USER)
    );

    private final Logger LOGGER = LoggerFactory.getLogger(RoleServiceImpl.class);

    @Lazy
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ConfigService configService;

    private record PrimaryOwner(String organisation, RoleScope roleScope) {}

    private final Map<PrimaryOwner, RoleEntity> primaryOwnersByOrganization = new ConcurrentHashMap<>();

    @Override
    public RoleEntity findById(final String roleId) {
        return GraviteeContext
            .getCurrentRoles()
            .computeIfAbsent(
                roleId,
                k -> {
                    try {
                        LOGGER.debug("Find Role by id");

                        return roleRepository.findById(k).map(this::convert).orElseThrow(() -> new RoleNotFoundException(k));
                    } catch (TechnicalException ex) {
                        LOGGER.error("An error occurs while trying to find a role : {}", k, ex);
                        throw new TechnicalManagementException("An error occurs while trying to find a role : " + k, ex);
                    }
                }
            );
    }

    @Override
    public Optional<RoleEntity> findByIdAndOrganizationId(String roleId, String organizationId) {
        try {
            return roleRepository
                .findByIdAndReferenceIdAndReferenceType(roleId, organizationId, RoleReferenceType.ORGANIZATION)
                .map(this::convert);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find roles by id and organizationId", ex);
            throw new TechnicalManagementException("An error occurs while trying to find roles by id and organizationId", ex);
        }
    }

    @Override
    public List<RoleEntity> findAllByOrganization(String organizationId) {
        try {
            LOGGER.debug("Find all Roles");
            return roleRepository
                .findAllByReferenceIdAndReferenceType(organizationId, RoleReferenceType.ORGANIZATION)
                .stream()
                .map(this::convert)
                .collect(toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all roles", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all roles", ex);
        }
    }

    @Override
    public RoleEntity create(ExecutionContext executionContext, final NewRoleEntity roleEntity) {
        try {
            Role role = convert(executionContext, roleEntity);
            if (
                roleRepository
                    .findByScopeAndNameAndReferenceIdAndReferenceType(
                        role.getScope(),
                        role.getName(),
                        executionContext.getOrganizationId(),
                        RoleReferenceType.ORGANIZATION
                    )
                    .isPresent()
            ) {
                throw new RoleAlreadyExistsException(role.getScope(), role.getName());
            }
            role.setId(UuidString.generateRandom());
            role.setCreatedAt(new Date());
            role.setUpdatedAt(role.getCreatedAt());
            role.setReferenceId(executionContext.getOrganizationId());
            role.setReferenceType(RoleReferenceType.ORGANIZATION);

            RoleEntity entity = convert(roleRepository.create(role));
            auditService.createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                Collections.singletonMap(ROLE, role.getScope() + ":" + role.getName()),
                ROLE_CREATED,
                role.getCreatedAt(),
                null,
                role
            );
            if (entity != null && entity.isDefaultRole()) {
                toggleDefaultRole(executionContext, roleEntity.getScope(), entity.getName());
            }
            return entity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create role {}", roleEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to create role " + roleEntity.getName(), ex);
        }
    }

    @Override
    public RoleEntity update(final ExecutionContext executionContext, final UpdateRoleEntity roleEntity) {
        if (isReserved(executionContext, roleEntity.getScope(), roleEntity.getName())) {
            throw new RoleReservedNameException(roleEntity.getName());
        }
        RoleScope scope = roleEntity.getScope();
        try {
            Role role = roleRepository
                .findById(roleEntity.getId())
                .filter(r ->
                    (
                        r.getReferenceType() == RoleReferenceType.ENVIRONMENT &&
                        r.getReferenceId().equalsIgnoreCase(executionContext.getEnvironmentId())
                    ) ||
                    (
                        r.getReferenceType() == RoleReferenceType.ORGANIZATION &&
                        r.getReferenceId().equalsIgnoreCase(executionContext.getOrganizationId())
                    )
                )
                .orElseThrow(() -> new RoleNotFoundException(roleEntity.getId()));
            Role updatedRole = convert(executionContext, roleEntity);
            updatedRole.setCreatedAt(role.getCreatedAt());
            updatedRole.setReferenceId(role.getReferenceId());
            updatedRole.setReferenceType(role.getReferenceType());
            RoleEntity entity = convert(roleRepository.update(updatedRole));
            auditService.createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                Collections.singletonMap(ROLE, role.getScope() + ":" + role.getName()),
                ROLE_UPDATED,
                updatedRole.getUpdatedAt(),
                role,
                updatedRole
            );
            if (entity != null && entity.isDefaultRole()) {
                toggleDefaultRole(executionContext, scope, entity.getName());
            }
            return entity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update role {}", roleEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to update role " + roleEntity.getName(), ex);
        }
    }

    @Override
    public void delete(final ExecutionContext executionContext, final String roleId) {
        try {
            Role role = roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
            RoleScope scope = convert(role.getScope());
            if (role.isDefaultRole() || role.isSystem()) {
                throw new RoleDeletionForbiddenException(scope, role.getName());
            }

            List<RoleEntity> defaultRoleByScopes = findDefaultRoleByScopes(executionContext.getOrganizationId(), scope);
            if (defaultRoleByScopes.isEmpty()) {
                throw new DefaultRoleNotFoundException();
            }
            membershipService.removeRoleUsage(roleId, defaultRoleByScopes.get(0).getId());

            roleRepository.delete(roleId);

            auditService.createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                Collections.singletonMap(ROLE, scope + ":" + role.getName()),
                ROLE_DELETED,
                role.getUpdatedAt(),
                role,
                null
            );
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete role {}", roleId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete role " + roleId, ex);
        }
    }

    @Override
    public List<RoleEntity> findByScope(RoleScope scope, final String organizationId) {
        try {
            LOGGER.debug("Find Roles by scope");
            return roleRepository
                .findByScopeAndReferenceIdAndReferenceType(convert(scope), organizationId, RoleReferenceType.ORGANIZATION)
                .stream()
                .map(this::convert)
                .sorted(comparing(RoleEntity::getName))
                .collect(toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find roles by scope", ex);
            throw new TechnicalManagementException("An error occurs while trying to find roles by scope", ex);
        }
    }

    @Override
    public Optional<RoleEntity> findByScopeAndName(RoleScope scope, String name, String organizationId) {
        try {
            LOGGER.debug("Find Roles by scope and name");

            return roleRepository
                .findByScopeAndNameAndReferenceIdAndReferenceType(convert(scope), name, organizationId, RoleReferenceType.ORGANIZATION)
                .map(this::convert);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find roles by scope", ex);
            throw new TechnicalManagementException("An error occurs while trying to find roles by scope", ex);
        }
    }

    @Override
    public List<RoleEntity> findDefaultRoleByScopes(final String organizationId, RoleScope... scopes) {
        try {
            LOGGER.debug("Find default Roles by scope");
            List<RoleEntity> roles = new ArrayList<>();
            for (RoleScope scope : scopes) {
                roles.addAll(
                    roleRepository
                        .findByScopeAndReferenceIdAndReferenceType(convert(scope), organizationId, RoleReferenceType.ORGANIZATION)
                        .stream()
                        .filter(Role::isDefaultRole)
                        .map(this::convert)
                        .toList()
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

    private void toggleDefaultRole(ExecutionContext executionContext, RoleScope scope, String newDefaultRoleName)
        throws TechnicalException {
        List<Role> roles = roleRepository
            .findByScopeAndReferenceIdAndReferenceType(convert(scope), executionContext.getOrganizationId(), RoleReferenceType.ORGANIZATION)
            .stream()
            .filter(Role::isDefaultRole)
            .toList();
        for (Role role : roles) {
            if (!role.getName().equals(newDefaultRoleName)) {
                Role previousRole = new Role(role);
                role.setDefaultRole(false);
                role.setUpdatedAt(new Date());
                roleRepository.update(role);
                auditService.createOrganizationAuditLog(
                    executionContext,
                    executionContext.getOrganizationId(),
                    Collections.singletonMap(ROLE, role.getScope() + ":" + role.getName()),
                    ROLE_UPDATED,
                    role.getUpdatedAt(),
                    previousRole,
                    role
                );
            }
        }
    }

    private Role convert(final ExecutionContext executionContext, final NewRoleEntity roleEntity) {
        final Role role = new Role();
        role.setName(generateId(executionContext, roleEntity.getScope(), roleEntity.getName()));
        role.setDescription(roleEntity.getDescription());
        role.setScope(convert(roleEntity.getScope()));
        role.setDefaultRole(roleEntity.isDefaultRole());
        role.setPermissions(convertPermissions(roleEntity.getScope(), roleEntity.getPermissions()));
        role.setCreatedAt(new Date());
        role.setUpdatedAt(role.getCreatedAt());
        return role;
    }

    private Role convert(final ExecutionContext executionContext, final UpdateRoleEntity roleEntity) {
        if (roleEntity == null) {
            return null;
        }
        final Role role = new Role();
        role.setName(generateId(executionContext, roleEntity.getScope(), roleEntity.getName()));
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
        int idx = 0;
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
        Stream
            .of(Permission.findByScope(scope))
            .forEach(perm -> {
                for (int action : perms) {
                    if (action / 100 == perm.getMask() / 100) {
                        List<Character> crud = new ArrayList<>();
                        for (RolePermissionAction rolePermissionAction : RolePermissionAction.values()) {
                            if (((action - perm.getMask()) & rolePermissionAction.getMask()) != 0) {
                                crud.add(rolePermissionAction.getId());
                            }
                        }
                        result.put(perm.getName(), ArrayUtils.toPrimitive(crud.toArray(new Character[0])));
                    }
                }
            });
        return result;
    }

    private io.gravitee.repository.management.model.RoleScope convert(RoleScope scope) {
        return scope == null ? null : io.gravitee.repository.management.model.RoleScope.valueOf(scope.name());
    }

    private RoleScope convert(io.gravitee.repository.management.model.RoleScope scope) {
        return scope == null ? null : RoleScope.valueOf(scope.name());
    }

    private String generateId(ExecutionContext executionContext, RoleScope scope, String name) {
        String id = name.trim().toUpperCase().replaceAll(" +", " ").replaceAll(" ", "_").replaceAll("[^\\w\\s]", "_").replaceAll("-+", "_");
        if (isReserved(executionContext, scope, id)) {
            throw new RoleReservedNameException(id);
        }
        return id;
    }

    private boolean isReserved(ExecutionContext executionContext, RoleScope scope, String name) {
        ConsoleConfigEntity config = configService.getConsoleConfig(executionContext);
        if (config.getManagement().getSystemRoleEdition().isEnabled()) {
            return scope == RoleScope.ORGANIZATION && SystemRole.ADMIN.name().equals(name);
        }
        return isSystemRole(name);
    }

    private boolean isSystemRole(String roleName) {
        for (SystemRole systemRole : SystemRole.values()) {
            if (systemRole.name().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void initialize(ExecutionContext executionContext, String organizationId) {
        initializeRoles.forEach(entry -> {
            LOGGER.info("     - {}", entry.getKey());
            create(executionContext, entry.getValue());
        });
    }

    @Override
    public void createOrUpdateSystemRoles(ExecutionContext executionContext, String organizationId) {
        try {
            //ORGANIZATION - ADMIN
            createOrUpdateSystemRole(
                executionContext,
                SystemRole.ADMIN,
                RoleScope.ORGANIZATION,
                OrganizationPermission.values(),
                organizationId
            );
            //ENVIRONMENT - ADMIN
            createOrUpdateSystemRole(
                executionContext,
                SystemRole.ADMIN,
                RoleScope.ENVIRONMENT,
                EnvironmentPermission.values(),
                organizationId
            );
            //API - PRIMARY_OWNER
            createOrUpdateSystemRole(
                executionContext,
                SystemRole.PRIMARY_OWNER,
                RoleScope.API,
                stream(ApiPermission.values()).filter(permission -> !REVIEWS.equals(permission)).toArray(Permission[]::new),
                organizationId
            );
            //APPLICATION - PRIMARY_OWNER
            createOrUpdateSystemRole(
                executionContext,
                SystemRole.PRIMARY_OWNER,
                RoleScope.APPLICATION,
                ApplicationPermission.values(),
                organizationId
            );
            //INTEGRATION - PRIMARY_OWNER
            createOrUpdateSystemRole(
                executionContext,
                SystemRole.PRIMARY_OWNER,
                RoleScope.INTEGRATION,
                IntegrationPermission.values(),
                organizationId
            );
            //GROUP - ADMINISTRATOR
            createOrUpdateSystemRole(executionContext, SystemRole.ADMIN, RoleScope.GROUP, GroupPermission.values(), organizationId);
        } catch (TechnicalManagementException ex) {
            LOGGER.error("An error occurs while trying to create admin roles", ex);
            throw new TechnicalManagementException("An error occurs while trying to create admin roles ", ex);
        }
    }

    @Override
    public RoleScope findScopeByMembershipReferenceType(MembershipReferenceType type) {
        return type.findScope();
    }

    @Override
    public RoleEntity findPrimaryOwnerRoleByOrganization(String organizationId, RoleScope roleScope) {
        return this.primaryOwnersByOrganization.computeIfAbsent(
                new PrimaryOwner(organizationId, roleScope),
                s -> findByScopeAndName(s.roleScope(), SystemRole.PRIMARY_OWNER.name(), s.organisation()).orElse(null)
            );
    }

    public void createOrUpdateSystemRole(
        ExecutionContext executionContext,
        SystemRole roleName,
        RoleScope roleScope,
        Permission[] permissions,
        String organizationId
    ) {
        try {
            Role systemRole = createSystemRoleWithoutPermissions(roleName.name(), roleScope, new Date());
            Map<String, char[]> perms = new HashMap<>();
            for (Permission perm : permissions) {
                perms.put(perm.getName(), new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() });
            }
            systemRole.setPermissions(convertPermissions(roleScope, perms));

            systemRole.setReferenceId(organizationId);
            systemRole.setReferenceType(RoleReferenceType.ORGANIZATION);

            Optional<Role> existingRole = roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                systemRole.getScope(),
                systemRole.getName(),
                organizationId,
                RoleReferenceType.ORGANIZATION
            );
            if (existingRole.isPresent() && permissionsAreDifferent(existingRole.get(), systemRole)) {
                systemRole.setId(existingRole.get().getId());
                systemRole.setUpdatedAt(new Date());
                roleRepository.update(systemRole);
                auditService.createOrganizationAuditLog(
                    executionContext,
                    organizationId,
                    Collections.singletonMap(ROLE, systemRole.getScope() + ":" + systemRole.getName()),
                    ROLE_UPDATED,
                    systemRole.getCreatedAt(),
                    existingRole,
                    systemRole
                );
            } else if (existingRole.isEmpty()) {
                roleRepository.create(systemRole);
                auditService.createOrganizationAuditLog(
                    executionContext,
                    organizationId,
                    Collections.singletonMap(ROLE, systemRole.getScope() + ":" + systemRole.getName()),
                    ROLE_CREATED,
                    systemRole.getCreatedAt(),
                    null,
                    systemRole
                );
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} {} roles", roleName.name(), roleScope.name(), ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to create " + roleName.name() + " " + roleScope.name() + " roles ",
                ex
            );
        }
    }

    private Role createSystemRoleWithoutPermissions(String name, RoleScope scope, Date date) {
        LOGGER.info("      - <" + scope + "> " + name + " (system)");
        Role systemRole = new Role();
        systemRole.setId(UuidString.generateRandom());
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
        return (
            stream(role1.getPermissions()).reduce(Math::addExact).orElse(0) !=
            stream(role2.getPermissions()).reduce(Math::addExact).orElse(0)
        );
    }
}
