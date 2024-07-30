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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.NewRoleEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.Permission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface RoleService {
    RoleEntity create(ExecutionContext executionContext, final NewRoleEntity roleEntity);
    void createOrUpdateSystemRole(
        ExecutionContext executionContext,
        SystemRole roleName,
        RoleScope roleScope,
        Permission[] permissions,
        String organizationId
    );
    void createOrUpdateSystemRoles(ExecutionContext executionContext, String organizationId);
    void delete(ExecutionContext executionContext, String roleId);
    List<RoleEntity> findAllByOrganization(String organizationId);
    RoleEntity findById(String roleId);
    Set<RoleEntity> findAllById(Set<String> roleIds);
    Optional<RoleEntity> findByIdAndOrganizationId(String roleId, String organizationId);
    List<RoleEntity> findByScope(RoleScope scope, String organizationId);
    Optional<RoleEntity> findByScopeAndName(RoleScope scope, String name, String organizationId);
    List<RoleEntity> findDefaultRoleByScopes(String organizationId, RoleScope... scopes);
    boolean hasPermission(Map<String, char[]> userPermissions, Permission permission, RolePermissionAction[] acls);
    void initialize(ExecutionContext executionContext, String organizationId);
    RoleEntity update(ExecutionContext executionContext, UpdateRoleEntity role);
    RoleScope findScopeByMembershipReferenceType(MembershipReferenceType type);
    RoleEntity findPrimaryOwnerRoleByOrganization(String organizationId, RoleScope roleScope);
}
