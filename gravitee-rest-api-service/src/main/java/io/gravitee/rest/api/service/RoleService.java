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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.NewRoleEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.Permission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface RoleService {
    RoleEntity              create                      (NewRoleEntity role);
    void                    createOrUpdateSystemRoles   (String organizationId);
    void                    delete                      (String roleId);
    List<RoleEntity>        findAll                     ();
    RoleEntity              findById                    (String roleId);
    List<RoleEntity>        findByScope                 (RoleScope scope);
    Optional<RoleEntity>    findByScopeAndName          (RoleScope scope, String name);
    List<RoleEntity>        findDefaultRoleByScopes     (RoleScope... scopes);
    boolean                 hasPermission               (Map<String, char[]> userPermissions, Permission permission, RolePermissionAction[] acls);
    void                    initialize                  (String organizationId);
    RoleEntity              update                      (UpdateRoleEntity role);
}
